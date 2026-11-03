import java.util.Arrays;

public class FloatingChatHeaderGeometryTest {
    private static int dp(float value, float density) {
        return (int) Math.ceil(value * density);
    }

    public static void main(String[] args) {
        int cases = 0;
        for (float density : new float[]{1, 1.5f, 2, 3}) {
            for (int screenDp : new int[]{320, 360, 412, 600, 960}) {
                for (int menuDp : new int[]{0, 48, 96, 144}) {
                    int screen = dp(screenDp, density);
                    int menu = dp(menuDp, density);
                    int container = screen - dp(58, density);
                    int region = FloatingChatHeaderGeometry.floatingHeaderTextWidth(container, menu, dp(64, density));
                    int textWidth = Math.max(0, region - dp(24, density));
                    int textLeft = dp(58, density) + (region - textWidth) / 2;
                    int menuLeft = screen - dp(58, density) - menu;
                    int avatarLeft = screen - dp(8, density) - (dp(42, density) - 2);
                    if (textLeft < dp(58, density) || textLeft + textWidth > menuLeft
                            || menuLeft + menu > avatarLeft || textWidth < 0) {
                        throw new AssertionError("Overlapping header at " + Arrays.toString(new int[]{screenDp, menuDp}) + " density=" + density);
                    }
                    // Adding a native action must consume its full width from the title region.
                    int smallerRegion = FloatingChatHeaderGeometry.floatingHeaderTextWidth(container, menu + dp(48, density), dp(64, density));
                    if (region - smallerRegion != Math.min(region, dp(48, density))) {
                        throw new AssertionError("Native action reservation lost");
                    }
                    cases++;
                }
            }
        }
        if (FloatingChatHeaderGeometry.floatingHeaderTextWidth(100, 200, 64) != 0
                || FloatingChatHeaderGeometry.floatingHeaderTextWidth(0, 0, 64) != 0) {
            throw new AssertionError("Narrow layouts must clamp title space to zero");
        }
        System.out.println("Floating chat header geometry: " + cases + " screen/menu/density cases passed");
    }
}
