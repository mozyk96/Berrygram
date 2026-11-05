import org.telegram.messenger.fork.voice.VoiceProfilePolicy;

public class VoiceProfilePolicyTest {
    public static void main(String[] args) {
        check(VoiceProfilePolicy.normalize("bogus").equals(VoiceProfilePolicy.STANDARD));
        check(VoiceProfilePolicy.normalize("clear").equals(VoiceProfilePolicy.CLEAR));

        check(VoiceProfilePolicy.bitrate(VoiceProfilePolicy.STANDARD, 30720) == 30720);
        check(VoiceProfilePolicy.bitrate(VoiceProfilePolicy.CLEAR, 30720) == 40000);
        check(VoiceProfilePolicy.bitrate(VoiceProfilePolicy.NATURAL, 20000) == 32000);
        check(VoiceProfilePolicy.bitrate(VoiceProfilePolicy.CLEAR, 64000) == 64000);
        check(VoiceProfilePolicy.bitrate(VoiceProfilePolicy.CLEAR, 1) == 40000);

        check(VoiceProfilePolicy.systemNoiseSuppression(VoiceProfilePolicy.CLEAR, false, true, false));
        check(!VoiceProfilePolicy.systemNoiseSuppression(VoiceProfilePolicy.CLEAR, false, true, true));
        check(!VoiceProfilePolicy.systemNoiseSuppression(VoiceProfilePolicy.NATURAL, true, true, false));
        check(!VoiceProfilePolicy.systemNoiseSuppression(VoiceProfilePolicy.CUSTOM, false, true, false));
        check(VoiceProfilePolicy.systemNoiseSuppression(VoiceProfilePolicy.CUSTOM, true, true, false));
        check(!VoiceProfilePolicy.systemNoiseSuppression(VoiceProfilePolicy.CLEAR, false, false, false));

        check(VoiceProfilePolicy.automaticGain(VoiceProfilePolicy.CLEAR, false, true));
        check(!VoiceProfilePolicy.automaticGain(VoiceProfilePolicy.NATURAL, true, true));
        check(!VoiceProfilePolicy.automaticGain(VoiceProfilePolicy.CLEAR, false, false));
        check(VoiceProfilePolicy.echoCancellation(VoiceProfilePolicy.CUSTOM, true, true));
        check(!VoiceProfilePolicy.echoCancellation(VoiceProfilePolicy.CLEAR, true, true));

        check(VoiceProfilePolicy.source(VoiceProfilePolicy.CLEAR, 0)
                == VoiceProfilePolicy.SOURCE_VOICE_RECOGNITION);
        check(VoiceProfilePolicy.source(VoiceProfilePolicy.STANDARD, 6)
                == VoiceProfilePolicy.SOURCE_DEFAULT);
        check(VoiceProfilePolicy.source(VoiceProfilePolicy.CUSTOM, 6)
                == VoiceProfilePolicy.SOURCE_VOICE_RECOGNITION);
        check(VoiceProfilePolicy.source(VoiceProfilePolicy.CUSTOM, 99)
                == VoiceProfilePolicy.SOURCE_DEFAULT);
        check(VoiceProfilePolicy.validSource(1) && !VoiceProfilePolicy.validSource(99));
        System.out.println("VoiceProfilePolicyTest passed");
    }

    static void check(boolean value) {
        if (!value) throw new AssertionError();
    }
}
