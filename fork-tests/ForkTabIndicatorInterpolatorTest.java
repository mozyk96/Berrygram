import org.telegram.ui.fork.navigation.ForkTabIndicatorInterpolator;

public class ForkTabIndicatorInterpolatorTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkClose(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.001f) {
            throw new AssertionError(message + " expected " + expected + " but was " + actual);
        }
    }

    public static void main(String[] args) {
        ForkTabIndicatorInterpolator.Anchor chats = new ForkTabIndicatorInterpolator.Anchor(50f, 40f);
        ForkTabIndicatorInterpolator.Anchor contacts = new ForkTabIndicatorInterpolator.Anchor(150f, 60f);
        final float maxStretch = 16f;

        // 0% - the indicator is exactly under the first tab.
        ForkTabIndicatorInterpolator.Result start = ForkTabIndicatorInterpolator.interpolate(chats, contacts, 0f, maxStretch, false);
        checkClose(50f, start.centerX, "0% keeps the indicator under the first tab");
        checkClose(40f, start.width, "0% keeps the width of the first tab");
        checkClose(0f, start.elongation, "0% has no stretch");

        // 25% - a quarter of the way to the next tab.
        ForkTabIndicatorInterpolator.Result quarter = ForkTabIndicatorInterpolator.interpolate(chats, contacts, 0.25f, maxStretch, false);
        checkClose(75f, quarter.centerX, "25% moves a quarter of the distance");
        checkClose(45f, quarter.width, "25% interpolates the width");
        check(quarter.elongation > 0f, "25% has a small stretch");

        // 50% - exactly between the two tabs.
        ForkTabIndicatorInterpolator.Result half = ForkTabIndicatorInterpolator.interpolate(chats, contacts, 0.5f, maxStretch, false);
        checkClose(100f, half.centerX, "50% is exactly between the tabs");
        checkClose(50f, half.width, "50% interpolates the width");
        check(half.elongation > quarter.elongation, "the stretch peaks in the middle of the movement");

        // 75% - nearly under the second tab.
        ForkTabIndicatorInterpolator.Result threeQuarter = ForkTabIndicatorInterpolator.interpolate(chats, contacts, 0.75f, maxStretch, false);
        checkClose(125f, threeQuarter.centerX, "75% moves three quarters of the distance");
        check(threeQuarter.elongation < half.elongation, "the stretch decreases towards the end");

        // 100% - exactly under the second tab.
        ForkTabIndicatorInterpolator.Result end = ForkTabIndicatorInterpolator.interpolate(chats, contacts, 1f, maxStretch, false);
        checkClose(150f, end.centerX, "100% reaches the second tab");
        checkClose(60f, end.width, "100% takes the width of the second tab");
        checkClose(0f, end.elongation, "100% has no stretch");

        // The other direction is symmetric.
        ForkTabIndicatorInterpolator.Result back = ForkTabIndicatorInterpolator.interpolate(contacts, chats, 0.5f, maxStretch, false);
        checkClose(100f, back.centerX, "the reverse direction interpolates as well");

        // Out of range values are clamped, a cancelled gesture can not overshoot.
        checkClose(50f, ForkTabIndicatorInterpolator.interpolate(chats, contacts, -1f, maxStretch, false).centerX, "a negative progress is clamped");
        checkClose(150f, ForkTabIndicatorInterpolator.interpolate(chats, contacts, 2f, maxStretch, false).centerX, "a progress above one is clamped");

        // Reduced motion disables the stretch completely.
        ForkTabIndicatorInterpolator.Result reduced = ForkTabIndicatorInterpolator.interpolate(chats, contacts, 0.5f, maxStretch, true);
        checkClose(0f, reduced.elongation, "reduced motion disables the stretch");
        checkClose(100f, reduced.centerX, "reduced motion still moves the indicator");

        // The stretch never exceeds the configured maximum.
        ForkTabIndicatorInterpolator.Anchor near = new ForkTabIndicatorInterpolator.Anchor(0f, 40f);
        ForkTabIndicatorInterpolator.Anchor far = new ForkTabIndicatorInterpolator.Anchor(400f, 40f);
        ForkTabIndicatorInterpolator.Result wide = ForkTabIndicatorInterpolator.interpolate(near, far, 0.5f, maxStretch, false);
        check(wide.elongation <= maxStretch + 0.001f, "the stretch is limited");

        // Anchor lookup for a fractional navigation position.
        float[] positions = {0f, 1f, 2f, 3f};
        check(ForkTabIndicatorInterpolator.findAnchorIndex(positions, 0f) == 0, "the first anchor is found");
        check(ForkTabIndicatorInterpolator.findAnchorIndex(positions, 0.5f) == 0, "a position between the first and second anchor uses the first");
        check(ForkTabIndicatorInterpolator.findAnchorIndex(positions, 1.5f) == 1, "a position between the second and third anchor uses the second");
        check(ForkTabIndicatorInterpolator.findAnchorIndex(positions, 3f) == 3, "the last anchor is found");
        check(ForkTabIndicatorInterpolator.findAnchorIndex(new float[0], 1f) == -1, "an empty anchor list has no anchor");
        check(ForkTabIndicatorInterpolator.findAnchorIndex(null, 1f) == -1, "a missing anchor list has no anchor");

        System.out.println("ForkTabIndicatorInterpolatorTest passed");
    }
}
