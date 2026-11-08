import org.telegram.messenger.fork.stealth.StealthPolicy;

/**
 * Verifies the master switch semantics of the quick Ghost Mode control: the
 * master switch hides the whole feature without destroying the detailed
 * private and group preferences, and an explicit Mark as read is never hidden.
 */
public class GhostMasterPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // The master switch is off, nothing is hidden even with detailed options on.
        boolean masterEnabled = false;
        boolean ghostMode = true;
        boolean stealthPrivate = true;
        boolean stealthGroups = true;

        check(!StealthPolicy.shouldSuppressRead(masterEnabled && ghostMode, stealthPrivate, stealthGroups, false, false),
                "The master switch must disable the private read suppression");
        check(!StealthPolicy.shouldSuppressRead(masterEnabled && ghostMode, stealthPrivate, stealthGroups, true, false),
                "The master switch must disable the group read suppression");
        check(!StealthPolicy.shouldSuppressSecondaryReads(masterEnabled && ghostMode, stealthPrivate, stealthGroups, false, false),
                "The master switch must disable the secondary read suppression");

        // The master switch is on again, the detailed options are active again.
        masterEnabled = true;
        check(StealthPolicy.shouldSuppressRead(masterEnabled && ghostMode, stealthPrivate, stealthGroups, false, false),
                "The private read suppression must be active again");
        check(StealthPolicy.shouldSuppressRead(masterEnabled && ghostMode, stealthPrivate, stealthGroups, true, false),
                "The group read suppression must be active again");
        check(StealthPolicy.shouldSuppressSecondaryReads(masterEnabled && ghostMode, stealthPrivate, stealthGroups, true, false),
                "The secondary read suppression must be active again");

        // The detailed options are independent of each other.
        check(!StealthPolicy.shouldSuppressRead(true, false, true, false, false),
                "The private option alone must not suppress private reads");
        check(!StealthPolicy.shouldSuppressRead(true, true, false, true, false),
                "The group option alone must not suppress group reads");

        // An explicit Mark as read is never hidden.
        check(!StealthPolicy.shouldSuppressRead(true, true, true, false, false, true),
                "An explicit Mark as read must never be suppressed");
        check(!StealthPolicy.shouldSuppressRead(true, true, true, true, false, true),
                "An explicit Mark as read must never be suppressed in groups");

        // Secret chats are never affected.
        check(!StealthPolicy.shouldSuppressRead(true, true, true, false, true),
                "Secret chats must never suppress read receipts");

        System.out.println("GhostMasterPolicyTest passed");
    }
}
