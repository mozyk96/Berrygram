package org.telegram.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.fork.panels.ForkGlassSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.GlassPolicy;
import org.telegram.messenger.fork.panels.GraphicsColorPolicy;
import org.telegram.messenger.fork.panels.PanelPolicy;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;

/**
 * Fork-owned popup surfaces.
 *
 * <p>Experimental Glass 2.0 uses the existing Telegram blur3 backdrop and adds
 * independent tint opacity, blur/diffusion, saturation, edge highlight and
 * shadow parameters. Telegram-owned providers are never modified.</p>
 */
public final class ForkGlassSurface {
    private ForkGlassSurface() { }

    public static BlurredBackgroundProvider provider(Theme.ResourcesProvider resourcesProvider) {
        return provider(resourcesProvider, ForkGlassSettings.config());
    }

    /** Builds a provider from an explicit snapshot, used by the live preview. */
    public static BlurredBackgroundProvider provider(Theme.ResourcesProvider resourcesProvider,
                                                     ForkGlassSettings.Config config) {
        String style = ForkPanelSettings.style();
        boolean experimental = "experimental".equals(style)
                && "experimental".equals(ForkGraphicsSurface.effectiveStyle());
        float strength = ForkPanelSettings.effectStrength() / 100f;
        final float opacity;
        final int strokeTop, strokeBottom, shadow;
        final float strokeWidth, shadowRadius;
        if (experimental) {
            opacity = GlassPolicy.opacity(config.transparency);
            strokeTop = colorWithAlpha(0xFFFFFFFF, GlassPolicy.strokeAlpha(config.edge, config.highlight, true));
            strokeBottom = colorWithAlpha(0xFFFFFFFF, GlassPolicy.strokeAlpha(config.edge, config.highlight, false));
            shadow = colorWithAlpha(0xFF000000, GlassPolicy.shadowAlpha(config.shadow));
            strokeWidth = AndroidUtilities.dpf2(GlassPolicy.strokeWidthDp(config.edge));
            shadowRadius = AndroidUtilities.dpf2(GlassPolicy.shadowRadiusDp(config.shadow));
        } else {
            opacity = 0.94f - 0.16f * strength;
            strokeTop = 0x44ffffff;
            strokeBottom = 0x22ffffff;
            shadow = 0x26000000;
            strokeWidth = AndroidUtilities.dpf2(2f / 3f);
            shadowRadius = AndroidUtilities.dpf2(5f);
        }
        final Integer tint = config.customTintEnabled()
                ? GraphicsColorPolicy.parseHex(config.customTint) : null;
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
                .setBackgroundColor((r, dark) -> {
                    int base = Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, r);
                    if (tint != null) base = ColorUtils.blendARGB(base, tint, 0.55f);
                    return Theme.multAlpha(base, opacity);
                })
                .setStrokeColorTop(strokeTop, strokeTop)
                .setStrokeColorBottom(strokeBottom, strokeBottom)
                .setShadowColor(shadow, shadow)
                .setShadowLayer(shadowRadius, 0, 0)
                .setStrokeWidth(strokeWidth, strokeWidth)
                .build();
    }

    /**
     * Adapts an existing Telegram provider (mini dock, reaction panel) without
     * changing its base color. Outside Experimental Glass the delegate is
     * returned untouched.
     */
    public static BlurredBackgroundProvider wrap(final BlurredBackgroundProvider delegate) {
        if (delegate == null || !"experimental".equals(ForkPanelSettings.style())
                || !"experimental".equals(ForkGraphicsSurface.effectiveStyle())) return delegate;
        final ForkGlassSettings.Config config = ForkGlassSettings.config();
        final float opacity = GlassPolicy.opacity(config.transparency);
        final int strokeTop = colorWithAlpha(0xFFFFFFFF, GlassPolicy.strokeAlpha(config.edge, config.highlight, true));
        final int strokeBottom = colorWithAlpha(0xFFFFFFFF, GlassPolicy.strokeAlpha(config.edge, config.highlight, false));
        final int shadow = colorWithAlpha(0xFF000000, GlassPolicy.shadowAlpha(config.shadow));
        final float strokeWidth = AndroidUtilities.dpf2(GlassPolicy.strokeWidthDp(config.edge));
        final float shadowRadius = AndroidUtilities.dpf2(GlassPolicy.shadowRadiusDp(config.shadow));
        return new BlurredBackgroundProvider() {
            @Override public int getShadowColor() { return shadow; }
            @Override public int getBackgroundColor() { return Theme.multAlpha(delegate.getBackgroundColor(), opacity); }
            @Override public int getStrokeColorTop() { return strokeTop; }
            @Override public int getStrokeColorBottom() { return strokeBottom; }
            @Override public float getStrokeWidthTop() { return strokeWidth; }
            @Override public float getStrokeWidthBottom() { return strokeWidth; }
            @Override public float getShadowRadius() { return shadowRadius; }
            @Override public float getShadowDx() { return 0; }
            @Override public float getShadowDy() { return 0; }
        };
    }

    /** Blur intensity for one-shot popup captures; keeps the caller's base when not experimental. */
    public static int captureBlurAmount(int base) {
        if (!"experimental".equals(ForkGraphicsSurface.effectiveStyle())) return base;
        return ForkGlassSettings.captureBlurAmount(base);
    }

    public static float backdropSaturation(float base) {
        if (!"experimental".equals(ForkGraphicsSurface.effectiveStyle())) return base;
        return ForkGlassSettings.backdropSaturation(base);
    }

    /**
     * Applies the configured saturation/diffusion to a captured backdrop.
     * Returns the input bitmap when no processing is needed.
     */
    public static Bitmap processBackdrop(Bitmap bitmap) {
        if (bitmap == null || !"experimental".equals(ForkGraphicsSurface.effectiveStyle())) return bitmap;
        ForkGlassSettings.Config config = ForkGlassSettings.config();
        float saturation = GlassPolicy.saturation(config.saturation, config.frost);
        if (Math.abs(saturation - 1f) < 0.03f) return bitmap;
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(saturation);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        new android.graphics.Canvas(result).drawBitmap(bitmap, 0, 0, paint);
        bitmap.recycle();
        return result;
    }

    private static int colorWithAlpha(int color, int alpha) {
        return ColorUtils.setAlphaComponent(color, Math.max(0, Math.min(255, alpha)));
    }
}
