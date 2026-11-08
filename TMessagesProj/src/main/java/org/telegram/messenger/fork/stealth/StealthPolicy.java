package org.telegram.messenger.fork.stealth;

/**
 * Pure policy rules for Berrygram Ghost Mode (stealth reading).
 * Evaluates whether read receipts should be suppressed without Android dependencies.
 */
public final class StealthPolicy {
    private StealthPolicy() { }

    /**
     * Determines whether sending an outgoing read receipt should be suppressed.
     * Secret chats and TTL-bearing content are NEVER suppressed to respect platform security.
     */
    public static boolean shouldSuppressRead(
            boolean ghostModeEnabled,
            boolean stealthPrivate,
            boolean stealthGroups,
            boolean isChannelOrGroup,
            boolean isEncrypted) {
        if (!ghostModeEnabled || isEncrypted) {
            return false;
        }
        return isChannelOrGroup ? stealthGroups : stealthPrivate;
    }

    /**
     * Read receipts requested explicitly by the user are never suppressed,
     * otherwise the unread state of a chat could never be cleared.
     */
    public static boolean shouldSuppressRead(
            boolean ghostModeEnabled,
            boolean stealthPrivate,
            boolean stealthGroups,
            boolean isChannelOrGroup,
            boolean isEncrypted,
            boolean forced) {
        if (forced) {
            return false;
        }
        return shouldSuppressRead(ghostModeEnabled, stealthPrivate, stealthGroups, isChannelOrGroup, isEncrypted);
    }

    /**
     * Validates whether a dialog ID is eligible for ghost mode operations.
     */
    public static boolean isEligible(long dialogId, boolean isEncrypted) {
        return dialogId != 0 && !isEncrypted;
    }
}
