import org.telegram.messenger.fork.voice.VoiceQualityPolicy;
public class VoiceQualityPolicyTest {
 public static void main(String[] a) {
  check(VoiceQualityPolicy.resolve("standard",48000,false,false)==30720);
  check(VoiceQualityPolicy.resolve("auto",48000,true,false)==20480);
  check(VoiceQualityPolicy.resolve("auto",48000,false,true)==20480);
  check(VoiceQualityPolicy.resolve("auto",48000,false,false)==30720);
  check(VoiceQualityPolicy.resolve("custom",48000,false,false)==48000);
  check(VoiceQualityPolicy.resolve("custom",999,false,false)==30720);
  check(VoiceQualityPolicy.resolve("invalid",48000,false,false)==30720);
  System.out.println("VoiceQualityPolicyTest passed");
 }
 static void check(boolean value){if(!value)throw new AssertionError();}
}
