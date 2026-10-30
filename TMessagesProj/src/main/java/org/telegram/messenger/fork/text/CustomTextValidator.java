package org.telegram.messenger.fork.text;

import java.util.HashMap;
import java.util.Map;

/** Compares formatter argument indices and types; unsupported formats are rejected. */
public final class CustomTextValidator {
    private CustomTextValidator() {}
    public static boolean valid(String original, String replacement) {
        if (original == null || replacement == null || replacement.length() > 500 || replacement.trim().isEmpty()) return false;
        for (int i = 0; i < replacement.length(); i++) {
            char c = replacement.charAt(i);
            if (Character.isISOControl(c) || (c >= '\u202a' && c <= '\u202e') || (c >= '\u2066' && c <= '\u2069')) return false;
        }
        Map<Integer, Character> left = arguments(original);
        Map<Integer, Character> right = arguments(replacement);
        return left != null && left.equals(right);
    }
    public static String withFact(String original, String replacement) {
        if (!valid(original, replacement)) return original;
        return positional(replacement) + " (" + positional(original) + ")";
    }
    private static String positional(String value) {
        StringBuilder out = new StringBuilder();
        int implicit = 1;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '%') { out.append(c); continue; }
            char next = value.charAt(++i);
            if (next == '%' || next == 'n') { out.append('%').append(next); continue; }
            if (Character.isDigit(next)) {
                out.append('%').append(next);
                while (Character.isDigit(value.charAt(++i))) out.append(value.charAt(i));
                out.append('$').append(value.charAt(++i));
            } else out.append('%').append(implicit++).append('$').append(next);
        }
        return out.toString();
    }
    private static Map<Integer, Character> arguments(String value) {
        Map<Integer, Character> args = new HashMap<>();
        int implicit = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '%') continue;
            if (++i >= value.length()) return null;
            char c = value.charAt(i);
            if (c == '%' || c == 'n') continue;
            int index = 0;
            int start = i;
            while (i < value.length() && Character.isDigit(value.charAt(i))) {
                index = index * 10 + (value.charAt(i++) - '0');
                if (index > 100) return null;
            }
            if (i > start) {
                if (i >= value.length() || value.charAt(i) != '$' || index == 0) return null;
                i++;
            } else index = implicit++;
            if (i >= value.length()) return null;
            c = value.charAt(i);
            if (c != 's' && c != 'd') return null;
            Character previous = args.put(index, c);
            if (previous != null && previous != c) return null;
        }
        return args;
    }
}
