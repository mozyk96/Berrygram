package org.telegram.ui;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.voice.VoiceQualitySettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
/** Controls the Opus bitrate actually used when a new voice recording starts. */
public final class ForkVoiceSettingsActivity extends BaseFragment {
    private LinearLayout rows;
    @Override public View createView(Context c) {
        actionBar.setTitle(c.getString(R.string.ForkVoiceTitle)); actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { @Override public void onItemClick(int id) { if (id == -1) finishFragment(); } });
        LinearLayout page = ForkSettingsLayout.page(c); rows = ForkSettingsLayout.section(c); ForkSettingsLayout.addSection(page, rows);
        ScrollView scroll = new ScrollView(c); scroll.setFillViewport(true); scroll.addView(page); fragmentView = scroll; render(); return scroll;
    }
    private void render() {
        if (rows == null) return; Context c = rows.getContext(); rows.removeAllViews();
        String mode = VoiceQualitySettings.mode();
        int label = "auto".equals(mode) ? R.string.ForkVoiceAuto : "custom".equals(mode) ? R.string.ForkVoiceCustom : R.string.ForkVoiceStandard;
        TextSettingsCell modeCell = new TextSettingsCell(c); modeCell.setTextAndValue(c.getString(R.string.ForkVoiceMode), c.getString(label), true);
        ForkSettingsLayout.interactive(modeCell);
        modeCell.setOnClickListener(v -> chooseMode()); rows.addView(modeCell);
        if ("custom".equals(mode)) {
            TextSettingsCell rate = new TextSettingsCell(c); rate.setTextAndValue(c.getString(R.string.ForkVoiceBitrate), (VoiceQualitySettings.customBitrate() / 1000) + " kbps", false);
            ForkSettingsLayout.interactive(rate);
            rate.setOnClickListener(v -> chooseBitrate()); rows.addView(rate);
        }
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(c); info.setText(c.getString(R.string.ForkVoiceInfo)); rows.addView(info);
    }
    private void chooseMode() {
        Context c = getParentActivity(); if (c == null) return;
        String[] modes = {"standard", "auto", "custom"}; int[] names = {R.string.ForkVoiceStandard, R.string.ForkVoiceAuto, R.string.ForkVoiceCustom};
        CharSequence[] labels = new CharSequence[modes.length]; for (int i=0; i<modes.length; i++) labels[i] = (modes[i].equals(VoiceQualitySettings.mode()) ? "✓ " : "") + c.getString(names[i]);
        showDialog(new AlertDialog.Builder(c).setTitle(c.getString(R.string.ForkVoiceMode)).setItems(labels, (d, i) -> { VoiceQualitySettings.setMode(modes[i]); render(); }).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
    }
    private void chooseBitrate() {
        Context c = getParentActivity(); if (c == null) return;
        int[] rates = {24000, 32000, 48000, 64000}; CharSequence[] labels = {"24 kbps", "32 kbps", "48 kbps", "64 kbps"};
        showDialog(new AlertDialog.Builder(c).setTitle(c.getString(R.string.ForkVoiceBitrate)).setItems(labels, (d, i) -> { VoiceQualitySettings.setCustomBitrate(rates[i]); render(); }).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
    }
    @Override public void onFragmentDestroy() { rows = null; super.onFragmentDestroy(); }
}
