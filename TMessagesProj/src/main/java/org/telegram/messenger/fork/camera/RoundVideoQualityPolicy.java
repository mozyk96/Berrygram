package org.telegram.messenger.fork.camera;

/** Bitrate decisions use server defaults unless an explicit supported preference exists. */
public final class RoundVideoQualityPolicy {
    private RoundVideoQualityPolicy() {}
    public static int supported(int standard, int requested, int min, int max) {
        return min > 0 && max >= min && requested >= min && requested <= max ? requested : standard;
    }
    /** Conservatively reduces an automatic stream after a slow connection or a large, long recording. */
    public static int adapt(int current, int standard, long elapsedMs, long encodedBytes, boolean slowConnection) {
        if (current <= 0 || standard <= 0) return current;
        if (!slowConnection && (elapsedMs < 30000 || encodedBytes < 5000000L)) return current;
        int reduced = (int) Math.min(standard, Math.max(250000L, standard * 7L / 10L));
        return Math.min(current, reduced);
    }
    public static int bitrate(String mode, int standard, int custom, boolean lowDevice, boolean slowConnection) {
        if (standard <= 0) return standard;
        if ("custom".equals(mode) && custom >= 250000 && custom <= 8000000) return custom;
        if ("auto".equals(mode) && (lowDevice || slowConnection)) return (int) Math.min(standard, Math.max(250000L, standard * 7L / 10L));
        return standard;
    }
}
