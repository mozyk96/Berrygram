import org.telegram.messenger.fork.storage.ForkDeletionPolicy;
import java.util.*;
import java.util.concurrent.*;
public class ForkDeletionPolicyTest {
 public static void main(String[] args) throws Exception {
  ForkDeletionPolicy.markLocal(0, 101, Arrays.asList(7,7,0,-1));
  check(ForkDeletionPolicy.isLocal(0,101,7), "local deletion missing");
  check(!ForkDeletionPolicy.isLocal(1,101,7), "cross-account suppression");
  check(!ForkDeletionPolicy.isLocal(0,-101,7), "channel/dialog id collision");
  check(!ForkDeletionPolicy.isLocal(0,102,7), "cross-dialog suppression");
  check(!ForkDeletionPolicy.isLocal(0,101,0), "unsent id tracked");
  org.telegram.messenger.UserConfig.setTestUserId(0,999);
  check(!ForkDeletionPolicy.isLocal(0,101,7), "reused account slot inherited local deletion");
  org.telegram.messenger.UserConfig.setTestUserId(0,100);
  check(ForkDeletionPolicy.isLocal(0,101,7), "original account marker lost");
  ExecutorService executor=Executors.newFixedThreadPool(4);
  for(int i=0;i<1000;i++){ final int id=i+100; executor.submit(()->ForkDeletionPolicy.markLocal(2,-500,Collections.singletonList(id))); }
  executor.shutdown(); check(executor.awaitTermination(10,TimeUnit.SECONDS), "concurrent marker timeout");
  for(int i=100;i<1100;i++) check(ForkDeletionPolicy.isLocal(2,-500,i), "concurrent marker lost");
  System.out.println("ForkDeletionPolicyTest passed (identity isolation, duplicate ids, unsent ids, concurrent local suppression)");
 }
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
