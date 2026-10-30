import java.util.*;
import org.telegram.messenger.fork.text.CustomTextPackValidator;
public class CustomTextPackValidatorTest {
 public static void main(String[] a) {
  Map<String,String> originals = new HashMap<>(); originals.put("Online", "online"); originals.put("Named", "%1$s typing");
  Map<String,String> values = new HashMap<>(); values.put("Online", "here"); values.put("Named", "%1$s writes");
  check(CustomTextPackValidator.valid(originals, values));
  values.put("Unrecognized", "bad"); check(!CustomTextPackValidator.valid(originals, values)); values.remove("Unrecognized");
  values.put("Named", "missing argument"); check(!CustomTextPackValidator.valid(originals, values));
  System.out.println("CustomTextPackValidatorTest passed");
 }
 static void check(boolean b) { if (!b) throw new AssertionError(); }
}
