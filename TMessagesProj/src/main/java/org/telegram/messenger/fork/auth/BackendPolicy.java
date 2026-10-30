package org.telegram.messenger.fork.auth;

/** Never silently replace an already-authorized account's backend. */
public final class BackendPolicy {
    private BackendPolicy() {}
    public static boolean shouldRestoreProduction(boolean isTestBackend, boolean isAccountAuthorized) {
        return isTestBackend && !isAccountAuthorized;
    }
}
