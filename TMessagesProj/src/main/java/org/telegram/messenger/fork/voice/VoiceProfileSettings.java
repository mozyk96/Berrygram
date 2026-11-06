package org.telegram.messenger.fork.voice;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Device-local voice profile and custom effect choices. */
public final class VoiceProfileSettings {
    private static final String PREFS = "fork_voice_profile";
    private static volatile String cachedProfile;

    private VoiceProfileSettings() { }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String profile() {
        String value = cachedProfile;
        if (value == null) {
            synchronized (VoiceProfileSettings.class) {
                value = cachedProfile;
                if (value == null) {
                    String stored;
                    try {
                        stored = prefs().getString("profile", VoiceProfilePolicy.STANDARD);
                    } catch (ClassCastException corrupt) {
                        prefs().edit().remove("profile").apply();
                        stored = VoiceProfilePolicy.STANDARD;
                    }
                    value = VoiceProfilePolicy.normalize(stored);
                    if (!value.equals(stored)) prefs().edit().putString("profile", value).apply();
                    cachedProfile = value;
                }
            }
        }
        return value;
    }

    public static synchronized void setProfile(String value) {
        String normalized = VoiceProfilePolicy.normalize(value);
        prefs().edit().putString("profile", normalized).apply();
        cachedProfile = normalized;
    }

    public static boolean customNoiseSuppression() {
        return safeBoolean("custom_ns", true);
    }

    public static void setCustomNoiseSuppression(boolean value) {
        prefs().edit().putBoolean("custom_ns", value).apply();
    }

    public static boolean customGain() {
        return safeBoolean("custom_agc", false);
    }

    public static void setCustomGain(boolean value) {
        prefs().edit().putBoolean("custom_agc", value).apply();
    }

    public static boolean customEcho() {
        return safeBoolean("custom_aec", false);
    }

    public static void setCustomEcho(boolean value) {
        prefs().edit().putBoolean("custom_aec", value).apply();
    }

    public static int customSource() {
        int stored = VoiceProfilePolicy.SOURCE_DEFAULT;
        try {
            stored = Integer.parseInt(safeString("custom_source", "0"));
        } catch (NumberFormatException ignored) { }
        return VoiceProfilePolicy.validSource(stored) ? stored
                : VoiceProfilePolicy.SOURCE_DEFAULT;
    }

    private static boolean safeBoolean(String key, boolean fallback) {
        try {
            return prefs().getBoolean(key, fallback);
        } catch (ClassCastException corrupt) {
            prefs().edit().remove(key).apply();
            return fallback;
        }
    }

    private static String safeString(String key, String fallback) {
        try {
            return prefs().getString(key, fallback);
        } catch (ClassCastException corrupt) {
            prefs().edit().remove(key).apply();
            return fallback;
        }
    }

    public static void setCustomSource(int value) {
        if (!VoiceProfilePolicy.validSource(value)) {
            throw new IllegalArgumentException("Invalid audio source");
        }
        prefs().edit().putString("custom_source", Integer.toString(value)).apply();
    }

    /**
     * Numeric values intentionally match android.media.MediaRecorder.AudioSource
     * so this settings layer stays testable without the Android framework.
     */
    public static int audioSource() {
        return VoiceProfilePolicy.source(profile(), customSource());
    }

    public static boolean noiseSuppression(boolean rnnoiseActive, boolean supported) {
        return VoiceProfilePolicy.systemNoiseSuppression(profile(),
                customNoiseSuppression(), supported, rnnoiseActive);
    }

    public static boolean automaticGain(boolean supported) {
        return VoiceProfilePolicy.automaticGain(profile(), customGain(), supported);
    }

    public static boolean echoCancellation(boolean supported) {
        return VoiceProfilePolicy.echoCancellation(profile(), customEcho(), supported);
    }

    public static int recordingBitrate(int resolved) {
        return VoiceProfilePolicy.bitrate(profile(), resolved);
    }

    static void resetForTests() {
        cachedProfile = null;
    }
}
