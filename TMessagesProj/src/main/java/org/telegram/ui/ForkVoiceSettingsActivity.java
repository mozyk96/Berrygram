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
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
/** Controls the Opus bitrate actually used when a new voice recording starts. */
public final class ForkVoiceSettingsActivity extends BaseFragment {
    private LinearLayout rows;
    @Override public View createView(Context c) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkVoiceTitle)); actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { @Override public void onItemClick(int id) { if (id == -1) finishFragment(); } });
        LinearLayout page = ForkSettingsLayout.page(c); rows = ForkSettingsLayout.section(c); ForkSettingsLayout.addSection(page, rows);
        ScrollView scroll = new ScrollView(c); scroll.setFillViewport(true); scroll.addView(page); fragmentView = ForkFloatingHeader.wrap(c, actionBar, scroll, ForkLocale.get(R.string.ForkVoiceTitle), this::finishFragment); render(); return fragmentView;
    }
    private void render() {
        if (rows == null) return; Context c = rows.getContext(); rows.removeAllViews();
        String mode = VoiceQualitySettings.mode();
        int label = "auto".equals(mode) ? R.string.ForkVoiceAuto : "custom".equals(mode) ? R.string.ForkVoiceCustom : R.string.ForkVoiceStandard;
        TextSettingsCell modeCell = new TextSettingsCell(c); modeCell.setTextAndValue(ForkLocale.get(R.string.ForkVoiceMode), ForkLocale.get(label), true);
        ForkSettingsLayout.interactive(modeCell);
        modeCell.setOnClickListener(this::chooseMode); rows.addView(modeCell);
        if ("custom".equals(mode)) {
            TextSettingsCell rate = new TextSettingsCell(c); rate.setTextAndValue(ForkLocale.get(R.string.ForkVoiceBitrate), (VoiceQualitySettings.customBitrate() / 1000) + " kbps", false);
            ForkSettingsLayout.interactive(rate);
            rate.setOnClickListener(this::chooseBitrate); rows.addView(rate);
        }
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(c); info.setText(ForkLocale.get(R.string.ForkVoiceInfo)); rows.addView(info);
    }
    private void chooseMode(View anchor) {
        Context c = getParentActivity(); if (c == null) return;
        String[] modes = {"standard", "auto", "custom"}; int[] names = {R.string.ForkVoiceStandard, R.string.ForkVoiceAuto, R.string.ForkVoiceCustom};
        CharSequence[] labels = new CharSequence[modes.length]; for (int i=0; i<modes.length; i++) labels[i] = ForkLocale.get(names[i]);
        ForkChoicePopup.show(this, anchor, labels, null, java.util.Arrays.asList(modes).indexOf(VoiceQualitySettings.mode()), i -> { VoiceQualitySettings.setMode(modes[i]); render(); });
    }
    private void chooseBitrate(View anchor) {
        Context c = getParentActivity(); if (c == null) return;
        int[] rates = {24000, 32000, 48000, 64000}; CharSequence[] labels = {"24 kbps", "32 kbps", "48 kbps", "64 kbps"};
        int selected = -1; for (int i=0; i<rates.length; i++) if (rates[i] == VoiceQualitySettings.customBitrate()) selected = i;
        ForkChoicePopup.show(this, anchor, labels, null, selected, i -> { VoiceQualitySettings.setCustomBitrate(rates[i]); render(); });
    }
    @Override public void onFragmentDestroy() { rows = null; super.onFragmentDestroy(); }
}
