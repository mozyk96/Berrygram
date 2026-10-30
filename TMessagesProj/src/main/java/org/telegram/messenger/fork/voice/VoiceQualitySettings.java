package org.telegram.messenger.fork.voice;
import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;
/** Local voice-recording preferences; existing recordings are never modified. */
public final class VoiceQualitySettings {
    private VoiceQualitySettings() { }
    private static SharedPreferences prefs() { return ApplicationLoader.applicationContext.getSharedPreferences("fork_voice_quality", Context.MODE_PRIVATE); }
    public static String mode() { return prefs().getString("mode", "standard"); }
    public static int customBitrate() {
        try { return Integer.parseInt(prefs().getString("bitrate", "32000")); } catch (NumberFormatException e) { return 32000; }
    }
    public static void setMode(String value) {
        if (!"standard".equals(value) && !"auto".equals(value) && !"custom".equals(value)) throw new IllegalArgumentException("Invalid mode");
        prefs().edit().putString("mode", value).apply();
    }
    public static void setCustomBitrate(int value) {
        if (value < 16000 || value > 64000) throw new IllegalArgumentException("Invalid bitrate");
        prefs().edit().putString("bitrate", Integer.toString(value)).putString("mode", "custom").apply();
    }
    public static int recordingBitrate() {
        return VoiceQualityPolicy.resolve(mode(), customBitrate(), SharedConfig.deviceIsLow(), ApplicationLoader.isConnectionSlow());
    }
}
