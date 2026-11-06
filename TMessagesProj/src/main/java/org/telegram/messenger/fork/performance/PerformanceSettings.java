package org.telegram.messenger.fork.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

/**
 * BerryGram Performance Engine profile storage. Supports Performance Center 2.0
 * with automatic hardware scanning and explicit manual quality overrides.
 */
public final class PerformanceSettings {
    private static final String PREFS = "fork_performance";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_CONFIG_MODE = "config_mode";
    private static final String KEY_MANUAL_LEVEL = "manual_level";
    private static final String KEY_CUSTOM_REDUCE_EFFECTS = "custom_reduce_effects";

    private static volatile String cachedProfile;
    private static volatile String cachedConfigMode;
    private static volatile String cachedManualLevel;
    private static volatile Boolean cachedCustomReduce;
    private static volatile Boolean cachedLowRam;
    private static volatile long thermalCheckedAt;
    private static volatile boolean cachedThermalPressure;

    private PerformanceSettings() { }

    private static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String profile() {
        String value = cachedProfile;
        if (value == null) {
            try {
                value = preferences().getString(KEY_PROFILE, PerformancePolicy.AUTOMATIC);
            } catch (ClassCastException corrupt) {
                value = PerformancePolicy.AUTOMATIC;
            }
            cachedProfile = value = PerformancePolicy.normalize(value);
        }
        return value;
    }

    public static void setProfile(String profile) {
        String normalized = PerformancePolicy.normalize(profile);
        preferences().edit().putString(KEY_PROFILE, normalized).apply();
        cachedProfile = normalized;
    }

    public static String configMode() {
        String value = cachedConfigMode;
        if (value == null) {
            try {
                value = preferences().getString(KEY_CONFIG_MODE, PerformancePolicy.CONFIG_AUTO);
            } catch (ClassCastException corrupt) {
                value = PerformancePolicy.CONFIG_AUTO;
            }
            cachedConfigMode = value = PerformancePolicy.normalizeConfigMode(value);
        }
        return value;
    }

    public static void setConfigMode(String mode) {
        String normalized = PerformancePolicy.normalizeConfigMode(mode);
        preferences().edit().putString(KEY_CONFIG_MODE, normalized).apply();
        cachedConfigMode = normalized;
    }

    public static String manualLevel() {
        String value = cachedManualLevel;
        if (value == null) {
            try {
                value = preferences().getString(KEY_MANUAL_LEVEL, PerformancePolicy.LEVEL_MEDIUM);
            } catch (ClassCastException corrupt) {
                value = PerformancePolicy.LEVEL_MEDIUM;
            }
            cachedManualLevel = value = PerformancePolicy.normalizeManualLevel(value);
        }
        return value;
    }

    public static void setManualLevel(String level) {
        String normalized = PerformancePolicy.normalizeManualLevel(level);
        preferences().edit().putString(KEY_MANUAL_LEVEL, normalized).apply();
        cachedManualLevel = normalized;
    }

    public interface HardwareTierProvider {
        String getTier();
    }
    private static volatile HardwareTierProvider hardwareTierProvider;

    public static void setHardwareTierProvider(HardwareTierProvider provider) {
        hardwareTierProvider = provider;
    }

    public static String autoTier() {
        if (hardwareTierProvider != null) {
            return hardwareTierProvider.getTier();
        }
        try {
            Class<?> clazz = Class.forName("org.telegram.messenger.fork.performance.ForkDeviceAnalyzer");
            java.lang.reflect.Method m = clazz.getMethod("getAssessment", boolean.class);
            Object assessment = m.invoke(null, false);
            java.lang.reflect.Field f = assessment.getClass().getField("tier");
            return (String) f.get(assessment);
        } catch (Throwable ignored) {
            return DeviceAnalyzerPolicy.TIER_MEDIUM;
        }
    }

    public static String effectiveTier() {
        return PerformancePolicy.resolveActiveTier(configMode(), manualLevel(), autoTier());
    }

    public static boolean customReduceEffects() {
        Boolean value = cachedCustomReduce;
        if (value == null) {
            cachedCustomReduce = value = preferences().getBoolean(KEY_CUSTOM_REDUCE_EFFECTS, false);
        }
        return value;
    }

    public static void setCustomReduceEffects(boolean value) {
        preferences().edit().putBoolean(KEY_CUSTOM_REDUCE_EFFECTS, value).apply();
        cachedCustomReduce = value;
    }

    public static boolean systemPowerSaver() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return false;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isPowerSaveMode();
    }

    private static boolean lowRamDevice() {
        Boolean cached = cachedLowRam;
        if (cached == null) {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) return false;
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            cachedLowRam = cached = activityManager != null && activityManager.isLowRamDevice();
        }
        return cached;
    }

    /** Thermal status is sampled at most every 15 seconds; never polled per frame. */
    private static boolean thermalPressure() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        long now = System.currentTimeMillis();
        if (now - thermalCheckedAt < 15000) return cachedThermalPressure;
        thermalCheckedAt = now;
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return cachedThermalPressure = false;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power == null) return cachedThermalPressure = false;
        int status;
        try {
            status = power.getCurrentThermalStatus();
        } catch (RuntimeException unavailable) {
            return cachedThermalPressure = false;
        }
        return cachedThermalPressure = status >= PowerManager.THERMAL_STATUS_MODERATE;
    }

    public static boolean reduceEffects() {
        boolean pressure = systemPowerSaver() || thermalPressure();
        if (pressure) return true;

        if (PerformancePolicy.CONFIG_MANUAL.equals(configMode())) {
            return PerformancePolicy.LEVEL_LOW.equals(manualLevel());
        }

        String currentAutoTier = autoTier();
        boolean lowDevice = DeviceAnalyzerPolicy.TIER_LOW.equals(currentAutoTier)
                || DeviceAnalyzerPolicy.TIER_LOW_MEDIUM.equals(currentAutoTier)
                || SharedConfig.deviceIsLow()
                || lowRamDevice();

        return PerformancePolicy.reduceEffects(profile(), false, lowDevice, customReduceEffects());
    }

    public static void reset() {
        preferences().edit()
                .remove(KEY_PROFILE)
                .remove(KEY_CONFIG_MODE)
                .remove(KEY_MANUAL_LEVEL)
                .remove(KEY_CUSTOM_REDUCE_EFFECTS)
                .apply();
        cachedProfile = PerformancePolicy.AUTOMATIC;
        cachedConfigMode = PerformancePolicy.CONFIG_AUTO;
        cachedManualLevel = PerformancePolicy.LEVEL_MEDIUM;
        cachedCustomReduce = false;
    }

    public static String deviceClassLabel() {
        return autoTier();
    }
}
