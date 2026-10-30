package org.telegram.messenger.fork.panels;
import android.content.Context;
import org.telegram.messenger.ApplicationLoader;
/** One device-local experimental switch; it does not alter Telegram's own settings. */
public final class ForkPanelSettings {
    private static volatile Boolean cached;
    private ForkPanelSettings() { }
    public static boolean enabled() {
        Boolean value = cached;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cached;
            if (value == null) cached = value = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).getBoolean("enabled", false);
        }
        return value;
    }
    public static synchronized void setEnabled(boolean value) {
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit().putBoolean("enabled", value).apply();
        cached = value;
    }
}
