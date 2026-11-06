package org.telegram.messenger.fork.panels;
import android.content.Context;
import org.telegram.messenger.ApplicationLoader;
/** One device-local experimental switch; it does not alter Telegram's own settings. */
public final class ForkPanelSettings {
    public static final String STYLE_GLASS = "glass";
    public static final String STYLE_BLUR = "blur";
    public static final String STYLE_NONE = "none";

    private static volatile Boolean cached;
    private static volatile Integer cachedRadius;
    private static volatile String cachedStyle;
    private static volatile Integer cachedStrength;
    private static volatile String cachedAnimation;
    private static volatile Boolean cachedAdaptive;
    private ForkPanelSettings() { }

    public static String visualMode() {
        String s = style();
        if ("glass".equals(s) || "liquid".equals(s) || "experimental".equals(s)) return STYLE_GLASS;
        if ("none".equals(s)) return STYLE_NONE;
        return STYLE_BLUR;
    }

    public static boolean enabled() { return true; }
    public static synchronized void setEnabled(boolean value) {
        setStyle("blur");
    }
    public static String style() {
        String value = cachedStyle;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cachedStyle;
            if (value == null) {
                android.content.SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE);
                value = prefs.getString("style", prefs.getBoolean("enabled", false) ? "blur" : "material");
                if (!"blur".equals(value) && !"liquid".equals(value) && !"experimental".equals(value) && !"glass".equals(value) && !"none".equals(value)) {
                    value = "blur";
                    prefs.edit().putString("style", value).putBoolean("enabled", true).apply();
                }
                cachedStyle = value;
            }
        }
        return value;
    }
    public static synchronized void setStyle(String value) {
        if ("material".equals(value)) value = "blur";
        if (!"blur".equals(value) && !"liquid".equals(value) && !"experimental".equals(value) && !"glass".equals(value) && !"none".equals(value)) throw new IllegalArgumentException("Invalid graphics style");
        boolean active = true;
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
    public static int effectStrength() {
        Integer value = cachedStrength;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cachedStrength;
            if (value == null) {
                String stored = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).getString("effect_strength", "50");
                try { value = Integer.parseInt(stored); } catch (NumberFormatException | NullPointerException ignored) { value = 50; }
                cachedStrength = value = PanelPolicy.effectStrength(value);
            }
        }
        return value;
    }
    public static synchronized void setEffectStrength(int requested) {
        int value = PanelPolicy.effectStrength(requested);
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit().putString("effect_strength", Integer.toString(value)).apply();
        cachedStrength = value;
    }
    public static String animationMode() {
        String value = cachedAnimation;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cachedAnimation;
            if (value == null) {
                value = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).getString("animation", "system");
                if (!"system".equals(value) && !"smooth".equals(value) && !"minimal".equals(value)) value = "system";
                cachedAnimation = value;
            }
        }
        return value;
    }
    public static synchronized void setAnimationMode(String value) {
        if (!"system".equals(value) && !"smooth".equals(value) && !"minimal".equals(value)) throw new IllegalArgumentException("Invalid animation mode");
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit().putString("animation", value).apply();
        cachedAnimation = value;
    }
    public static boolean adaptiveEffects() {
        Boolean value = cachedAdaptive;
        if (value == null) synchronized (ForkPanelSettings.class) {
            value = cachedAdaptive;
            if (value == null) cachedAdaptive = value = ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).getBoolean("adaptive", true);
        }
        return value;
    }
    public static synchronized void setAdaptiveEffects(boolean value) {
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE).edit().putBoolean("adaptive", value).apply();
        cachedAdaptive = value;
    }
    public static boolean deviceAllowsEffects(boolean deviceCapable) { return PanelPolicy.graphicsCapable(deviceCapable, adaptiveEffects()); }
}
