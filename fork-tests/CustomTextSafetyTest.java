import org.telegram.messenger.fork.text.CustomTextValidator;
public class CustomTextSafetyTest {
 public static void main(String[] a) {
  if (CustomTextValidator.valid("online", "away\u202E")) throw new AssertionError("bidi override");
  if (CustomTextValidator.valid("online", "away\n\n")) throw new AssertionError("line breaks");
  System.out.println("CustomTextSafetyTest passed");
 }
}
