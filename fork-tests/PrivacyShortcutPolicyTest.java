import org.telegram.messenger.fork.privacy.PrivacyShortcutPolicy;
public class PrivacyShortcutPolicyTest {
 public static void main(String[] args) {
  check(!PrivacyShortcutPolicy.canOpenRules(false, false));
  check(!PrivacyShortcutPolicy.canOpenRules(true, true));
  check(PrivacyShortcutPolicy.canOpenRules(true, false));
  System.out.println("PrivacyShortcutPolicyTest passed");
 }
 static void check(boolean v) { if (!v) throw new AssertionError(); }
}
