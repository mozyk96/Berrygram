package org.telegram.ui;

import android.content.Context;

/** Theme-aware neutral surface support for BerryGram settings. */
public final class ForkGraphicsSurface {
    private ForkGraphicsSurface() { }

    /**
     * Kept for existing call sites. The old Android dynamic-neutral color produced
     * unrelated gray/green surfaces; BerryGram must stay on Telegram theme colors.
     */
    public static int materialColor(Context context, int fallback) {
        return fallback;
    }

    public static String effectiveStyle() {
        return org.telegram.messenger.fork.panels.PanelPolicy.effectiveStyle(
                org.telegram.messenger.fork.panels.ForkPanelSettings.style(),
                effectsSuppressed(),
                android.os.Build.VERSION.SDK_INT,
                org.telegram.messenger.fork.panels.ForkPanelSettings.deviceAllowsEffects(!org.telegram.messenger.SharedConfig.deviceIsLow()));
    }

    public static boolean effectsSuppressed() {
        return org.telegram.messenger.fork.performance.PerformanceSettings.reduceEffects()
                || systemPowerSaver() || org.telegram.messenger.LiteMode.isPowerSaverApplied();
    }

    /**
     * True when the selected mode is No Background. The chat header, the
     * composer island and the bottom tabs then draw no plate and no blur.
     * Popup menus stay on the Telegram material so they keep their contrast.
     */
    public static boolean backgroundless() {
        return org.telegram.messenger.fork.panels.PanelPolicy.isBackgroundless(effectiveStyle());
    }

    /** True when a blur-based surface may be drawn at all (Blur or Liquid Glass). */
    public static boolean blurSurfacesEnabled() {
        String style = effectiveStyle();
        return !backgroundless() && !"material".equals(style);
    }

    public static boolean systemPowerSaver() {
        Context context = org.telegram.messenger.ApplicationLoader.applicationContext;
        if (context == null) return false;
        android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isPowerSaveMode();
    }
}
