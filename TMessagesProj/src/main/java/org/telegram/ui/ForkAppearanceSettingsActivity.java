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
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

/** Controls the supported popup graphics and their preview. */
public final class ForkAppearanceSettingsActivity extends BaseFragment {
    private SeekBar effectControl;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkAppearanceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout controls = ForkSettingsLayout.section(context);
        TextSettingsCell style = new TextSettingsCell(context);
        style.setTextAndValue(ForkLocale.get(R.string.ForkGraphicsStyle), styleLabel(), false);
        style.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(style);
        controls.addView(style);
        TextSettingsCell glass = new TextSettingsCell(context);
        glass.setTextAndValue(ForkLocale.get(R.string.ForkGlassTitle),
                ForkLocale.get(R.string.ForkGlassSubtitle), false);
        glass.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(glass);
        glass.setVisibility("experimental".equals(ForkPanelSettings.style()) ? View.VISIBLE : View.GONE);
        glass.setOnClickListener(v -> presentFragment(new ForkExperimentalGlassActivity()));
        controls.addView(glass);
        TextSettingsCell radius = new TextSettingsCell(context);
        radius.setTextAndValue(ForkLocale.get(R.string.ForkAppearanceRadius), ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()), false);
        controls.addView(radius);
        SeekBar slider = new SeekBar(context);
        slider.setMax(20);
        slider.setProgress(ForkPanelSettings.radiusDp() - 12);
        slider.setContentDescription(ForkLocale.get(R.string.ForkAppearanceRadius));
        tint(slider, accent(context));
        int padding = AndroidUtilities.dp(20);
        slider.setPadding(padding, 0, padding, 0);
        controls.addView(slider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        TextSettingsCell intensity = new TextSettingsCell(context);
        intensity.setTextAndValue(ForkLocale.get(R.string.ForkGraphicsIntensity), ForkLocale.get(R.string.ForkGraphicsIntensityValue, ForkPanelSettings.effectStrength()), false);
        controls.addView(intensity);
        SeekBar effect = new SeekBar(context);
        effectControl = effect;
        effect.setMax(100);
        effect.setProgress(ForkPanelSettings.effectStrength());
        effect.setContentDescription(ForkLocale.get(R.string.ForkGraphicsIntensity));
        effect.setEnabled(effectsAvailable());
        effect.setPadding(padding, 0, padding, 0);
        controls.addView(effect, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        effect.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!fromUser) return;
                ForkPanelSettings.setEffectStrength(value);
                intensity.setTextAndValue(ForkLocale.get(R.string.ForkGraphicsIntensity), ForkLocale.get(R.string.ForkGraphicsIntensityValue, ForkPanelSettings.effectStrength()), false);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        TextCheckCell animations = new TextCheckCell(context);
        animations.setTextAndCheck(ForkLocale.get(R.string.ForkAdvancedAnimations), org.telegram.messenger.fork.panels.ForkAnimationSettings.fluidPopupsEnabled(), false);
        ForkSettingsLayout.interactive(animations);
        animations.setOnClickListener(v -> {
            boolean enabled = !org.telegram.messenger.fork.panels.ForkAnimationSettings.fluidPopupsEnabled();
            org.telegram.messenger.fork.panels.ForkAnimationSettings.setFluidPopupsEnabled(enabled);
            animations.setChecked(enabled);
        });
        controls.addView(animations);
        TextCheckCell dock = new TextCheckCell(context);
        dock.setTextAndCheck(ForkLocale.get(R.string.ForkMiniDock), org.telegram.messenger.fork.panels.ForkDockSettings.miniDockEnabled(), false);
        ForkSettingsLayout.interactive(dock);
        dock.setOnClickListener(v -> {
            boolean enabled = !org.telegram.messenger.fork.panels.ForkDockSettings.miniDockEnabled();
            org.telegram.messenger.fork.panels.ForkDockSettings.setMiniDockEnabled(enabled);
            dock.setChecked(enabled);
        });
        controls.addView(dock);
        TextCheckCell adaptive = new TextCheckCell(context);
        adaptive.setTextAndCheck(ForkLocale.get(R.string.ForkGraphicsAdaptive), ForkPanelSettings.adaptiveEffects(), false);
        ForkSettingsLayout.interactive(adaptive);
        adaptive.setOnClickListener(v -> {
            boolean enabled = !ForkPanelSettings.adaptiveEffects();
            ForkPanelSettings.setAdaptiveEffects(enabled);
            adaptive.setChecked(enabled);
            effect.setEnabled(effectsAvailable());
        });
        controls.addView(adaptive);
        TextSettingsCell color = new TextSettingsCell(context);
        color.setTextAndValue(ForkLocale.get(R.string.ForkColorTitle), accentLabel(), false);
        color.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(color);
        controls.addView(color);
        applyAccent(slider, effect, accent(context));
        color.setOnClickListener(v -> chooseAccent(context, slider, effect, color));
        style.setOnClickListener(v -> {
            String[] modes = {"blur", "liquid", "experimental"};
            CharSequence[] labels = {
                    ForkLocale.get(R.string.ForkGraphicsBlur),
                    ForkLocale.get(R.string.ForkGraphicsLiquid),
                    ForkLocale.get(R.string.ForkGraphicsExperimental)
            };
            ForkChoicePopup.show(this, v, labels, null, java.util.Arrays.asList(modes).indexOf(ForkPanelSettings.style()), index -> {
                        if ("experimental".equals(modes[index])) {
                            ForkPanelSettings.setStyle("experimental");
                            org.telegram.messenger.LiteMode.setAllFlags(
                                    org.telegram.messenger.LiteMode.getValue(true) | org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS);
                        } else {
                            org.telegram.messenger.LiteMode.toggleFlag(org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS, "liquid".equals(modes[index]));
                        }
                        style.setTextAndValue(ForkLocale.get(R.string.ForkGraphicsStyle), styleLabel(), false);
                        glass.setVisibility("experimental".equals(ForkPanelSettings.style()) ? View.VISIBLE : View.GONE);
                        effect.setEnabled(effectsAvailable());
                        ForkSettingsLayout.refreshSection(controls, false);
                        ForkFloatingHeader.refresh(fragmentView);
                        applyAccent(slider, effect, accent(context));
                    });
        });
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!fromUser) return;
                ForkPanelSettings.setRadiusDp(value + 12);
                ForkSettingsLayout.refreshSection(controls, false);
                radius.setTextAndValue(ForkLocale.get(R.string.ForkAppearanceRadius), ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()), false);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        ForkSettingsLayout.addSection(page, controls);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkGraphicsInfo) + "\n\n" + ForkLocale.get(R.string.ForkAppearanceRadiusInfo));
        page.addView(info);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkAppearanceTitle), this::finishFragment);
        return fragmentView;
    }

    private static String accentLabel() {
        String value = ForkPanelSettings.customAccent();
        return value == null ? ForkLocale.get(R.string.ForkColorSystem) : value;
    }
    private static String styleLabel() {
        String style = ForkPanelSettings.style();
        if ("liquid".equals(style)) return ForkLocale.get(R.string.ForkGraphicsLiquid);
        if ("experimental".equals(style)) return ForkLocale.get(R.string.ForkGraphicsExperimental);
        return ForkLocale.get(R.string.ForkGraphicsBlur);
    }
    private static String animationLabel() {
        String mode = ForkPanelSettings.animationMode();
        return ForkLocale.get("minimal".equals(mode) ? R.string.ForkAnimationMinimal : "smooth".equals(mode) ? R.string.ForkAnimationSmooth : R.string.ForkAnimationSystem);
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
    private static boolean effectsAvailable() {
        return org.telegram.messenger.fork.panels.PanelPolicy.useGlass(ForkPanelSettings.enabled(),
                ForkGraphicsSurface.effectsSuppressed(),
                Build.VERSION.SDK_INT, ForkPanelSettings.deviceAllowsEffects(!org.telegram.messenger.SharedConfig.deviceIsLow()));
    }
    private static void applyAccent(SeekBar slider, SeekBar effect, int color) {
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        if ("material".equals(ForkPanelSettings.style())) background = ForkGraphicsSurface.materialColor(slider.getContext(), background);
        int readable = GraphicsColorPolicy.readableAccent(color, background);
        tint(slider, readable);
        tint(effect, readable);
    }
    private void chooseAccent(Context context, SeekBar slider, SeekBar effect, TextSettingsCell row) {
        ForkHsvPicker picker = new ForkHsvPicker(context, accent(context), value -> applyAccent(slider, effect, value));
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(ForkLocale.get(R.string.ForkColorTitle))
                .setView(picker)
                .setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.Save), null)
                .setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), (d, w) -> applyAccent(slider, effect, accent(context)))
                .setNeutralButton(ForkLocale.get(R.string.ForkColorSystem), (d, w) -> {
                    ForkPanelSettings.setCustomAccent(null);
                    applyAccent(slider, effect, accent(context));
                    row.setTextAndValue(ForkLocale.get(R.string.ForkColorTitle), accentLabel(), false);
                }).create();
        if (showDialog(dialog, d -> applyAccent(slider, effect, accent(context))) != null) {
            View save = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (save != null) save.setOnClickListener(v -> {
                if (!picker.valid()) { picker.showError(); return; }
                ForkPanelSettings.setCustomAccent(GraphicsColorPolicy.formatHex(picker.selectedColor()));
                row.setTextAndValue(ForkLocale.get(R.string.ForkColorTitle), accentLabel(), false);
                dialog.dismiss();
            });
        } else applyAccent(slider, effect, accent(context));
    }
    @Override public void onResume() { super.onResume(); if (effectControl != null) effectControl.setEnabled(effectsAvailable()); if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView); }
    @Override public void onFragmentDestroy() { effectControl = null; super.onFragmentDestroy(); }
}
