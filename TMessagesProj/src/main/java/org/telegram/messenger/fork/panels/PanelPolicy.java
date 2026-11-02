package org.telegram.messenger.fork.panels;
/** Existing Telegram glass is requested only when rendering capability and power state allow it. */
public final class PanelPolicy {
    private PanelPolicy() { }
    public static int popupRadiusDp(boolean experimental) { return experimental ? 20 : 12; }
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
}
