package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.R;
import org.telegram.messenger.fork.performance.PerformancePolicy;
import org.telegram.messenger.fork.performance.PerformanceSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

/** BerryGram Performance Engine profile and non-sensitive diagnostics. */
public final class ForkPerformanceSettingsActivity extends BaseFragment {
    private LinearLayout rows;
    private LinearLayout diagnostics;

    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkPerformanceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout page = ForkSettingsLayout.page(context);
        rows = ForkSettingsLayout.section(context);
        diagnostics = ForkSettingsLayout.section(context);
        ForkSettingsLayout.addSection(page, rows);
        ForkSettingsLayout.addSection(page, diagnostics);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkPerformanceInfo));
        page.addView(info);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = scroll;
        rebuild(context);
        return fragmentView;
    }

    private void rebuild(Context context) {
        if (rows == null || diagnostics == null) return;
        rows.removeAllViews();
        final String profile = PerformanceSettings.profile();
        addRow(context, ForkLocale.get(R.string.ForkPerformanceProfile), profileLabel(profile), v -> {
            CharSequence[] labels = {
                    ForkLocale.get(R.string.ForkPerformanceAutomatic),
                    ForkLocale.get(R.string.ForkPerformanceSmooth),
                    ForkLocale.get(R.string.ForkPerformanceBattery),
                    ForkLocale.get(R.string.ForkPerformanceCustom)
            };
            String[] values = {PerformancePolicy.AUTOMATIC, PerformancePolicy.SMOOTH, PerformancePolicy.BATTERY, PerformancePolicy.CUSTOM};
            int selected = 0;
            for (int i = 0; i < values.length; i++) if (values[i].equals(profile)) selected = i;
            ForkChoicePopup.show(this, v, labels, null, selected, index -> {
                PerformanceSettings.setProfile(values[index]);
                rebuild(context);
                if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView);
            });
        });
        if (PerformancePolicy.CUSTOM.equals(profile)) {
            TextCheckCell reduce = new TextCheckCell(context);
            reduce.setTextAndCheck(ForkLocale.get(R.string.ForkPerformanceReduceEffects), PerformanceSettings.customReduceEffects(), false);
            ForkSettingsLayout.interactive(reduce);
            reduce.setOnClickListener(v -> {
                boolean enabled = !PerformanceSettings.customReduceEffects();
                PerformanceSettings.setCustomReduceEffects(enabled);
                reduce.setChecked(enabled);
                rebuild(context);
                if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView);
            });
            rows.addView(reduce);
        }
        TextSettingsCell reset = new TextSettingsCell(context);
        reset.setText(ForkLocale.get(R.string.ForkPerformanceReset), false);
        ForkSettingsLayout.interactive(reset);
        reset.setOnClickListener(v -> {
            PerformanceSettings.reset();
            rebuild(context);
            if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView);
        });
        rows.addView(reset);
        rebuildDiagnostics(context);
    }

    private void rebuildDiagnostics(Context context) {
        diagnostics.removeAllViews();
        TextSettingsCell title = new TextSettingsCell(context);
        title.setText(ForkLocale.get(R.string.ForkPerformanceDiagnostics), true);
        diagnostics.addView(title);
        addDiagnostic(context, R.string.ForkPerformanceDeviceClass, PerformanceSettings.deviceClassLabel());
        addDiagnostic(context, R.string.ForkPerformanceActiveProfile, profileLabel(PerformanceSettings.profile()));
        addDiagnostic(context, R.string.ForkPerformancePowerSaver,
                ForkLocale.get(PerformanceSettings.systemPowerSaver() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff));
        addDiagnostic(context, R.string.ForkPerformanceGraphics, styleLabel());
        addDiagnostic(context, R.string.ForkPerformanceAnimations,
                ForkLocale.get(ForkSurfaceAnimation.enabled() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff));
    }

    private void addRow(Context context, CharSequence title, CharSequence value, View.OnClickListener listener) {
        TextSettingsCell cell = new TextSettingsCell(context);
        cell.setTextAndValue(title, value, true);
        ForkSettingsLayout.interactive(cell);
        cell.setOnClickListener(listener);
        rows.addView(cell);
    }

    private void addDiagnostic(Context context, int title, String value) {
        TextSettingsCell cell = new TextSettingsCell(context);
        cell.setTextAndValue(ForkLocale.get(title), value, false);
        diagnostics.addView(cell);
    }

    private static String profileLabel(String profile) {
        if (PerformancePolicy.SMOOTH.equals(profile)) return ForkLocale.get(R.string.ForkPerformanceSmooth);
        if (PerformancePolicy.BATTERY.equals(profile)) return ForkLocale.get(R.string.ForkPerformanceBattery);
        if (PerformancePolicy.CUSTOM.equals(profile)) return ForkLocale.get(R.string.ForkPerformanceCustom);
        return ForkLocale.get(R.string.ForkPerformanceAutomatic);
    }

    private static String styleLabel() {
        String style = ForkGraphicsSurface.effectiveStyle();
        if ("liquid".equals(style)) return ForkLocale.get(R.string.ForkGraphicsLiquid);
        if ("blur".equals(style)) return ForkLocale.get(R.string.ForkGraphicsBlur);
        return ForkLocale.get(R.string.ForkPerformanceOpaque);
    }

    @Override public void onResume() {
        super.onResume();
        if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView);
        if (rows != null) rebuild(rows.getContext());
    }

    @Override public void onFragmentDestroy() { rows = null; diagnostics = null; super.onFragmentDestroy(); }
}
