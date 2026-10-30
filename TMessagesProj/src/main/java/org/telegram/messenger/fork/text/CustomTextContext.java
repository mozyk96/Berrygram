package org.telegram.messenger.fork.text;

/** Optional thread-scoped rendering context, restored after nested use. */
public final class CustomTextContext {
    private static final ThreadLocal<Value> CONTEXT = new ThreadLocal<>();
    private CustomTextContext() {}
    public static final class Value {
        public final int account;
        public final long dialogId;
        private String original;
        private int originalVersion;
        private Value(int account, long dialogId) { this.account = account; this.dialogId = dialogId; }
    }
    public static final class Scope implements AutoCloseable {
        private final Value previous;
        private final Thread owner;
        private final Value value;
        private boolean closed;
        private Scope(Value previous, Value value) { this.previous = previous; this.value = value; owner = Thread.currentThread(); }
        public String original() { return value.original; }
        public void clearOriginal() { value.original = null; }
        public void close() {
            if (closed) return;
            if (owner != Thread.currentThread()) throw new IllegalStateException("Scope must close on its owner thread");
            if (previous == null) CONTEXT.remove(); else CONTEXT.set(previous);
            closed = true;
        }
    }
    public static Scope open(int account, long dialogId) {
        if (account < 0) throw new IllegalArgumentException("Invalid rendering scope");
        Value value = new Value(account, dialogId);
        Scope scope = new Scope(CONTEXT.get(), value);
        CONTEXT.set(value);
        return scope;
    }
    public static Value current() { return CONTEXT.get(); }
    public static int originalVersion() {
        Value value = CONTEXT.get();
        return value == null ? 0 : value.originalVersion;
    }
    public static void recordOriginal(String original) {
        Value value = CONTEXT.get();
        if (value != null) { value.original = original; value.originalVersion++; }
    }
    public static void formatOriginalPluralComma(java.util.Locale locale, Object... args) {
        Value value = CONTEXT.get();
        if (value == null || value.original == null) return;
        value.original = value.original.replace("%d", "%1$s").replace("%1$d", "%1$s");
        formatOriginal(locale, args);
    }
    public static void formatOriginal(java.util.Locale locale, Object... args) {
        Value value = CONTEXT.get();
        if (value == null || value.original == null) return;
        try { value.original = String.format(locale == null ? java.util.Locale.getDefault() : locale, value.original, args); }
        catch (java.util.IllegalFormatException ignored) { value.original = null; }
    }
}
