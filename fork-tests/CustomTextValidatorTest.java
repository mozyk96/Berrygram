import org.telegram.messenger.fork.text.CustomTextValidator;
public class CustomTextValidatorTest {
 public static void main(String[] a) {
  check(CustomTextValidator.valid("Online", "В мережі"));
  check(!CustomTextValidator.valid("Online", "Неправильно %s"));
  check(CustomTextValidator.valid("%1$s has %2$d", "%2$d — %1$s"));
  check(!CustomTextValidator.valid("%1$s has %2$d", "%1$d — %2$s"));
  check(!CustomTextValidator.valid("%s", "Oops %"));
  check(CustomTextValidator.valid("100%% %s", "%s 100%%"));
  System.out.println("CustomTextValidatorTest passed");
 }
 static void check(boolean x) {if(!x) throw new AssertionError();}
}
