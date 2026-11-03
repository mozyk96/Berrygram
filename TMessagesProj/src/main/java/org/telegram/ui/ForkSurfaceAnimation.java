package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.fork.panels.ForkAnimationSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;

/** Finite source-to-surface reveal. Restores all transient render properties on cancellation. */
public final class ForkSurfaceAnimation {
    private ForkSurfaceAnimation() { }

    public static boolean enabled() {
        return ForkAnimationSettings.fluidPopupsEnabled()
                && AndroidUtilities.getAnimatorDurationScale() > 0f
                && !ForkGraphicsSurface.effectsSuppressed();
    }

    public static AnimatorSet create(View view, Rect sourceOnScreen, boolean opening, boolean fromBottom) {
        int width = view.getWidth() > 0 ? view.getWidth() : view.getMeasuredWidth();
        int height = view.getHeight() > 0 ? view.getHeight() : view.getMeasuredHeight();
        if (!enabled() || width <= 0 || height <= 0) return null;
        boolean liquid = "liquid".equals(ForkGraphicsSurface.effectiveStyle());
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float sourceX = sourceOnScreen == null ? width : sourceOnScreen.exactCenterX() - location[0];
        float sourceY = sourceOnScreen == null ? (fromBottom ? height : 0) : sourceOnScreen.exactCenterY() - location[1];
        float seedWidth = Math.min(width, sourceOnScreen == null ? AndroidUtilities.dp(36) : Math.max(AndroidUtilities.dp(24), Math.min(AndroidUtilities.dp(48), sourceOnScreen.width())));
        float seedHeight = Math.min(height, sourceOnScreen == null ? AndroidUtilities.dp(36) : Math.max(AndroidUtilities.dp(24), Math.min(AndroidUtilities.dp(48), sourceOnScreen.height())));
        float left = Math.max(0, Math.min(width - seedWidth, sourceX - seedWidth / 2));
        float top = Math.max(0, Math.min(height - seedHeight, sourceY - seedHeight / 2));
        RectF seed = new RectF(left, top, left + seedWidth, top + seedHeight);
        RectF bounds = new RectF(seed);
        final float originalScaleX = view.getScaleX(), originalScaleY = view.getScaleY();
        final float originalX = view.getTranslationX(), originalY = view.getTranslationY();
        final float originalPivotX = view.getPivotX(), originalPivotY = view.getPivotY();
        final float startScaleX = seedWidth / width, startScaleY = seedHeight / height;
        view.setPivotX(width / 2f);
        view.setPivotY(height / 2f);
        ViewOutlineProvider originalProvider = view.getOutlineProvider();
        boolean originalClip = view.getClipToOutline();
        float finalRadius = AndroidUtilities.dp(ForkPanelSettings.radiusDp());
        float initialRadius = Math.min(seedWidth, seedHeight) / 2;
        float[] radius = { opening ? initialRadius : finalRadius };
        if (!opening) bounds.set(0, 0, width, height);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline outline) {
                outline.setRoundRect(Math.round(bounds.left), Math.round(bounds.top),
                        Math.round(bounds.right), Math.round(bounds.bottom), radius[0]);
            }
        });
        view.setClipToOutline(true);
        view.invalidateOutline();
        ValueAnimator progress = ValueAnimator.ofFloat(opening ? 0f : 1f, opening ? 1f : 0f);
        progress.addUpdateListener(animation -> {
            float p = (float) animation.getAnimatedValue();
            // Transform the surface itself, including its background, from the real anchor.
            // The outline adds rounded expansion rather than revealing an already full-size menu.
            float stretch = liquid ? 0.035f * (float) Math.sin(Math.PI * p) : 0f;
            view.setScaleX(originalScaleX * (startScaleX + (1 - startScaleX) * p + stretch));
            view.setScaleY(originalScaleY * (startScaleY + (1 - startScaleY) * p));
            view.setTranslationX(originalX + (sourceX - width / 2f) * (1 - p));
            view.setTranslationY(originalY + (sourceY - height / 2f) * (1 - p));
            // Liquid stretches horizontally first, then settles; Blur has a quieter uniform reveal.
            float horizontal = liquid ? Math.min(1f, p + 0.16f * (float) Math.sin(Math.PI * p)) : p;
            bounds.set(seed.left * (1 - horizontal), seed.top * (1 - p),
                    seed.right + (width - seed.right) * horizontal,
                    seed.bottom + (height - seed.bottom) * p);
            radius[0] = initialRadius + (finalRadius - initialRadius) * p;
            view.invalidateOutline();
        });
        AnimatorSet result = new AnimatorSet();
        result.playTogether(progress);
        result.setDuration(opening ? (liquid ? 300 : 220) : (liquid ? 200 : 160));
        result.setInterpolator(new DecelerateInterpolator(liquid ? 2f : 1.5f));
        result.addListener(new AnimatorListenerAdapter() {
            private boolean restored;
            private void restore() {
                if (restored) return;
                restored = true;
                view.setScaleX(originalScaleX);
                view.setScaleY(originalScaleY);
                view.setTranslationX(originalX);
                view.setTranslationY(originalY);
                view.setPivotX(originalPivotX);
                view.setPivotY(originalPivotY);
                view.setOutlineProvider(originalProvider);
                view.setClipToOutline(originalClip);
                view.invalidateOutline();
            }
            @Override public void onAnimationCancel(Animator animation) { restore(); }
            @Override public void onAnimationEnd(Animator animation) { restore(); }
        });
        return result;
    }
}
