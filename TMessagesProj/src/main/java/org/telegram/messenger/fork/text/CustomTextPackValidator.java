package org.telegram.messenger.fork.text;
import java.util.Map;
public final class CustomTextPackValidator {
    private CustomTextPackValidator() {}
    public static boolean valid(Map<String, String> originals, Map<String, String> values) {
        if (originals == null || values == null || values.size() > originals.size()) return false;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String original = originals.get(e.getKey());
            if (original == null || !CustomTextValidator.valid(original, e.getValue())) return false;
        }
        return true;
    }
}
