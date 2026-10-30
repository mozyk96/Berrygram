import java.util.*;
import org.telegram.messenger.fork.gestures.FavoriteReactionPolicy;
public class FavoriteReactionPolicyTest {
 public static void main(String[] args) {
  check(FavoriteReactionPolicy.indices(Arrays.asList("a","b","c"),Arrays.asList("c","a")).equals(Arrays.asList(2,0,1)));
  check(FavoriteReactionPolicy.indices(Arrays.asList("a",null,"b"),Arrays.asList("missing","b")).equals(Arrays.asList(2,0,1)));
  check(FavoriteReactionPolicy.indices(Arrays.asList("a","b"),Collections.emptyList()).equals(Arrays.asList(0,1)));
  System.out.println("FavoriteReactionPolicyTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
