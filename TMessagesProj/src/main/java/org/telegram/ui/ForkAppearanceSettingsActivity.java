package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.content.res.ColorStateList;
import android.os.Build;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.GraphicsColorPolicy;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.ItemOptions;

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
        tint(slider, accent(context));
        int padding = AndroidUtilities.dp(20);
        slider.setPadding(padding, 0, padding, 0);
        controls.addView(slider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        TextSettingsCell color = new TextSettingsCell(context);
        color.setTextAndValue(ForkLocale.get(R.string.ForkColorTitle), accentLabel(), false);
        color.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(color);
        controls.addView(color);
        TextSettingsCell preview = new TextSettingsCell(context);
        preview.setText(ForkLocale.get(R.string.ForkAppearancePreview), false);
        preview.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(preview);
        preview.setOnClickListener(v -> ItemOptions.makeOptions(this, v)
                .add(R.drawable.msg_check_s, ForkLocale.get(R.string.ForkAppearancePreviewItem), () -> { })
                .show());
        controls.addView(preview);
        applyAccent(slider, preview, accent(context));
        color.setOnClickListener(v -> chooseAccent(context, slider, color, preview));
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

    private static String accentLabel() {
        String value = ForkPanelSettings.customAccent();
        return value == null ? ForkLocale.get(R.string.ForkColorSystem) : value;
    }
    static int accent(Context context) {
        Integer custom = GraphicsColorPolicy.parseHex(ForkPanelSettings.customAccent());
        if (custom != null) return custom;
        if (Build.VERSION.SDK_INT >= 31) {
            try { return context.getColor(android.R.color.system_accent1_500); } catch (RuntimeException ignored) { }
        }
        return Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
    }
    private static void tint(SeekBar slider, int color) {
        ColorStateList tint = ColorStateList.valueOf(color);
        slider.setProgressTintList(tint);
        slider.setThumbTintList(tint);
    }
    private static void applyAccent(SeekBar slider, TextSettingsCell preview, int color) {
        int readable = GraphicsColorPolicy.readableAccent(color, Theme.getColor(Theme.key_windowBackgroundWhite));
        tint(slider, readable);
        preview.getTextView().setTextColor(readable);
    }
    private void chooseAccent(Context context, SeekBar slider, TextSettingsCell row, TextSettingsCell preview) {
        ForkHsvPicker picker = new ForkHsvPicker(context, accent(context), value -> applyAccent(slider, preview, value));
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(ForkLocale.get(R.string.ForkColorTitle))
                .setView(picker)
                .setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.Save), null)
                .setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), (d, w) -> applyAccent(slider, preview, accent(context)))
                .setNeutralButton(ForkLocale.get(R.string.ForkColorSystem), (d, w) -> {
                    ForkPanelSettings.setCustomAccent(null);
                    applyAccent(slider, preview, accent(context));
                    row.setTextAndValue(ForkLocale.get(R.string.ForkColorTitle), accentLabel(), false);
                }).create();
        if (showDialog(dialog, d -> applyAccent(slider, preview, accent(context))) != null) {
            View save = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (save != null) save.setOnClickListener(v -> {
                if (!picker.valid()) { picker.showError(); return; }
                ForkPanelSettings.setCustomAccent(GraphicsColorPolicy.formatHex(picker.selectedColor()));
                row.setTextAndValue(ForkLocale.get(R.string.ForkColorTitle), accentLabel(), false);
                dialog.dismiss();
            });
        } else applyAccent(slider, preview, accent(context));
    }
}
