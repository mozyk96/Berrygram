package org.telegram.messenger.fork.panels;

import android.content.Context;

import org.telegram.messenger.ApplicationLoader;

/** Device-local opt-in for the experimental floating chat header. */
public final class ForkChatHeaderSettings {
    private static volatile Boolean cached;

    private ForkChatHeaderSettings() { }

    public static boolean enabled() {
        Boolean value = cached;
        if (value == null) {
            cached = value = ApplicationLoader.applicationContext
                    .getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                    .getBoolean("floating_chat_header", false);
        }
        return value;
    }

    public static void setEnabled(boolean enabled) {
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .edit().putBoolean("floating_chat_header", enabled).apply();
        cached = enabled;
    }
}
