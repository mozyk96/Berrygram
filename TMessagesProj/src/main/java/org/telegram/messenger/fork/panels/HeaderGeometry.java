package org.telegram.messenger.fork.panels;

/** Pixel geometry shared by the compact stories row and its floating controls. */
public final class HeaderGeometry {
    private HeaderGeometry() { }
    public static int archiveInitialOffset(int position, boolean firstLayout, int offset, int collapsedHeaderOffset) {
        return firstLayout && position == 0 ? Math.max(offset, collapsedHeaderOffset) : offset;
    }
    public static int storiesWidth(int windowWidth, int controlsWidth, int insetsAndGap) {
        return Math.max(0, windowWidth - Math.max(0, controlsWidth) - Math.max(0, insetsAndGap));
    }
}
