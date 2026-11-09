package org.telegram.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.panels.ForkGlassSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.GlassPolicy;
import org.telegram.messenger.fork.panels.GraphicsColorPolicy;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

/** Experimental Glass 2.0 controls with a real in-place preview. */
public final class ForkExperimentalGlassActivity extends ForkSettingsFragment {
    private LinearLayout rows;
    private ForkGlassPreview preview;
    private TextSettingsCell presetRow, tintRow;
    private SeekBar transparency, blur, frost, saturation, edge, highlight, shadow, radius;
    private final java.util.ArrayList<SliderBinding> bindings = new java.util.ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkGlassTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        bindings.clear();
        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout previewCard = ForkSettingsLayout.section(context);
        preview = new ForkGlassPreview(context);
        previewCard.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(150)));
        ForkSettingsLayout.addSection(page, previewCard);

        rows = ForkSettingsLayout.section(context);
        ForkSettingsLayout.addSection(page, rows);

        presetRow = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(presetRow);
        presetRow.setOnClickListener(this::choosePreset);
        rows.addView(presetRow);

        ForkGlassSettings.Config config = ForkGlassSettings.config();
        int accent = ForkAppearanceSettingsActivity.accent(context);
        transparency = addSlider(context, rows, R.string.ForkGlassTransparency,
                R.string.ForkGlassValuePercent, 0, 100, config.transparency, value -> {
                    ForkGlassSettings.setTransparency(value);
                    refreshPreview();
                }, accent);
        blur = addSlider(context, rows, R.string.ForkGlassBlur,
                R.string.ForkGlassValuePercent, 0, 100, config.blur, value -> {
                    ForkGlassSettings.setBlur(value);
                    refreshPreview();
                }, accent);
        frost = addSlider(context, rows, R.string.ForkGlassFrost,
                R.string.ForkGlassValuePercent, 0, 100, config.frost, value -> {
                    ForkGlassSettings.setFrost(value);
                    refreshPreview();
                }, accent);
        saturation = addSlider(context, rows, R.string.ForkGlassSaturation,
                R.string.ForkGlassValuePercent, 30, 180, config.saturation, value -> {
                    ForkGlassSettings.setSaturation(value);
                    refreshPreview();
                }, accent);
        edge = addSlider(context, rows, R.string.ForkGlassEdge,
                R.string.ForkGlassValuePercent, 0, 100, config.edge, value -> {
                    ForkGlassSettings.setEdge(value);
                    refreshPreview();
                }, accent);
        highlight = addSlider(context, rows, R.string.ForkGlassHighlight,
                R.string.ForkGlassValuePercent, 0, 100, config.highlight, value -> {
                    ForkGlassSettings.setHighlight(value);
                    refreshPreview();
                }, accent);
        shadow = addSlider(context, rows, R.string.ForkGlassShadow,
                R.string.ForkGlassValuePercent, 0, 100, config.shadow, value -> {
                    ForkGlassSettings.setShadow(value);
                    refreshPreview();
                }, accent);
        radius = addSlider(context, rows, R.string.ForkAppearanceRadius,
                R.string.ForkAppearanceRadiusValue, 12, 32, ForkPanelSettings.radiusDp(), value -> {
                    ForkPanelSettings.setRadiusDp(value);
                    refreshPreview();
                }, accent);
        tintRow = new TextSettingsCell(context);
        ForkSettingsLayout.interactive(tintRow);
        tintRow.setOnClickListener(this::chooseTint);
        rows.addView(tintRow);

        TextCheckCell adaptive = new TextCheckCell(context);
        adaptive.setTextAndCheck(ForkLocale.get(R.string.ForkGraphicsAdaptive),
                ForkGlassSettings.config().adaptive, true);
        ForkSettingsLayout.interactive(adaptive);
        adaptive.setOnClickListener(v -> {
            boolean enabled = !ForkGlassSettings.config().adaptive;
            ForkGlassSettings.setAdaptive(enabled);
            adaptive.setChecked(enabled);
            refreshPreview();
        });
        rows.addView(adaptive);

        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkGlassInfo));
        page.addView(info);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll,
                ForkLocale.get(R.string.ForkGlassTitle), this::finishFragment);
        syncRows();
        return fragmentView;
    }

    private SeekBar addSlider(Context context, LinearLayout parent, int title, int valueFormat,
                              int min, int max, int initial, ValueSetter setter, int accent) {
        TextSettingsCell row = new TextSettingsCell(context);
        row.setTextAndValue(ForkLocale.get(title),
                ForkLocale.get(valueFormat, initial), false);
        parent.addView(row);
        SeekBar slider = new SeekBar(context);
        bindings.add(new SliderBinding(row, title, valueFormat, min, slider));
        slider.setMax(max - min);
        slider.setProgress(initial - min);
        slider.setContentDescription(ForkLocale.get(title));
        ColorStateList tint = ColorStateList.valueOf(accent);
        slider.setProgressTintList(tint);
        slider.setThumbTintList(tint);
        int padding = AndroidUtilities.dp(20);
        slider.setPadding(padding, 0, padding, 0);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = min + progress;
                setter.set(value);
                row.setTextAndValue(ForkLocale.get(title), ForkLocale.get(valueFormat, value), false);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        parent.addView(slider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        return slider;
    }

    private void choosePreset(View anchor) {
        String[] presets = {GlassPolicy.PRESET_CLEAR, GlassPolicy.PRESET_FROSTED,
                GlassPolicy.PRESET_BALANCED, GlassPolicy.PRESET_DEEP, GlassPolicy.PRESET_CUSTOM};
        CharSequence[] labels = {
                ForkLocale.get(R.string.ForkGlassPresetClear),
                ForkLocale.get(R.string.ForkGlassPresetFrosted),
                ForkLocale.get(R.string.ForkGlassPresetBalanced),
                ForkLocale.get(R.string.ForkGlassPresetDeep),
                ForkLocale.get(R.string.ForkVoiceCustom)
        };
        int selected = java.util.Arrays.asList(presets).indexOf(
                GlassPolicy.normalizePreset(ForkGlassSettings.config().preset));
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            ForkGlassSettings.applyPreset(presets[index]);
            syncRows();
            refreshPreview();
        });
    }

    private void chooseTint(View anchor) {
        Context context = getParentActivity();
        if (context == null) return;
        ForkGlassSettings.Config config = ForkGlassSettings.config();
        Integer saved = GraphicsColorPolicy.parseHex(config.customTint);
        ForkHsvPicker picker = new ForkHsvPicker(context,
                saved != null ? saved : ForkAppearanceSettingsActivity.accent(context),
                value -> { });
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(ForkLocale.get(R.string.ForkGlassTint))
                .setView(picker)
                .setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.Save), null)
                .setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), null)
                .setNeutralButton(ForkLocale.get(R.string.ForkColorSystem), (d, w) -> {
                    ForkGlassSettings.setTintMode("theme", null);
                    syncRows();
                    refreshPreview();
                }).create();
        if (showDialog(dialog, d -> { }) != null) {
            View save = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (save != null) save.setOnClickListener(v -> {
                if (!picker.valid()) {
                    picker.showError();
                    return;
                }
                ForkGlassSettings.setTintMode("custom",
                        GraphicsColorPolicy.formatHex(picker.selectedColor()));
                syncRows();
                refreshPreview();
                dialog.dismiss();
            });
        }
    }

    private void syncRows() {
        ForkGlassSettings.Config config = ForkGlassSettings.config();
        presetRow.setTextAndValue(ForkLocale.get(R.string.ForkGlassPreset),
                presetLabel(config.preset), false);
        tintRow.setTextAndValue(ForkLocale.get(R.string.ForkGlassTint), tintLabel(config), false);
        setSlider(transparency, config.transparency, 0);
        setSlider(blur, config.blur, 0);
        setSlider(frost, config.frost, 0);
        setSlider(saturation, config.saturation, 30);
        setSlider(edge, config.edge, 0);
        setSlider(highlight, config.highlight, 0);
        setSlider(shadow, config.shadow, 0);
        setSlider(radius, ForkPanelSettings.radiusDp(), 12);
        for (SliderBinding binding : bindings) {
            int value = binding.min + binding.slider.getProgress();
            binding.row.setTextAndValue(ForkLocale.get(binding.title),
                    ForkLocale.get(binding.valueFormat, value), false);
        }
    }

    private static void setSlider(SeekBar slider, int value, int min) {
        if (slider != null) slider.setProgress(value - min);
    }

    private static final class SliderBinding {
        final TextSettingsCell row;
        final int title;
        final int valueFormat;
        final int min;
        final SeekBar slider;

        SliderBinding(TextSettingsCell row, int title, int valueFormat, int min, SeekBar slider) {
            this.row = row;
            this.title = title;
            this.valueFormat = valueFormat;
            this.min = min;
            this.slider = slider;
        }
    }

    private static String presetLabel(String preset) {
        if (GlassPolicy.PRESET_CLEAR.equals(preset)) return ForkLocale.get(R.string.ForkGlassPresetClear);
        if (GlassPolicy.PRESET_FROSTED.equals(preset)) return ForkLocale.get(R.string.ForkGlassPresetFrosted);
        if (GlassPolicy.PRESET_DEEP.equals(preset)) return ForkLocale.get(R.string.ForkGlassPresetDeep);
        if (GlassPolicy.PRESET_BALANCED.equals(preset)) return ForkLocale.get(R.string.ForkGlassPresetBalanced);
        return ForkLocale.get(R.string.ForkVoiceCustom);
    }

    private static String tintLabel(ForkGlassSettings.Config config) {
        if (config.customTintEnabled()) return config.customTint;
        return ForkLocale.get(R.string.ForkColorSystem);
    }

    private void refreshPreview() {
        if (preview != null) preview.setConfig(ForkGlassSettings.config());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPreview();
        syncRows();
        if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView);
    }

    @Override
    public void onFragmentDestroy() {
        rows = null;
        preview = null;
        presetRow = null;
        tintRow = null;
        transparency = blur = frost = saturation = edge = highlight = shadow = radius = null;
        bindings.clear();
        super.onFragmentDestroy();
    }

    private interface ValueSetter {
        void set(int value);
    }
}
