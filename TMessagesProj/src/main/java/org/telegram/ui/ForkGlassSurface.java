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
        String eff = ForkGraphicsSurface.effectiveStyle();
        boolean experimental = org.telegram.messenger.fork.panels.PanelPolicy.isGlassStyle(eff);
        boolean isNone = org.telegram.messenger.fork.panels.PanelPolicy.STYLE_NONE.equals(eff);
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
        } else if (isNone) {
            // Use the theme's menu color without a glass tint or hairline.
            opacity = 1f;
            strokeTop = 0;
            strokeBottom = 0;
            shadow = 0x26000000;
            strokeWidth = 0;
            shadowRadius = AndroidUtilities.dpf2(5f);
        } else {
            opacity = 0.94f - 0.16f * strength;
            strokeTop = 0x44ffffff;
            strokeBottom = 0x22ffffff;
            shadow = 0x26000000;
            strokeWidth = AndroidUtilities.dpf2(2f / 3f);
            shadowRadius = AndroidUtilities.dpf2(5f);
        }
        final Integer tint = !isNone && config.customTintEnabled()
                ? GraphicsColorPolicy.parseHex(config.customTint) : null;
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
                .setBackgroundColor((r, dark) -> {
                    int base = Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, r);
                    if (tint != null) base = ColorUtils.blendARGB(base, tint, 0.55f);
                    return isNone ? ColorUtils.setAlphaComponent(base, 255) : Theme.multAlpha(base, opacity);
                })
                .setStrokeColorTop(strokeTop, strokeTop)
                .setStrokeColorBottom(strokeBottom, strokeBottom)
                .setShadowColor(shadow, shadow)
                .setShadowLayer(shadowRadius, 0, 0)
                .setStrokeWidth(strokeWidth, strokeWidth)
                .build();
    }

    /**
     * Keeps the delegate's theme colors, opting into glass decoration or an
     * opaque plate according to the current appearance mode.
     */
    public static BlurredBackgroundProvider wrap(final org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProvider delegate) {
        if (delegate == null) return null;
        // This wrapper is evaluated on every color refresh, so switching to or
        // from Experimental Glass takes effect without recreating the host view.
        return new BlurredBackgroundProvider() {
            private boolean experimental() {
                String eff = ForkGraphicsSurface.effectiveStyle();
                return org.telegram.messenger.fork.panels.PanelPolicy.isGlassStyle(eff);
            }

            @Override public int getShadowColor() {
                if (!experimental()) return delegate.getShadowColor();
                return colorWithAlpha(0xFF000000,
                        GlassPolicy.shadowAlpha(ForkGlassSettings.config().shadow));
            }

            @Override public int getBackgroundColor() {
                if (delegate instanceof org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed) {
                    ((org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed) delegate).updateColors();
                }
                if (ForkGraphicsSurface.noEffects()) return ColorUtils.setAlphaComponent(delegate.getBackgroundColor(), 255);
                if (!experimental()) return delegate.getBackgroundColor();
                ForkGlassSettings.Config config = ForkGlassSettings.config();
                int base = delegate.getBackgroundColor();
                Integer tint = config.customTintEnabled() ? GraphicsColorPolicy.parseHex(config.customTint) : null;
                if (tint != null) base = ColorUtils.blendARGB(base, tint, 0.55f);
                return Theme.multAlpha(base, GlassPolicy.opacity(config.transparency));
            }

            @Override public int getStrokeColorTop() {
                if (ForkGraphicsSurface.noEffects()) return 0;
                if (!experimental()) return delegate.getStrokeColorTop();
                ForkGlassSettings.Config config = ForkGlassSettings.config();
                return colorWithAlpha(0xFFFFFFFF,
                        GlassPolicy.strokeAlpha(config.edge, config.highlight, true));
            }

            @Override public int getStrokeColorBottom() {
                if (ForkGraphicsSurface.noEffects()) return 0;
                if (!experimental()) return delegate.getStrokeColorBottom();
                ForkGlassSettings.Config config = ForkGlassSettings.config();
                return colorWithAlpha(0xFFFFFFFF,
                        GlassPolicy.strokeAlpha(config.edge, config.highlight, false));
            }

            @Override public int getOutlineColor() { return ForkGraphicsSurface.noEffects() || experimental() ? 0 : delegate.getOutlineColor(); }
            @Override public int getOutlineShadowColor() { return ForkGraphicsSurface.noEffects() || experimental() ? 0 : delegate.getOutlineShadowColor(); }
            @Override public float getOutlineWidth() { return ForkGraphicsSurface.noEffects() || experimental() ? 0 : delegate.getOutlineWidth(); }

            @Override public float getStrokeWidthTop() {
                if (!experimental()) return (delegate instanceof BlurredBackgroundProvider ? ((BlurredBackgroundProvider) delegate).getStrokeWidthTop() : 0);
                return AndroidUtilities.dpf2(GlassPolicy.strokeWidthDp(
                        ForkGlassSettings.config().edge));
            }

            @Override public float getStrokeWidthBottom() {
                if (!experimental()) return (delegate instanceof BlurredBackgroundProvider ? ((BlurredBackgroundProvider) delegate).getStrokeWidthBottom() : 0);
                return AndroidUtilities.dpf2(GlassPolicy.strokeWidthDp(
                        ForkGlassSettings.config().edge));
            }

            @Override public float getShadowRadius() {
                if (!experimental()) return (delegate instanceof BlurredBackgroundProvider ? ((BlurredBackgroundProvider) delegate).getShadowRadius() : Math.max(0, delegate.getShadowRadiusOverride()));
                return AndroidUtilities.dpf2(GlassPolicy.shadowRadiusDp(
                        ForkGlassSettings.config().shadow));
            }

            @Override public float getShadowDx() {
                return experimental() ? 0 : (delegate instanceof BlurredBackgroundProvider ? ((BlurredBackgroundProvider) delegate).getShadowDx() : 0);
            }

            @Override public float getShadowDy() {
                return experimental() ? 0 : (delegate instanceof BlurredBackgroundProvider ? ((BlurredBackgroundProvider) delegate).getShadowDy() : Math.max(0, delegate.getShadowDyOverride()));
            }
        };
    }

    /** Blur intensity for one-shot popup captures; keeps the caller's base when not experimental. */
    public static int captureBlurAmount(int base) {
        String eff = ForkGraphicsSurface.effectiveStyle();
        if (PanelPolicy.STYLE_NONE.equals(eff)) return 0;
        if (!org.telegram.messenger.fork.panels.PanelPolicy.isGlassStyle(eff)) return base;
        return org.telegram.messenger.fork.performance.PerformancePolicy.captureDownscale(
                ForkGlassSettings.captureBlurAmount(base),
                org.telegram.messenger.fork.performance.PerformanceSettings.effectiveTier());
    }

    public static float backdropSaturation(float base) {
        String eff = ForkGraphicsSurface.effectiveStyle();
        if (!org.telegram.messenger.fork.panels.PanelPolicy.isGlassStyle(eff)) return base;
        return ForkGlassSettings.backdropSaturation(base);
    }

    /**
     * Applies the configured saturation/diffusion to a captured backdrop.
     * Returns the input bitmap when no processing is needed.
     */
    public static Bitmap processBackdrop(Bitmap bitmap) {
        String eff = ForkGraphicsSurface.effectiveStyle();
        if (bitmap == null || !org.telegram.messenger.fork.panels.PanelPolicy.isGlassStyle(eff)) return bitmap;
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
