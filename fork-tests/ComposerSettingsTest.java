import org.telegram.messenger.fork.panels.ForkComposerSettings;

public class ComposerSettingsTest {
    public static void main(String[] args) {
        check(!ForkComposerSettings.isExperimentalComposerEnabled());
        ForkComposerSettings.setExperimentalComposerEnabled(true);
        check(ForkComposerSettings.isExperimentalComposerEnabled());
        ForkComposerSettings.setExperimentalComposerEnabled(false);
        check(!ForkComposerSettings.isExperimentalComposerEnabled());
        System.out.println("ComposerSettingsTest passed");
    }

    static void check(boolean v) {
        if (!v) throw new AssertionError();
    }
}
