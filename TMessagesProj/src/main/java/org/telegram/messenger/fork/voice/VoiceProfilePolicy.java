package org.telegram.messenger.fork.voice;

/**
 * Pure mapping from a voice profile to supported capture behavior.
 *
 * <p>Profiles never change the Telegram-compatible Ogg/Opus output format.
 * They select source, bitrate target and optional platform audio effects.</p>
 */
public final class VoiceProfilePolicy {
    public static final String STANDARD = "standard";
    public static final String AUTOMATIC = "automatic";
    public static final String CLEAR = "clear";
    public static final String NATURAL = "natural";
    public static final String CUSTOM = "custom";

    public static final int SOURCE_DEFAULT = 0;
    public static final int SOURCE_MIC = 1;
    public static final int SOURCE_CAMCORDER = 5;
    public static final int SOURCE_VOICE_RECOGNITION = 6;

    private VoiceProfilePolicy() { }

    public static String normalize(String profile) {
        if (AUTOMATIC.equals(profile) || CLEAR.equals(profile)
                || NATURAL.equals(profile) || CUSTOM.equals(profile)) {
            return profile;
        }
        return STANDARD;
    }

    public static int bitrate(String profile, int resolved) {
        int base = Math.max(16000, Math.min(64000, resolved));
        switch (normalize(profile)) {
            case CLEAR:
                return Math.min(64000, Math.max(base, 40000));
            case NATURAL:
                return Math.min(64000, Math.max(base, 32000));
            case AUTOMATIC:
            case STANDARD:
            case CUSTOM:
            default:
                return base;
        }
    }

    /**
     * System noise suppression is not combined with RNNoise, because two
     * suppressors in series degrade speech and add latency.
     */
    public static boolean systemNoiseSuppression(String profile, boolean customEnabled,
                                                 boolean supported, boolean rnnoiseActive) {
        if (!supported || rnnoiseActive) return false;
        switch (normalize(profile)) {
            case AUTOMATIC:
            case CLEAR:
                return true;
            case CUSTOM:
                return customEnabled;
            case NATURAL:
            case STANDARD:
            default:
                return false;
        }
    }

    public static boolean automaticGain(String profile, boolean customEnabled, boolean supported) {
        if (!supported) return false;
        switch (normalize(profile)) {
            case AUTOMATIC:
            case CLEAR:
                return true;
            case CUSTOM:
                return customEnabled;
            case NATURAL:
            case STANDARD:
            default:
                return false;
        }
    }

    public static boolean echoCancellation(String profile, boolean customEnabled, boolean supported) {
        if (!supported) return false;
        return CUSTOM.equals(normalize(profile)) && customEnabled;
    }

    public static int source(String profile, int customSource) {
        switch (normalize(profile)) {
            case CLEAR:
                return SOURCE_VOICE_RECOGNITION;
            case CUSTOM:
                return validSource(customSource) ? customSource : SOURCE_DEFAULT;
            case AUTOMATIC:
            case NATURAL:
            case STANDARD:
            default:
                return SOURCE_DEFAULT;
        }
    }

    public static boolean validSource(int source) {
        return source == SOURCE_DEFAULT || source == SOURCE_MIC
                || source == SOURCE_CAMCORDER || source == SOURCE_VOICE_RECOGNITION;
    }
}
