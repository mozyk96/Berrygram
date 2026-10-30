package org.telegram.messenger.fork.camera;
import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
/** Device-local camera preference; no camera opens or recordings occur here. */
public final class RoundCameraSettings {
    private RoundCameraSettings() {}
    private static SharedPreferences prefs() { return ApplicationLoader.applicationContext.getSharedPreferences("fork_round_camera", Context.MODE_PRIVATE); }
    public static String mode() { return prefs().getString("mode", "standard"); }
    public static void setMode(String mode) {
        if (!"standard".equals(mode) && !RoundCameraPolicy.explicit(mode)) throw new IllegalArgumentException("Invalid camera mode");
        prefs().edit().putString("mode", mode).apply();
    }
    public static String qualityMode() { return prefs().getString("quality_mode", "standard"); }
    public static int customBitrate() {
        try { return Integer.parseInt(prefs().getString("quality_bitrate", "1000000")); }
        catch (NumberFormatException ignored) { return 1000000; }
    }
    public static void setQuality(String mode, int bitrate) {
        if (!"standard".equals(mode) && !"auto".equals(mode) && !"custom".equals(mode)) throw new IllegalArgumentException("Invalid quality mode");
        if ("custom".equals(mode) && (bitrate < 250000 || bitrate > 8000000)) throw new IllegalArgumentException("Invalid bitrate");
        SharedPreferences.Editor editor = prefs().edit().putString("quality_mode", mode);
        if ("custom".equals(mode)) editor.putString("quality_bitrate", Integer.toString(bitrate));
        editor.apply();
    }
    public static boolean lastFront() { return prefs().getBoolean("last_front", true); }
    public static void recordFront(boolean front) { prefs().edit().putBoolean("last_front", front).apply(); }
}
