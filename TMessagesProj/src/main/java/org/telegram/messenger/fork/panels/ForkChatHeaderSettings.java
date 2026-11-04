package org.telegram.messenger.fork.panels;

/** Compatibility shim: saved experimental header preferences are ignored. */
public final class ForkChatHeaderSettings {
    private ForkChatHeaderSettings() { }

    public static boolean enabled() {
        return false;
    }

    public static void setEnabled(boolean enabled) {
        // The native Telegram header is always used.
    }
}
