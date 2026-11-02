package org.telegram.ui;

import android.content.Context;
import android.os.Build;
import org.telegram.ui.ActionBar.Theme;

/** Theme-aware neutral surface, using system dynamic colors where available. */
public final class ForkGraphicsSurface {
    private ForkGraphicsSurface() { }
    public static int materialColor(Context context, int fallback) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                return context.getColor(Theme.isCurrentThemeDark() ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_50);
            } catch (RuntimeException ignored) { }
        }
        return fallback;
    }
    public static boolean systemPowerSaver() {
        Context context = org.telegram.messenger.ApplicationLoader.applicationContext;
        if (context == null) return false;
        android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isPowerSaveMode();
    }
}
