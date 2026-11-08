import org.telegram.messenger.fork.storage.ForkArchivedRestorePolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForkArchivedRestorePolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // Only deleted messages are restored.
        check(!ForkArchivedRestorePolicy.shouldRestore(5, false, new HashSet<Integer>()),
                "A message that was not deleted must not be restored");
        check(ForkArchivedRestorePolicy.shouldRestore(5, true, new HashSet<Integer>()),
                "A deleted message must be restored");
        check(!ForkArchivedRestorePolicy.shouldRestore(0, true, new HashSet<Integer>()),
                "A message without an id must not be restored");
        check(!ForkArchivedRestorePolicy.shouldRestore(-3, true, new HashSet<Integer>()),
                "A negative message id must not be restored");

        // Messages that are still present are never restored twice.
        Set<Integer> existing = new HashSet<>(Arrays.asList(5, 7));
        check(!ForkArchivedRestorePolicy.shouldRestore(5, true, existing),
                "An already loaded message must not be restored again");
        check(ForkArchivedRestorePolicy.shouldRestore(6, true, existing),
                "An unknown deleted message must be restored");
        check(ForkArchivedRestorePolicy.shouldRestore(5, true, null),
                "Without a known id list the message is restored");

        // Insertion keeps the chat sorted by date.
        List<Integer> dates = new ArrayList<>(Arrays.asList(10, 20, 30, 40));
        check(ForkArchivedRestorePolicy.insertIndex(dates, 5) == 0, "Older message goes first");
        check(ForkArchivedRestorePolicy.insertIndex(dates, 25) == 2, "Message goes between 20 and 30");
        check(ForkArchivedRestorePolicy.insertIndex(dates, 40) == 4, "Equal date is inserted after the existing entry");
        check(ForkArchivedRestorePolicy.insertIndex(dates, 99) == 4, "Newer message goes last");
        check(ForkArchivedRestorePolicy.insertIndex(new ArrayList<Integer>(), 5) == 0, "Empty list inserts at the start");
        check(ForkArchivedRestorePolicy.insertIndex(null, 5) == 0, "Missing list inserts at the start");

        // The amount of restored tombstones stays bounded.
        check(ForkArchivedRestorePolicy.limit(10) == 10, "Small amount is kept");
        check(ForkArchivedRestorePolicy.limit(ForkArchivedRestorePolicy.MAX_RESTORED + 50)
                == ForkArchivedRestorePolicy.MAX_RESTORED, "Large amount is limited");

        System.out.println("ForkArchivedRestorePolicyTest passed");
    }
}
