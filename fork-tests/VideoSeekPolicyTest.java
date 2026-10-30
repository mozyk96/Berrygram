import org.telegram.messenger.fork.player.VideoSeekPolicy;
public class VideoSeekPolicyTest {
 public static void main(String[] args) {
  check(VideoSeekPolicy.target(5000,6000,-1)==0);
  check(VideoSeekPolicy.target(5000,6000,1)==6000);
  check(VideoSeekPolicy.target(15000,60000,1)==25000);
  check(VideoSeekPolicy.target(100,60000,0)==100);
  check(VideoSeekPolicy.direction(20,300)==-1);
  check(VideoSeekPolicy.direction(150,300)==0);
  check(VideoSeekPolicy.direction(280,300)==1);
  check(VideoSeekPolicy.target(0,-1,1)==0);
  check(VideoSeekPolicy.canSeek(0,60000,-1));
  check(VideoSeekPolicy.canSeek(60000,60000,1));
  check(!VideoSeekPolicy.canSeek(0,-1,-1));
  check(!VideoSeekPolicy.canSeek(0,60000,0));
  check(!VideoSeekPolicy.canSeek(-1,60000,1));
  System.out.println("VideoSeekPolicyTest passed");
 }
 static void check(boolean v) {if(!v) throw new AssertionError();}
}
