import org.telegram.messenger.fork.panels.GlassPolicy;

public class GlassPolicyTest {
    public static void main(String[] args) {
        check(GlassPolicy.opacity(0) > 0.9f);
        check(GlassPolicy.opacity(100) < 0.25f);
        check(GlassPolicy.opacity(100) < GlassPolicy.opacity(50));
        check(GlassPolicy.opacity(50) < GlassPolicy.opacity(0));

        int low = GlassPolicy.captureBlurAmount(0, 0, false, false);
        int high = GlassPolicy.captureBlurAmount(100, 100, false, false);
        check(high > low);
        check(GlassPolicy.captureBlurAmount(100, 100, true, true)
                < GlassPolicy.captureBlurAmount(100, 100, false, true));
        check(high <= 38 && low >= 6);

        check(GlassPolicy.saturation(110, 0) > GlassPolicy.saturation(110, 100));
        check(GlassPolicy.saturation(30, 100) >= 0.18f);
        check(GlassPolicy.saturation(180, 0) <= 1.8f);

        for (String preset : new String[]{GlassPolicy.PRESET_CLEAR,
                GlassPolicy.PRESET_FROSTED, GlassPolicy.PRESET_BALANCED,
                GlassPolicy.PRESET_DEEP}) {
            int[] values = GlassPolicy.presetValues(preset);
            check(values.length == 7);
            for (int i = 0; i < values.length; i++) {
                check(values[i] >= 0);
            }
            check(values[0] <= 100 && values[6] <= 100);
        }
        check(GlassPolicy.presetValues(GlassPolicy.PRESET_CLEAR)[0]
                > GlassPolicy.presetValues(GlassPolicy.PRESET_DEEP)[0]);
        check(GlassPolicy.presetValues(GlassPolicy.PRESET_FROSTED)[2]
                > GlassPolicy.presetValues(GlassPolicy.PRESET_CLEAR)[2]);

        check(GlassPolicy.normalizePreset("bogus").equals(GlassPolicy.PRESET_CUSTOM));
        check(GlassPolicy.clampPercent(-4) == 0 && GlassPolicy.clampPercent(140) == 100);
        check(GlassPolicy.radiusDp(2) == 12 && GlassPolicy.radiusDp(99) == 32);
        System.out.println("GlassPolicyTest passed");
    }

    static void check(boolean value) {
        if (!value) throw new AssertionError();
    }
}
