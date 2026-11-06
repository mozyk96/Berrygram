package org.telegram.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.panels.ForkAnimationSettings;
import org.telegram.messenger.fork.panels.ForkDockSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.PanelPolicy;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.Arrays;
import java.util.List;

/**
 * Unified Appearance Settings for Berrygram 1.4.0.
 * Exactly 3 visual modes: Glass 2.0, Blur, No effects.
 * Obsolete sliders and obsolete device checkboxes removed.
 */
public final class ForkAppearanceSettingsActivity extends BaseFragment {

    private TextSettingsCell styleCell;
    private TextSettingsCell glassPreviewCell;
    private TextSettingsCell radiusCell;
    private SeekBar radiusSlider;

    @Override
    public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.ForkAppearanceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout controls = ForkSettingsLayout.section(context);

        // Visual Mode Selector (Glass 2.0, Blur, None)
        styleCell = new TextSettingsCell(context);
        styleCell.setTextAndValue(LocaleController.getString(R.string.ForkGraphicsStyle), styleLabel(), false);
        styleCell.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(styleCell);
        controls.addView(styleCell);

        // Glass 2.0 Live Preview
        glassPreviewCell = new TextSettingsCell(context);
        glassPreviewCell.setTextAndValue(LocaleController.getString(R.string.ForkGlassTitle),
                LocaleController.getString(R.string.ForkGlassSubtitle), false);
        glassPreviewCell.setIcon(R.drawable.msg_palette);
        ForkSettingsLayout.interactive(glassPreviewCell);
        glassPreviewCell.setVisibility(PanelPolicy.STYLE_GLASS.equals(ForkPanelSettings.style()) ? View.VISIBLE : View.GONE);
        glassPreviewCell.setOnClickListener(v -> presentFragment(new ForkExperimentalGlassActivity()));
        controls.addView(glassPreviewCell);

        styleCell.setOnClickListener(v -> {
            String[] modes = {PanelPolicy.STYLE_GLASS, PanelPolicy.STYLE_BLUR, PanelPolicy.STYLE_NONE};
            CharSequence[] labels = {
                    LocaleController.getString(R.string.ForkGraphicsGlass),
                    LocaleController.getString(R.string.ForkGraphicsBlur),
                    LocaleController.getString(R.string.ForkGraphicsNone)
            };
            int currentSelection = Arrays.asList(modes).indexOf(ForkPanelSettings.style());
            if (currentSelection < 0) currentSelection = 0;

            ForkChoicePopup.show(this, v, labels, null, currentSelection, index -> {
                String chosen = modes[index];
                ForkPanelSettings.setStyle(chosen);
                if (PanelPolicy.STYLE_GLASS.equals(chosen)) {
                    org.telegram.messenger.LiteMode.setAllFlags(
                            org.telegram.messenger.LiteMode.getValue(true) | org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS);
                } else {
                    org.telegram.messenger.LiteMode.toggleFlag(org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS, false);
                }
                styleCell.setTextAndValue(LocaleController.getString(R.string.ForkGraphicsStyle), styleLabel(), false);
                glassPreviewCell.setVisibility(PanelPolicy.STYLE_GLASS.equals(chosen) ? View.VISIBLE : View.GONE);
                ForkSettingsLayout.refreshSection(controls, false);
                ForkFloatingHeader.refresh(fragmentView);
            });
        });

        // Corner Radius Slider
        radiusCell = new TextSettingsCell(context);
        radiusCell.setTextAndValue(LocaleController.getString(R.string.ForkAppearanceRadius),
                LocaleController.formatString(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()), false);
        controls.addView(radiusCell);

        radiusSlider = new SeekBar(context);
        radiusSlider.setMax(20);
        radiusSlider.setProgress(ForkPanelSettings.radiusDp() - 12);
        radiusSlider.setContentDescription(LocaleController.getString(R.string.ForkAppearanceRadius));
        int accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        radiusSlider.setProgressTintList(ColorStateList.valueOf(accentColor));
        radiusSlider.setThumbTintList(ColorStateList.valueOf(accentColor));
        int padding = AndroidUtilities.dp(20);
        radiusSlider.setPadding(padding, 0, padding, 0);
        controls.addView(radiusSlider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));

        radiusSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!fromUser) return;
                ForkPanelSettings.setRadiusDp(value + 12);
                radiusCell.setTextAndValue(LocaleController.getString(R.string.ForkAppearanceRadius),
                        LocaleController.formatString(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()), false);
            }

            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });

        // Advanced Animations 2.0
        TextCheckCell animations = new TextCheckCell(context);
        animations.setTextAndCheck(LocaleController.getString(R.string.ForkAdvancedAnimations),
                ForkAnimationSettings.fluidPopupsEnabled(), false);
        ForkSettingsLayout.interactive(animations);
        animations.setOnClickListener(v -> {
            boolean enabled = !ForkAnimationSettings.fluidPopupsEnabled();
            ForkAnimationSettings.setFluidPopupsEnabled(enabled);
            animations.setChecked(enabled);
        });
        controls.addView(animations);

        // Mini Dock
        TextCheckCell dock = new TextCheckCell(context);
        dock.setTextAndCheck(LocaleController.getString(R.string.ForkMiniDock),
                ForkDockSettings.miniDockEnabled(), false);
        ForkSettingsLayout.interactive(dock);
        dock.setOnClickListener(v -> {
            boolean enabled = !ForkDockSettings.miniDockEnabled();
            ForkDockSettings.setMiniDockEnabled(enabled);
            dock.setChecked(enabled);
        });
        controls.addView(dock);

        ForkSettingsLayout.addSection(page, controls);

        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(LocaleController.getString(R.string.ForkGraphicsInfoUnified));
        page.addView(info);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll,
                LocaleController.getString(R.string.ForkAppearanceTitle), this::finishFragment);
        return fragmentView;
    }

    public static int accent(Context context) {
        Integer custom = org.telegram.messenger.fork.panels.GraphicsColorPolicy.parseHex(ForkPanelSettings.customAccent());
        if (custom != null) return custom;
        if (android.os.Build.VERSION.SDK_INT >= 31 && context != null) {
            try { return context.getColor(android.R.color.system_accent1_500); } catch (RuntimeException ignored) { }
        }
        return Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
    }

    private static String styleLabel() {
        String style = ForkPanelSettings.style();
        if (PanelPolicy.STYLE_BLUR.equals(style)) {
            return LocaleController.getString(R.string.ForkGraphicsBlur);
        } else if (PanelPolicy.STYLE_NONE.equals(style)) {
            return LocaleController.getString(R.string.ForkGraphicsNone);
        }
        return LocaleController.getString(R.string.ForkGraphicsGlass);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (glassPreviewCell != null) {
            glassPreviewCell.setVisibility(PanelPolicy.STYLE_GLASS.equals(ForkPanelSettings.style()) ? View.VISIBLE : View.GONE);
        }
        if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView);
    }
}
