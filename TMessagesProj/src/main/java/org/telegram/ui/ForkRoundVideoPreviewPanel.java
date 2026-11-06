package org.telegram.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.camera.CameraInfo;
import org.telegram.messenger.camera.CameraView;
import org.telegram.messenger.fork.camera.RoundCameraPolicy;
import org.telegram.messenger.fork.camera.RoundCameraSettings;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

/**
 * Embedded round-video preview: two circular icon-only controls on the left and
 * a large circular live preview on the right. It never records or sends media.
 */
public final class ForkRoundVideoPreviewPanel extends ViewGroup {
    public interface PermissionRequester {
        void requestCameraPermission();
    }

    private static final int PERMISSION_TIMEOUT_MS = 8000;

    private final FrameLayout previewHolder;
    private final ImageView inactiveIcon;
    private final ImageView powerButton;
    private final ImageView flipButton;
    private final PermissionRequester permissionRequester;

    private CameraView camera;
    private boolean resumed;
    private boolean attached;
    private boolean previewOn;
    private boolean permissionPending;
    private boolean cameraInitRequested;
    private boolean availableFront, availableBack;
    private boolean currentFront;
    private boolean scrollVisible = true;
    private int previewDiameter;
    private final ViewTreeObserver.OnScrollChangedListener scrollListener =
            this::updateScrollVisibility;

    private final Runnable onInitialized = this::attachCamera;
    private final Runnable onTimeout = () -> {
        if (!resumed || !previewOn) return;
        previewOn = false;
        stopCamera();
        updatePowerState();
    };

