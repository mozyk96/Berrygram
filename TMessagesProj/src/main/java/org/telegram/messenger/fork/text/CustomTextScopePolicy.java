package org.telegram.messenger.fork.text;
/** Status labels are changed only in explicitly scoped chat/profile rendering. */
public final class CustomTextScopePolicy {
    private CustomTextScopePolicy() {}
    public static boolean allowed(boolean status, boolean scoped) { return !status || scoped; }
}
