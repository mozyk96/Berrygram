package org.telegram.messenger.fork.panels;

import android.content.Context;
import org.telegram.messenger.ApplicationLoader;

/** Independent optional header appearance; legacy floating-header flags are ignored. */
public final class ForkChatHeaderSettings {
    private ForkChatHeaderSettings() { }
    public static boolean isIosStyle() {
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .getBoolean("ios_chat_header", false);
    }
    public static void setIosStyle(boolean enabled) {
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .edit().putBoolean("ios_chat_header", enabled).apply();
    }
    public static boolean enabled() { return false; }
    public static void setEnabled(boolean enabled) { /* Legacy preference intentionally ignored. */ }
}
