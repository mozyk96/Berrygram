package org.telegram.messenger.fork.panels;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Persisted Experimental Glass 2.0 controls.
 *
 * <p>All values are device-local. Existing installations are migrated from the
 * single legacy effect-strength value without resetting any other preference.</p>
 */
public final class ForkGlassSettings {
    private static final String PREFS = "fork_panels";
    private static final Object LOCK = new Object();

    private static volatile Config cached;

    private ForkGlassSettings() { }

    public static final class Config {
        public final String preset;
        public final int transparency;
        public final int blur;
        public final int frost;
        public final int saturation;
        public final int edge;
        public final int highlight;
        public final int shadow;
        public final String tintMode;
        public final String customTint;
        public final boolean adaptive;

        Config(String preset, int transparency, int blur, int frost, int saturation,
               int edge, int highlight, int shadow, String tintMode, String customTint,
               boolean adaptive) {
            this.preset = preset;
            this.transparency = transparency;
            this.blur = blur;
            this.frost = frost;
            this.saturation = saturation;
            this.edge = edge;
            this.highlight = highlight;
            this.shadow = shadow;
            this.tintMode = tintMode;
            this.customTint = customTint;
            this.adaptive = adaptive;
        }

        public boolean customTintEnabled() {
            return "custom".equals(tintMode) && GraphicsColorPolicy.parseHex(customTint) != null;
        }
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Config config() {
        Config value = cached;
        if (value != null) return value;
        synchronized (LOCK) {
            value = cached;
            if (value == null) {
                value = load();
                cached = value;
            }
        }
        return value;
    }

    private static Config load() {
        SharedPreferences preferences = prefs();
        if (!preferences.contains("glass_transparency")) {
            migrateLegacy(preferences);
        }
        String preset = GlassPolicy.normalizePreset(safeString(preferences,
                "glass_preset", GlassPolicy.PRESET_CUSTOM));
        int transparency = GlassPolicy.clampPercent(safeInt(preferences,
                "glass_transparency", GlassPolicy.DEFAULT_TRANSPARENCY));
        int blur = GlassPolicy.clampPercent(safeInt(preferences,
                "glass_blur", GlassPolicy.DEFAULT_BLUR));
        int frost = GlassPolicy.clampPercent(safeInt(preferences,
                "glass_frost", GlassPolicy.DEFAULT_FROST));
        int saturation = GlassPolicy.clamp(safeInt(preferences,
                "glass_saturation", GlassPolicy.DEFAULT_SATURATION), 30, 180);
        int edge = GlassPolicy.clampPercent(safeInt(preferences,
                "glass_edge", GlassPolicy.DEFAULT_EDGE));
        int highlight = GlassPolicy.clampPercent(safeInt(preferences,
                "glass_highlight", GlassPolicy.DEFAULT_HIGHLIGHT));
        int shadow = GlassPolicy.clampPercent(safeInt(preferences,
                "glass_shadow", GlassPolicy.DEFAULT_SHADOW));
        String tintMode = safeString(preferences, "glass_tint_mode", "theme");
        if (!"theme".equals(tintMode) && !"custom".equals(tintMode)) tintMode = "theme";
        String customTint = safeString(preferences, "glass_tint", null);
        if (customTint != null) {
            Integer parsed = GraphicsColorPolicy.parseHex(customTint);
            customTint = parsed == null ? null : GraphicsColorPolicy.formatHex(parsed);
        }
        boolean adaptive = safeBoolean(preferences, "glass_adaptive", true);
        return new Config(preset, transparency, blur, frost, saturation, edge, highlight,
                shadow, tintMode, customTint, adaptive);
    }

    private static int safeInt(SharedPreferences preferences, String key, int fallback) {
        try {
            return preferences.getInt(key, fallback);
        } catch (ClassCastException corrupt) {
            preferences.edit().remove(key).apply();
            return fallback;
        }
    }

    private static boolean safeBoolean(SharedPreferences preferences, String key,
                                       boolean fallback) {
        try {
            return preferences.getBoolean(key, fallback);
        } catch (ClassCastException corrupt) {
            preferences.edit().remove(key).apply();
            return fallback;
        }
    }

    private static String safeString(SharedPreferences preferences, String key,
                                     String fallback) {
        try {
            return preferences.getString(key, fallback);
        } catch (ClassCastException corrupt) {
            preferences.edit().remove(key).apply();
            return fallback;
        }
    }

    private static void migrateLegacy(SharedPreferences preferences) {
        int strength = GlassPolicy.clampPercent(
                ForkPanelSettings.effectStrength());
        int transparency = GlassPolicy.clampPercent(Math.round(
                (0.15f + 0.24f * strength / 100f) / 0.79f * 100f));
        SharedPreferences.Editor editor = preferences.edit()
                .putString("glass_preset", GlassPolicy.PRESET_CUSTOM)
                .putInt("glass_transparency", transparency)
                .putInt("glass_blur", strength)
                .putInt("glass_frost", GlassPolicy.DEFAULT_FROST)
                .putInt("glass_saturation", GlassPolicy.DEFAULT_SATURATION)
                .putInt("glass_edge", GlassPolicy.DEFAULT_EDGE)
                .putInt("glass_highlight", GlassPolicy.DEFAULT_HIGHLIGHT)
                .putInt("glass_shadow", GlassPolicy.DEFAULT_SHADOW)
                .putBoolean("glass_adaptive", true);
        editor.apply();
    }

