package org.telegram.messenger.fork.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure ordering/filtering for the quick-reaction panel. Indices always point at the
 * native allowed list, which remains the authority for chat permissions and premium
 * restrictions.
 */
public final class ReactionFilterPolicy {
    private ReactionFilterPolicy() { }

    /**
     * Returns native-list indices in favorite order. When none of the preferred
     * reactions are allowed, the full native list is returned so the panel is never
     * empty and the user can still react. Duplicate native entries are used once.
     */
    public static List<Integer> indices(List<String> favorites, List<String> nativeEmojis) {
        ArrayList<Integer> result = new ArrayList<>();
        if (nativeEmojis == null || nativeEmojis.isEmpty()) return result;
        Set<Integer> usedIndices = new HashSet<>();
        List<String> normalized = ForkReactionSettings.normalize(favorites);
        for (String favorite : normalized) {
            for (int i = 0; i < nativeEmojis.size(); i++) {
                if (usedIndices.contains(i) || !favorite.equals(nativeEmojis.get(i))) continue;
                result.add(i);
                usedIndices.add(i);
                break;
            }
        }
        if (result.isEmpty()) {
            for (int i = 0; i < nativeEmojis.size(); i++) result.add(i);
        }
        return result;
    }

    /** True when the ordered result really is a narrowed subset of the native list. */
    public static boolean narrowed(List<Integer> indices, int nativeSize) {
        return indices != null && nativeSize > 0 && indices.size() < nativeSize;
    }
}
