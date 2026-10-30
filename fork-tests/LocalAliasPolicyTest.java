import org.telegram.messenger.fork.text.LocalAliasPolicy;
public class LocalAliasPolicyTest {
 public static void main(String[] args) {
  if (!LocalAliasPolicy.key(1, 42L).equals("alias:1:42")) throw new AssertionError();
  if (LocalAliasPolicy.key(1, 42L).equals(LocalAliasPolicy.key(2, 42L))) throw new AssertionError();
  if (!LocalAliasPolicy.display("Alice", "Friend").equals("Friend (Alice)")) throw new AssertionError();
  if (!LocalAliasPolicy.display("Alice", null).equals("Alice")) throw new AssertionError();
  if (LocalAliasPolicy.valid("bad\u202e")) throw new AssertionError();
  System.out.println("LocalAliasPolicyTest passed");
 }
}
