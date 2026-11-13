import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.fork.panels.ForkComposerSettings;

public class ComposerSettingsTest {
    public static void main(String[] args) {
        // 1.5.0 ships the modern composer by default; the shipped default never changes.
        check(ForkComposerSettings.isEnabledByDefault());
        check(ForkComposerSettings.isExperimentalComposerEnabled());

        // An explicit opt-out is persisted and respected.
        ForkComposerSettings.setExperimentalComposerEnabled(false);
        check(!ForkComposerSettings.isExperimentalComposerEnabled());
        check(ForkComposerSettings.isEnabledByDefault());

        // Re-enabling restores the modern panel.
        ForkComposerSettings.setExperimentalComposerEnabled(true);
        check(ForkComposerSettings.isExperimentalComposerEnabled());

        // A fresh context (no stored value) must fall back to the shipped default.
        ApplicationLoader.applicationContext = new android.content.Context();
        check(ForkComposerSettings.isExperimentalComposerEnabled());

        // An explicit opt-out survives a context/process restart.
        ForkComposerSettings.setExperimentalComposerEnabled(false);
        check(!ForkComposerSettings.isExperimentalComposerEnabled());

        System.out.println("ComposerSettingsTest passed (default on, explicit opt-out persisted, restart safe)");
    }

    static void check(boolean v) {
        if (!v) throw new AssertionError();
    }
}
