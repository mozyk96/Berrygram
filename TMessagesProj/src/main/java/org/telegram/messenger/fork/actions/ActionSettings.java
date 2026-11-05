package org.telegram.messenger.fork.actions;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.util.HashMap;
import java.util.Map;

/** Account-slot-local customization; contains no message/account content. */
public final class ActionSettings {
    public static final String[] GESTURES = {"tap", "double_tap", "long_press", "swipe_left", "swipe_right"};
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, String> gestureCache = new HashMap<>();
    private static int cacheAccount = -1;
    private static long cacheUserId = Long.MIN_VALUE;

    private ActionSettings() { }

    public static SharedPreferences preferences(int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) throw new IllegalArgumentException("Invalid account slot");
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_actions_" + account + "_" + UserConfig.getInstance(account).getClientUserId(), Context.MODE_PRIVATE);
    }

    /** Swipe tracking calls this on every touch move, so avoid repeated preference I/O. */
    private static void ensureGestureCache(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        if (account == cacheAccount && userId == cacheUserId) return;
        gestureCache.clear();
        SharedPreferences prefs = preferences(account);
        for (String gesture : GESTURES) {
            String value = prefs.getString("gesture_" + gesture, "native");
            if (ActionRegistry.contains(value)) gestureCache.put(gesture, value);
        }
        cacheAccount = account;
        cacheUserId = userId;
    }

    public static String gesture(int account, String gesture) {
        synchronized (CACHE_LOCK) {
            try {
                ensureGestureCache(account);
                String value = gestureCache.get(gesture);
                return ActionRegistry.contains(value) ? value : "native";
            } catch (ClassCastException corrupt) {
                return "native";
            }
        }
    }

    public static void setGesture(int account, String gesture, String action) {
        if (!java.util.Arrays.asList(GESTURES).contains(gesture) || !ActionRegistry.contains(action)) throw new IllegalArgumentException("Unknown binding");
        preferences(account).edit().putString("gesture_" + gesture, action).apply();
        synchronized (CACHE_LOCK) {
            long userId = UserConfig.getInstance(account).getClientUserId();
            if (account == cacheAccount && userId == cacheUserId) gestureCache.put(gesture, action);
        }
    }

    public static void resetGestures(int account) {
        SharedPreferences.Editor e = preferences(account).edit();
        for (String gesture : GESTURES) e.remove("gesture_" + gesture);
        e.apply();
        synchronized (CACHE_LOCK) {
            if (account == cacheAccount) gestureCache.clear();
        }
    }
}
