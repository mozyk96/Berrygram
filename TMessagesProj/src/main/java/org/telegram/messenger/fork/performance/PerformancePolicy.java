package org.telegram.messenger.fork.performance;

/** Pure profile resolution. No Android state, no preference mutation. */
public final class PerformancePolicy {
    public static final String AUTOMATIC = "automatic";
    public static final String SMOOTH = "smooth";
    public static final String BATTERY = "battery";
    public static final String CUSTOM = "custom";

    private PerformancePolicy() { }

    public static String normalize(String profile) {
        if (SMOOTH.equals(profile) || BATTERY.equals(profile) || CUSTOM.equals(profile)) return profile;
        return AUTOMATIC;
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
