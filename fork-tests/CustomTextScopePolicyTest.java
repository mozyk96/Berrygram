import org.telegram.messenger.fork.text.CustomTextScopePolicy;
public class CustomTextScopePolicyTest {
  public static void main(String[] args) {
    check(!CustomTextScopePolicy.allowed(true, false));
    check(CustomTextScopePolicy.allowed(true, true));
    check(CustomTextScopePolicy.allowed(false, false));
    System.out.println("CustomTextScopePolicyTest passed");
  }
  static void check(boolean value) { if (!value) throw new AssertionError(); }
}
