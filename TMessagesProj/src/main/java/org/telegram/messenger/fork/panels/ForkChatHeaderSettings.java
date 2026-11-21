package org.telegram.messenger.fork.panels;

import android.content.Context;
import org.telegram.messenger.ApplicationLoader;

/** Independent optional header appearance; legacy floating-header flags are ignored. */
public final class ForkChatHeaderSettings {
    // Both controls store the same boolean: 2.0.2 Floating is the new switch's ON state.
    private static final String FLOATING_HEADER_KEY = "ios_chat_header";

    private ForkChatHeaderSettings() { }
    public static boolean isIosStyle() {
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .getBoolean(FLOATING_HEADER_KEY, false);
    }
    public static void setIosStyle(boolean enabled) {
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .edit().putBoolean(FLOATING_HEADER_KEY, enabled).apply();
    }
    public static boolean enabled() { return false; }
    public static void setEnabled(boolean enabled) { /* Legacy preference intentionally ignored. */ }
}
