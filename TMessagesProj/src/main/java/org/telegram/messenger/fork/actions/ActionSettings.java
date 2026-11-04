package org.telegram.messenger.fork.actions;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

/** Account-slot-local customization; contains no message/account content. */
public final class ActionSettings {
    public static final String[] GESTURES = {"tap", "double_tap", "long_press", "swipe_left", "swipe_right"};
    private ActionSettings() { }
    public static SharedPreferences preferences(int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) throw new IllegalArgumentException("Invalid account slot");
        return ApplicationLoader.applicationContext.getSharedPreferences("fork_actions_" + account + "_" + UserConfig.getInstance(account).getClientUserId(), Context.MODE_PRIVATE);
    }
    public static String gesture(int account, String gesture) {
        try {
            String value = preferences(account).getString("gesture_" + gesture, "native");
            return ActionRegistry.contains(value) ? value : "native";
        } catch (ClassCastException corrupt) { return "native"; }
    }
    public static void setGesture(int account, String gesture, String action) {
        if (!java.util.Arrays.asList(GESTURES).contains(gesture) || !ActionRegistry.contains(action)) throw new IllegalArgumentException("Unknown binding");
        preferences(account).edit().putString("gesture_" + gesture, action).apply();
    }
    public static void resetGestures(int account) {
        SharedPreferences.Editor e = preferences(account).edit();
        for (String gesture : GESTURES) e.remove("gesture_" + gesture);
        e.apply();
    }
}
