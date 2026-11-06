import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.fork.panels.ForkGlassSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.GlassPolicy;

public class GlassSettingsTest {
    public static void main(String[] args) throws Exception {
        ForkPanelSettings.setStyle("experimental");
        ForkPanelSettings.setEffectStrength(50);
        android.content.SharedPreferences prefs = ApplicationLoader.applicationContext
                .getSharedPreferences("fork_panels", 0);
        prefs.edit().remove("glass_transparency").remove("glass_blur").remove("glass_frost")
                .remove("glass_saturation").remove("glass_edge").remove("glass_highlight")
                .remove("glass_shadow").remove("glass_preset").remove("glass_tint_mode")
                .remove("glass_tint").remove("glass_adaptive").apply();
        resetCache();

        ForkGlassSettings.Config migrated = ForkGlassSettings.config();
        check(migrated.transparency >= 30 && migrated.transparency <= 40);
        check(migrated.blur == 50);
        check(GlassPolicy.PRESET_CUSTOM.equals(migrated.preset));

        ForkGlassSettings.applyPreset(GlassPolicy.PRESET_CLEAR);
        check(GlassPolicy.PRESET_CLEAR.equals(ForkGlassSettings.config().preset));
        check(ForkGlassSettings.config().transparency == 84);
        check(ForkPanelSettings.effectStrength() == 32);
        ForkGlassSettings.setBlur(77);
        check(ForkGlassSettings.config().blur == 77 && ForkPanelSettings.effectStrength() == 77);
        check(GlassPolicy.PRESET_CUSTOM.equals(ForkGlassSettings.config().preset));
        ForkGlassSettings.setTransparency(500);
        check(ForkGlassSettings.config().transparency == 100);
        ForkGlassSettings.setSaturation(5);
        check(ForkGlassSettings.config().saturation == 30);
        ForkGlassSettings.setSaturation(500);
        check(ForkGlassSettings.config().saturation == 180);
        ForkGlassSettings.setTintMode("custom", "#15aBcd");
        check("#15ABCD".equals(ForkGlassSettings.config().customTint));
        check(ForkGlassSettings.config().customTintEnabled());
        ForkGlassSettings.setTintMode("theme", null);
        check(!ForkGlassSettings.config().customTintEnabled());
        boolean rejected = false;
        try {
            ForkGlassSettings.setTintMode("custom", "bad");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected);
        resetCache();
        check(ForkGlassSettings.config().blur == 77);
        System.out.println("GlassSettingsTest passed");
    }

    private static void resetCache() throws Exception {
        java.lang.reflect.Method method = ForkGlassSettings.class
                .getDeclaredMethod("resetForTests");
        method.setAccessible(true);
        method.invoke(null);
    }

    static void check(boolean value) {
        if (!value) throw new AssertionError();
    }
}
