package org.telegram.ui.fork.navigation;

/**
 * Pure interpolation rules for the Berrygram bottom navigation indicator.
 *
 * <p>The pager owns the navigation position, the indicator only renders it.
 * Every value is derived from the same transition state, so the indicator can
 * never lag behind or fight with the page movement.</p>
 */
public final class ForkTabIndicatorInterpolator {
    /** Geometry of a single navigation anchor (one tab). */
    public static final class Anchor {
        public final float centerX;
        public final float width;

        public Anchor(float centerX, float width) {
            this.centerX = centerX;
            this.width = width;
        }
    }

    /** Interpolated indicator geometry. */
    public static final class Result {
        public final float centerX;
        public final float width;
        public final float elongation;

        Result(float centerX, float width, float elongation) {
            this.centerX = centerX;
            this.width = width;
            this.elongation = elongation;
        }
    }

    private ForkTabIndicatorInterpolator() { }

    public static float clampFraction(float fraction) {
        if (fraction < 0f) return 0f;
        if (fraction > 1f) return 1f;
        return fraction;
    }

    public static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    /**
     * Interpolates between two anchors.
     *
     * @param from          anchor of the page the transition starts from
     * @param to            anchor of the page the transition goes to
     * @param fraction      transition progress in the range 0..1
     * @param maxStretch    maximum extra width of the indicator
     * @param reducedMotion when true the stretch effect is disabled
     */
    public static Result interpolate(Anchor from, Anchor to, float fraction, float maxStretch, boolean reducedMotion) {
        float f = clampFraction(fraction);
        float centerX = lerp(from.centerX, to.centerX, f);
        float width = lerp(from.width, to.width, f);
        float elongation = 0f;
        if (!reducedMotion) {
            float distance = Math.abs(to.centerX - from.centerX);
            // The stretch peaks in the middle of the movement and is gone at both ends,
            // so the indicator never stays detached from the selected tab.
            float peak = Math.min(maxStretch, distance * 0.18f);
            elongation = peak * (float) Math.sin(Math.PI * f);
        }
        return new Result(centerX, width, elongation);
    }

    /**
     * Finds the anchors that surround the given navigation position.
     *
     * @param centers  center X of every visible anchor, ordered by position
     * @param widths   width of every visible anchor, ordered by position
     * @param positions navigation position of every visible anchor, ordered ascending
     * @return the index of the anchor at or before the position, or -1
     */
    public static int findAnchorIndex(float[] positions, float position) {
        if (positions == null || positions.length == 0) {
            return -1;
        }
        int index = -1;
        for (int a = 0; a < positions.length; a++) {
            if (positions[a] <= position + 0.0001f) {
                index = a;
            } else {
                break;
            }
        }
        return index;
    }
}
