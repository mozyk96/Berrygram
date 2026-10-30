package org.telegram.messenger.fork.camera;
/** Maps a user's starting-camera preference without changing Telegram's default. */
public final class RoundCameraPolicy {
    private RoundCameraPolicy() {}
    public static boolean initial(String mode, boolean lastFront, boolean upstreamFront) {
        if ("front".equals(mode)) return true;
        if ("back".equals(mode)) return false;
        if ("last".equals(mode)) return lastFront;
        return upstreamFront;
    }
    public static boolean fps60Allowed(boolean front, boolean back, boolean encoder) {
        return front && back && encoder;
    }
    public static int previewFacing(boolean preferFront, boolean frontAvailable, boolean backAvailable) {
        if (preferFront && frontAvailable || !backAvailable && frontAvailable) return 1;
        return backAvailable ? 0 : -1;
    }
    public static boolean camera2Enabled(boolean hasPreference, boolean preference, boolean serverDefault) {
        return hasPreference ? preference : serverDefault;
    }
    public static int gestureChoice(float x, float y, int width, int height) {
        if (width <= 0 || height <= 0 || Float.isNaN(x) || Float.isNaN(y) || x < 0 || x >= width || y < 0 || y >= height) return -1;
        return x < width / 2f ? 0 : 1;
    }
    public static boolean explicit(String mode) {
        return "front".equals(mode) || "back".equals(mode) || "last".equals(mode) || "gesture".equals(mode);
    }
}
