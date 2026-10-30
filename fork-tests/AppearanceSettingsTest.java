import org.telegram.messenger.fork.appearance.AppearanceSettings;

public final class AppearanceSettingsTest {
    public static void main(String[] args) {
        AppearanceSettings defaults = AppearanceSettings.defaults();
        check(defaults.radiusFor(AppearanceSettings.Surface.BUBBLE) == 12, "default bubble radius");
        check(defaults.radiusFor(AppearanceSettings.Surface.BUTTON) == 10, "default button radius");
        AppearanceSettings custom = defaults.withGlobalPreset(AppearanceSettings.RadiusPreset.SHARP)
                .withRadiusOverride(AppearanceSettings.Surface.BUBBLE, 99);
        check(custom.radiusFor(AppearanceSettings.Surface.BUBBLE) == 24, "clamp override");
        check(custom.radiusFor(AppearanceSettings.Surface.BUTTON) == 4, "preset applied");
        check(custom.withoutRadiusOverride(AppearanceSettings.Surface.BUBBLE)
                .radiusFor(AppearanceSettings.Surface.BUBBLE) == 4, "remove override");
        check(defaults.radiusFor(AppearanceSettings.Surface.BUBBLE) == 12, "immutable defaults");
        check(custom.withRadiusOverride(AppearanceSettings.Surface.BUTTON, -1)
                .radiusFor(AppearanceSettings.Surface.BUTTON) == 0, "lower clamp");
        check(AppearanceSettings.forTelegramBubbleRadius(17).radiusFor(AppearanceSettings.Surface.BUBBLE) == 17, "retain upstream radius");
        check(AppearanceSettings.forTelegramBubbleRadius(22).radiusFor(AppearanceSettings.Surface.BUBBLE) == 17, "upstream safe maximum");
        check(AppearanceSettings.forTelegramBubbleRadius(-4).radiusFor(AppearanceSettings.Surface.BUBBLE) == 0, "upstream safe minimum");
        System.out.println("AppearanceSettingsTest passed");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
