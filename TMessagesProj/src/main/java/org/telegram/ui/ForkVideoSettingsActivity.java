package org.telegram.ui;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.camera.RoundCameraSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
/** Compact entry points to the real round-video controls. */
public final class ForkVideoSettingsActivity extends BaseFragment {
    private TextSettingsCell camera, behavior, quality;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkVideoTitle)); actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { @Override public void onItemClick(int id) { if (id == -1) finishFragment(); } });
        LinearLayout page = ForkSettingsLayout.page(context), rows = ForkSettingsLayout.section(context);
        camera = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(camera);
        camera.setOnClickListener(v -> ForkRoundCameraShortcuts.show(this, this::refreshValues)); rows.addView(camera);
        behavior = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(behavior);
        behavior.setOnClickListener(v -> ForkRoundCameraShortcuts.showBehavior(this, this::refreshValues)); rows.addView(behavior);
        quality = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(quality);
        quality.setOnClickListener(v -> ForkRoundCameraShortcuts.showQuality(this, this::refreshValues)); rows.addView(quality);
        TextSettingsCell test = new TextSettingsCell(context); test.setText(ForkLocale.get(R.string.ForkCameraPreviewTitle), false);
        ForkSettingsLayout.interactive(test);
        test.setOnClickListener(v -> presentFragment(new ForkCameraTestActivity())); rows.addView(test);
        ForkSettingsLayout.addSection(page, rows);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkCameraFallback) + "\n\n" + ForkLocale.get(R.string.ForkVideoQualityDescription));
        page.addView(info);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.addView(page); fragmentView = scroll; refreshValues(); return scroll;
    }
    private void refreshValues() {
        if (camera == null || behavior == null || quality == null) return;
        Context c = camera.getContext();
        String mode = RoundCameraSettings.mode();
        int cameraLabel = "front".equals(mode) ? R.string.ForkCameraFront : "back".equals(mode) ? R.string.ForkCameraBack : R.string.ForkCameraChoiceAuto;
        int behaviorLabel = "last".equals(mode) ? R.string.ForkCameraLast : "gesture".equals(mode) ? R.string.ForkCameraGesture : org.telegram.messenger.fork.camera.RoundCameraPolicy.fixedCamera(mode) ? R.string.ForkCameraBehaviorFixed : R.string.ForkCameraStandard;
        camera.setTextAndValue(ForkLocale.get(R.string.ForkCameraChoiceTitle), ForkLocale.get(cameraLabel), true);
        behavior.setTextAndValue(ForkLocale.get(R.string.ForkCameraBehavior), ForkLocale.get(behaviorLabel), true);
        String qualityMode = RoundCameraSettings.qualityMode();
        int qualityLabel = "auto".equals(qualityMode) ? R.string.ForkVideoQualityAuto : "custom".equals(qualityMode) ? R.string.ForkVideoQualityCustom : R.string.ForkVideoQualityStandard;
        quality.setTextAndValue(ForkLocale.get(R.string.ForkVideoQualityTitle), ForkLocale.get(qualityLabel), true);
    }
    @Override public void onFragmentDestroy() { camera = null; behavior = null; quality = null; super.onFragmentDestroy(); }
}
