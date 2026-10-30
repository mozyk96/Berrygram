import org.telegram.messenger.fork.text.CustomTextValidator;
public class CustomTextFormatTest {
 public static void main(String[] args) {
  check(CustomTextValidator.withFact("%s typing", "%s writes").equals("%1$s writes (%1$s typing)"));
  check(String.format(CustomTextValidator.withFact("%s typing", "%1$s writes"), "Alice").equals("Alice writes (Alice typing)"));
  check(CustomTextValidator.withFact("100%% %s", "%s ok").equals("%1$s ok (100%% %1$s)"));
  System.out.println("CustomTextFormatTest passed");
 }
 static void check(boolean b) { if (!b) throw new AssertionError(); }
}
