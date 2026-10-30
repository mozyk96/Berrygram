import org.telegram.messenger.fork.player.CaptionTextPolicy;
public class CaptionTextPolicyTest {
 public static void main(String[] args) {
  check(CaptionTextPolicy.take(null, 10)==0);
  check(CaptionTextPolicy.take("", 10)==0);
  check(CaptionTextPolicy.take("hello", 10)==5);
  check(CaptionTextPolicy.take("hello", 3)==3);
  check(CaptionTextPolicy.take("hello", 0)==0);
  check(CaptionTextPolicy.take("a\uD83D\uDE00b", 2)==1);
  check(CaptionTextPolicy.take("a\uD83D\uDE00b", 3)==3);
  System.out.println("CaptionTextPolicyTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
