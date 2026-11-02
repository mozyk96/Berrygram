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
}
