package org.telegram.messenger.fork.voice;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

/**
 * Attaches only platform-supported audio effects to the active AudioRecord
 * session. Effects are released with the recorder and never run for playback.
 */
public final class VoiceProcessing {
    private VoiceProcessing() { }

    public static final class Handle {
        final NoiseSuppressor noiseSuppressor;
        final AutomaticGainControl gain;
        final AcousticEchoCanceler echo;

        Handle(NoiseSuppressor noiseSuppressor, AutomaticGainControl gain,
               AcousticEchoCanceler echo) {
            this.noiseSuppressor = noiseSuppressor;
            this.gain = gain;
            this.echo = echo;
        }

        public boolean hasAny() {
            return noiseSuppressor != null || gain != null || echo != null;
        }
    }

    public static boolean noiseSuppressorAvailable() {
        try {
            return NoiseSuppressor.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean gainAvailable() {
        try {
            return AutomaticGainControl.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean echoAvailable() {
        try {
            return AcousticEchoCanceler.isAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int source() {
        int source = VoiceProfileSettings.audioSource();
        return source >= 0 ? source : MediaRecorder.AudioSource.DEFAULT;
    }

    public static Handle attach(int sessionId, boolean rnnoiseActive) {
        if (sessionId == AudioManager.ERROR) return new Handle(null, null, null);
        NoiseSuppressor ns = null;
        AutomaticGainControl agc = null;
        AcousticEchoCanceler aec = null;
        try {
            if (VoiceProfileSettings.noiseSuppression(rnnoiseActive,
                    noiseSuppressorAvailable())) {
                ns = NoiseSuppressor.create(sessionId);
                if (ns != null) {
                    ns.setEnabled(true);
                    if (!ns.getEnabled()) {
                        ns.release();
                        ns = null;
                    }
                }
            }
        } catch (Throwable ignored) {
            ns = release(ns);
        }
        try {
            if (VoiceProfileSettings.automaticGain(gainAvailable())) {
                agc = AutomaticGainControl.create(sessionId);
                if (agc != null) {
                    agc.setEnabled(true);
                    if (!agc.getEnabled()) {
                        agc.release();
                        agc = null;
                    }
                }
            }
        } catch (Throwable ignored) {
            agc = release(agc);
        }
        try {
            if (VoiceProfileSettings.echoCancellation(echoAvailable())) {
                aec = AcousticEchoCanceler.create(sessionId);
                if (aec != null) {
                    aec.setEnabled(true);
                    if (!aec.getEnabled()) {
                        aec.release();
                        aec = null;
                    }
                }
            }
        } catch (Throwable ignored) {
            aec = release(aec);
        }
        return new Handle(ns, agc, aec);
    }

    public static void release(Handle handle) {
        if (handle == null) return;
        release(handle.noiseSuppressor);
        release(handle.gain);
        release(handle.echo);
    }

    private static NoiseSuppressor release(NoiseSuppressor effect) {
        if (effect == null) return null;
        try {
            effect.setEnabled(false);
        } catch (Throwable ignored) { }
        try {
            effect.release();
        } catch (Throwable ignored) { }
        return null;
    }

    private static AutomaticGainControl release(AutomaticGainControl effect) {
        if (effect == null) return null;
        try {
            effect.setEnabled(false);
        } catch (Throwable ignored) { }
        try {
            effect.release();
        } catch (Throwable ignored) { }
        return null;
    }

    private static AcousticEchoCanceler release(AcousticEchoCanceler effect) {
        if (effect == null) return null;
        try {
            effect.setEnabled(false);
        } catch (Throwable ignored) { }
        try {
            effect.release();
        } catch (Throwable ignored) { }
        return null;
    }

    /** Diagnostic line for the settings page; no recording data is included. */
    public static String availabilitySummary() {
        return "NS=" + (noiseSuppressorAvailable() ? "yes" : "no")
                + ", AGC=" + (gainAvailable() ? "yes" : "no")
                + ", AEC=" + (echoAvailable() ? "yes" : "no");
    }
}
