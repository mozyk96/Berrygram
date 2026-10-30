package org.telegram.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.camera.CameraInfo;
import org.telegram.messenger.camera.CameraView;
import org.telegram.messenger.fork.camera.RoundCameraPolicy;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

/** Camera-only preview. It does not record, store or send media. */
public final class ForkCameraTestActivity extends BaseFragment {
    private static final int CAMERA_PERMISSION = 7102;
    private FrameLayout preview;
    private TextView status;
    private TextSettingsCell start, flip;
    private CameraView camera;
    private boolean resumed;
    private boolean startAfterResume;
    private boolean cameraInitRequested;
    private boolean availableFront, availableBack;
    private final Runnable onInitialized = this::attachCamera;
    private final Runnable onTimeout = () -> {
        if (!resumed || status == null) return;
        stopPreview();
        status.setText(R.string.ForkCameraPreviewWaiting);
    };
    @Override public View createView(Context c) {
        actionBar.setTitle(c.getString(R.string.ForkCameraPreviewTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        int size = Math.max(AndroidUtilities.dp(128), Math.min(AndroidUtilities.dp(260), AndroidUtilities.displaySize.x - AndroidUtilities.dp(32)));
        preview = new FrameLayout(c);
        preview.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); }
        });
        preview.setClipToOutline(true);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(size, size);
        previewParams.setMargins(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(12));
        content.addView(preview, previewParams);
        status = new TextView(c);
        status.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        status.setGravity(Gravity.CENTER);
        status.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        status.setText(R.string.ForkCameraPreviewInfo);
        content.addView(status);
        start = new TextSettingsCell(c); start.setCanDisable(true); start.setText(c.getString(R.string.ForkCameraPreviewStart), true);
        start.setOnClickListener(v -> startPreview()); content.addView(start);
        flip = new TextSettingsCell(c); flip.setCanDisable(true); flip.setText(c.getString(R.string.ForkCameraPreviewFlip), false);
        flip.setEnabled(false); flip.setOnClickListener(v -> { if (camera != null && availableFront && availableBack) camera.switchCamera(); });
        content.addView(flip);
        android.widget.ScrollView scroll = new android.widget.ScrollView(c);
        scroll.setFillViewport(true);
        scroll.addView(content);
        fragmentView = scroll;
        return scroll;
    }
    @Override public void onResume() { super.onResume(); resumed = true; if (startAfterResume) { startAfterResume = false; startPreview(); } }
    private void startPreview() {
        if (!resumed || getParentActivity() == null || camera != null) return;
        if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            return;
        }
        if (!CameraView.isCameraAllowed()) { status.setText(R.string.ForkCameraPreviewUnavailable); return; }
        status.setText(R.string.ForkCameraPreviewLoading);
        start.setEnabled(false);
        cameraInitRequested = true;
        CameraController controller = CameraController.getInstance();
        AndroidUtilities.cancelRunOnUIThread(onTimeout);
        AndroidUtilities.runOnUIThread(onTimeout, 8000);
        if (controller.isCameraInitied()) attachCamera(); else controller.initCamera(onInitialized);
    }
    private void attachCamera() {
        if (!resumed || preview == null || camera != null || !CameraView.isCameraAllowed()) return;
        availableFront = availableBack = false;
        java.util.ArrayList<CameraInfo> cameras = CameraController.getInstance().getCameras();
        if (cameras != null) for (CameraInfo info : cameras) {
            if (info.isFrontface()) availableFront = true; else availableBack = true;
        }
        int facing = RoundCameraPolicy.previewFacing(true, availableFront, availableBack);
        if (facing < 0) { AndroidUtilities.cancelRunOnUIThread(onTimeout); start.setEnabled(true); status.setText(R.string.ForkCameraPreviewUnavailable); return; }
        camera = new CameraView(preview.getContext(), facing == 1);
        CameraView activeCamera = camera;
        camera.setDelegate(() -> { if (camera != activeCamera || !resumed) return; AndroidUtilities.cancelRunOnUIThread(onTimeout); if (status != null) status.setText(R.string.ForkCameraPreviewInfo); });
        preview.addView(camera, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        flip.setEnabled(availableFront && availableBack);
    }
    @Override public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] results) {
        if (requestCode == CAMERA_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (resumed) startPreview(); else startAfterResume = true;
        }
        else if (requestCode == CAMERA_PERMISSION && status != null) status.setText(R.string.ForkCameraPreviewPermission);
    }
    private void stopPreview() {
        AndroidUtilities.cancelRunOnUIThread(onTimeout);
        if (cameraInitRequested) CameraController.getInstance().cancelOnInitRunnable(onInitialized);
        cameraInitRequested = false;
        if (camera != null) { camera.destroy(true, null); if (preview != null) preview.removeView(camera); camera = null; }
        if (flip != null) flip.setEnabled(false);
        if (start != null) start.setEnabled(true);
    }
    @Override public void onPause() {
        resumed = false;
        stopPreview();
        super.onPause();
    }
    @Override public void onFragmentDestroy() {
        resumed = false;
        stopPreview();
        super.onFragmentDestroy();
    }
}
