package org.telegram.messenger.fork.storage;

/**
 * Pure rules of the Berry archive history import.
 *
 * <p>The importer walks a chat in two directions. The backfill cursor moves
 * into the past, the update cursor moves into the future. Keeping the rules in
 * one place makes them testable without an Android runtime and prevents the
 * classic bug where a finished page is treated as a finished import.</p>
 */
public final class ForkArchiveImportPolicy {
    private ForkArchiveImportPolicy() { }

    /** True when the message belongs to the not yet imported older history. */
    public static boolean shouldBackfill(int messageId, int backfillCursor) {
        return messageId > 0 && (backfillCursor == 0 || messageId < backfillCursor);
    }

    /**
     * Advances the backfill cursor. The cursor only ever moves into the past,
     * and the first page establishes it instead of keeping the empty value.
     */
    public static int nextBackfillCursor(int currentCursor, int pageSmallestId) {
        if (pageSmallestId <= 0) {
            return currentCursor;
        }
        if (currentCursor == 0) {
            return pageSmallestId;
        }
        return Math.min(currentCursor, pageSmallestId);
    }

    /** True when the message is newer than everything imported forwards. */
    public static boolean isNewer(int messageId, int updateCursor) {
        return messageId > updateCursor;
    }

    /** Advances the update cursor, it never moves backwards. */
    public static int nextUpdateCursor(int currentCursor, int messageId) {
        return Math.max(currentCursor, messageId);
    }

    /** Messages that must never be archived. */
    public static boolean isEligible(boolean hasAction, int ttl, int mediaTtl) {
        return !hasAction && ttl <= 0 && mediaTtl <= 0;
    }

    /**
     * True when a history page is the last one.
     *
     * <p>A short page means the history is exhausted. An empty page or a page
     * that repeats the requested offset also ends the walk, otherwise the
     * importer would loop on the oldest page forever.</p>
     */
    public static boolean reachedEnd(int pageSize, int requestedPageSize, int pageSmallestId, int previousOffsetId) {
        if (pageSize < requestedPageSize) {
            return true;
        }
        return pageSmallestId <= 0 || pageSmallestId == previousOffsetId;
    }
}
