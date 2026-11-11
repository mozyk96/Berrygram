package org.telegram.ui.Components.blur3.drawable;

import org.telegram.utils.glass.source.GlassSource;
import org.telegram.utils.glass.source.GlassSourceWrapped;

/** Compatibility wrapper for fork-owned popup surfaces. */
public class BlurredBackgroundDrawableSource extends BlurredBackgroundDrawable {
    private final GlassSourceWrapped wrapped;

    public BlurredBackgroundDrawableSource(GlassSource source) {
        this(new GlassSourceWrapped(), source);
    }

    private BlurredBackgroundDrawableSource(GlassSourceWrapped wrapped, GlassSource source) {
        super(wrapped);
        this.wrapped = wrapped;
        wrapped.setSource(source);
    }

    public void setSource(GlassSource source) {
        wrapped.setSource(source);
        invalidateSelf();
    }

    @Override public GlassSource getSource() {
        return wrapped.getSource();
    }
}