    public ForkRoundVideoPreviewPanel(Context context, PermissionRequester permissionRequester) {
        super(context);
        this.permissionRequester = permissionRequester;
        setClipChildren(false);

        previewHolder = new FrameLayout(context);
        previewHolder.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        previewHolder.setClipToOutline(true);
        previewHolder.setBackgroundColor(0xFF16181C);
        addView(previewHolder);

        inactiveIcon = new ImageView(context);
        inactiveIcon.setImageResource(R.drawable.camera);
        inactiveIcon.setScaleType(ImageView.ScaleType.CENTER);
        inactiveIcon.setColorFilter(new PorterDuffColorFilter(0x66FFFFFF, PorterDuff.Mode.MULTIPLY));
        inactiveIcon.setContentDescription(ForkLocale.get(R.string.ForkCameraPreviewOff));
        previewHolder.addView(inactiveIcon, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        powerButton = makeCircleButton(context, R.drawable.settings_power,
                R.string.ForkCameraPreviewPower);
        powerButton.setOnClickListener(v -> setPreviewOn(!previewOn));
        addView(powerButton);

        flipButton = makeCircleButton(context, R.drawable.stream_flip,
                R.string.ForkCameraPreviewSwitch);
        flipButton.setOnClickListener(v -> switchCamera());
        addView(flipButton);

        updatePowerState();
        updateFlipState();
    }

    private ImageView makeCircleButton(Context context, int icon, int description) {
        ImageView button = new ImageView(context);
        button.setImageResource(icon);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12),
                AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        button.setContentDescription(ForkLocale.get(description));
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(48),
                Theme.getColor(Theme.key_windowBackgroundWhite),
                Theme.getColor(Theme.key_actionBarDefaultSelector)));
        return button;
    }

    public void setResumed(boolean value) {
        resumed = value;
        if (value) {
            if (previewOn && attached && getWindowVisibility() == VISIBLE && !permissionPending) {
                startCamera();
            }
        } else {
            stopCamera();
        }
    }

    public void setPreviewOn(boolean value) {
        previewOn = value;
        if (value && resumed && attached && getWindowVisibility() == VISIBLE) {
            startCamera();
        } else if (!value) {
            stopCamera();
        }
        updatePowerState();
    }

    public boolean isPreviewOn() {
        return previewOn;
    }

    public void onCameraPermissionResult(boolean granted) {
        permissionPending = false;
        if (granted && previewOn && resumed) {
            startCamera();
        } else {
            if (!granted) previewOn = false;
            updatePowerState();
        }
    }

    private void startCamera() {
        if (!resumed || !previewOn || !scrollVisible || attached == false
                || getWindowVisibility() != VISIBLE || camera != null || context() == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23
                && context().checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (!permissionPending) {
                permissionPending = true;
                if (permissionRequester != null) permissionRequester.requestCameraPermission();
            }
            updatePowerState();
            return;
        }
        if (!CameraView.isCameraAllowed()) {
            previewOn = false;
            updatePowerState();
            return;
        }
        updatePowerState();
        cameraInitRequested = true;
        CameraController controller = CameraController.getInstance();
        AndroidUtilities.cancelRunOnUIThread(onTimeout);
        AndroidUtilities.runOnUIThread(onTimeout, PERMISSION_TIMEOUT_MS);
        if (controller.isCameraInitied()) attachCamera();
        else controller.initCamera(onInitialized);
    }

    private void attachCamera() {
        if (!resumed || !previewOn || attached == false || camera != null
                || !CameraView.isCameraAllowed() || getWidth() == 0) {
            return;
        }
        availableFront = availableBack = false;
        ArrayList<CameraInfo> cameras = CameraController.getInstance().getCameras();
        if (cameras != null) {
            for (CameraInfo info : cameras) {
                if (info.isFrontface()) availableFront = true;
                else availableBack = true;
            }
        }
        boolean preferFront = RoundCameraPolicy.initial(
                RoundCameraSettings.mode(), RoundCameraSettings.lastFront(), true);
        int facing = RoundCameraPolicy.previewFacing(preferFront, availableFront, availableBack);
        if (facing < 0) {
            previewOn = false;
            updatePowerState();
            return;
        }
        currentFront = facing == 1;
        camera = new CameraView(getContext(), currentFront);
        CameraView active = camera;
        camera.setDelegate(() -> {
            if (camera != active || !resumed) return;
            AndroidUtilities.cancelRunOnUIThread(onTimeout);
            inactiveIcon.setVisibility(View.GONE);
        });
        previewHolder.addView(camera, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        inactiveIcon.setVisibility(View.GONE);
        updateFlipState();
        requestLayout();
    }

    private void switchCamera() {
        if (camera == null || !availableFront || !availableBack) return;
        camera.switchCamera();
        currentFront = !currentFront;
        RoundCameraSettings.recordFront(currentFront);
        updateFlipState();
    }

    private void stopCamera() {
        AndroidUtilities.cancelRunOnUIThread(onTimeout);
        if (cameraInitRequested) CameraController.getInstance().cancelOnInitRunnable(onInitialized);
        cameraInitRequested = false;
        if (camera != null) {
            CameraView active = camera;
            camera = null;
            active.destroy(true, null);
            previewHolder.removeView(active);
        }
        inactiveIcon.setVisibility(View.VISIBLE);
        updateFlipState();
    }

    private void updatePowerState() {
        boolean active = previewOn && camera != null;
        int color = active ? ForkAppearanceSettingsActivity.accent(getContext())
                : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3);
        powerButton.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        powerButton.setAlpha(previewOn ? 1f : 0.72f);
    }

    private void updateFlipState() {
        boolean enabled = camera != null && availableFront && availableBack;
        flipButton.setEnabled(enabled);
        flipButton.setAlpha(enabled ? 1f : 0.35f);
        flipButton.setColorFilter(new PorterDuffColorFilter(
                Theme.getColor(enabled ? Theme.key_windowBackgroundWhiteBlackText
                        : Theme.key_windowBackgroundWhiteGrayText3), PorterDuff.Mode.MULTIPLY));
    }

    private Context context() {
        return getContext();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        post(this::updateScrollVisibility);
        if (previewOn) startCamera();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        if (getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }
        stopCamera();
        super.onDetachedFromWindow();
    }

    private void updateScrollVisibility() {
        if (!attached || getWidth() == 0 || getHeight() == 0) return;
        Rect visible = new Rect();
        boolean shown = getGlobalVisibleRect(visible)
                && visible.height() > AndroidUtilities.dp(24);
        if (shown == scrollVisible) return;
        scrollVisible = shown;
        if (!shown) stopCamera();
        else if (previewOn && resumed) startCamera();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            if (previewOn && resumed) startCamera();
        } else {
            stopCamera();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && previewOn && resumed && attached && camera == null
                && cameraInitRequested) {
            attachCamera();
        }
        updateScrollVisibility();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width = AndroidUtilities.displaySize.x;
        }
        int padding = AndroidUtilities.dp(16);
        int button = AndroidUtilities.dp(48);
        int gap = AndroidUtilities.dp(14);
        int column = button;
        int available = Math.max(AndroidUtilities.dp(120),
                width - padding * 2 - column - gap);
        previewDiameter = Math.min(available, AndroidUtilities.dp(232));
        int height = previewDiameter + padding * 2;
        setMeasuredDimension(width, height);
        int exactButton = MeasureSpec.makeMeasureSpec(button, MeasureSpec.EXACTLY);
        powerButton.measure(exactButton, exactButton);
        flipButton.measure(exactButton, exactButton);
        previewHolder.measure(
                MeasureSpec.makeMeasureSpec(previewDiameter, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewDiameter, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int padding = AndroidUtilities.dp(16);
        int button = AndroidUtilities.dp(48);
        int gap = AndroidUtilities.dp(14);
        int columnHeight = button * 2 + gap;
        int startY = (height - columnHeight) / 2;
        powerButton.layout(padding, startY, padding + button, startY + button);
        flipButton.layout(padding, startY + button + gap, padding + button,
                startY + button * 2 + gap);
        int previewLeft = width - padding - previewDiameter;
        int previewTop = (height - previewDiameter) / 2;
        previewHolder.layout(previewLeft, previewTop, previewLeft + previewDiameter,
                previewTop + previewDiameter);
    }
}
