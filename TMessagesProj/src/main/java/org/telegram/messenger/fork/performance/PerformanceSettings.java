package org.telegram.messenger.fork.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

/**
 * BerryGram Performance Engine profile storage. It reuses Telegram's existing
 * rendering/lightweight-mode decisions instead of introducing a second renderer.
 */
public final class PerformanceSettings {
    private static final String PREFS = "fork_performance";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_CUSTOM_REDUCE_EFFECTS = "custom_reduce_effects";
    private static volatile String cachedProfile;
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
        return PerformancePolicy.reduceEffects(profile(), pressure,
                SharedConfig.deviceIsLow() || lowRamDevice(), customReduceEffects());
    }

    public static void reset() {
        preferences().edit().remove(KEY_PROFILE).remove(KEY_CUSTOM_REDUCE_EFFECTS).apply();
        cachedProfile = PerformancePolicy.AUTOMATIC;
        cachedCustomReduce = false;
    }

    public static String deviceClassLabel() {
        int deviceClass = SharedConfig.getDevicePerformanceClass();
        if (deviceClass == SharedConfig.PERFORMANCE_CLASS_LOW) return "low";
        if (deviceClass == SharedConfig.PERFORMANCE_CLASS_HIGH) return "high";
        return "average";
    }
}
