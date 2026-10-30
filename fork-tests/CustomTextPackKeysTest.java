import org.telegram.messenger.fork.text.CustomTextPackKeys;
public class CustomTextPackKeysTest {
    public static void main(String[] args) {
        check(CustomTextPackKeys.key("custom", "Online").equals("Online"));
        check(CustomTextPackKeys.key("pack-abc123", "Online").equals("pack:pack-abc123:Online"));
        try { CustomTextPackKeys.key("../bad", "Online"); throw new AssertionError(); } catch (IllegalArgumentException expected) { }
        try { CustomTextPackKeys.key("custom", "bad:key"); throw new AssertionError(); } catch (IllegalArgumentException expected) { }
        System.out.println("CustomTextPackKeysTest passed");
    }
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
}
