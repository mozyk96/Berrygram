/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.ActionBar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationNotificationsLocker;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.utils.ViewOutlineProviderImpl;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class ActionBarPopupWindow extends PopupWindow {

    private static Method layoutInScreenMethod;
    private static final Field superListenerField;
    private static DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    private AnimatorSet windowAnimatorSet;
    private boolean animationEnabled = true;
    private int dismissAnimationDuration = 150;
    private boolean isClosingAnimated;
    private int currentAccount = UserConfig.selectedAccount;
    private boolean pauseNotifications;
    private long outEmptyTime = -1;
    private boolean scaleOut;
    private Rect forkAnimationSource;
    private boolean forkFluidShown;
    private Integer forkOriginalAnimationStyle;
    private ViewTreeObserver forkPreDrawObserver;
    private ViewTreeObserver.OnPreDrawListener forkPreDrawListener;

    /** Explicit source for absolute-positioned menus (the showAtLocation parent is only a container). */
    public void setAnimationSource(View source) {
        forkAnimationSource = null;
        if (source != null) {
            int[] location = new int[2];
            source.getLocationOnScreen(location);
            forkAnimationSource = new Rect(location[0], location[1],
                    location[0] + source.getWidth(), location[1] + source.getHeight());
        }
    }

    private ActionBarPopupWindowLayout forkPopupLayout() {
        View root = getContentView();
        if (root instanceof ActionBarPopupWindowLayout) return (ActionBarPopupWindowLayout) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (group.getChildAt(i) instanceof ActionBarPopupWindowLayout) {
                    return (ActionBarPopupWindowLayout) group.getChildAt(i);
                }
            }
        }
        return null;
    }

    private boolean startForkFluidAnimation() {
        ActionBarPopupWindowLayout content = forkPopupLayout();
        if (!animationEnabled || content == null || !org.telegram.ui.ForkSurfaceAnimation.enabled()) return false;
        if (windowAnimatorSet != null || forkFluidShown) return true;
        if (!content.isLaidOut() || !content.isAttachedToWindow()) return true;
        AnimatorSet animation = org.telegram.ui.ForkSurfaceAnimation.create(content, forkAnimationSource, true, content.shownFromBottom);
        if (animation == null) return false;
        forkFluidShown = true;
        windowAnimatorSet = animation;
        animation.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animator) {
                if (windowAnimatorSet == animator) windowAnimatorSet = null;
            }
        });
        content.setBackScaleX(1f);
        content.setBackScaleY(1f);
        content.setBackAlpha(255);
        for (int i = 0; i < content.getItemsCount(); i++) {
            View item = content.getItemAt(i);
            item.setAlpha(item.isEnabled() ? 1f : 0.5f);
        }
        animation.start();
        return true;
    }

    private void clearForkPreDraw() {
        if (forkPreDrawObserver != null && forkPreDrawObserver.isAlive() && forkPreDrawListener != null) {
            forkPreDrawObserver.removeOnPreDrawListener(forkPreDrawListener);
        }
        forkPreDrawObserver = null;
        forkPreDrawListener = null;
    }

    private void prepareForkFluidAnimation() {
        clearForkPreDraw();
        // Clear ownership before cancel: Android also calls onAnimationEnd on cancellation.
        AnimatorSet previous = windowAnimatorSet;
        windowAnimatorSet = null;
        if (previous != null) previous.cancel();
        if (isClosingAnimated && pauseNotifications) notificationsLocker.unlock();
        isClosingAnimated = false;
        forkFluidShown = false;
        if (animationEnabled && forkPopupLayout() != null && org.telegram.ui.ForkSurfaceAnimation.enabled()) {
            if (forkOriginalAnimationStyle == null) forkOriginalAnimationStyle = getAnimationStyle();
            setAnimationStyle(0);
        } else if (forkOriginalAnimationStyle != null) {
            setAnimationStyle(forkOriginalAnimationStyle);
            forkOriginalAnimationStyle = null;
        }
    }

    private void postForkFluidAnimation() {
        clearForkPreDraw();
        View root = getContentView();
        if (root == null || !animationEnabled || !org.telegram.ui.ForkSurfaceAnimation.enabled()) return;
        forkPreDrawObserver = root.getViewTreeObserver();
        forkPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                clearForkPreDraw();
                if (isShowing() && !isClosingAnimated) startForkFluidAnimation();
                return true;
            }
        };
        forkPreDrawObserver.addOnPreDrawListener(forkPreDrawListener);
    }

    static {
        Field f = null;
        try {
            f = PopupWindow.class.getDeclaredField("mOnScrollChangedListener");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            /* ignored */
        }
        superListenerField = f;
    }

    private static final ViewTreeObserver.OnScrollChangedListener NOP = () -> {
        /* do nothing */
    };

    private ViewTreeObserver.OnScrollChangedListener mSuperScrollListener;
    private ViewTreeObserver mViewTreeObserver;
    private AnimationNotificationsLocker notificationsLocker = new AnimationNotificationsLocker();

    public void setScaleOut(boolean b) {
        scaleOut = b;
    }

    public interface OnDispatchKeyEventListener {
        void onDispatchKeyEvent(KeyEvent keyEvent);
    }

    public static class ActionBarPopupWindowLayout extends FrameLayout {
        public final static int FLAG_USE_SWIPEBACK = 1;
        public final static int FLAG_SHOWN_FROM_BOTTOM = 2;
        public final static int FLAG_DONT_USE_SCROLLVIEW = 4;
        public boolean updateAnimation;
        public boolean clipChildren;
        public boolean swipeBackGravityRight;
        public boolean swipeBackGravityBottom;

        private OnDispatchKeyEventListener mOnDispatchKeyEventListener;
        private float backScaleX = 1;
        private float backScaleY = 1;
        private boolean startAnimationPending = false;
        private int backAlpha = 255;
        private int lastStartedChild = 0;
        public boolean shownFromBottom;
        private boolean animationEnabled = true;
        private ArrayList<AnimatorSet> itemAnimators;
        private HashMap<View, Integer> positions = new HashMap<>();
        private int gapStartY = -1000000;
        private int gapEndY = -1000000;
        private final Rect bgPaddings = new Rect();
        private onSizeChangedListener onSizeChangedListener;
        private float reactionsEnterProgress = 1f;

        private PopupSwipeBackLayout swipeBackLayout;
        @Nullable
        private ScrollView scrollView;
        protected LinearLayout linearLayout;

        private int backgroundColor = Color.WHITE;
        protected Drawable backgroundDrawable;
        private boolean forkGlassBackground;
        private boolean forkStyleEligible;
        private Drawable forkStockBackground;
        private Drawable forkExperimentalBackground;
        private Drawable forkMaterialBackground;
        private boolean forkMaterialApplied;
        private int forkMaterialColor;
        private int forkMaterialRadius = -1;
        private int forkExperimentalRadius = -1;
        private String forkResolvedStyle;

        private boolean fitItems;
        private final Theme.ResourcesProvider resourcesProvider;
        private View topView;
        protected ActionBarPopupWindow window;

        public int subtractBackgroundHeight;
        Rect rect;

        public Rect getPadding() {
            return bgPaddings;
        }

        public ActionBarPopupWindowLayout(Context context) {
            this(context, null);
        }

        public ActionBarPopupWindowLayout(Context context, Theme.ResourcesProvider resourcesProvider) {
            this(context, R.drawable.popup_fixed_alert2, resourcesProvider);
        }

        public ActionBarPopupWindowLayout(Context context, int resId, Theme.ResourcesProvider resourcesProvider) {
            this(context, resId, resourcesProvider, 0);
        }

        public ActionBarPopupWindowLayout(Context context, int resId, Theme.ResourcesProvider resourcesProvider, int flags) {
            super(context);
            this.resourcesProvider = resourcesProvider;

            if (resId != 0) {
                // Menus in the action bar use alert4, while chat/context menus also use
                // alert and alert2. Leave custom/transparent popup surfaces untouched.
                forkStyleEligible = resId == R.drawable.popup_fixed_alert || resId == R.drawable.popup_fixed_alert2 || resId == R.drawable.popup_fixed_alert3 || resId == R.drawable.popup_fixed_alert4;
                forkStockBackground = getResources().getDrawable(resId).mutate();
                if (forkStyleEligible) refreshForkStyle();
                forkGlassBackground = forkStyleEligible && !"material".equals(forkResolvedStyle);
                if (forkGlassBackground) {
                    forkExperimentalRadius = org.telegram.messenger.fork.panels.ForkPanelSettings.radiusDp();
                    forkExperimentalBackground = Theme.createRoundRectDrawable(dp(forkExperimentalRadius), Color.WHITE);
                    backgroundDrawable = forkExperimentalBackground;
                } else {
                    forkMaterialApplied = forkStyleEligible && "material".equals(forkResolvedStyle);
                    if (forkMaterialApplied) {
                        forkMaterialRadius = org.telegram.messenger.fork.panels.ForkPanelSettings.radiusDp();
                        forkMaterialBackground = Theme.createRoundRectDrawable(dp(forkMaterialRadius), Color.WHITE);
                    }
                    backgroundDrawable = forkMaterialApplied ? forkMaterialBackground : forkStockBackground;
                }
                setPadding(dp(8), dp(8), dp(8), dp(8));
            }
            if (backgroundDrawable != null) {
                backgroundDrawable.getPadding(bgPaddings);
                ThemeBind.backgroundColor(this, Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider);
            }


            setWillNotDraw(false);

            if ((flags & FLAG_SHOWN_FROM_BOTTOM) > 0) {
                shownFromBottom = true;
            }

            if ((flags & FLAG_USE_SWIPEBACK) > 0) {
                swipeBackLayout = new PopupSwipeBackLayout(context, resourcesProvider);
                addView(swipeBackLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }

            if ((flags & FLAG_DONT_USE_SCROLLVIEW) == 0) {
                try {
                    scrollView = new ScrollView(context);
                    scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            invalidate();
                        }
                    });
                    scrollView.setVerticalScrollBarEnabled(false);
                    if (swipeBackLayout != null) {
                        swipeBackLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, shownFromBottom ? Gravity.BOTTOM : Gravity.TOP));
                    } else {
                        addView(scrollView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }

            linearLayout = new LinearLayout(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    if (fitItems) {
                        int maxWidth = 0;
                        int fixWidth = 0;
                        gapStartY = -1000000;
                        gapEndY = -1000000;
                        ArrayList<View> viewsToFix = null;
                        for (int a = 0, N = getChildCount(); a < N; a++) {
                            View view = getChildAt(a);
                            if (view.getVisibility() == GONE) {
                                continue;
                            }
                            Object tag = view.getTag(R.id.width_tag);
                            Object tag2 = view.getTag(R.id.object_tag);
                            Object fitToWidth = view.getTag(R.id.fit_width_tag);
                            if (tag != null) {
                                view.getLayoutParams().width = LayoutHelper.WRAP_CONTENT;
                            }
                            measureChildWithMargins(view, widthMeasureSpec, 0, heightMeasureSpec, 0);
                            if (fitToWidth != null) {

                            } else if (!(tag instanceof Integer) && tag2 == null) {
                                maxWidth = Math.max(maxWidth, view.getMeasuredWidth());
                                continue;
                            } else if (tag instanceof Integer) {
                                fixWidth = Math.max((Integer) tag, view.getMeasuredWidth());
                                gapStartY = view.getMeasuredHeight();
                                gapEndY = gapStartY + dp(6);
                            }
                            if (viewsToFix == null) {
                                viewsToFix = new ArrayList<>();
                            }
                            viewsToFix.add(view);
                        }
                        if (viewsToFix != null) {
                            for (int a = 0, N = viewsToFix.size(); a < N; a++) {
                                viewsToFix.get(a).getLayoutParams().width = Math.max(maxWidth, fixWidth);
                            }
                        }
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }

                @Override
                protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                    if (child instanceof GapView && backgroundDrawable != null) {
                        return false;
                    }
                    return super.drawChild(canvas, child, drawingTime);
                }
            };
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            if (scrollView != null) {
                scrollView.addView(linearLayout, new ScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else if (swipeBackLayout != null) {
                swipeBackLayout.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, shownFromBottom ? Gravity.BOTTOM : Gravity.TOP));
            } else {
                addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }
        }

        @Nullable
        public PopupSwipeBackLayout getSwipeBack() {
            return swipeBackLayout;
        }

        public int addViewToSwipeBack(View v) {
            final FrameLayout.LayoutParams params = LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, shownFromBottom ? Gravity.BOTTOM : Gravity.TOP);
            if (swipeBackLayout == null) {
                // Defensive fallback for callers that did not request FLAG_USE_SWIPEBACK.
                addView(v, params);
                return getChildCount() - 1;
            }
            swipeBackLayout.addView(v, params);
            return swipeBackLayout.getChildCount() - 1;
        }

        public void setFitItems(boolean value) {
            fitItems = value;
        }

        public void setShownFromBottom(boolean value) {
            shownFromBottom = value;
        }

        public void setDispatchKeyEventListener(OnDispatchKeyEventListener listener) {
            mOnDispatchKeyEventListener = listener;
        }

        public int getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(int color) {
            if (backgroundColor != color && backgroundDrawable != null) {
                backgroundDrawable.setColorFilter(new PorterDuffColorFilter(backgroundColor = color, PorterDuff.Mode.MULTIPLY));
                forkMaterialColor = 0;
            }
        }

        @Keep
        public void setBackAlpha(int value) {
            if (backAlpha != value) {
                invalidate();
            }
            backAlpha = value;
        }

        @Keep
        public int getBackAlpha() {
            return backAlpha;
        }

        @Keep
        public void setBackScaleX(float value) {
            if (backScaleX != value) {
                backScaleX = value;
                invalidate();
                if (onSizeChangedListener != null) {
                    onSizeChangedListener.onSizeChanged();
                }
            }
        }

        @Keep
        public void setBackScaleY(float value) {
            if (backScaleY != value) {
                backScaleY = value;
                if (animationEnabled && updateAnimation) {
                    int height = getMeasuredHeight() - dp(16);
                    if (shownFromBottom) {
                        for (int a = lastStartedChild; a >= 0; a--) {
                            View child = getItemAt(a);
                            if (child == null || child.getVisibility() != VISIBLE || child instanceof GapView) {
                                continue;
                            }
                            Integer position = positions.get(child);
                            if (position != null && height - (position * dp(48) + dp(32)) > value * height) {
                                break;
                            }
                            lastStartedChild = a - 1;
                            startChildAnimation(child);
                        }
                    } else {
                        int count = getItemsCount();
                        int h = 0;
                        for (int a = 0; a < count; a++) {
                            View child = getItemAt(a);
                            if (child.getVisibility() != VISIBLE) {
                                continue;
                            }
                            h += child.getMeasuredHeight();
                            if (a < lastStartedChild) {
                                continue;
                            }
                            Integer position = positions.get(child);
                            if (position != null && h - dp(24) > value * height) {
                                break;
                            }
                            lastStartedChild = a + 1;
                            startChildAnimation(child);
                        }
                    }
                }
                invalidate();
                if (onSizeChangedListener != null) {
                    onSizeChangedListener.onSizeChanged();
                }
            }
        }

        @Override
        public void setBackgroundDrawable(Drawable drawable) {
            backgroundColor = Color.WHITE;
            backgroundDrawable = drawable;
            if (backgroundDrawable != null) {
                backgroundDrawable.getPadding(bgPaddings);
            } else {
                bgPaddings.setEmpty();
            }
        }

        private void startChildAnimation(View child) {
            if (animationEnabled) {
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(child, View.ALPHA, 0f, child.isEnabled() ? 1f : 0.5f),
                        ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, dp(shownFromBottom ? 6 : -6), 0));
                animatorSet.setDuration(180);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        itemAnimators.remove(animatorSet);

                        if (child instanceof ActionBarMenuSubItem) {
                            ((ActionBarMenuSubItem) child).onItemShown();
                        }
                    }
                });
                animatorSet.setInterpolator(decelerateInterpolator);
                animatorSet.start();
                if (itemAnimators == null) {
                    itemAnimators = new ArrayList<>();
                }
                itemAnimators.add(animatorSet);
            }
        }

        public void setAnimationEnabled(boolean value) {
            animationEnabled = value;
        }

        @Override
        public void addView(View child) {
            linearLayout.addView(child);
        }

        public void addView(View child, LinearLayout.LayoutParams layoutParams) {
            linearLayout.addView(child, layoutParams);
        }

        public int getViewsCount() {
            return linearLayout.getChildCount();
        }

        public int precalculateHeight() {
            int MOST_SPEC = View.MeasureSpec.makeMeasureSpec(dp(1000), View.MeasureSpec.AT_MOST);
            linearLayout.measure(MOST_SPEC, MOST_SPEC);
            return linearLayout.getMeasuredHeight();
        }

        public void removeInnerViews() {
            linearLayout.removeAllViews();
        }

        public float getBackScaleX() {
            return backScaleX;
        }

        public float getBackScaleY() {
            return backScaleY;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (mOnDispatchKeyEventListener != null) {
                mOnDispatchKeyEventListener.onDispatchKeyEvent(event);
            }
            return super.dispatchKeyEvent(event);
        }

        private void refreshForkStyle() {
            forkResolvedStyle = org.telegram.ui.ForkGraphicsSurface.effectiveStyle();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (forkStyleEligible) refreshForkStyle();
        }

        Path path;

        @Override
        protected void dispatchDraw(Canvas canvas) {
            // Action-bar popup layouts are cached, so refresh the surface when the
            // preference or power/device policy changes instead of requiring the screen to reopen.
            if (forkStyleEligible) refreshForkStyle();
            boolean glass = forkStyleEligible && !"material".equals(forkResolvedStyle);
            boolean material = forkStyleEligible && !glass;
            int radius = org.telegram.messenger.fork.panels.ForkPanelSettings.radiusDp();
            boolean ownsSurface = backgroundDrawable == forkExperimentalBackground || backgroundDrawable == forkStockBackground || backgroundDrawable == forkMaterialBackground;
            if (forkStyleEligible && ownsSurface && (glass != forkGlassBackground || material != forkMaterialApplied || (glass && radius != forkExperimentalRadius) || (material && radius != forkMaterialRadius))) {
                forkGlassBackground = glass;
                forkMaterialApplied = material;
                if (glass && (forkExperimentalBackground == null || forkExperimentalRadius != radius)) {
                    forkExperimentalRadius = radius;
                    forkExperimentalBackground = Theme.createRoundRectDrawable(dp(radius), Color.WHITE);
                }
                if (material && (forkMaterialBackground == null || forkMaterialRadius != radius)) {
                    forkMaterialRadius = radius;
                    forkMaterialBackground = Theme.createRoundRectDrawable(dp(radius), Color.WHITE);
                }
                backgroundDrawable = glass ? forkExperimentalBackground : material ? forkMaterialBackground : forkStockBackground;
                forkMaterialColor = 0;
                backgroundDrawable.getPadding(bgPaddings);
                backgroundDrawable.setColorFilter(new PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.MULTIPLY));
            }
            if (material && backgroundDrawable == forkMaterialBackground) {
                int tint = org.telegram.ui.ForkGraphicsSurface.materialColor(getContext(), backgroundColor);
                if (tint != forkMaterialColor) {
                    forkMaterialColor = tint;
                    backgroundDrawable.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.MULTIPLY));
                }
            }
            if (swipeBackGravityRight) {
                setTranslationX(getMeasuredWidth() * (1f - backScaleX));
                if (topView != null) {
                    topView.setTranslationX(getMeasuredWidth() * (1f - backScaleX));
                    topView.setAlpha(1f - swipeBackLayout.transitionProgress);
                    float h = topView.getMeasuredHeight() - dp(16);
                    float yOffset = -h * swipeBackLayout.transitionProgress;
                    topView.setTranslationY(yOffset);
                    setTranslationY(yOffset);
                }
            }
            if (swipeBackGravityBottom) {
                setTranslationY(getMeasuredHeight() * (1f - backScaleY));
            }
            if (backgroundDrawable != null) {
                int start = gapStartY - (scrollView == null ? 0 : scrollView.getScrollY());
                int end = gapEndY - (scrollView == null ? 0 : scrollView.getScrollY());
                boolean hasGap = false;
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    if (linearLayout.getChildAt(i) instanceof GapView && linearLayout.getChildAt(i).getVisibility() == View.VISIBLE) {
                        hasGap = true;
                        break;
                    }
                }
                for (int a = 0; a < 2; a++) {
                    if (a == 1 && start < -dp(16)) {
                        break;
                    }
                    int saveCount = canvas.getSaveCount();
                    boolean applyAlpha = true;
                    if (hasGap && backAlpha != 255) {
                        canvas.saveLayerAlpha(0, bgPaddings.top, getMeasuredWidth(), getMeasuredHeight(), backAlpha, Canvas.ALL_SAVE_FLAG);
                        applyAlpha = false;
                    }  else if (gapStartY != -1000000) {
                        canvas.save();
                        canvas.clipRect(0, bgPaddings.top, getMeasuredWidth(), getMeasuredHeight());
                    }
                    int requestedAlpha = applyAlpha ? backAlpha : 255;
                    backgroundDrawable.setAlpha(backgroundDrawable instanceof org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable
                        ? requestedAlpha : org.telegram.messenger.fork.panels.PanelPolicy.popupAlpha(requestedAlpha, forkGlassBackground));
                    if (shownFromBottom) {
                        final int height = getMeasuredHeight();
                        AndroidUtilities.rectTmp2.set(0, (int) (height * (1.0f - backScaleY)), (int) (getMeasuredWidth() * backScaleX), height);
                    } else {
                        if (start > -dp(16)) {
                            int h = (int) (getMeasuredHeight() * backScaleY);
                            if (a == 0) {
                                if (swipeBackLayout != null && swipeBackLayout.stickToRight) {
                                    AndroidUtilities.rectTmp2.set(getMeasuredWidth() - (int) (getMeasuredWidth() * backScaleX), (scrollView == null ? 0 : -scrollView.getScrollY()) + (gapStartY != -1000000 ? dp(1) : 0), getMeasuredWidth(), (gapStartY != -1000000 ? Math.min(h, start + dp(16)) : h) - subtractBackgroundHeight);
                                } else {
                                    AndroidUtilities.rectTmp2.set(0, (scrollView == null ? 0 : -scrollView.getScrollY()) + (gapStartY != -1000000 ? dp(1) : 0), (int) (getMeasuredWidth() * backScaleX), (gapStartY != -1000000 ? Math.min(h, start + dp(16)) : h) - subtractBackgroundHeight);
                                }
                            } else {
                                if (h < end) {
                                    if (gapStartY != -1000000) {
                                        canvas.restore();
                                    }
                                    continue;
                                }
                                if (swipeBackLayout != null && swipeBackLayout.stickToRight) {
                                    AndroidUtilities.rectTmp2.set(getMeasuredWidth() - (int) (getMeasuredWidth() * backScaleX), end, getMeasuredWidth(), h - subtractBackgroundHeight);
                                } else {
                                    AndroidUtilities.rectTmp2.set(0, end, (int) (getMeasuredWidth() * backScaleX), h - subtractBackgroundHeight);
                                }
                            }
                        } else {
                            if (swipeBackLayout != null && swipeBackLayout.stickToRight) {
                                AndroidUtilities.rectTmp2.set(getMeasuredWidth() - (int) (getMeasuredWidth() * backScaleX), (gapStartY < 0 ? 0 : -dp(16)), getMeasuredWidth(), (int) (getMeasuredHeight() * backScaleY) - subtractBackgroundHeight);
                            } else {
                                AndroidUtilities.rectTmp2.set(0, (gapStartY < 0 ? 0 : -dp(16)), (int) (getMeasuredWidth() * backScaleX), (int) (getMeasuredHeight() * backScaleY) - subtractBackgroundHeight);
                            }
                        }
                    }
                    if (reactionsEnterProgress != 1f) {
                        if (rect == null) {
                            rect = new Rect();
                        }
                        rect.set(AndroidUtilities.rectTmp2.right, AndroidUtilities.rectTmp2.top, AndroidUtilities.rectTmp2.right, AndroidUtilities.rectTmp2.top);
                        AndroidUtilities.lerp(rect, AndroidUtilities.rectTmp2, reactionsEnterProgress, AndroidUtilities.rectTmp2);
                    }
                    backgroundDrawable.setBounds(AndroidUtilities.rectTmp2);
                    backgroundDrawable.draw(canvas);
                    if (clipChildren) {
                        AndroidUtilities.rectTmp2.left += bgPaddings.left;
                        AndroidUtilities.rectTmp2.top += bgPaddings.top;
                        AndroidUtilities.rectTmp2.right -= bgPaddings.right;
                        AndroidUtilities.rectTmp2.bottom -= bgPaddings.bottom;
                        canvas.clipRect(AndroidUtilities.rectTmp2);
                    }
                    if (hasGap) {
                        canvas.save();
                        AndroidUtilities.rectTmp.set(backgroundDrawable.getBounds());
                        AndroidUtilities.rectTmp.inset(dp(8), dp(8));
                        if (path == null) {
                            path = new Path();
                        } else {
                            path.rewind();
                        }
                        float gapRadius = dp(ownsSurface && forkStyleEligible ? org.telegram.messenger.fork.panels.PanelPolicy.surfaceRadiusDp(org.telegram.messenger.fork.panels.ForkPanelSettings.style(), radius) : 12);
                        path.addRoundRect(AndroidUtilities.rectTmp, gapRadius, gapRadius, Path.Direction.CW);
                        canvas.clipPath(path);
                        for (int i = 0; i < linearLayout.getChildCount(); i++) {
                            if (linearLayout.getChildAt(i) instanceof GapView && linearLayout.getChildAt(i).getVisibility() == View.VISIBLE) {
                                canvas.save();
                                float x = 0, y = 0;
                                GapView child = (GapView) linearLayout.getChildAt(i);
                                View view = child;
                                while (view != this) {
                                    x += view.getX();
                                    y += view.getY();
                                    view = (View) view.getParent();
                                    if (view == null) {
                                        break;
                                    }
                                }
                                canvas.translate(x, y * (scrollView == null ? 1f : scrollView.getScaleY()) - (scrollView == null ? 0 : scrollView.getScrollY()));
                                child.draw(canvas);
                                canvas.restore();
                            }
                        }
                        canvas.restore();
                    }
                    canvas.restoreToCount(saveCount);
                }
            }
            if (reactionsEnterProgress != 1f) {
                canvas.saveLayerAlpha((float) AndroidUtilities.rectTmp2.left, (float) AndroidUtilities.rectTmp2.top, AndroidUtilities.rectTmp2.right, AndroidUtilities.rectTmp2.bottom, (int) (255 * reactionsEnterProgress), Canvas.ALL_SAVE_FLAG);
                float scale = 0.5f + reactionsEnterProgress * 0.5f;
                canvas.scale(scale, scale, AndroidUtilities.rectTmp2.right, AndroidUtilities.rectTmp2.top);
                super.dispatchDraw(canvas);
                canvas.restore();
            } else {
                super.dispatchDraw(canvas);
            }
        }

        public Drawable getBackgroundDrawable() {
            return backgroundDrawable;
        }

        public boolean hasForkStyleBackground() {
            return forkStyleEligible && backgroundDrawable != null && (backgroundDrawable == forkStockBackground || backgroundDrawable == forkExperimentalBackground || backgroundDrawable == forkMaterialBackground);
        }

        public int getItemsCount() {
            return linearLayout.getChildCount();
        }

        public View getItemAt(int index) {
            return linearLayout.getChildAt(index);
        }

        public void scrollToTop() {
            if (scrollView != null) {
                scrollView.scrollTo(0, 0);
            }
        }

        public void setupRadialSelectors(int color) {
            int count = linearLayout.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = linearLayout.getChildAt(a);
                child.setBackground(Theme.createRadSelectorDrawable(color, a == 0 ? 6 : 0, a == count - 1 ? 6 : 0));
            }
        }

        public void updateRadialSelectors() {
            int count = linearLayout.getChildCount();
            View firstVisible = null;
            View lastVisible = null;
            for (int a = 0; a < count; a++) {
                View child = linearLayout.getChildAt(a);
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                if (firstVisible == null) {
                    firstVisible = child;
                }
                lastVisible = child;
            }

            boolean prevGap = false;
            for (int a = 0; a < count; a++) {
                View child = linearLayout.getChildAt(a);
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                Object tag = child.getTag(R.id.object_tag);
                if (child instanceof ActionBarMenuSubItem) {
                    ((ActionBarMenuSubItem) child).updateSelectorBackground(child == firstVisible || prevGap, child == lastVisible);
                }
                if (tag != null) {
                    prevGap = true;
                } else {
                    prevGap = false;
                }
            }
        }

        protected int getThemedColor(int key) {
            return Theme.getColor(key, resourcesProvider);
        }

        public void setOnSizeChangedListener(ActionBarPopupWindow.onSizeChangedListener onSizeChangedListener) {
            this.onSizeChangedListener = onSizeChangedListener;
        }

        public int getVisibleHeight() {
            return (int) (getMeasuredHeight() * backScaleY);
        }

        public void setTopView(View topView) {
            this.topView = topView;
        }

        public void setSwipeBackForegroundColor(int color) {
            getSwipeBack().setForegroundColor(color);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (swipeBackLayout != null) {
                swipeBackLayout.invalidateTransforms(!startAnimationPending);
            }
        }

        public void setParentWindow(ActionBarPopupWindow popupWindow) {
            window = popupWindow;
        }

        public void setReactionsTransitionProgress(float transitionEnterProgress) {
            this.reactionsEnterProgress = transitionEnterProgress;
            invalidate();
        }
    }

    public ActionBarPopupWindow() {
        super();
        init();
    }

    public ActionBarPopupWindow(Context context) {
        super(context);
        init();
    }

    public ActionBarPopupWindow(int width, int height) {
        super(width, height);
        init();
    }

    public ActionBarPopupWindow(View contentView) {
        super(contentView);
        init();
    }

    public ActionBarPopupWindow(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, focusable);
        init();
    }

    public ActionBarPopupWindow(View contentView, int width, int height) {
        super(contentView, width, height);
        init();
    }

    public void setAnimationEnabled(boolean value) {
        animationEnabled = value;
    }

    private static int forkPopupDuration(int duration) {
        return org.telegram.messenger.fork.panels.PanelPolicy.popupAnimationDuration(
                org.telegram.messenger.fork.panels.ForkPanelSettings.animationMode(), duration,
                AndroidUtilities.getAnimatorDurationScale() > 0f
                        && !org.telegram.ui.ForkGraphicsSurface.effectsSuppressed());
    }

    @SuppressWarnings("PrivateAPI")
    public void setLayoutInScreen(boolean value) {
        try {
            if (layoutInScreenMethod == null) {
                layoutInScreenMethod = PopupWindow.class.getDeclaredMethod("setLayoutInScreenEnabled", boolean.class);
                layoutInScreenMethod.setAccessible(true);
            }
            layoutInScreenMethod.invoke(this, true);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void init() {
        View contentView = getContentView();
        if (contentView instanceof ActionBarPopupWindowLayout && ((ActionBarPopupWindowLayout) contentView).getSwipeBack() != null) {
            setTouchInterceptor((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    Drawable backgroundDrawable = ((ActionBarPopupWindowLayout) contentView).getBackgroundDrawable();
                    AndroidUtilities.rectTmp.set(backgroundDrawable.getBounds());
                    AndroidUtilities.rectTmp.offset(contentView.getX(), contentView.getY());
                    if (!AndroidUtilities.rectTmp.contains(e.getX(), e.getY())) {
                        dismiss();
                        return true;
                    }
                }
                return false;
            });
        }
        if (superListenerField != null) {
            try {
                mSuperScrollListener = (ViewTreeObserver.OnScrollChangedListener) superListenerField.get(this);
                superListenerField.set(this, NOP);
            } catch (Exception e) {
                mSuperScrollListener = null;
            }
        }
    }

    public void setDismissAnimationDuration(int value) {
        dismissAnimationDuration = value;
    }

    private void unregisterListener() {
        if (mSuperScrollListener != null && mViewTreeObserver != null) {
            if (mViewTreeObserver.isAlive()) {
                mViewTreeObserver.removeOnScrollChangedListener(mSuperScrollListener);
            }
            mViewTreeObserver = null;
        }
    }

    private void registerListener(View anchor) {
        if (mSuperScrollListener != null) {
            ViewTreeObserver vto = (anchor.getWindowToken() != null) ? anchor.getViewTreeObserver() : null;
            if (vto != mViewTreeObserver) {
                if (mViewTreeObserver != null && mViewTreeObserver.isAlive()) {
                    mViewTreeObserver.removeOnScrollChangedListener(mSuperScrollListener);
                }
                if ((mViewTreeObserver = vto) != null) {
                    vto.addOnScrollChangedListener(mSuperScrollListener);
                }
            }
        }
    }

    public void dimBehind() {
        dimBehind(0.2f);
    }

    public void dimBehind(float amount) {
        View container = getContentView().getRootView();
        Context context = getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = amount;
        wm.updateViewLayout(container, p);
    }

    public void setFocusableFlag(boolean enable) {
        View container = getContentView().getRootView();
        Context context = getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();

        if (p != null) {
            if (enable) {
                p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            } else {
                p.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            }
            wm.updateViewLayout(container, p);
        }
    }

    private void dismissDim() {
        View container = getContentView().getRootView();
        Context context = getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (container.getLayoutParams() == null || !(container.getLayoutParams() instanceof WindowManager.LayoutParams)) {
            return;
        }
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        try {
            if ((p.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0) {
                p.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                p.dimAmount = 0.0f;
                wm.updateViewLayout(container, p);
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        try {
            setAnimationSource(anchor);
            prepareForkFluidAnimation();
            super.showAsDropDown(anchor, xoff, yoff);
            registerListener(anchor);
            postForkFluidAnimation();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static AnimatorSet startAnimation(ActionBarPopupWindowLayout content) {
        AnimatorSet fluid = org.telegram.ui.ForkSurfaceAnimation.create(content, null, true, content.shownFromBottom);
        if (fluid != null) {
            fluid.start();
            return fluid;
        }
        content.startAnimationPending = true;
        content.setTranslationY(0);
        content.setAlpha(1.0f);
        content.setPivotX(content.getMeasuredWidth());
        content.setPivotY(0);
        final int count = content.getItemsCount();
        content.positions.clear();
        int visibleCount = 0;
        for (int a = 0; a < count; a++) {
            View child = content.getItemAt(a);
            if (child instanceof GapView) {
                continue;
            }
            child.setAlpha(0.0f);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            content.positions.put(child, visibleCount);
            visibleCount++;
        }
        if (content.shownFromBottom) {
            content.lastStartedChild = count - 1;
        } else {
            content.lastStartedChild = 0;
        }
        float finalScaleY = 1f;
        if (content.getSwipeBack() != null) {
            content.getSwipeBack().invalidateTransforms();
            finalScaleY = content.backScaleY;
        }
        AnimatorSet windowAnimatorSet = new AnimatorSet();
        ValueAnimator childtranslations = ValueAnimator.ofFloat(0, 1);
        childtranslations.addUpdateListener(anm -> {
            final int count2 = content.getItemsCount();
            final float t = (float) anm.getAnimatedValue();
            for (int a = 0; a < count2; a++) {
                View child = content.getItemAt(a);
                if (child instanceof GapView) {
                    continue;
                }
                float at = AndroidUtilities.cascade(t, content.shownFromBottom ? count2 - 1 - a : a, count2, 4);
                child.setTranslationY((1f - at) * dp(-6));
                child.setAlpha(at * (child.isEnabled() ? 1f : 0.5f));
            }
        });
        content.updateAnimation = false;
        content.clipChildren = true;
        windowAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(content, "backScaleY", 0.0f, finalScaleY),
                ObjectAnimator.ofInt(content, "backAlpha", 0, 255),
                childtranslations
        );
        windowAnimatorSet.setDuration(forkPopupDuration(150 + 16 * visibleCount));
        windowAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                content.startAnimationPending = false;
                int count = content.getItemsCount();
                for (int a = 0; a < count; a++) {
                    View child = content.getItemAt(a);
                    if (child instanceof GapView) {
                        continue;
                    }
                    child.setTranslationY(0);
                    child.setAlpha(child.isEnabled() ? 1f : 0.5f);
                }
            }
        });
        windowAnimatorSet.start();
        return windowAnimatorSet;
    }

    public void startAnimation() {
        if (startForkFluidAnimation()) return;
        if (animationEnabled) {
            if (windowAnimatorSet != null) {
                return;
            }

            ViewGroup viewGroup = (ViewGroup) getContentView();
            ActionBarPopupWindowLayout content = null;
            if (viewGroup instanceof ActionBarPopupWindowLayout) {
                content = (ActionBarPopupWindowLayout) viewGroup;
                content.startAnimationPending = true;
            } else {
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    if (viewGroup.getChildAt(i) instanceof ActionBarPopupWindowLayout) {
                        content = (ActionBarPopupWindowLayout) viewGroup.getChildAt(i);
                        content.startAnimationPending = true;
                    }
                }
            }
            content.setTranslationY(0);
            content.setAlpha(1.0f);
            content.setPivotX(content.getMeasuredWidth());
            content.setPivotY(0);
            int count = content.getItemsCount();
            content.positions.clear();
            int visibleCount = 0;
            for (int a = 0; a < count; a++) {
                View child = content.getItemAt(a);
                child.setAlpha(0.0f);
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                content.positions.put(child, visibleCount);
                visibleCount++;
            }
            if (content.shownFromBottom) {
                content.lastStartedChild = count - 1;
            } else {
                content.lastStartedChild = 0;
            }
            float finalScaleY = 1f;
            if (content.getSwipeBack() != null) {
                content.getSwipeBack().invalidateTransforms();
                finalScaleY = content.backScaleY;
            }
            windowAnimatorSet = new AnimatorSet();
            windowAnimatorSet.playTogether(
                    ObjectAnimator.ofFloat(content, "backScaleY", 0.0f, finalScaleY),
                    ObjectAnimator.ofInt(content, "backAlpha", 0, 255));
            windowAnimatorSet.setDuration(forkPopupDuration(150 + 16 * visibleCount));
            windowAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (windowAnimatorSet == animation) windowAnimatorSet = null;
                    ViewGroup viewGroup = (ViewGroup) getContentView();
                    ActionBarPopupWindowLayout content = null;
                    if (viewGroup instanceof ActionBarPopupWindowLayout) {
                        content = (ActionBarPopupWindowLayout) viewGroup;
                        content.startAnimationPending = false;
                    } else {
                        for (int i = 0; i < viewGroup.getChildCount(); i++) {
                            if (viewGroup.getChildAt(i) instanceof ActionBarPopupWindowLayout) {
                                content = (ActionBarPopupWindowLayout) viewGroup.getChildAt(i);
                                content.startAnimationPending = false;
                            }
                        }
                    }
                    int count = content.getItemsCount();
                    for (int a = 0; a < count; a++) {
                        View child = content.getItemAt(a);
                        if (child instanceof GapView) {
                            continue;
                        }
                        child.setAlpha(child.isEnabled() ? 1f : 0.5f);
                    }
                }
            });
            windowAnimatorSet.start();
        }
    }

    @Override
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        super.update(anchor, xoff, yoff, width, height);
        registerListener(anchor);
    }

    @Override
    public void update(View anchor, int width, int height) {
        super.update(anchor, width, height);
        registerListener(anchor);
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        prepareForkFluidAnimation();
        super.showAtLocation(parent, gravity, x, y);
        unregisterListener();
        postForkFluidAnimation();
    }

    @Override
    public void dismiss() {
        dismiss(true);
    }

    public void setPauseNotifications(boolean value) {
        pauseNotifications = value;
    }

    public void dismiss(boolean animated) {
        clearForkPreDraw();
        animated = animated && AndroidUtilities.getAnimatorDurationScale() > 0f
                && !org.telegram.ui.ForkGraphicsSurface.effectsSuppressed();
        setFocusable(false);
        dismissDim();
        if (windowAnimatorSet != null) {
            if (animated && isClosingAnimated) {
                return;
            }
            AnimatorSet previous = windowAnimatorSet;
            windowAnimatorSet = null;
            previous.cancel();
            if (isClosingAnimated && pauseNotifications) notificationsLocker.unlock();
        }
        isClosingAnimated = false;
        ActionBarPopupWindowLayout fluidContent = forkPopupLayout();
        if (animationEnabled && animated && forkFluidShown && fluidContent != null && outEmptyTime <= 0) {
            AnimatorSet fluid = org.telegram.ui.ForkSurfaceAnimation.create(fluidContent, forkAnimationSource, false, fluidContent.shownFromBottom);
            if (fluid != null) {
                isClosingAnimated = true;
                windowAnimatorSet = fluid;
                fluid.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        if (windowAnimatorSet != animation) return;
                        windowAnimatorSet = null;
                        isClosingAnimated = false;
                        forkFluidShown = false;
                        try { ActionBarPopupWindow.super.dismiss(); } catch (Exception ignore) { }
                        unregisterListener();
                        if (pauseNotifications) notificationsLocker.unlock();
                    }
                });
                if (pauseNotifications) notificationsLocker.lock();
                fluid.start();
                return;
            }
        }
        forkFluidShown = false;
        if (animationEnabled && animated) {
            isClosingAnimated = true;
            ViewGroup viewGroup = (ViewGroup) getContentView();
            ActionBarPopupWindowLayout content = null;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (viewGroup.getChildAt(i) instanceof ActionBarPopupWindowLayout) {
                    content = (ActionBarPopupWindowLayout) viewGroup.getChildAt(i);
                }
            }
            if (content != null) {
                if (content.itemAnimators != null && !content.itemAnimators.isEmpty()) {
                    for (int a = 0, N = content.itemAnimators.size(); a < N; a++) {
                        AnimatorSet animatorSet = content.itemAnimators.get(a);
                        animatorSet.removeAllListeners();
                        animatorSet.cancel();
                    }
                    content.itemAnimators.clear();
                }
            }
            windowAnimatorSet = new AnimatorSet();
            if (outEmptyTime > 0) {
                windowAnimatorSet.playTogether(ValueAnimator.ofFloat(0, 1f));
                windowAnimatorSet.setDuration(outEmptyTime);
            } else if (scaleOut) {
                windowAnimatorSet.playTogether(
                        ObjectAnimator.ofFloat(viewGroup, View.SCALE_Y, 0.8f),
                        ObjectAnimator.ofFloat(viewGroup, View.SCALE_X, 0.8f),
                        ObjectAnimator.ofFloat(viewGroup, View.ALPHA, 0.0f));
                windowAnimatorSet.setDuration(dismissAnimationDuration);
            } else {
                windowAnimatorSet.playTogether(
                        ObjectAnimator.ofFloat(viewGroup, View.TRANSLATION_Y, viewGroup.getTranslationY() + dp((content != null && content.shownFromBottom) ? 5 : -5)),
                        ObjectAnimator.ofFloat(viewGroup, View.ALPHA, 0.0f));
                windowAnimatorSet.setDuration(dismissAnimationDuration);
            }

            windowAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (windowAnimatorSet != animation) return;
                    windowAnimatorSet = null;
                    isClosingAnimated = false;
                    setFocusable(false);
                    try {
                        ActionBarPopupWindow.super.dismiss();
                    } catch (Exception ignore) {

                    }
                    unregisterListener();
                    if (pauseNotifications) {
                        notificationsLocker.unlock();
                    }
                }
            });
            if (pauseNotifications) {
                notificationsLocker.lock();
            }
            windowAnimatorSet.start();
        } else {
            try {
                super.dismiss();
            } catch (Exception ignore) {

            }
            unregisterListener();
        }
    }

    public void setEmptyOutAnimation(long time) {
        outEmptyTime = time;
    }

    public interface onSizeChangedListener {
        void onSizeChanged();
    }

    public static class GapView extends FrameLayout {

        Drawable shadowDrawable;

        public GapView(Context context, Theme.ResourcesProvider resourcesProvider) {
            this(context, resourcesProvider, Theme.key_actionBarDefaultSubmenuSeparator);
        }

        public GapView(Context context, int color, int shadowColor) {
            super(context);
            this.shadowDrawable = Theme.getThemedDrawable(getContext(), R.drawable.greydivider, shadowColor);
            setBackgroundColor(color);
        }

        public GapView(Context context, Theme.ResourcesProvider resourcesProvider, int colorKey) {
            this(context, Theme.getColor(colorKey, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundGrayShadow, resourcesProvider));
        }

        public void setColor(int color) {
            setBackgroundColor(color);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (shadowDrawable != null) {
                shadowDrawable.setBounds(0, 0, getWidth(), getHeight());
                shadowDrawable.draw(canvas);
            }
        }
    }
}
