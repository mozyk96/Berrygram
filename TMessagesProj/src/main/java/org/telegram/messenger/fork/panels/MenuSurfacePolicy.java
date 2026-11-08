package org.telegram.messenger.fork.panels;

/**
 * Maps the user-visible appearance mode to the message-menu surface renderer.
 *
 * <p>Glass 2.0 uses BerryGram's tint/blur provider, Blur keeps Telegram's matte
 * blurred provider, and No Effects must not capture or blur the backdrop at all.</p>
 */
public final class MenuSurfacePolicy {
    public static final int OPAQUE = 0;
    public static final int BLUR = 1;
    public static final int GLASS = 2;

    private MenuSurfacePolicy() { }

    public static int surface(String effectiveStyle) {
        if (PanelPolicy.STYLE_GLASS.equals(effectiveStyle)
                || "liquid".equals(effectiveStyle)
                || "experimental".equals(effectiveStyle)) {
            return GLASS;
        }
        if (PanelPolicy.STYLE_BLUR.equals(effectiveStyle)) {
            return BLUR;
        }
        // "none" and the "material" fallback are both fully opaque.
        return OPAQUE;
    }
}