    private static void update(Config config) {
        synchronized (LOCK) {
            prefs().edit()
                    .putString("glass_preset", config.preset)
                    .putInt("glass_transparency", config.transparency)
                    .putInt("glass_blur", config.blur)
                    .putInt("glass_frost", config.frost)
                    .putInt("glass_saturation", config.saturation)
                    .putInt("glass_edge", config.edge)
                    .putInt("glass_highlight", config.highlight)
                    .putInt("glass_shadow", config.shadow)
                    .putString("glass_tint_mode", config.tintMode)
                    .putString("glass_tint", config.customTint)
                    .putBoolean("glass_adaptive", config.adaptive)
                    .apply();
            cached = config;
        }
    }

    public static synchronized void applyPreset(String preset) {
        String normalized = GlassPolicy.normalizePreset(preset);
        if (GlassPolicy.PRESET_CUSTOM.equals(normalized)) {
            Config old = config();
            update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, old.blur, old.frost,
                    old.saturation, old.edge, old.highlight, old.shadow, old.tintMode,
                    old.customTint, old.adaptive));
            return;
        }
        int[] values = GlassPolicy.presetValues(normalized);
        Config old = config();
        update(new Config(normalized, values[0], values[1], values[2], values[3], values[4],
                values[5], values[6], old.tintMode, old.customTint, old.adaptive));
        ForkPanelSettings.setEffectStrength(values[1]);
    }

    public static synchronized void setTransparency(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, GlassPolicy.clampPercent(value), old.blur,
                old.frost, old.saturation, old.edge, old.highlight, old.shadow, old.tintMode,
                old.customTint, old.adaptive));
    }

    public static synchronized void setBlur(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, GlassPolicy.clampPercent(value),
                old.frost, old.saturation, old.edge, old.highlight, old.shadow, old.tintMode,
                old.customTint, old.adaptive));
        ForkPanelSettings.setEffectStrength(value);
    }

    public static synchronized void setFrost(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, old.blur,
                GlassPolicy.clampPercent(value), old.saturation, old.edge, old.highlight,
                old.shadow, old.tintMode, old.customTint, old.adaptive));
    }

    public static synchronized void setSaturation(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, old.blur, old.frost,
                GlassPolicy.clamp(value, 30, 180), old.edge, old.highlight, old.shadow,
                old.tintMode, old.customTint, old.adaptive));
    }

    public static synchronized void setEdge(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, old.blur, old.frost,
                old.saturation, GlassPolicy.clampPercent(value), old.highlight, old.shadow,
                old.tintMode, old.customTint, old.adaptive));
    }

    public static synchronized void setHighlight(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, old.blur, old.frost,
                old.saturation, old.edge, GlassPolicy.clampPercent(value), old.shadow,
                old.tintMode, old.customTint, old.adaptive));
    }

    public static synchronized void setShadow(int value) {
        Config old = config();
        update(new Config(GlassPolicy.PRESET_CUSTOM, old.transparency, old.blur, old.frost,
                old.saturation, old.edge, old.highlight, GlassPolicy.clampPercent(value),
                old.tintMode, old.customTint, old.adaptive));
    }

    public static synchronized void setTintMode(String mode, String customTint) {
        Config old = config();
        String normalized = "custom".equals(mode) ? "custom" : "theme";
        String color = null;
        if ("custom".equals(normalized)) {
            Integer parsed = GraphicsColorPolicy.parseHex(customTint);
            if (parsed == null) throw new IllegalArgumentException("Invalid glass tint");
            color = GraphicsColorPolicy.formatHex(parsed);
        }
        update(new Config(old.preset, old.transparency, old.blur, old.frost, old.saturation,
                old.edge, old.highlight, old.shadow, normalized, color, old.adaptive));
    }

    public static synchronized void setAdaptive(boolean value) {
        Config old = config();
        update(new Config(old.preset, old.transparency, old.blur, old.frost, old.saturation,
                old.edge, old.highlight, old.shadow, old.tintMode, old.customTint, value));
    }

    public static boolean adaptive() {
        return config().adaptive && ForkPanelSettings.adaptiveEffects();
    }

    public static int captureBlurAmount(int base) {
        Config config = config();
        int amount = GlassPolicy.captureBlurAmount(config.blur, config.frost, adaptive(),
                org.telegram.messenger.SharedConfig.deviceIsLow());
        return Math.max(6, base + (amount - 18));
    }

    public static float backdropSaturation(float base) {
        Config config = config();
        float factor = GlassPolicy.saturation(config.saturation, config.frost) / 1.10f;
        return Math.max(0.35f, base * factor);
    }

    static void resetForTests() {
        synchronized (LOCK) {
            cached = null;
        }
    }
}
