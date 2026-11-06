package org.telegram.messenger.fork.performance;

/** Pure profile resolution and Performance Center 2.0 policy. No Android state, no preference mutation. */
public final class PerformancePolicy {
    public static final String AUTOMATIC = "automatic";
    public static final String SMOOTH = "smooth";
    public static final String BATTERY = "battery";
    public static final String CUSTOM = "custom";

    public static final String CONFIG_AUTO = "auto";
    public static final String CONFIG_MANUAL = "manual";

    public static final String LEVEL_LOW = "low";
    public static final String LEVEL_MEDIUM = "medium";
    public static final String LEVEL_HIGH = "high";

    private PerformancePolicy() { }

    public static String normalize(String profile) {
        if (SMOOTH.equals(profile) || BATTERY.equals(profile) || CUSTOM.equals(profile)) return profile;
        return AUTOMATIC;
    }

    public static String normalizeConfigMode(String mode) {
        if (CONFIG_MANUAL.equals(mode)) return CONFIG_MANUAL;
        return CONFIG_AUTO;
    }

    public static String normalizeManualLevel(String level) {
        if (LEVEL_LOW.equals(level) || LEVEL_HIGH.equals(level)) return level;
        return LEVEL_MEDIUM;
    }

    /**
     * Resolves the active performance tier based on user config mode and scanned hardware.
     */
    public static String resolveActiveTier(String configMode, String manualLevel, String autoTier) {
        if (CONFIG_MANUAL.equals(normalizeConfigMode(configMode))) {
            return normalizeManualLevel(manualLevel);
        }
        return autoTier != null ? autoTier : LEVEL_MEDIUM;
    }

    /**
     * True when BerryGram should fall back to opaque surfaces and skip nonessential
     * reveal animations. System power saving is handled by the caller as well.
     */
    public static boolean reduceEffects(String profile, boolean systemPowerSaver, boolean lowDevice, boolean customReduceEffects) {
        if (systemPowerSaver) return true;
        switch (normalize(profile)) {
            case BATTERY:
                return true;
            case AUTOMATIC:
                return lowDevice;
            case CUSTOM:
                return customReduceEffects;
            case SMOOTH:
            default:
                return false;
        }
    }
}
