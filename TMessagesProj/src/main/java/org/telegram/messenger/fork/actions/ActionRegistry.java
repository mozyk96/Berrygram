package org.telegram.messenger.fork.actions;

import java.util.*;

/** Stable presentation IDs. Eligibility and execution belong to Telegram's native menu. */
public final class ActionRegistry {
    public static final String REACTION = "reaction";
    public static final List<String> IDS = Collections.unmodifiableList(Arrays.asList("native", "none", "menu", "reply", "forward", "copy", "edit", "delete", "pin", "save", "reaction"));
    private ActionRegistry() { }
    public static boolean contains(String id) { return IDS.contains(id); }
    public static int[] nativeOptions(String id) {
        if ("reply".equals(id)) return new int[]{8};
        if ("forward".equals(id)) return new int[]{2};
        if ("copy".equals(id)) return new int[]{3};
        if ("edit".equals(id)) return new int[]{12};
        if ("delete".equals(id)) return new int[]{1};
        if ("pin".equals(id)) return new int[]{13,14};
        if ("save".equals(id)) return new int[]{4,7,10};
        return new int[0];
    }
    public static String forNativeOption(int option) {
        for (String id : IDS) for (int value : nativeOptions(id)) if (value == option) return id;
        return null;
    }
    public static int availableOption(String id, List<Integer> allowed) {
        for (int option : nativeOptions(id)) if (allowed.contains(option)) return option;
        return -1;
    }
    public static boolean needsConfirmation(String id) { return "delete".equals(id) || "pin".equals(id); }
    public static boolean needsReaction(String id) { return "reaction".equals(id); }
}
