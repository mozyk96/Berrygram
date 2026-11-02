package org.telegram.messenger.fork.panels;
import android.content.Context;
import org.telegram.messenger.ApplicationLoader;
/** One device-local experimental switch; it does not alter Telegram's own settings. */
public final class ForkPanelSettings {
    private static volatile Boolean cached;
    private static volatile Integer cachedRadius;
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
    public static int radiusDp() {
        Integer value = cachedRadius;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cachedRadius;
            if (value == null) {
                String stored = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).getString("radius", "26");
                try { value = Integer.parseInt(stored); } catch (NumberFormatException | NullPointerException ignored) { value = 26; }
                cachedRadius = value = PanelPolicy.popupRadiusDp(true, value);
            }
        }
        return value;
    }
    public static synchronized void setRadiusDp(int requested) {
        int value = PanelPolicy.popupRadiusDp(true, requested);
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit().putString("radius", Integer.toString(value)).apply();
        cachedRadius = value;
    }
}
