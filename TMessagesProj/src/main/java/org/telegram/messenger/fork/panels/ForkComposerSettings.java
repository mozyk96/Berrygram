package org.telegram.messenger.fork.panels;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Persisted preferences for Berrygram's experimental chat input panel.
 * Default is disabled (standard Telegram composer).
 */
public final class ForkComposerSettings {
    public static final String KEY_EXPERIMENTAL_COMPOSER = "experimental_composer";

    private ForkComposerSettings() { }

    public static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_composer", Context.MODE_PRIVATE);
    }

    public static boolean isExperimentalComposerEnabled() {
        if (ApplicationLoader.applicationContext == null) return false;
        return preferences().getBoolean(KEY_EXPERIMENTAL_COMPOSER, false);
    }

    public static void setExperimentalComposerEnabled(boolean enabled) {
        if (ApplicationLoader.applicationContext == null) return;
        preferences().edit().putBoolean(KEY_EXPERIMENTAL_COMPOSER, enabled).apply();
    }
}
