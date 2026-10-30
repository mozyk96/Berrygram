package org.telegram.messenger.fork.auth;

import java.util.Arrays;

/** Transient credentials; never writes values to persistent storage. */
public final class SessionCredentials {
    private final int id;
    private final char[] hash;
    private boolean cleared;

    private SessionCredentials(int id, char[] hash) {
        this.id = id;
        this.hash = hash.clone();
    }

    public static SessionCredentials create(String id, char[] hash) {
        int parsed;
        try {
            if (id == null || !id.matches("[0-9]+")) throw new IllegalArgumentException("Invalid API ID");
            parsed = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid API ID");
        }
        if (parsed <= 0 || hash == null || hash.length != 32) throw new IllegalArgumentException("Invalid credentials");
        for (char c : hash) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                throw new IllegalArgumentException("Invalid API hash");
            }
        }
        return new SessionCredentials(parsed, hash);
    }

    public int apiId() { ensureAvailable(); return id; }
    public String apiHash() { ensureAvailable(); return new String(hash); }
    public boolean isCleared() { return cleared; }
    public void clear() { Arrays.fill(hash, '\0'); cleared = true; }
    private void ensureAvailable() { if (cleared) throw new IllegalStateException("Credentials cleared"); }
}
