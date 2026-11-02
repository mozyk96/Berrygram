package org.telegram.messenger.fork.panels;

import java.util.Locale;

/** Parsing and contrast for the optional opaque BerryGram accent. */
public final class GraphicsColorPolicy {
    private GraphicsColorPolicy() { }
    public static Integer parseHex(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (text.startsWith("#")) text = text.substring(1);
        if (!text.matches("[0-9a-fA-F]{6}")) return null;
        return (int) (0xff000000L | Long.parseLong(text, 16));
    }
    public static String formatHex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xffffff); }
    private static double component(int n) {
        double v = n / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
    private static double luminance(int color) {
        return 0.2126 * component((color >>> 16) & 255) + 0.7152 * component((color >>> 8) & 255) + 0.0722 * component(color & 255);
    }
    public static double contrastRatio(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }
    public static int readableAccent(int color, int surface) {
        int target = contrastText(surface);
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        int t = target & 255;
        for (int i = 0; i <= 100; i++) {
            int candidate = 0xff000000 | ((r * (100-i) + t*i) / 100 << 16) | ((g * (100-i) + t*i) / 100 << 8) | ((b * (100-i) + t*i) / 100);
            if (contrastRatio(candidate, surface) >= 4.5) return candidate;
        }
        return target;
    }
    public static int contrastText(int color) {
        double luminance = luminance(color);
        return (luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05) ? 0xff000000 : 0xffffffff;
    }
}
