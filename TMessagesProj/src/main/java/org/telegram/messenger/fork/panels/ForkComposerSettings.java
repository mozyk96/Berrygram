package org.telegram.messenger.fork.panels;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Persisted preference for Berrygram's modern chat input panel.
 *
 * <p>Berrygram 1.5.0 ships the redesigned composer by default: a separate
 * rounded text field and a distinct circular voice/video control. Users who
 * prefer Telegram's classic panel can still switch it off; that explicit
 * choice is persisted and respected.</p>
 */
public final class ForkComposerSettings {
    public static final String KEY_EXPERIMENTAL_COMPOSER = "experimental_composer";
    /** The 1.5.0 default. SharedPreferences keeps an explicit user choice. */
    public static final boolean DEFAULT_ENABLED = true;

    private ForkComposerSettings() { }

    public static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_composer", Context.MODE_PRIVATE);
    }

    public static boolean isEnabledByDefault() {
        return DEFAULT_ENABLED;
    }

    public static boolean isExperimentalComposerEnabled() {
        if (ApplicationLoader.applicationContext == null) return DEFAULT_ENABLED;
        return preferences().getBoolean(KEY_EXPERIMENTAL_COMPOSER, DEFAULT_ENABLED);
    }

    public static void setExperimentalComposerEnabled(boolean enabled) {
        if (ApplicationLoader.applicationContext == null) return;
        preferences().edit().putBoolean(KEY_EXPERIMENTAL_COMPOSER, enabled).apply();
    }
}
