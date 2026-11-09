package org.telegram.ui;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.camera.RoundCameraSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
/** Compact entry points to the real round-video controls. */
public final class ForkVideoSettingsActivity extends ForkSettingsFragment {
    private static final int CAMERA_PERMISSION = 7205;
    private TextSettingsCell camera, quality, profile, capabilities;
    private ForkRoundVideoPreviewPanel previewPanel;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkVideoTitle)); actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { @Override public void onItemClick(int id) { if (id == -1) finishFragment(); } });
        LinearLayout page = ForkSettingsLayout.page(context), rows = ForkSettingsLayout.section(context);
        camera = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(camera);
        camera.setOnClickListener(v -> ForkRoundCameraShortcuts.show(this, v, this::refreshValues)); rows.addView(camera);
        quality = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(quality);
        quality.setOnClickListener(v -> ForkRoundCameraShortcuts.showQuality(this, v, this::refreshValues)); rows.addView(quality);
        profile = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(profile);
        profile.setOnClickListener(this::chooseProfile); rows.addView(profile);
        capabilities = new TextSettingsCell(context);
        capabilities.setOnClickListener(v -> {
            org.telegram.ui.ActionBar.AlertDialog dialog = new org.telegram.ui.ActionBar.AlertDialog.Builder(context)
                    .setTitle(ForkLocale.get(R.string.ForkCameraCapabilities))
                    .setMessage(org.telegram.messenger.fork.camera.CameraCapabilities.describeAll())
                    .setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.OK), null)
                    .create();
            showDialog(dialog);
        });
        ForkSettingsLayout.interactive(capabilities); rows.addView(capabilities);
        ForkSettingsLayout.addSection(page, rows);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkCameraFallback) + "\n\n" + ForkLocale.get(R.string.ForkVideoQualityDescription));
        page.addView(info);
        previewPanel = new ForkRoundVideoPreviewPanel(context, () -> {
            if (getParentActivity() != null) {
                getParentActivity().requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_PERMISSION);
            }
        });
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        previewParams.setMargins(AndroidUtilities.dp(12), AndroidUtilities.dp(12),
                AndroidUtilities.dp(12), 0);
        page.addView(previewPanel, previewParams);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.addView(page); fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkVideoTitle), this::finishFragment); refreshValues(); return fragmentView;
    }
    private void refreshValues() {
        if (camera == null || quality == null || profile == null) return;
        Context c = camera.getContext();
        String mode = RoundCameraSettings.mode();
        int cameraLabel = "front".equals(mode) ? R.string.ForkCameraFront : "back".equals(mode) ? R.string.ForkCameraBack : R.string.ForkCameraChooseDuring;
        camera.setTextAndValue(ForkLocale.get(R.string.ForkCameraChoiceTitle), ForkLocale.get(cameraLabel), true);
        String qualityMode = RoundCameraSettings.qualityMode();
        int qualityLabel = "auto".equals(qualityMode) ? R.string.ForkVideoQualityAuto : "custom".equals(qualityMode) ? R.string.ForkVideoQualityCustom : R.string.ForkVideoQualityStandard;
        quality.setTextAndValue(ForkLocale.get(R.string.ForkVideoQualityTitle), ForkLocale.get(qualityLabel), true);
        profile.setTextAndValue(ForkLocale.get(R.string.ForkCameraProfile),
                ForkLocale.get(profileLabel(org.telegram.messenger.fork.camera.CameraProfileSettings.mode())), true);
        capabilities.setTextAndValue(ForkLocale.get(R.string.ForkCameraCapabilities),
                ForkLocale.get(R.string.ForkCameraCapabilitiesValue), false);
    }

    private void chooseProfile(View anchor) {
        String[] modes = {
                org.telegram.messenger.fork.camera.CameraProfilePolicy.AUTO,
                org.telegram.messenger.fork.camera.CameraProfilePolicy.QUALITY,
                org.telegram.messenger.fork.camera.CameraProfilePolicy.BALANCED,
                org.telegram.messenger.fork.camera.CameraProfilePolicy.PERFORMANCE,
                org.telegram.messenger.fork.camera.CameraProfilePolicy.CUSTOM
        };
        CharSequence[] labels = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) labels[i] = ForkLocale.get(profileLabel(modes[i]));
        int selected = java.util.Arrays.asList(modes).indexOf(
                org.telegram.messenger.fork.camera.CameraProfileSettings.mode());
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            org.telegram.messenger.fork.camera.CameraProfileSettings.setMode(modes[index]);
            refreshValues();
        });
    }

    private static int profileLabel(String mode) {
        if (org.telegram.messenger.fork.camera.CameraProfilePolicy.QUALITY.equals(mode)) return R.string.ForkCameraProfileQuality;
        if (org.telegram.messenger.fork.camera.CameraProfilePolicy.BALANCED.equals(mode)) return R.string.ForkCameraProfileBalanced;
        if (org.telegram.messenger.fork.camera.CameraProfilePolicy.PERFORMANCE.equals(mode)) return R.string.ForkCameraProfilePerformance;
        if (org.telegram.messenger.fork.camera.CameraProfilePolicy.CUSTOM.equals(mode)) return R.string.ForkVoiceCustom;
        return R.string.ForkCameraProfileAuto;
    }
    @Override public void onResume() {
        super.onResume();
        if (previewPanel != null) previewPanel.setResumed(true);
    }
    @Override public void onPause() {
        if (previewPanel != null) previewPanel.setResumed(false);
        super.onPause();
    }
    @Override public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] results) {
        if (requestCode == CAMERA_PERMISSION && previewPanel != null) {
            previewPanel.onCameraPermissionResult(results.length > 0
                    && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED);
        }
    }
    @Override public void onFragmentDestroy() {
        if (previewPanel != null) previewPanel.setResumed(false);
        camera = null; quality = null; profile = null; capabilities = null; previewPanel = null;
        super.onFragmentDestroy();
    }
}
