package org.telegram.messenger.fork.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Presentation only: indices always refer to the original native menu tuples. */
public final class ActionMenuPolicy {
    private ActionMenuPolicy() { }

    public static final class Plan {
        public final List<Integer> mainIndices;
        public final List<Integer> moreIndices;
        private Plan(List<Integer> main, List<Integer> more) {
            mainIndices = Collections.unmodifiableList(main);
            moreIndices = Collections.unmodifiableList(more);
        }
    }

    public static Plan plan(List<Integer> options, List<String> order, Set<String> hidden, Set<String> pinned) {
        List<Integer> main = new ArrayList<>(), more = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String id = ActionRegistry.forNativeOption(options.get(i));
            (id != null && hidden.contains(id) ? more : main).add(i);
        }
        // Stable sort preserves native ordering for duplicate categories and unknown actions.
        main.sort((a, b) -> {
            String left = ActionRegistry.forNativeOption(options.get(a));
            String right = ActionRegistry.forNativeOption(options.get(b));
            int pin = Boolean.compare(pinned.contains(right), pinned.contains(left));
            if (pin != 0) return pin;
            return Integer.compare(rank(order, left), rank(order, right));
        });
        return new Plan(main, more);
    }

    private static int rank(List<String> order, String id) {
        int index = id == null ? -1 : order.indexOf(id);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
