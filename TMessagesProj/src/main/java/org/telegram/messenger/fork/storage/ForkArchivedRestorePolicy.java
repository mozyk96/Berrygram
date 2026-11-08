package org.telegram.messenger.fork.storage;

import java.util.List;
import java.util.Set;

/**
 * Pure rules for restoring locally preserved deleted messages into an open
 * chat. Kept free of Android dependencies so the behaviour can be verified
 * without a device.
 */
public final class ForkArchivedRestorePolicy {
    /** Upper bound of restored tombstones per chat, keeps the chat list fast. */
    public static final int MAX_RESTORED = 200;

    private ForkArchivedRestorePolicy() { }

    /**
     * @param messageId    id of the archived message
     * @param deleted      the message was deleted in Telegram
     * @param existingIds  ids already present in the loaded chat
     */
    public static boolean shouldRestore(int messageId, boolean deleted, Set<Integer> existingIds) {
        if (messageId <= 0 || !deleted) {
            return false;
        }
        return existingIds == null || !existingIds.contains(messageId);
    }

    /**
     * Returns the index at which a message with {@code targetDate} has to be
     * inserted so that the chat stays sorted by date. Date separators are
     * ignored, they are only visual.
     */
    public static int insertIndex(List<Integer> dates, int targetDate) {
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        for (int a = 0; a < dates.size(); a++) {
            Integer date = dates.get(a);
            if (date != null && date > targetDate) {
                return a;
            }
        }
        return dates.size();
    }

    /** Limits the amount of restored messages and keeps the newest ones. */
    public static int limit(int count) {
        return count > MAX_RESTORED ? MAX_RESTORED : count;
    }
}
