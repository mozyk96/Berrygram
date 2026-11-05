package org.telegram.messenger.fork.actions;

import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Standard emoji preferences. Availability and sending remain Telegram's responsibility. */
public final class ForkReactionSettings {
    public static final String ACTION_ID = ActionRegistry.REACTION;
    public static final List<String> CHOICES = Collections.unmodifiableList(Arrays.asList(
            "👍", "❤", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩", "🙏"));
    private static final String FAVORITES = "reaction_favorites";
    private static final String DOUBLE_TAP = "reaction_double_tap";
    private static final List<String> DEFAULTS = Collections.unmodifiableList(Arrays.asList("👍", "❤", "🔥", "👏"));
    private static final Object CACHE_LOCK = new Object();
    private static int cacheAccount = -1;
    private static long cacheUserId = Long.MIN_VALUE;
    private static boolean cachedCustomized;
    private static List<String> cachedFavorites;
    private static String cachedDoubleTap;
    private ForkReactionSettings() { }

    /** Syntax guard only: Telegram's enabled catalog and chat allowed list remain authoritative. */
    public static boolean standardEmoji(String value) {
        if (value == null || value.isEmpty() || value.length() > 64) return false;
        boolean base = false;
        boolean joinPending = false;
        int count = 0;
        for (int offset = 0; offset < value.length();) {
            int point = value.codePointAt(offset);
            offset += Character.charCount(point);
            if (++count > 32) return false;
            if (point == 0x200D || point == 0xFE0F || point == 0xFE0E || point == 0x20E3) {
                if (!base || joinPending || point == 0x200D && offset == value.length()) return false;
                joinPending = point == 0x200D;
                continue;
            }
            if (point >= 0x1F3FB && point <= 0x1F3FF) {
                if (!base || joinPending) return false;
                continue;
            }
            boolean emojiBase = point >= 0x1F000 && point <= 0x1FAFF
                    || point >= 0x2300 && point <= 0x27BF
                    || point >= 0x2194 && point <= 0x21FF
                    || point >= 0x2B00 && point <= 0x2BFF
                    || point == 0xA9 || point == 0xAE || point == 0x203C || point == 0x2049
                    || point == 0x2122 || point == 0x2139 || point == 0x3030 || point == 0x303D
                    || point == 0x3297 || point == 0x3299;
            if (!emojiBase || Character.isLetter(point) || Character.isISOControl(point)) return false;
            base = true;
            joinPending = false;
        }
        return base;
    }

    private static void ensureCache(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        if (account == cacheAccount && userId == cacheUserId) return;
        String raw = read(account, FAVORITES, "");
        List<String> normalized = normalize(Arrays.asList(raw.split(",")));
        cachedFavorites = normalized.isEmpty() ? new ArrayList<>(DEFAULTS) : normalized;
        cachedCustomized = ActionSettings.preferences(account).contains(FAVORITES);
        cachedDoubleTap = read(account, DOUBLE_TAP, "");
        cacheAccount = account;
        cacheUserId = userId;
    }

    /** Visible for tests and external preference migrations. */
    public static void invalidateCache() {
        synchronized (CACHE_LOCK) {
            cacheAccount = -1;
            cacheUserId = Long.MIN_VALUE;
            cachedFavorites = null;
            cachedDoubleTap = null;
        }
    }

    public static boolean customized(int account) {
        synchronized (CACHE_LOCK) {
            ensureCache(account);
            return cachedCustomized;
        }
    }

    public static List<String> favorites(int account) {
        synchronized (CACHE_LOCK) {
            ensureCache(account);
            return new ArrayList<>(cachedFavorites);
        }
    }
    private static String read(int account, String key, String fallback) {
        try { return ActionSettings.preferences(account).getString(key, fallback); }
        catch (ClassCastException corrupt) { return fallback; }
    }
    public static List<String> normalize(List<String> values) {
        ArrayList<String> result = new ArrayList<>();
        if (values != null) for (String value : values) {
            if (standardEmoji(value) && !result.contains(value)) result.add(value);
            if (result.size() == 4) break;
        }
        return result;
    }
    /** Intersect ordered preferences with the native allowed standard emoji list. */
    public static List<String> allowedFavorites(List<String> favorites, List<String> allowed) {
        List<String> result = normalize(favorites);
        result.retainAll(allowed);
        return result;
    }
    public static void setFavorites(int account, List<String> values) {
        List<String> normalized = normalize(values);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Choose one to four standard reactions");
        StringBuilder encoded = new StringBuilder();
        for (String value : normalized) { if (encoded.length() > 0) encoded.append(','); encoded.append(value); }
        ActionSettings.preferences(account).edit().putString(FAVORITES, encoded.toString()).apply();
        invalidateCache();
    }
    public static void useNativeFavorites(int account) {
        ActionSettings.preferences(account).edit().remove(FAVORITES).apply();
        invalidateCache();
    }
    public static void move(int account, int from, int to) {
        List<String> values = favorites(account);
        if (from < 0 || to < 0 || from >= values.size() || to >= values.size()) return;
        values.add(to, values.remove(from));
        setFavorites(account, values);
    }
    public static String doubleTapReaction(int account, String fallback) {
        synchronized (CACHE_LOCK) {
            ensureCache(account);
            return standardEmoji(cachedDoubleTap) ? cachedDoubleTap : fallback;
        }
    }
    public static void setDoubleTapReaction(int account, String value) {
        if (value == null) {
            ActionSettings.preferences(account).edit().remove(DOUBLE_TAP).apply();
            ActionSettings.setGesture(account, "double_tap", "native");
        }
        else {
            if (!standardEmoji(value)) throw new IllegalArgumentException("Unsupported reaction");
            ActionSettings.preferences(account).edit().putString(DOUBLE_TAP, value).apply();
            ActionSettings.setGesture(account, "double_tap", ActionRegistry.REACTION);
        }
        invalidateCache();
    }
    public static void reset(int account) {
        ActionSettings.preferences(account).edit().remove(FAVORITES).remove(DOUBLE_TAP).apply();
        if (ActionRegistry.REACTION.equals(ActionSettings.gesture(account, "double_tap"))) {
            ActionSettings.setGesture(account, "double_tap", "native");
        }
        invalidateCache();
    }
}
