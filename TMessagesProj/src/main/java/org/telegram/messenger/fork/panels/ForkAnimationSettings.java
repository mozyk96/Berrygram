package org.telegram.messenger.fork.panels;

import android.content.Context;
import org.telegram.messenger.ApplicationLoader;

/** Device-local opt-in; independent of the selected graphics surface. */
public final class ForkAnimationSettings {
    private ForkAnimationSettings() { }

    public static boolean fluidPopupsEnabled() {
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_animations", Context.MODE_PRIVATE)
                .getBoolean("fluid_popups", false);
    }

    public static void setFluidPopupsEnabled(boolean enabled) {
        ApplicationLoader.applicationContext.getSharedPreferences("fork_animations", Context.MODE_PRIVATE)
                .edit().putBoolean("fluid_popups", enabled).apply();
    }
}
