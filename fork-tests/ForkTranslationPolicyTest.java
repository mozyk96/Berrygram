import org.telegram.messenger.fork.text.ForkTranslationPolicy;

/**
 * Verifies the translation preference rules: no secret chats, no Saved
 * Messages, no empty target language and auto translation only when enabled.
 */
public class ForkTranslationPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // Eligible dialogs.
        check(ForkTranslationPolicy.isEligible(false, false, false),
                "A regular chat must be eligible");
        check(!ForkTranslationPolicy.isEligible(true, false, false),
                "A secret chat must not be eligible");
        check(!ForkTranslationPolicy.isEligible(false, true, false),
                "An encrypted dialog must not be eligible");
        check(!ForkTranslationPolicy.isEligible(false, false, true),
                "Saved Messages must not be eligible");

        // Target language normalisation.
        check("uk".equals(ForkTranslationPolicy.sanitizeLanguage("uk")),
                "A language code must survive unchanged");
        check("uk".equals(ForkTranslationPolicy.sanitizeLanguage("  uk  ")),
                "Surrounding whitespace must be trimmed");
        check("".equals(ForkTranslationPolicy.sanitizeLanguage(null)),
                "A missing language means automatic detection");
        check("".equals(ForkTranslationPolicy.sanitizeLanguage("")),
                "An empty language means automatic detection");
        check("".equals(ForkTranslationPolicy.sanitizeLanguage("   ")),
                "A blank language means automatic detection");
        check("".equals(ForkTranslationPolicy.sanitizeLanguage("auto")),
                "The automatic marker must not overwrite the dialog language");

        // Automatic detection must not be stored as a target language.
        check(!ForkTranslationPolicy.shouldSetTarget(""),
                "An empty target language must not be applied");
        check(!ForkTranslationPolicy.shouldSetTarget(null),
                "A null target language must not be applied");
        check(ForkTranslationPolicy.shouldSetTarget("uk"),
                "A concrete target language must be applied");

        // Auto translation is opt in only.
        check(!ForkTranslationPolicy.shouldTranslate(true, false),
                "Auto translation must stay off by default");
        check(ForkTranslationPolicy.shouldTranslate(true, true),
                "Auto translation must apply when enabled");
        check(!ForkTranslationPolicy.shouldTranslate(false, true),
                "Auto translation must never apply to an ineligible dialog");

        System.out.println("ForkTranslationPolicyTest passed");
    }
}
