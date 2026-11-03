import org.telegram.messenger.fork.panels.HeaderGeometry;
public class HeaderGeometryTest {
    public static void main(String[] args) {
        for (int width : new int[]{160, 240, 320, 360, 600, 960}) {
            for (int count : new int[]{2, 3}) for (int density : new int[]{1, 2, 3}) {
                int w = width * density, controls = count * 52 * density, gap = 24 * density;
                int stories = HeaderGeometry.storiesWidth(w, controls, gap);
                if (stories < 0 || stories > w) throw new AssertionError("Invalid story bounds");
                if (stories > 0 && stories + gap + controls > w) throw new AssertionError("Story/control overlap");
                if (width >= 320 && stories < 56 * density) throw new AssertionError("No usable story touch area");
            }
        }
        if (HeaderGeometry.storiesWidth(320, 156, 24) >= HeaderGeometry.storiesWidth(320, 104, 24)) throw new AssertionError("Hidden lock button did not release story space");
        System.out.println("HeaderGeometryTest passed: density, narrow screens, 2/3 controls and no overlap");
    }
}
