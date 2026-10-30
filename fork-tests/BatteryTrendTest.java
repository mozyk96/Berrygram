import org.telegram.messenger.fork.performance.BatteryTrend;
public class BatteryTrendTest {
 public static void main(String[] args) {
  BatteryTrend t=new BatteryTrend();
  check(!t.update(80,false,0));
  check(!t.update(77,false,120000));
  check(t.update(75,false,300000));
  check(t.active(300001));
  check(!t.update(74,true,360000));
  check(!t.active(360001));
  BatteryTrend slow=new BatteryTrend(); slow.update(80,false,1);
  check(!slow.update(75,false,700001));
  check(!slow.update(-1,false,700002));
  BatteryTrend expiry=new BatteryTrend(); expiry.update(80,false,1); expiry.update(75,false,1000);
  check(!expiry.active(1300001));
  System.out.println("BatteryTrendTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
