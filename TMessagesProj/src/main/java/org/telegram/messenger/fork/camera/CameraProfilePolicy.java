package org.telegram.messenger.fork.camera;

/**
 * Pure adaptive-camera decisions. Only settings that the platform actually
 * exposes are selected; callers must still validate against the active camera.
 */
public final class CameraProfilePolicy {
    public static final String AUTO = "auto";
    public static final String QUALITY = "quality";
    public static final String BALANCED = "balanced";
    public static final String PERFORMANCE = "performance";
    public static final String CUSTOM = "custom";

    private CameraProfilePolicy() { }

    public static String normalize(String profile) {
        if (QUALITY.equals(profile) || BALANCED.equals(profile)
                || PERFORMANCE.equals(profile) || CUSTOM.equals(profile)) {
            return profile;
        }
        return AUTO;
    }

    public static float bitrateScale(String profile) {
        switch (normalize(profile)) {
            case QUALITY:
                return 1.25f;
            case PERFORMANCE:
                return 0.72f;
            case BALANCED:
            case AUTO:
            case CUSTOM:
            default:
                return 1f;
        }
    }

    public static int jpegQuality(String profile) {
        switch (normalize(profile)) {
            case QUALITY:
                return 100;
            case PERFORMANCE:
                return 90;
            case BALANCED:
                return 95;
            case AUTO:
            case CUSTOM:
            default:
                return 97;
        }
    }

    /** Caps the user's requested round-video frame rate; never raises it. */
    public static int targetFps(String profile, int requestedFps, boolean lowDevice) {
        int requested = requestedFps <= 0 ? 30 : requestedFps;
        switch (normalize(profile)) {
            case PERFORMANCE:
                return Math.min(requested, 30);
            case BALANCED:
                return lowDevice ? Math.min(requested, 30) : requested;
            case QUALITY:
            case AUTO:
            case CUSTOM:
            default:
                return requested;
        }
    }

    public static boolean limitRoundVideoTo30(String profile, boolean lowDevice) {
        return targetFps(profile, 60, lowDevice) <= 30;
    }

    /**
     * Preferred capture range for Camera2 video. Returns {lower, upper}.
     * A device that does not expose 60 fps falls back to 30.
     */
    public static int[] preferredFpsBounds(String profile, boolean supports60, boolean lowDevice) {
        if (!supports60) return new int[]{30, 30};
        switch (normalize(profile)) {
            case PERFORMANCE:
                return new int[]{30, 30};
            case BALANCED:
                return lowDevice ? new int[]{30, 30} : new int[]{30, 60};
            case QUALITY:
            case AUTO:
            case CUSTOM:
            default:
                return new int[]{30, 60};
        }
    }

    /**
     * Selects the closest advertised FPS range. Arrays must be the same length.
     * Returns -1 when no range is usable.
     */
    public static int selectFpsRange(int[] lowers, int[] uppers, int targetFps) {
        if (lowers == null || uppers == null || lowers.length == 0
                || lowers.length != uppers.length) {
            return -1;
        }
        int bestContaining = -1;
        int bestContainingLower = -1;
        int bestBelow = -1;
        int bestBelowUpper = -1;
        int bestOverall = -1;
        int bestOverallUpper = -1;
        for (int i = 0; i < lowers.length; i++) {
            int lower = lowers[i];
            int upper = uppers[i];
            if (lower <= targetFps && upper >= targetFps) {
                if (lower > bestContainingLower
                        || (lower == bestContainingLower && upper < uppers[bestContaining])) {
                    bestContaining = i;
                    bestContainingLower = lower;
                }
            } else if (upper <= targetFps) {
                if (upper > bestBelowUpper) {
                    bestBelow = i;
                    bestBelowUpper = upper;
                }
            }
            if (upper > bestOverallUpper) {
                bestOverall = i;
                bestOverallUpper = upper;
            }
        }
        if (bestContaining >= 0) return bestContaining;
        if (bestBelow >= 0) return bestBelow;
        return bestOverall;
    }

    /** Short-side cap for recording resolutions; 0 means no cap. */
    public static int resolutionShortSideCap(String profile, boolean lowDevice) {
        switch (normalize(profile)) {
            case PERFORMANCE:
                return 720;
            case BALANCED:
                return lowDevice ? 720 : 1080;
            case QUALITY:
            case AUTO:
            case CUSTOM:
            default:
                return 0;
        }
    }

    /** Stabilization is only requested when the active camera advertises it. */
    public static boolean stabilizationEnabled(String profile, boolean supported) {
        if (!supported) return false;
        return !PERFORMANCE.equals(normalize(profile));
    }
}
