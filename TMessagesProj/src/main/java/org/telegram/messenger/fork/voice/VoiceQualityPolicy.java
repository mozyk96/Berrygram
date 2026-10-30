package org.telegram.messenger.fork.voice;
/** Opus voice bitrate in bits per second. */
public final class VoiceQualityPolicy {
    public static final int STANDARD = 30 * 1024;
    private VoiceQualityPolicy() { }
    public static int resolve(String mode, int custom, boolean lowDevice, boolean slowConnection) {
        if ("custom".equals(mode) && custom >= 16000 && custom <= 64000) return custom;
        if ("auto".equals(mode) && (lowDevice || slowConnection)) return 20 * 1024;
        return STANDARD;
    }
}
