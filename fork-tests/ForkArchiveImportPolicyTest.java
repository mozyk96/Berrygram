import org.telegram.messenger.fork.storage.ForkArchiveImportPolicy;

/**
 * Verifies the archive import cursors: the backfill walks into the past, the
 * update walk into the future, a page is never mistaken for the end of the
 * history and ineligible content is rejected.
 */
public class ForkArchiveImportPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // Backfill: the first page establishes the cursor.
        check(ForkArchiveImportPolicy.nextBackfillCursor(0, 950) == 950,
                "The first page must establish the backfill cursor");
        // Later pages only move it into the past.
        check(ForkArchiveImportPolicy.nextBackfillCursor(950, 850) == 850,
                "The backfill cursor must move into the past");
        check(ForkArchiveImportPolicy.nextBackfillCursor(850, 900) == 850,
                "The backfill cursor must never move forwards");
        check(ForkArchiveImportPolicy.nextBackfillCursor(850, 0) == 850,
                "An empty page must not reset the backfill cursor");

        // Every message older than the cursor is still missing, the cursor
        // itself and newer messages belong to an earlier pass.
        check(ForkArchiveImportPolicy.shouldBackfill(900, 950), "An older message must be imported");
        check(!ForkArchiveImportPolicy.shouldBackfill(950, 950), "The cursor message is already imported");
        check(!ForkArchiveImportPolicy.shouldBackfill(1000, 950), "A newer message is not backfilled");
        check(ForkArchiveImportPolicy.shouldBackfill(1000, 0), "An empty cursor accepts every message");
        check(!ForkArchiveImportPolicy.shouldBackfill(0, 0), "An invalid message id is rejected");
        check(!ForkArchiveImportPolicy.shouldBackfill(-5, 0), "A negative message id is rejected");

        // Update: forwards only, and a resumed import continues from the cursor.
        check(ForkArchiveImportPolicy.isNewer(1001, 1000), "A newer message is imported");
        check(!ForkArchiveImportPolicy.isNewer(1000, 1000), "The cursor message is not imported twice");
        check(!ForkArchiveImportPolicy.isNewer(999, 1000), "An older message is not imported forwards");
        check(ForkArchiveImportPolicy.nextUpdateCursor(1000, 1005) == 1005,
                "The update cursor must move forwards");
        check(ForkArchiveImportPolicy.nextUpdateCursor(1005, 1001) == 1005,
                "The update cursor must never move backwards");

        // Eligible content only.
        check(ForkArchiveImportPolicy.isEligible(false, 0, 0), "A plain message is eligible");
        check(!ForkArchiveImportPolicy.isEligible(true, 0, 0), "A service message is not eligible");
        check(!ForkArchiveImportPolicy.isEligible(false, 10, 0), "A self destructing message is not eligible");
        check(!ForkArchiveImportPolicy.isEligible(false, 0, 10), "Self destructing media is not eligible");

        // End of history: short page, empty page or repeated offset.
        check(ForkArchiveImportPolicy.reachedEnd(40, 100, 100, 200), "A short page ends the walk");
        check(ForkArchiveImportPolicy.reachedEnd(100, 100, 0, 200), "An empty page ends the walk");
        check(ForkArchiveImportPolicy.reachedEnd(100, 100, 200, 200), "A repeated offset ends the walk");
        check(!ForkArchiveImportPolicy.reachedEnd(100, 100, 150, 200), "A full advancing page continues the walk");

        System.out.println("ForkArchiveImportPolicyTest passed");
    }
}
