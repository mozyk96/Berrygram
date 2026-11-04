import java.util.*;
import org.telegram.messenger.fork.actions.*;
public class ActionMenuPolicyTest {
 static void check(boolean value) { if (!value) throw new AssertionError(); }
 public static void main(String[] args) {
  List<Integer> options=Arrays.asList(99,8,3,13,14,4,1,7,10,98);
  ActionMenuPolicy.Plan p=ActionMenuPolicy.plan(options,Collections.emptyList(),Collections.emptySet(),Collections.emptySet());
  check(p.mainIndices.equals(Arrays.asList(0,1,2,3,4,5,6,7,8,9)) && p.moreIndices.isEmpty());
  p=ActionMenuPolicy.plan(options,Arrays.asList("save","copy","reply"),new HashSet<>(Arrays.asList("save","pin")),Collections.singleton("reply"));
  check(p.mainIndices.equals(Arrays.asList(1,2,0,6,9)));
  check(p.moreIndices.equals(Arrays.asList(3,4,5,7,8)));
  p=ActionMenuPolicy.plan(options,Collections.emptyList(),new HashSet<>(ActionRegistry.IDS),new HashSet<>(ActionRegistry.IDS));
  check(p.mainIndices.equals(Arrays.asList(0,9)));
  Set<Integer> all=new HashSet<>(p.mainIndices); all.addAll(p.moreIndices);
  check(all.size()==options.size());
  check(p.mainIndices.size()+p.moreIndices.size()==options.size());
  p=ActionMenuPolicy.plan(Arrays.asList(3,8,8,99),Arrays.asList("reply","copy"),Collections.emptySet(),Collections.emptySet());
  check(p.mainIndices.equals(Arrays.asList(1,2,0,3)));
  System.out.println("ActionMenuPolicyTest passed");
 }
}
