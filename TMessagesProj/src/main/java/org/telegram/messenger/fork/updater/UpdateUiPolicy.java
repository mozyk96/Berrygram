package org.telegram.messenger.fork.updater;

/** Primary action shown by the update screen. */
public final class UpdateUiPolicy {
    public static final int CHECK = 0, DOWNLOAD = 1, INSTALL = 2, BUSY = 3, RETRY = 4;
    private UpdateUiPolicy() {}
    public static int action(boolean busy, boolean available, boolean verified, boolean failed) {
        if (busy) return BUSY;
        if (verified) return INSTALL;
        if (failed) return RETRY;
        return available ? DOWNLOAD : CHECK;
    }
}
