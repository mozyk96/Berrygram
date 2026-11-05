package org.telegram.messenger.fork.panels;

/**
 * Pure calculations for Experimental Glass 2.0.
 *
 * <p>The renderer still uses Telegram's blur3 drawables, but transparency, blur,
 * diffusion, saturation, edges and shadows are now independent inputs. Keeping
 * the math here makes the settings testable without Android.</p>
 */
public final class GlassPolicy {
    public static final String PRESET_CLEAR = "clear";
    public static final String PRESET_FROSTED = "frosted";
    public static final String PRESET_BALANCED = "balanced";
    public static final String PRESET_DEEP = "deep";
    public static final String PRESET_CUSTOM = "custom";

    public static final int DEFAULT_TRANSPARENCY = 55;
    public static final int DEFAULT_BLUR = 55;
    public static final int DEFAULT_FROST = 40;
    public static final int DEFAULT_SATURATION = 110;
    public static final int DEFAULT_EDGE = 65;
    public static final int DEFAULT_HIGHLIGHT = 55;
    public static final int DEFAULT_SHADOW = 45;

    private GlassPolicy() { }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    public static String normalizePreset(String preset) {
        if (PRESET_CLEAR.equals(preset) || PRESET_FROSTED.equals(preset)
                || PRESET_BALANCED.equals(preset) || PRESET_DEEP.equals(preset)) {
            return preset;
        }
        return PRESET_CUSTOM;
    }

    /** Returns transparency, blur, frost, saturation, edge, highlight, shadow. */
    public static int[] presetValues(String preset) {
        switch (normalizePreset(preset)) {
            case PRESET_CLEAR:
                return new int[]{84, 32, 8, 128, 55, 70, 28};
            case PRESET_FROSTED:
                return new int[]{32, 86, 92, 78, 72, 62, 55};
            case PRESET_DEEP:
                return new int[]{16, 44, 26, 96, 82, 72, 78};
            case PRESET_BALANCED:
            default:
                return new int[]{DEFAULT_TRANSPARENCY, DEFAULT_BLUR, DEFAULT_FROST,
                        DEFAULT_SATURATION, DEFAULT_EDGE, DEFAULT_HIGHLIGHT, DEFAULT_SHADOW};
        }
    }

    /**
     * Surface tint opacity. 0% is almost opaque, 100% is the maximum safe
     * transparency. The lower bound never drops to zero so text remains legible.
     */
    public static float opacity(int transparency) {
        return 0.97f - 0.79f * clampPercent(transparency) / 100f;
    }

    /**
     * Blur amount for the one-shot popup backdrop capture. Frost adds real
     * diffusion on top of the base blur; low-end devices get a bounded fallback.
     */
    public static int captureBlurAmount(int blur, int frost, boolean adaptive, boolean lowDevice) {
        int amount = 8 + Math.round(18f * clampPercent(blur) / 100f)
                + Math.round(10f * clampPercent(frost) / 100f);
        if (adaptive && lowDevice) {
            amount = Math.max(8, Math.round(amount * 0.62f));
        }
        return clamp(amount, 6, 38);
    }

    /** Saturation applied to the captured backdrop before it is composited. */
    public static float saturation(int saturationPercent, int frost) {
        float base = clamp(saturationPercent, 30, 180) / 100f;
        float diffusion = 1f - 0.38f * clampPercent(frost) / 100f;
        return Math.max(0.18f, base * diffusion);
    }

    public static int strokeAlpha(int edge, int highlight, boolean top) {
        int bounded = clampPercent(edge);
        int accent = top ? clampPercent(highlight) : bounded;
        int alpha = Math.round(18f + 0.47f * bounded + 0.20f * accent);
        return clamp(alpha, 0, 255);
    }

    public static float strokeWidthDp(int edge) {
        return 0.45f + 0.85f * clampPercent(edge) / 100f;
    }

    public static int shadowAlpha(int depth) {
        return Math.round(6f + 0.62f * clampPercent(depth));
    }

    public static float shadowRadiusDp(int depth) {
        return 1.5f + 8.5f * clampPercent(depth) / 100f;
    }

    /** Corner radius uses the existing BerryGram appearance range. */
    public static int radiusDp(int requested) {
        return clamp(requested, 12, 32);
    }
}
