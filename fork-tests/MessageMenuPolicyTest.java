import java.util.*;
import org.telegram.messenger.fork.menu.MessageMenuPolicy;
public class MessageMenuPolicyTest {
 public static void main(String[] args) {
  List<String> source=Arrays.asList(null,"reply",null,"copy","forward");
  check(MessageMenuPolicy.indices(source,Collections.emptyList(),Collections.emptySet(),Collections.emptySet()).equals(Arrays.asList(0,1,2,3,4)));
  check(MessageMenuPolicy.indices(source,Arrays.asList("forward","copy","reply"),Collections.singleton("copy"),Collections.singleton("forward")).equals(Arrays.asList(4,0,1,2)));
  check(MessageMenuPolicy.indices(Arrays.asList("reply",null,"reply"),Collections.emptyList(),Collections.emptySet(),Collections.singleton("reply")).equals(Arrays.asList(0,2,1)));
  check(MessageMenuPolicy.indices(source,Collections.emptyList(),Collections.singleton("unknown"),Collections.singleton("unknown")).equals(Arrays.asList(0,1,2,3,4)));
  System.out.println("MessageMenuPolicyTest passed");
 }
 static void check(boolean v) {if(!v) throw new AssertionError();}
}
