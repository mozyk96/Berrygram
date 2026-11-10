import org.telegram.messenger.fork.updater.UpdateUiPolicy;
public class UpdateUiPolicyTest {
 public static void main(String[] args) {
  check(UpdateUiPolicy.action(false,false,false,false)==UpdateUiPolicy.CHECK);
  check(UpdateUiPolicy.action(false,true,false,false)==UpdateUiPolicy.DOWNLOAD);
  check(UpdateUiPolicy.action(false,true,true,false)==UpdateUiPolicy.INSTALL);
  check(UpdateUiPolicy.action(true,true,false,false)==UpdateUiPolicy.BUSY);
  check(UpdateUiPolicy.action(false,true,false,true)==UpdateUiPolicy.RETRY);
  System.out.println("UpdateUiPolicyTest passed");
 }
 static void check(boolean condition) { if (!condition) throw new AssertionError(); }
}
