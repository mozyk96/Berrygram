import java.util.Locale;
import org.telegram.messenger.fork.text.CustomTextContext;
public class CustomTextOriginalTest {
 public static void main(String[] args) {
  try (CustomTextContext.Scope s = CustomTextContext.open(0, 42)) {
   CustomTextContext.recordOriginal("%1$s is typing");
   CustomTextContext.formatOriginal(Locale.US, "Alex");
   check("Alex is typing".equals(s.original()));
   s.clearOriginal(); check(s.original() == null);
   CustomTextContext.recordOriginal("%1$d members");
   CustomTextContext.formatOriginalPluralComma(Locale.US, "1,234");
   check("1,234 members".equals(s.original()));
  }
  System.out.println("CustomTextOriginalTest passed");
 }
 static void check(boolean b) {if (!b) throw new AssertionError();}
}
