package org.telegram.ui;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
/** Compact entry points to the real round-video controls. */
public final class ForkVideoSettingsActivity extends BaseFragment {
    @Override public View createView(Context context) {
        actionBar.setTitle(context.getString(R.string.ForkVideoTitle)); actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { @Override public void onItemClick(int id) { if (id == -1) finishFragment(); } });
        LinearLayout rows = new LinearLayout(context); rows.setOrientation(LinearLayout.VERTICAL);
        rows.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        TextSettingsCell camera = new TextSettingsCell(context); camera.setText(context.getString(R.string.ForkRoundCameraTitle), true);
        camera.setOnClickListener(v -> ForkRoundCameraShortcuts.show(this)); rows.addView(camera);
        TextSettingsCell quality = new TextSettingsCell(context); quality.setText(context.getString(R.string.ForkVideoQualityTitle), true);
        quality.setOnClickListener(v -> ForkRoundCameraShortcuts.showQuality(this)); rows.addView(quality);
        TextSettingsCell test = new TextSettingsCell(context); test.setText(context.getString(R.string.ForkCameraPreviewTitle), false);
        test.setOnClickListener(v -> presentFragment(new ForkCameraTestActivity())); rows.addView(test);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.addView(rows); fragmentView = scroll; return scroll;
    }
}
