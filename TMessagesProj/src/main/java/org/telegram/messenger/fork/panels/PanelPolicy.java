package org.telegram.messenger.fork.panels;

/**
 * Visual surface and graphics policy for Berrygram.
 * Coordinates Glass 2.0, Blur, and No Effects (none/material) modes.
 */
public final class PanelPolicy {
    public static final String STYLE_GLASS = "glass";
    public static final String STYLE_BLUR = "blur";
    public static final String STYLE_NONE = "none";

    private PanelPolicy() { }

    public static String normalizeStyle(String style) {
        if ("glass".equals(style) || "experimental".equals(style) || "liquid".equals(style)) {
            return STYLE_GLASS;
        }
        if ("blur".equals(style)) {
            return STYLE_BLUR;
        }
        return STYLE_NONE;
    }

    public static int popupRadiusDp(boolean experimental) { return experimental ? 26 : 12; }
    public static int popupRadiusDp(boolean experimental, int requested) { return experimental ? Math.max(12, Math.min(32, requested)) : 12; }

    public static int surfaceRadiusDp(String style, int requested) {
        return "material".equals(style) || "none".equals(style) || "blur".equals(style) || "glass".equals(style) || "liquid".equals(style) || "experimental".equals(style) ? popupRadiusDp(true, requested) : 12;
    }

    public static int settingsRadiusDp() { return 18; }
    public static int settingsGapDp() { return 6; }
    public static int effectStrength(int value) { return Math.max(0, Math.min(100, value)); }
    public static boolean graphicsCapable(boolean deviceCapable, boolean adaptive) { return deviceCapable || !adaptive; }

    public static int popupAnimationDuration(String mode, int base, boolean systemAnimations) {
        if (!systemAnimations || "minimal".equals(mode)) return 0;
        int bounded = Math.max(0, base);
        return "smooth".equals(mode) ? Math.min(300, bounded + 40) : bounded;
    }

    public static int clampPopupOffset(int anchorX, int offset, int popupWidth, int left, int right, int margin) {
        int min = left + margin;
        int max = Math.max(min, right - margin - popupWidth);
        return Math.max(min, Math.min(max, anchorX + offset)) - anchorX;
    }

    public static int popupAlpha(int alpha, boolean experimental) {
        int bounded = Math.max(0, Math.min(255, alpha));
        return experimental ? bounded * 240 / 255 : bounded;
    }

    public static boolean useGlass(boolean enabled, boolean savingPower, int sdk, boolean capable) {
        return enabled && !savingPower && sdk >= 33 && capable;
    }

    public static String effectiveStyle(String requested, boolean savingPower, int sdk, boolean capable) {
        if (("glass".equals(requested) || "liquid".equals(requested) || "blur".equals(requested) || "experimental".equals(requested))
                && useGlass(true, savingPower, sdk, capable)) {
            return requested;
        }
        return "material";
    }

    public static boolean captureMenuGlass(boolean enabled, boolean savingPower, int sdk, boolean capable, boolean hasFactory, boolean eligible) {
        return !hasFactory && eligible && useGlass(enabled, savingPower, sdk, capable);
    }
}
