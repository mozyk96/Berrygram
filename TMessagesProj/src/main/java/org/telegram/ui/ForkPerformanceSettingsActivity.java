package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
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
    private static final int DIAG_COUNT = 5;

    private TextSettingsCell profileRow;
    private TextCheckCell reduceRow;
    private TextSettingsCell liteModeRow;
    private TextSettingsCell cacheRow;
    private TextSettingsCell copyDiagnosticsRow;
    private final TextSettingsCell[] diagnosticRows = new TextSettingsCell[DIAG_COUNT];

    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkPerformanceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout rows = ForkSettingsLayout.section(context);
        profileRow = new TextSettingsCell(context);
        profileRow.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceProfile), "", false);
        ForkSettingsLayout.interactive(profileRow);
        profileRow.setOnClickListener(this::chooseProfile);
        rows.addView(profileRow);

        reduceRow = new TextCheckCell(context);
        reduceRow.setTextAndCheck(ForkLocale.get(R.string.ForkPerformanceReduceEffects), false, false);
        ForkSettingsLayout.interactive(reduceRow);
        reduceRow.setOnClickListener(v -> {
            PerformanceSettings.setCustomReduceEffects(!PerformanceSettings.customReduceEffects());
            refresh();
        });
        rows.addView(reduceRow);

        liteModeRow = new TextSettingsCell(context);
        liteModeRow.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceLiteMode), ForkLocale.get(R.string.ForkPerformanceLiteModeValue), true);
        ForkSettingsLayout.interactive(liteModeRow);
        liteModeRow.setOnClickListener(v -> presentFragment(new LiteModeSettingsActivity()));
        rows.addView(liteModeRow);

        cacheRow = new TextSettingsCell(context);
        cacheRow.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceStorage), ForkLocale.get(R.string.ForkPerformanceStorageValue), true);
        ForkSettingsLayout.interactive(cacheRow);
        cacheRow.setOnClickListener(v -> presentFragment(new CacheControlActivity()));
        rows.addView(cacheRow);

        TextSettingsCell reset = new TextSettingsCell(context);
        reset.setText(ForkLocale.get(R.string.ForkPerformanceReset), false);
        ForkSettingsLayout.interactive(reset);
        reset.setOnClickListener(v -> {
            PerformanceSettings.reset();
            refresh();
        });
        rows.addView(reset);
        ForkSettingsLayout.addSection(page, rows);

        LinearLayout diagnostics = ForkSettingsLayout.section(context);
        TextSettingsCell title = new TextSettingsCell(context);
        title.setText(ForkLocale.get(R.string.ForkPerformanceDiagnostics), true);
        diagnostics.addView(title);
        for (int i = 0; i < DIAG_COUNT; i++) {
            TextSettingsCell cell = new TextSettingsCell(context);
            cell.setTextAndValue("", "", false);
            diagnosticRows[i] = cell;
            diagnostics.addView(cell);
        }
        copyDiagnosticsRow = new TextSettingsCell(context);
        copyDiagnosticsRow.setText(ForkLocale.get(R.string.ForkPerformanceCopyDiagnostics), true);
        ForkSettingsLayout.interactive(copyDiagnosticsRow);
        copyDiagnosticsRow.setOnClickListener(v -> copyDiagnostics());
        diagnostics.addView(copyDiagnosticsRow);
        ForkSettingsLayout.addSection(page, diagnostics);

        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkPerformanceInfo));
        page.addView(info);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = scroll;
        refresh();
        return fragmentView;
    }

    private void chooseProfile(View anchor) {
        String profile = PerformanceSettings.profile();
        CharSequence[] labels = {
                ForkLocale.get(R.string.ForkPerformanceAutomatic),
                ForkLocale.get(R.string.ForkPerformanceSmooth),
                ForkLocale.get(R.string.ForkPerformanceBattery),
                ForkLocale.get(R.string.ForkPerformanceCustom)
        };
        String[] values = {PerformancePolicy.AUTOMATIC, PerformancePolicy.SMOOTH, PerformancePolicy.BATTERY, PerformancePolicy.CUSTOM};
        int selected = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(profile)) selected = i;
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            PerformanceSettings.setProfile(values[index]);
            refresh();
        });
    }

    private void refresh() {
        if (profileRow == null) return;
        String profile = PerformanceSettings.profile();
        profileRow.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceProfile), profileLabel(profile), false);
        boolean custom = PerformancePolicy.CUSTOM.equals(profile);
        reduceRow.setVisibility(custom ? View.VISIBLE : View.GONE);
        if (custom) reduceRow.setChecked(PerformanceSettings.customReduceEffects());

        diagnosticRows[0].setTextAndValue(ForkLocale.get(R.string.ForkPerformanceDeviceClass), PerformanceSettings.deviceClassLabel(), false);
        diagnosticRows[1].setTextAndValue(ForkLocale.get(R.string.ForkPerformanceActiveProfile), profileLabel(profile), false);
        diagnosticRows[2].setTextAndValue(ForkLocale.get(R.string.ForkPerformancePowerSaver),
                ForkLocale.get(PerformanceSettings.systemPowerSaver() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff), false);
        diagnosticRows[3].setTextAndValue(ForkLocale.get(R.string.ForkPerformanceGraphics), styleLabel(), false);
        diagnosticRows[4].setTextAndValue(ForkLocale.get(R.string.ForkPerformanceAnimations),
                ForkLocale.get(ForkSurfaceAnimation.enabled() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff), false);
    }

    private void copyDiagnostics() {
        StringBuilder text = new StringBuilder();
        text.append("BerryGram ").append(org.telegram.messenger.BuildVars.BUILD_VERSION_STRING).append('\n');
        text.append("Performance profile: ").append(profileLabel(PerformanceSettings.profile())).append('\n');
        text.append("Device class: ").append(PerformanceSettings.deviceClassLabel()).append('\n');
        text.append("System power saver: ").append(PerformanceSettings.systemPowerSaver()).append('\n');
        text.append("Effective graphics: ").append(styleLabel()).append('\n');
        text.append("Reveal animations: ").append(ForkSurfaceAnimation.enabled()).append('\n');
        text.append("Android SDK: ").append(android.os.Build.VERSION.SDK_INT);
        AndroidUtilities.addToClipboard(text.toString());
        if (getParentActivity() != null) {
            org.telegram.ui.Components.BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.copy, ForkLocale.get(R.string.ForkPerformanceDiagnosticsCopied))
                    .show();
        }
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
        refresh();
    }
}
