import org.telegram.messenger.fork.stealth.StealthPolicy;

public class StealthPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // Disabled ghost mode never suppresses
        check(!StealthPolicy.shouldSuppressRead(false, true, true, false, false), "Disabled ghost mode should not suppress");
        check(!StealthPolicy.shouldSuppressRead(false, true, true, true, false), "Disabled ghost mode should not suppress for groups");

        // Encrypted dialogs are never suppressed
        check(!StealthPolicy.shouldSuppressRead(true, true, true, false, true), "Encrypted chat must never suppress");
        check(!StealthPolicy.shouldSuppressRead(true, true, true, true, true), "Encrypted chat must never suppress");

        // Enabled ghost mode with private on and groups off
        check(StealthPolicy.shouldSuppressRead(true, true, false, false, false), "Should suppress private read");
        check(!StealthPolicy.shouldSuppressRead(true, true, false, true, false), "Should not suppress group read when groups off");

        // Enabled ghost mode with private off and groups on
        check(!StealthPolicy.shouldSuppressRead(true, false, true, false, false), "Should not suppress private read when private off");
        check(StealthPolicy.shouldSuppressRead(true, false, true, true, false), "Should suppress group read when groups on");

        // Eligibility check
        check(StealthPolicy.isEligible(12345L, false), "Valid normal chat is eligible");
        check(!StealthPolicy.isEligible(0L, false), "Dialog 0 is not eligible");
        check(!StealthPolicy.isEligible(12345L, true), "Encrypted dialog is not eligible");

        // Explicit user "Mark as read" is never suppressed (1.4.0 Beta 2 fix)
        check(!StealthPolicy.shouldSuppressRead(true, true, true, false, false, true), "Forced private read must not be suppressed");
        check(!StealthPolicy.shouldSuppressRead(true, true, true, true, false, true), "Forced group read must not be suppressed");
        check(!StealthPolicy.shouldSuppressRead(true, true, false, false, false, true), "Forced read must not be suppressed with groups off");
        check(StealthPolicy.shouldSuppressRead(true, true, false, false, false, false), "Automatic private read is still suppressed without force");
        check(StealthPolicy.shouldSuppressRead(true, false, true, true, false, false), "Automatic group read is still suppressed without force");

        // Secondary read signals (mentions, reactions, read contents) follow the
        // same private/group split, but never apply to secret chats.
        check(!StealthPolicy.shouldSuppressSecondaryReads(false, true, false, false, false), "Disabled ghost mode keeps secondary reads");
        check(StealthPolicy.shouldSuppressSecondaryReads(true, true, false, false, false), "Private secondary reads are hidden");
        check(!StealthPolicy.shouldSuppressSecondaryReads(true, true, false, true, false), "Group secondary reads stay when groups are off");
        check(StealthPolicy.shouldSuppressSecondaryReads(true, false, true, true, false), "Group secondary reads are hidden when groups are on");
        check(!StealthPolicy.shouldSuppressSecondaryReads(true, true, true, false, true), "Secret chats never suppress secondary reads");
        check(!StealthPolicy.shouldSuppressSecondaryReads(true, true, true, true, true), "Secret chats never suppress secondary reads");

        System.out.println("StealthPolicyTest passed");
    }
}
