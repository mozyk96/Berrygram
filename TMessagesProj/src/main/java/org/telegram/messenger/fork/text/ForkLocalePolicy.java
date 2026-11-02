package org.telegram.messenger.fork.text;

import java.util.Locale;

/** Selects the language used for BerryGram's bundled UI translations. */
public final class ForkLocalePolicy {
    private ForkLocalePolicy() {}
    public static Locale select(Locale telegram, Locale system) {
        return telegram != null ? telegram : system;
    }
}
