package org.telegram.messenger.fork.storage;

import java.util.List;
import org.telegram.messenger.UserConfig;
import java.util.concurrent.ConcurrentHashMap;

/** Immediate identity-scoped suppression; durable markers are owned by the archive database. */
public final class ForkDeletionPolicy {
    private static final ConcurrentHashMap<String, Boolean> local = new ConcurrentHashMap<>();
    private ForkDeletionPolicy() { }
    private static String key(int account, long owner, long dialog, int message) { return account + ":" + owner + ":" + dialog + ":" + message; }
    public static void markLocal(int account, long dialog, List<Integer> messages) {
        markLocal(account, UserConfig.getInstance(account).getClientUserId(), dialog, messages);
    }
    public static void markLocal(int account, long owner, long dialog, List<Integer> messages) {
        if (owner <= 0 || dialog == 0 || messages == null) return;
        for (Integer message : messages) if (message != null && message > 0) local.put(key(account, owner, dialog, message), true);
    }
    public static boolean isLocal(int account, long dialog, int message) {
        return message > 0 && local.containsKey(key(account, UserConfig.getInstance(account).getClientUserId(), dialog, message));
    }
}
