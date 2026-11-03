import org.telegram.messenger.fork.camera.RoundCameraSettings;
public class RoundCameraSettingsTest {
 public static void main(String[] args) {
  android.content.SharedPreferences prefs = org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("fork_round_camera", 0);
  check("front".equals(RoundCameraSettings.mode()));
  RoundCameraSettings.recordFront(false);
  prefs.edit().putString("mode", "last").apply();
  check("back".equals(RoundCameraSettings.mode()));
  check("back".equals(prefs.getString("mode", "")));
  prefs.edit().putString("mode", "gesture").apply();
  check("choose".equals(RoundCameraSettings.mode()));
  check("choose".equals(prefs.getString("mode", "")));
  RoundCameraSettings.setMode("front");
  check("front".equals(RoundCameraSettings.mode()));
  boolean rejected=false;
  try { RoundCameraSettings.setMode("invalid"); } catch (IllegalArgumentException expected) { rejected=true; }
  check(rejected && "front".equals(RoundCameraSettings.mode()));
  RoundCameraSettings.setMode("choose");
  check("choose".equals(RoundCameraSettings.mode()));
  RoundCameraSettings.setQuality("custom", 1500000);
  check("custom".equals(RoundCameraSettings.qualityMode()) && RoundCameraSettings.customBitrate()==1500000);
  rejected=false;
  try { RoundCameraSettings.setQuality("custom", -1); } catch (IllegalArgumentException expected) { rejected=true; }
  check(rejected && RoundCameraSettings.customBitrate()==1500000);
  RoundCameraSettings.setQuality("auto", 0);
  check("auto".equals(RoundCameraSettings.qualityMode()));
  System.out.println("RoundCameraSettingsTest passed");
 }
 static void check(boolean value) {if (!value) throw new AssertionError();}
}
