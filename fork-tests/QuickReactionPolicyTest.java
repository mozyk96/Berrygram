import org.telegram.messenger.fork.gestures.QuickReactionPolicy;
public class QuickReactionPolicyTest {
 public static void main(String[] args) {
  check(QuickReactionPolicy.choose(QuickReactionPolicy.DISABLED,"global",false,true)==null);
  check("personal".equals(QuickReactionPolicy.choose("personal","global",true,true)));
  check("global".equals(QuickReactionPolicy.choose("blocked","global",false,true)));
  check(QuickReactionPolicy.choose("blocked",null,false,false)==null);
  check(!QuickReactionPolicy.key(1,10).equals(QuickReactionPolicy.key(2,10)));
  System.out.println("QuickReactionPolicyTest passed");
 }
 static void check(boolean ok) { if(!ok) throw new AssertionError(); }
}
