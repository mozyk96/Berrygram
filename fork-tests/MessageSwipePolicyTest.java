import org.telegram.messenger.fork.gestures.MessageSwipePolicy;
public class MessageSwipePolicyTest {
 public static void main(String[] args) {
  check(MessageSwipePolicy.direction(-50,20,true,false,false)==-1);
  check(MessageSwipePolicy.direction(50,20,true,true,true)==1);
  check(MessageSwipePolicy.direction(50,20,true,true,false)==0);
  check(MessageSwipePolicy.direction(50,20,true,false,true)==0);
  check(MessageSwipePolicy.direction(-10,20,true,true,true)==0);
  System.out.println("MessageSwipePolicyTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
