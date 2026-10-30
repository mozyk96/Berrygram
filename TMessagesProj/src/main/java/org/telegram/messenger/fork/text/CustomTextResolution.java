package org.telegram.messenger.fork.text;

/** Resolves independently validated phrase templates without accessing Android or user data. */
public final class CustomTextResolution {
    private CustomTextResolution() {}
    public static String select(String original, String global, String personal) {
        if (CustomTextValidator.valid(original, personal)) return personal;
        if (CustomTextValidator.valid(original, global)) return global;
        return original;
    }
    public static String key(long accountId, long dialogId, String resourceKey) {
        if (accountId <= 0 || dialogId == 0 || resourceKey == null || !resourceKey.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid custom text scope");
        }
        return "chat:" + accountId + ":" + dialogId + ":" + resourceKey;
    }
}
