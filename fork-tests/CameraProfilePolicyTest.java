import org.telegram.messenger.fork.camera.CameraProfilePolicy;

public class CameraProfilePolicyTest {
    public static void main(String[] args) {
        check(CameraProfilePolicy.normalize("bogus").equals(CameraProfilePolicy.AUTO));
        check(CameraProfilePolicy.normalize("quality").equals(CameraProfilePolicy.QUALITY));

        check(CameraProfilePolicy.bitrateScale(CameraProfilePolicy.QUALITY) > 1f);
        check(CameraProfilePolicy.bitrateScale(CameraProfilePolicy.PERFORMANCE) < 1f);
        check(CameraProfilePolicy.bitrateScale(CameraProfilePolicy.AUTO) == 1f);
        check(CameraProfilePolicy.jpegQuality(CameraProfilePolicy.QUALITY)
                > CameraProfilePolicy.jpegQuality(CameraProfilePolicy.PERFORMANCE));

        check(CameraProfilePolicy.targetFps(CameraProfilePolicy.QUALITY, 60, false) == 60);
        check(CameraProfilePolicy.targetFps(CameraProfilePolicy.PERFORMANCE, 60, false) == 30);
        check(CameraProfilePolicy.targetFps(CameraProfilePolicy.BALANCED, 60, true) == 30);
        check(CameraProfilePolicy.targetFps(CameraProfilePolicy.BALANCED, 60, false) == 60);
        check(CameraProfilePolicy.limitRoundVideoTo30(CameraProfilePolicy.PERFORMANCE, false));

        int[] no60 = CameraProfilePolicy.preferredFpsBounds(CameraProfilePolicy.QUALITY, false, false);
        check(no60[0] == 30 && no60[1] == 30);
        int[] perf = CameraProfilePolicy.preferredFpsBounds(CameraProfilePolicy.PERFORMANCE, true, false);
        check(perf[0] == 30 && perf[1] == 30);
        int[] quality = CameraProfilePolicy.preferredFpsBounds(CameraProfilePolicy.QUALITY, true, false);
        check(quality[1] == 60);

        int[] lowers = {15, 30, 60};
        int[] uppers = {30, 60, 60};
        check(CameraProfilePolicy.selectFpsRange(lowers, uppers, 30) == 1);
        check(CameraProfilePolicy.selectFpsRange(lowers, uppers, 60) == 2);
        int[] fallbackLowers = {15};
        int[] fallbackUppers = {30};
        check(CameraProfilePolicy.selectFpsRange(fallbackLowers, fallbackUppers, 60) == 0);
        check(CameraProfilePolicy.selectFpsRange(new int[0], new int[0], 30) == -1);

        check(CameraProfilePolicy.resolutionShortSideCap(CameraProfilePolicy.PERFORMANCE, false) == 720);
        check(CameraProfilePolicy.resolutionShortSideCap(CameraProfilePolicy.QUALITY, true) == 0);
        check(CameraProfilePolicy.stabilizationEnabled(CameraProfilePolicy.QUALITY, true));
        check(!CameraProfilePolicy.stabilizationEnabled(CameraProfilePolicy.PERFORMANCE, true));
        check(!CameraProfilePolicy.stabilizationEnabled(CameraProfilePolicy.QUALITY, false));
        System.out.println("CameraProfilePolicyTest passed");
    }

    static void check(boolean value) {
        if (!value) throw new AssertionError();
    }
}
