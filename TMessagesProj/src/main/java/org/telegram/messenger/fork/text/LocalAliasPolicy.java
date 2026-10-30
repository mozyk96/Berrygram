package org.telegram.messenger.fork.text;
public final class LocalAliasPolicy {
    private LocalAliasPolicy() {}
    public static String key(long account, long dialogId) { return "alias:" + account + ":" + dialogId; }
    public static boolean valid(String alias) {
        if (alias == null || alias.trim().isEmpty() || alias.length() > 100) return false;
        for (int i = 0; i < alias.length(); i++) {
            char c = alias.charAt(i);
            if (Character.isISOControl(c) || (c >= '\u202a' && c <= '\u202e') || (c >= '\u2066' && c <= '\u2069')) return false;
        }
        return true;
    }
    public static String display(String original, String alias) {
        return valid(alias) && original != null && !original.isEmpty() ? alias + " (" + original + ")" : original;
    }
}
