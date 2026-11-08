import org.telegram.messenger.fork.panels.MenuSurfacePolicy;

public class MenuSurfacePolicyTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(MenuSurfacePolicy.surface("glass") == MenuSurfacePolicy.GLASS);
        check(MenuSurfacePolicy.surface("liquid") == MenuSurfacePolicy.GLASS);
        check(MenuSurfacePolicy.surface("experimental") == MenuSurfacePolicy.GLASS);
        check(MenuSurfacePolicy.surface("blur") == MenuSurfacePolicy.BLUR);
        check(MenuSurfacePolicy.surface("none") == MenuSurfacePolicy.OPAQUE);
        check(MenuSurfacePolicy.surface("material") == MenuSurfacePolicy.OPAQUE);
        check(MenuSurfacePolicy.surface(null) == MenuSurfacePolicy.OPAQUE);
        System.out.println("MenuSurfacePolicyTest passed");
    }
}
