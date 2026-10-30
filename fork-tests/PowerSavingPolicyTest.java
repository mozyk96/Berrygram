import org.telegram.messenger.fork.performance.PowerSavingPolicy;
public class PowerSavingPolicyTest {
 public static void main(String[] args) {
  check(PowerSavingPolicy.enabled(30,30));
  check(!PowerSavingPolicy.enabled(31,30));
  check(!PowerSavingPolicy.enabled(-1,30));
  check(!PowerSavingPolicy.enabled(101,30));
  check(!PowerSavingPolicy.enabled(0,0));
  check(PowerSavingPolicy.defaultThreshold()==30);
  System.out.println("PowerSavingPolicyTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
