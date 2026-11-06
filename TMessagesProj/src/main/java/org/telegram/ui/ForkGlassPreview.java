package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.fork.panels.ForkGlassSettings;
import org.telegram.messenger.fork.panels.GlassPolicy;
import org.telegram.messenger.fork.panels.GraphicsColorPolicy;
import org.telegram.ui.ActionBar.Theme;

/**
 * In-place preview for Experimental Glass 2.0. The backdrop is a synthetic
 * wallpaper so sliders can show transparency, diffusion, tint, edge and shadow
 * without capturing sensitive chat content.
 */
final class ForkGlassPreview extends FrameLayout {
    private final BackdropView backdrop;
    private final GlassView glass;

    ForkGlassPreview(Context context) {
        super(context);
        setClipChildren(false);
        backdrop = new BackdropView(context);
        addView(backdrop, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        glass = new GlassView(context);
        addView(glass, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setConfig(ForkGlassSettings.config());
    }

    void setConfig(ForkGlassSettings.Config config) {
        backdrop.setBlur(GlassPolicy.captureBlurAmount(config.blur, config.frost,
                config.adaptive && org.telegram.messenger.fork.panels.ForkPanelSettings.adaptiveEffects(),
                org.telegram.messenger.SharedConfig.deviceIsLow()));
        glass.setConfig(config);
    }

    private static final class BackdropView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shape = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF upperBar = new RectF();
        private final RectF lowerBar = new RectF();
        private LinearGradient gradient;
        private int blurAmount;

        BackdropView(Context context) {
            super(context);
            shape.setStyle(Paint.Style.FILL);
        }

        void setBlur(int amount) {
            if (blurAmount == amount) return;
            blurAmount = amount;
            if (Build.VERSION.SDK_INT >= 31) {
                float radius = AndroidUtilities.dpf2(Math.max(0.5f, amount * 0.42f));
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                        radius, radius, Shader.TileMode.CLAMP));
            }
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            int accent = ForkAppearanceSettingsActivity.accent(getContext());
            int deep = ColorUtils.blendARGB(accent, Color.BLACK, 0.55f);
            int light = ColorUtils.blendARGB(accent, Color.WHITE, 0.55f);
            gradient = new LinearGradient(0, 0, w, h,
                    new int[]{deep, accent, light}, null, Shader.TileMode.CLAMP);
            lowerBar.set(w * 0.08f, h * 0.62f, w * 0.46f, h * 0.70f);
            upperBar.set(w * 0.54f, h * 0.18f, w * 0.92f, h * 0.26f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (gradient == null) return;
            paint.setShader(gradient);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
            shape.setColor(0x55FFFFFF);
            canvas.drawCircle(getWidth() * 0.22f, getHeight() * 0.28f, getHeight() * 0.22f, shape);
            shape.setColor(0x33000000);
            canvas.drawCircle(getWidth() * 0.82f, getHeight() * 0.72f, getHeight() * 0.30f, shape);
            shape.setColor(0x44FFFFFF);
            canvas.drawRoundRect(lowerBar, dp(8), dp(8), shape);
            shape.setColor(0x33000000);
            canvas.drawRoundRect(upperBar, dp(8), dp(8), shape);
        }

        private float dp(float value) {
            return AndroidUtilities.dpf2(value);
        }
    }

    private static final class GlassView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint frost = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final RectF lowerRect = new RectF();
        private ForkGlassSettings.Config config;
        private float cachedRadius;
        private float cachedShadowRadius;
        private float cachedShadowDy;
        private float cachedStrokeWidth;
        private int cachedShadowAlpha;
        private int cachedFill;
        private int cachedFrost;
        private int cachedStrokeTop;
        private int cachedStrokeBottom;
        private int cachedText;
        private int cachedLine;

        GlassView(Context context) {
            super(context);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            stroke.setStyle(Paint.Style.STROKE);
            text.setTextSize(AndroidUtilities.dp(20));
            text.setFakeBoldText(true);
            line.setStrokeWidth(AndroidUtilities.dp(2));
        }

        void setConfig(ForkGlassSettings.Config config) {
            this.config = config;
            float opacity = GlassPolicy.opacity(config.transparency);
            int base = Theme.getColor(Theme.key_windowBackgroundWhite);
            if (config.customTintEnabled()) {
                Integer tint = GraphicsColorPolicy.parseHex(config.customTint);
                if (tint != null) base = ColorUtils.blendARGB(base, tint, 0.55f);
            }
            cachedFill = ColorUtils.setAlphaComponent(base, Math.round(255 * opacity));
            cachedFrost = ColorUtils.setAlphaComponent(
                    Theme.getColor(Theme.key_windowBackgroundWhite),
                    Math.round(28f * GlassPolicy.clampPercent(config.frost) / 100f));
            cachedStrokeTop = ColorUtils.setAlphaComponent(Color.WHITE,
                    GlassPolicy.strokeAlpha(config.edge, config.highlight, true));
            cachedStrokeBottom = ColorUtils.setAlphaComponent(Color.WHITE,
                    GlassPolicy.strokeAlpha(config.edge, config.highlight, false));
            cachedText = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            cachedLine = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3);
            cachedRadius = AndroidUtilities.dpf2(GlassPolicy.radiusDp(
                    org.telegram.messenger.fork.panels.ForkPanelSettings.radiusDp()));
            cachedShadowRadius = AndroidUtilities.dpf2(
                    GlassPolicy.shadowRadiusDp(config.shadow));
            cachedShadowDy = AndroidUtilities.dpf2(1.5f);
            cachedShadowAlpha = ColorUtils.setAlphaComponent(Color.BLACK,
                    GlassPolicy.shadowAlpha(config.shadow));
            cachedStrokeWidth = AndroidUtilities.dpf2(GlassPolicy.strokeWidthDp(config.edge));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (config == null) return;
            float inset = AndroidUtilities.dp(12);
            rect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            fill.setColor(cachedFill);
            fill.setShadowLayer(cachedShadowRadius, 0, cachedShadowDy, cachedShadowAlpha);
            canvas.drawRoundRect(rect, cachedRadius, cachedRadius, fill);
            fill.clearShadowLayer();

            frost.setColor(cachedFrost);
            canvas.drawRoundRect(rect, cachedRadius, cachedRadius, frost);

            stroke.setStrokeWidth(cachedStrokeWidth);
            stroke.setColor(cachedStrokeTop);
            canvas.drawRoundRect(rect, cachedRadius, cachedRadius, stroke);
            stroke.setColor(cachedStrokeBottom);
            lowerRect.set(rect);
            lowerRect.top = rect.centerY();
            canvas.save();
            canvas.clipRect(lowerRect);
            canvas.drawRoundRect(rect, cachedRadius, cachedRadius, stroke);
            canvas.restore();

            text.setColor(cachedText);
            canvas.drawText("Aa", rect.left + AndroidUtilities.dp(18),
                    rect.centerY() + AndroidUtilities.dp(7), text);
            line.setColor(cachedLine);
            canvas.drawLine(rect.left + AndroidUtilities.dp(54), rect.centerY() - AndroidUtilities.dp(4),
                    rect.right - AndroidUtilities.dp(18), rect.centerY() - AndroidUtilities.dp(4), line);
            canvas.drawLine(rect.left + AndroidUtilities.dp(54), rect.centerY() + AndroidUtilities.dp(9),
                    rect.right - AndroidUtilities.dp(54), rect.centerY() + AndroidUtilities.dp(9), line);
        }
    }
}
