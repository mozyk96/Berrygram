package org.telegram.messenger.fork.panels;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Device-local appearance preference, independent of Telegram's tab configuration. */
public final class ForkDockSettings {
    public static final String KEY_MINI_DOCK = "mini_dock";

    private ForkDockSettings() { }

    public static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_dock", Context.MODE_PRIVATE);
    }

    public static boolean miniDockEnabled() {
        return preferences().getBoolean(KEY_MINI_DOCK, false);
    }

    public static void setMiniDockEnabled(boolean enabled) {
        preferences().edit().putBoolean(KEY_MINI_DOCK, enabled).apply();
    }
}
