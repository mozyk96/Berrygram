import org.telegram.messenger.fork.updater.BackgroundUpdatePolicy;
public class BackgroundUpdatePolicyTest {
 public static void main(String[] args) {
  check(!BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.OFF, true, false));
  check(BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.WIFI, true, false));
  check(!BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.WIFI, false, true));
  check(BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.MOBILE, false, true));
  check(!BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.MOBILE, true, false));
  check(BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.ANY, true, false));
  check(BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.ANY, false, true));
  check(!BackgroundUpdatePolicy.allowed(BackgroundUpdatePolicy.ANY, false, false));
  check(!BackgroundUpdatePolicy.allowed(999, true, true));
  System.out.println("BackgroundUpdatePolicyTest passed");
 }
 static void check(boolean value) { if (!value) throw new AssertionError(); }
}
