package org.telegram.messenger.fork.panels;
/** Existing Telegram glass is requested only when rendering capability and power state allow it. */
public final class PanelPolicy {
    private PanelPolicy() { }
    public static int popupRadiusDp(boolean experimental) { return experimental ? 26 : 12; }
    public static int popupRadiusDp(boolean experimental, int requested) { return experimental ? Math.max(12, Math.min(32, requested)) : 12; }
    public static int settingsRadiusDp() { return 18; }
    public static int settingsGapDp() { return 6; }
    public static int effectStrength(int value) { return Math.max(0, Math.min(100, value)); }
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
        if (("liquid".equals(requested) || "blur".equals(requested)) && useGlass(true, savingPower, sdk, capable)) return requested;
        return "material";
    }
    public static boolean captureMenuGlass(boolean enabled, boolean savingPower, int sdk, boolean capable, boolean hasFactory, boolean eligible) {
        return !hasFactory && eligible && useGlass(enabled, savingPower, sdk, capable);
    }
}
