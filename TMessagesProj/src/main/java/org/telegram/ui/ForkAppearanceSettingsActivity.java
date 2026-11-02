package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

/** Controls the existing popup renderer; unfinished graphic modes are intentionally absent. */
public final class ForkAppearanceSettingsActivity extends BaseFragment {
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkAppearanceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout controls = ForkSettingsLayout.section(context);
        TextCheckCell panels = new TextCheckCell(context);
        panels.setTextAndCheck(ForkLocale.get(R.string.ForkPanelsTitle), ForkPanelSettings.enabled(), false);
        ForkSettingsLayout.interactive(panels);
        controls.addView(panels);
        TextSettingsCell radius = new TextSettingsCell(context);
        radius.setTextAndValue(ForkLocale.get(R.string.ForkAppearanceRadius), ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()), false);
        controls.addView(radius);
        SeekBar slider = new SeekBar(context);
        slider.setMax(20);
        slider.setProgress(ForkPanelSettings.radiusDp() - 12);
        slider.setContentDescription(ForkLocale.get(R.string.ForkAppearanceRadius));
        slider.setEnabled(ForkPanelSettings.enabled());
        int padding = AndroidUtilities.dp(20);
        slider.setPadding(padding, 0, padding, 0);
        controls.addView(slider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        panels.setOnClickListener(v -> {
            boolean enabled = !ForkPanelSettings.enabled();
            ForkPanelSettings.setEnabled(enabled);
            panels.setChecked(enabled);
            slider.setEnabled(enabled);
        });
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!fromUser) return;
                ForkPanelSettings.setRadiusDp(value + 12);
                radius.setTextAndValue(ForkLocale.get(R.string.ForkAppearanceRadius), ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()), false);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        ForkSettingsLayout.addSection(page, controls);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkPanelsInfo) + "\n\n" + ForkLocale.get(R.string.ForkAppearanceRadiusInfo));
        page.addView(info);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = scroll;
        return scroll;
    }
}
