import org.telegram.messenger.fork.text.CustomTextResolution;
public class CustomTextResolutionTest {
    public static void main(String[] args) {
        check(CustomTextResolution.select("online", "global", "personal").equals("personal"));
        check(CustomTextResolution.select("online", "global", null).equals("global"));
        check(CustomTextResolution.select("online", null, null).equals("online"));
        check(CustomTextResolution.select("%s", "%s global", "%d broken").equals("%s global"));
        check(CustomTextResolution.key(1, 42, "Online").equals("chat:1:42:Online"));
        check(!CustomTextResolution.key(1, 42, "Online").equals(CustomTextResolution.key(2, 42, "Online")));
        System.out.println("CustomTextResolutionTest passed");
    }
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
}
