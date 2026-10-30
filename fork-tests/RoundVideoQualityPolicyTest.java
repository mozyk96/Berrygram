import org.telegram.messenger.fork.camera.RoundVideoQualityPolicy;
public class RoundVideoQualityPolicyTest {
 public static void main(String[] a) {
  check(RoundVideoQualityPolicy.bitrate("standard",1000000,1500000,false,false)==1000000);
  check(RoundVideoQualityPolicy.bitrate("custom",1000000,1500000,false,false)==1500000);
  check(RoundVideoQualityPolicy.bitrate("custom",1000000,-1,false,false)==1000000);
  check(RoundVideoQualityPolicy.bitrate("auto",1000000,1500000,true,false)==700000);
  check(RoundVideoQualityPolicy.bitrate("auto",1000000,1500000,false,true)==700000);
  check(RoundVideoQualityPolicy.bitrate("auto",1000000,1500000,false,false)==1000000);
  check(RoundVideoQualityPolicy.bitrate("auto",100000,1500000,true,false)==100000);
  check(RoundVideoQualityPolicy.bitrate("unknown",1000000,1500000,false,false)==1000000);
  check(RoundVideoQualityPolicy.supported(1000000,1500000,500000,2000000)==1500000);
  check(RoundVideoQualityPolicy.supported(1000000,4000000,500000,2000000)==1000000);
  check(RoundVideoQualityPolicy.supported(1000000,500000,0,0)==1000000);
  check(RoundVideoQualityPolicy.adapt(1000000,1000000,20000,1000000,false)==1000000);
  check(RoundVideoQualityPolicy.adapt(1000000,1000000,30000,5000000,false)==700000);
  check(RoundVideoQualityPolicy.adapt(1000000,1000000,5000,100000,true)==700000);
  check(RoundVideoQualityPolicy.adapt(700000,1000000,40000,6000000,false)==700000);
  check(RoundVideoQualityPolicy.adapt(100000,100000,40000,6000000,true)==100000);
  check(RoundVideoQualityPolicy.adapt(1000000,1000000,29999,5000000,false)==1000000);
  check(RoundVideoQualityPolicy.adapt(1000000,1000000,30000,4999999,false)==1000000);
  System.out.println("RoundVideoQualityPolicyTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
