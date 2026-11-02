package org.telegram.messenger.fork.panels;
import android.content.Context;
import org.telegram.messenger.ApplicationLoader;
/** One device-local experimental switch; it does not alter Telegram's own settings. */
public final class ForkPanelSettings {
    private static volatile Boolean cached;
    private static volatile Integer cachedRadius;
    private static volatile String cachedStyle;
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
        setStyle(value ? "blur" : "material");
    }
    public static String style() {
        String value = cachedStyle;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cachedStyle;
            if (value == null) {
                android.content.SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE);
                value = prefs.getString("style", prefs.getBoolean("enabled", false) ? "blur" : "material");
                if (!"material".equals(value) && !"blur".equals(value) && !"liquid".equals(value)) value = "material";
                cachedStyle = value;
            }
        }
        return value;
    }
    public static synchronized void setStyle(String value) {
        if (!"material".equals(value) && !"blur".equals(value) && !"liquid".equals(value)) throw new IllegalArgumentException("Invalid graphics style");
        boolean active = !"material".equals(value);
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit().putString("style", value).putBoolean("enabled", active).apply();
        cached = active;
        cachedStyle = value;
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
    public static String customAccent() {
        String value = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).getString("accent", null);
        Integer color = GraphicsColorPolicy.parseHex(value);
        return color == null ? null : GraphicsColorPolicy.formatHex(color);
    }
    public static void setCustomAccent(String value) {
        android.content.SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit();
        if (value == null) editor.remove("accent");
        else {
            Integer color = GraphicsColorPolicy.parseHex(value);
            if (color == null) throw new IllegalArgumentException("Invalid accent color");
            editor.putString("accent", GraphicsColorPolicy.formatHex(color));
        }
        editor.apply();
    }
}
