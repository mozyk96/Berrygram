package org.telegram.messenger.fork.updater;

/** Ephemeral user intent: never persist an installer launch across recreation. UI thread only. */
public final class UpdateInstallIntent {
    private boolean pending;
    public void request() { pending = true; }
    public void cancel() { pending = false; }
    public boolean pending() { return pending; }
    public boolean consume(boolean attached, boolean resumed, boolean focused, boolean verified) {
        if (!pending || !attached || !resumed || !focused || !verified) return false;
        pending = false;
        return true;
    }
}
