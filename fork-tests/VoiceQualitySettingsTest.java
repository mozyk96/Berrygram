import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.fork.voice.VoiceQualitySettings;
public class VoiceQualitySettingsTest {
 public static void main(String[] a) {
  check("standard".equals(VoiceQualitySettings.mode()));
  check(VoiceQualitySettings.recordingBitrate()==30720);
  VoiceQualitySettings.setMode("auto"); SharedConfig.low=true;
  check(VoiceQualitySettings.recordingBitrate()==20480); SharedConfig.low=false;
  ApplicationLoader.slow=true; check(VoiceQualitySettings.recordingBitrate()==20480); ApplicationLoader.slow=false;
  VoiceQualitySettings.setCustomBitrate(48000); check("custom".equals(VoiceQualitySettings.mode()) && VoiceQualitySettings.recordingBitrate()==48000);
  boolean bad=false; try { VoiceQualitySettings.setCustomBitrate(1); } catch(IllegalArgumentException expected){bad=true;} check(bad && VoiceQualitySettings.recordingBitrate()==48000);
  System.out.println("VoiceQualitySettingsTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
