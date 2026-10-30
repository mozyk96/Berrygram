import org.telegram.messenger.fork.camera.RoundCameraSettings;
public class RoundCameraSettingsTest {
 public static void main(String[] args) {
  check("standard".equals(RoundCameraSettings.mode()));
  RoundCameraSettings.setMode("last");
  RoundCameraSettings.recordFront(false);
  check("last".equals(RoundCameraSettings.mode()) && !RoundCameraSettings.lastFront());
  RoundCameraSettings.setMode("front");
  check("front".equals(RoundCameraSettings.mode()));
  boolean rejected=false;
  try { RoundCameraSettings.setMode("invalid"); } catch (IllegalArgumentException expected) { rejected=true; }
  check(rejected && "front".equals(RoundCameraSettings.mode()));
  RoundCameraSettings.setMode("gesture");
  check("gesture".equals(RoundCameraSettings.mode()));
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
