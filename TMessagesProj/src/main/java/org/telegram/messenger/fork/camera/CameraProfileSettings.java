package org.telegram.messenger.fork.camera;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

/** Device-local adaptive camera profile; no camera is opened from here. */
public final class CameraProfileSettings {
    private static final String PREFS = "fork_camera_profile";
    private static volatile String cached;

    private CameraProfileSettings() { }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String mode() {
        String value = cached;
        if (value == null) {
            synchronized (CameraProfileSettings.class) {
                value = cached;
                if (value == null) {
                    String stored;
                    try {
                        stored = prefs().getString("mode", CameraProfilePolicy.AUTO);
                    } catch (ClassCastException corrupt) {
                        prefs().edit().remove("mode").apply();
                        stored = CameraProfilePolicy.AUTO;
                    }
                    value = CameraProfilePolicy.normalize(stored);
                    if (!value.equals(stored)) prefs().edit().putString("mode", value).apply();
                    cached = value;
                }
            }
        }
        return value;
    }

    public static synchronized void setMode(String value) {
        String normalized = CameraProfilePolicy.normalize(value);
        prefs().edit().putString("mode", normalized).apply();
        cached = normalized;
    }

    public static int jpegQuality() {
        return CameraProfilePolicy.jpegQuality(mode());
    }

    public static float bitrateScale() {
        return CameraProfilePolicy.bitrateScale(mode());
    }

    public static int scaleBitrate(int bitrate, int min, int max) {
        int scaled = Math.round(bitrate * bitrateScale());
        if (min > 0 && max >= min) scaled = Math.max(min, Math.min(max, scaled));
        return scaled;
    }

    public static boolean limitRoundVideoTo30() {
        return CameraProfilePolicy.limitRoundVideoTo30(mode(), SharedConfig.deviceIsLow());
    }

    public static int preferredFps(int requested, boolean supports60) {
        return CameraProfilePolicy.targetFps(mode(), requested, SharedConfig.deviceIsLow());
    }

    public static boolean stabilizationEnabled(boolean supported) {
        return CameraProfilePolicy.stabilizationEnabled(mode(), supported);
    }

    public static int resolutionShortSideCap() {
        return CameraProfilePolicy.resolutionShortSideCap(mode(), SharedConfig.deviceIsLow());
    }

    public static String labelKey() {
        return mode();
    }

    static void resetForTests() {
        cached = null;
    }
}
