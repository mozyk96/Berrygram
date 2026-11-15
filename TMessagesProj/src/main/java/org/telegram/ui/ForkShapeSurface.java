package org.telegram.ui;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.NinePatchDrawable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.PanelPolicy;
import org.telegram.ui.Components.blur3.utils.NinePatchBuilder;

/** Shared rounded modal geometry, preserving the stock content/shadow insets. */
public final class ForkShapeSurface {
    private ForkShapeSurface() { }

    public static int largeRadiusDp() {
        return PanelPolicy.largeSurfaceRadiusDp(ForkPanelSettings.radiusDp());
    }

    public static Drawable modalBackground(Drawable stock, boolean bottomSheet) {
        Rect original = new Rect();
        stock.getPadding(original);
        float radius = AndroidUtilities.dp(largeRadiusDp());
        float bottom = bottomSheet ? 0 : radius;
        NinePatchDrawable rounded = NinePatchBuilder.createNinePatch(null, Color.WHITE,
                new float[] {radius, radius, radius, radius, bottom, bottom, bottom, bottom},
                AndroidUtilities.dp(2), 0x24000000, 0, AndroidUtilities.dp(1), Color.WHITE);
        Rect generated = new Rect();
        rounded.getPadding(generated);
        return new InsetDrawable(rounded, original.left - generated.left, original.top - generated.top,
                original.right - generated.right, original.bottom - generated.bottom);
    }
}
