package org.telegram.messenger.fork.text;

/**
 * Pure rules of the Berrygram translation preferences.
 *
 * <p>Translation reuses Telegram's own provider, so the rules only decide when
 * the stored preferences may be applied. Keeping them free of Android types
 * makes the edge cases testable: secret chats, Saved Messages and an empty
 * target language must never enable anything.</p>
 */
public final class ForkTranslationPolicy {
    private ForkTranslationPolicy() { }

    /** Translation is never applied to secret chats, encrypted dialogs or Saved Messages. */
    public static boolean isEligible(boolean isSecretChat, boolean isEncryptedDialog, boolean isSelfDialog) {
        return !isSecretChat && !isEncryptedDialog && !isSelfDialog;
    }

    /**
     * Normalises a stored language code. A missing or blank code means automatic
     * detection and must not overwrite Telegram's per dialog language.
     */
    public static String sanitizeLanguage(String code) {
        if (code == null) {
            return "";
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty() || "auto".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
    }

    /** A concrete target language is only stored when one was selected. */
    public static boolean shouldSetTarget(String sanitizedLanguage) {
        return sanitizedLanguage != null && !sanitizedLanguage.isEmpty();
    }

    /** Automatic translation is opt in and only for eligible dialogs. */
    public static boolean shouldTranslate(boolean eligible, boolean autoTranslate) {
        return eligible && autoTranslate;
    }
}
