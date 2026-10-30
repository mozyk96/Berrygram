import org.telegram.messenger.fork.camera.RoundCameraPolicy;
public class RoundCameraPolicyTest {
 public static void main(String[] args) {
  check(RoundCameraPolicy.initial("standard",false,false)==false);
  check(RoundCameraPolicy.initial("front",false,false));
  check(!RoundCameraPolicy.initial("back",true,true));
  check(RoundCameraPolicy.initial("last",true,false));
  check(!RoundCameraPolicy.initial("last",false,true));
  check(RoundCameraPolicy.initial("invalid",false,true));
  check(RoundCameraPolicy.explicit("front"));
  check(!RoundCameraPolicy.explicit("standard"));
  check(RoundCameraPolicy.gestureChoice(10, 10, 100, 50)==0);
  check(RoundCameraPolicy.gestureChoice(60, 10, 100, 50)==1);
  check(RoundCameraPolicy.gestureChoice(-1, 10, 100, 50)==-1);
  check(RoundCameraPolicy.gestureChoice(50, 51, 100, 50)==-1);
  check(RoundCameraPolicy.gestureChoice(Float.NaN, 10, 100, 50)==-1);
  check(RoundCameraPolicy.camera2Enabled(false, false, true));
  check(!RoundCameraPolicy.camera2Enabled(false, true, false));
  check(!RoundCameraPolicy.camera2Enabled(true, false, true));
  check(RoundCameraPolicy.camera2Enabled(true, true, false));
  check(RoundCameraPolicy.previewFacing(true,true,true)==1);
  check(RoundCameraPolicy.previewFacing(false,true,true)==0);
  check(RoundCameraPolicy.previewFacing(true,false,true)==0);
  check(RoundCameraPolicy.previewFacing(false,true,false)==1);
  check(RoundCameraPolicy.previewFacing(true,false,false)==-1);
  check(RoundCameraPolicy.fps60Allowed(true,true,true));
  check(!RoundCameraPolicy.fps60Allowed(false,true,true));
  check(!RoundCameraPolicy.fps60Allowed(true,true,false));
  System.out.println("RoundCameraPolicyTest passed");
 }
 static void check(boolean value) { if (!value) throw new AssertionError(); }
}
