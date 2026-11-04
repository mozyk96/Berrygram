package org.telegram.messenger.fork.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Menu presentation is account-local and never changes native action availability. */
public final class ActionMenuSettings {
    private static final String ORDER = "menu_order", HIDDEN = "menu_hidden", PINNED = "menu_pinned";
    private ActionMenuSettings() { }

    public static List<String> editableIds() {
        List<String> result = new ArrayList<>();
        for (String id : ActionRegistry.IDS) if (ActionRegistry.nativeOptions(id).length > 0) result.add(id);
        return result;
    }

    private static List<String> read(int account, String key) {
        String raw;
        try { raw = ActionSettings.preferences(account).getString(key, ""); }
        catch (ClassCastException corrupt) { return new ArrayList<>(); }
        List<String> result = new ArrayList<>(), allowed = editableIds();
        for (String id : raw.split(",")) if (allowed.contains(id) && !result.contains(id)) result.add(id);
        return result;
    }

    public static List<String> order(int account) {
        List<String> result = read(account, ORDER);
        for (String id : editableIds()) if (!result.contains(id)) result.add(id);
        return result;
    }
    public static boolean visible(int account, String id) { return !read(account, HIDDEN).contains(id); }
    public static boolean pinned(int account, String id) { return read(account, PINNED).contains(id); }
    public static void setVisible(int account, String id, boolean visible) { set(account, HIDDEN, id, !visible); }
    public static void setPinned(int account, String id, boolean pinned) { set(account, PINNED, id, pinned); }
    private static void set(int account, String key, String id, boolean enabled) {
        if (!editableIds().contains(id)) throw new IllegalArgumentException("Unknown menu action");
        List<String> values = read(account, key);
        values.remove(id);
        if (enabled) values.add(id);
        ActionSettings.preferences(account).edit().putString(key, String.join(",", values)).apply();
    }
    public static void move(int account, int from, int to) {
        List<String> values = order(account);
        if (from < 0 || to < 0 || from >= values.size() || to >= values.size()) return;
        values.add(to, values.remove(from));
        ActionSettings.preferences(account).edit().putString(ORDER, String.join(",", values)).apply();
    }
    public static ActionMenuPolicy.Plan plan(int account, List<Integer> nativeOptions) {
        return ActionMenuPolicy.plan(nativeOptions, read(account, ORDER),
                new HashSet<>(read(account, HIDDEN)), new HashSet<>(read(account, PINNED)));
    }
    public static void reset(int account) {
        ActionSettings.preferences(account).edit().remove(ORDER).remove(HIDDEN).remove(PINNED).apply();
    }
}
