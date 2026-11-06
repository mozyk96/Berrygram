package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.panels.PanelPolicy;
import org.telegram.messenger.fork.performance.DeviceAnalyzerPolicy;
import org.telegram.messenger.fork.performance.ForkDeviceAnalyzer;
import org.telegram.messenger.fork.performance.PerformancePolicy;
import org.telegram.messenger.fork.performance.PerformanceSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.Arrays;
import java.util.Locale;

/**
 * Performance Center 2.0 with integrated Device Analyzer and hardware diagnostics.
 */
public final class ForkPerformanceSettingsActivity extends BaseFragment {
    private static final int DIAG_COUNT = 6;

    private TextSettingsCell configModeRow;
    private TextSettingsCell manualLevelRow;
    private TextSettingsCell scanButtonRow;
    private TextSettingsCell liteModeRow;
    private TextSettingsCell cacheRow;
    private TextSettingsCell copyDiagnosticsRow;
    private final TextSettingsCell[] diagnosticRows = new TextSettingsCell[DIAG_COUNT];

    @Override
    public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.ForkPerformanceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout rows = ForkSettingsLayout.section(context);

        // Scan Hardware Button
        scanButtonRow = new TextSettingsCell(context);
        scanButtonRow.setTextAndValue(LocaleController.getString(R.string.ForkDeviceScan),
                LocaleController.getString(R.string.ForkDeviceScanAction), false);
        scanButtonRow.setIcon(R.drawable.msg_settings);
        ForkSettingsLayout.interactive(scanButtonRow);
        scanButtonRow.setOnClickListener(v -> scanDeviceAndShowResult(context));
        rows.addView(scanButtonRow);

        // Performance Mode (Automatic vs Manual)
        configModeRow = new TextSettingsCell(context);
        configModeRow.setTextAndValue(LocaleController.getString(R.string.ForkPerformanceConfigMode), "", false);
        ForkSettingsLayout.interactive(configModeRow);
        configModeRow.setOnClickListener(this::chooseConfigMode);
        rows.addView(configModeRow);

        // Manual Level Selector (Low, Medium, High)
        manualLevelRow = new TextSettingsCell(context);
        manualLevelRow.setTextAndValue(LocaleController.getString(R.string.ForkPerformanceManualLevel), "", false);
        ForkSettingsLayout.interactive(manualLevelRow);
        manualLevelRow.setOnClickListener(this::chooseManualLevel);
        rows.addView(manualLevelRow);

        liteModeRow = new TextSettingsCell(context);
        liteModeRow.setTextAndValue(LocaleController.getString(R.string.ForkPerformanceLiteMode),
                LocaleController.getString(R.string.ForkPerformanceLiteModeValue), true);
        ForkSettingsLayout.interactive(liteModeRow);
        liteModeRow.setOnClickListener(v -> presentFragment(new LiteModeSettingsActivity()));
        rows.addView(liteModeRow);

        cacheRow = new TextSettingsCell(context);
        cacheRow.setTextAndValue(LocaleController.getString(R.string.ForkPerformanceStorage),
                LocaleController.getString(R.string.ForkPerformanceStorageValue), true);
        ForkSettingsLayout.interactive(cacheRow);
        cacheRow.setOnClickListener(v -> presentFragment(new CacheControlActivity()));
        rows.addView(cacheRow);

        TextSettingsCell reset = new TextSettingsCell(context);
        reset.setText(LocaleController.getString(R.string.ForkPerformanceReset), false);
        ForkSettingsLayout.interactive(reset);
        reset.setOnClickListener(v -> {
            PerformanceSettings.reset();
            refresh();
        });
        rows.addView(reset);
        ForkSettingsLayout.addSection(page, rows);

        LinearLayout diagnostics = ForkSettingsLayout.section(context);
        TextSettingsCell title = new TextSettingsCell(context);
        title.setText(LocaleController.getString(R.string.ForkPerformanceDiagnostics), true);
        diagnostics.addView(title);
        for (int i = 0; i < DIAG_COUNT; i++) {
            TextSettingsCell cell = new TextSettingsCell(context);
            cell.setTextAndValue("", "", false);
            diagnosticRows[i] = cell;
            diagnostics.addView(cell);
        }
        copyDiagnosticsRow = new TextSettingsCell(context);
        copyDiagnosticsRow.setText(LocaleController.getString(R.string.ForkPerformanceCopyDiagnostics), true);
        ForkSettingsLayout.interactive(copyDiagnosticsRow);
        copyDiagnosticsRow.setOnClickListener(v -> copyDiagnostics());
        diagnostics.addView(copyDiagnosticsRow);
        ForkSettingsLayout.addSection(page, diagnostics);

        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(LocaleController.getString(R.string.ForkPerformanceInfoUnified));
        page.addView(info);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = scroll;
        refresh();
        return fragmentView;
    }

    private void chooseConfigMode(View anchor) {
        CharSequence[] labels = {
                LocaleController.getString(R.string.ForkPerformanceAutoMode),
                LocaleController.getString(R.string.ForkPerformanceManualMode)
        };
        String[] values = {PerformancePolicy.CONFIG_AUTO, PerformancePolicy.CONFIG_MANUAL};
        int selected = PerformancePolicy.CONFIG_MANUAL.equals(PerformanceSettings.configMode()) ? 1 : 0;
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            PerformanceSettings.setConfigMode(values[index]);
            refresh();
        });
    }

    private void chooseManualLevel(View anchor) {
        CharSequence[] labels = {
                LocaleController.getString(R.string.ForkPerformanceLevelLow),
                LocaleController.getString(R.string.ForkPerformanceLevelMedium),
                LocaleController.getString(R.string.ForkPerformanceLevelHigh)
        };
        String[] values = {PerformancePolicy.LEVEL_LOW, PerformancePolicy.LEVEL_MEDIUM, PerformancePolicy.LEVEL_HIGH};
        int selected = Arrays.asList(values).indexOf(PerformanceSettings.manualLevel());
        if (selected < 0) selected = 1;
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            PerformanceSettings.setManualLevel(values[index]);
            refresh();
        });
    }

    private void scanDeviceAndShowResult(Context context) {
        DeviceAnalyzerPolicy.DeviceSpecs specs = ForkDeviceAnalyzer.scanDevice();
        DeviceAnalyzerPolicy.Assessment assessment = ForkDeviceAnalyzer.getAssessment(true);

        StringBuilder sb = new StringBuilder();
        sb.append(LocaleController.getString(R.string.ForkDeviceSpecsCpu)).append(": ")
                .append(specs.cpuCores).append(" cores @ ").append(specs.maxCpuFreqMhz).append(" MHz\n");
        sb.append(LocaleController.getString(R.string.ForkDeviceSpecsGpu)).append(": ")
                .append(TextUtils.isEmpty(specs.gpuRenderer) ? "Unknown" : specs.gpuRenderer).append("\n");
        sb.append(LocaleController.getString(R.string.ForkDeviceSpecsRam)).append(": ")
                .append(String.format(Locale.US, "%.1f GB", specs.totalRamBytes / (1024.0 * 1024.0 * 1024.0))).append("\n");
        sb.append(LocaleController.getString(R.string.ForkDeviceSpecsDisplay)).append(": ")
                .append(specs.screenWidth).append("x").append(specs.screenHeight)
                .append(" @ ").append((int) specs.refreshRate).append(" Hz\n");
        sb.append(LocaleController.getString(R.string.ForkDeviceSpecsAndroid)).append(": ")
                .append("API ").append(specs.apiLevel).append("\n\n");
        sb.append(LocaleController.getString(R.string.ForkDeviceAssessedTier)).append(": ")
                .append(tierLabel(assessment.tier).toUpperCase(Locale.ROOT)).append(" (")
                .append(assessment.score).append("/100)\n\n");
        sb.append(LocaleController.getString(R.string.ForkDeviceAssessmentReason)).append(":\n")
                .append(assessment.reason);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.ForkDeviceScanResults));
        builder.setMessage(sb.toString());
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(builder.create());
        refresh();
    }

    private void refresh() {
        if (configModeRow == null) return;
        boolean isManual = PerformancePolicy.CONFIG_MANUAL.equals(PerformanceSettings.configMode());
        configModeRow.setTextAndValue(LocaleController.getString(R.string.ForkPerformanceConfigMode),
                LocaleController.getString(isManual ? R.string.ForkPerformanceManualMode : R.string.ForkPerformanceAutoMode), false);

        manualLevelRow.setVisibility(isManual ? View.VISIBLE : View.GONE);
        manualLevelRow.setTextAndValue(LocaleController.getString(R.string.ForkPerformanceManualLevel),
                tierLabel(PerformanceSettings.manualLevel()), false);

        DeviceAnalyzerPolicy.Assessment assessment = ForkDeviceAnalyzer.getAssessment(false);
        diagnosticRows[0].setTextAndValue(LocaleController.getString(R.string.ForkPerformanceDeviceClass),
                tierLabel(assessment.tier) + " (" + assessment.score + " pts)", false);
        diagnosticRows[1].setTextAndValue(LocaleController.getString(R.string.ForkPerformanceEffectiveTier),
                tierLabel(PerformanceSettings.effectiveTier()), false);
        diagnosticRows[2].setTextAndValue(LocaleController.getString(R.string.ForkPerformancePowerSaver),
                LocaleController.getString(PerformanceSettings.systemPowerSaver() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff), false);
        diagnosticRows[3].setTextAndValue(LocaleController.getString(R.string.ForkPerformanceGraphics),
                styleLabel(), false);
        diagnosticRows[4].setTextAndValue(LocaleController.getString(R.string.ForkPerformanceAnimations),
                LocaleController.getString(ForkSurfaceAnimation.enabled() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff), false);
        diagnosticRows[5].setTextAndValue(LocaleController.getString(R.string.ForkPerformanceReduceEffectsActive),
                LocaleController.getString(PerformanceSettings.reduceEffects() ? R.string.ForkPerformanceOn : R.string.ForkPerformanceOff), false);
    }

    private void copyDiagnostics() {
        DeviceAnalyzerPolicy.DeviceSpecs specs = ForkDeviceAnalyzer.scanDevice();
        DeviceAnalyzerPolicy.Assessment assessment = ForkDeviceAnalyzer.getAssessment(false);
        StringBuilder text = new StringBuilder();
        text.append("BerryGram ").append(org.telegram.messenger.BuildVars.BUILD_VERSION_STRING).append('\n');
        text.append("Config mode: ").append(PerformanceSettings.configMode()).append('\n');
        text.append("Effective tier: ").append(PerformanceSettings.effectiveTier()).append('\n');
        text.append("Auto assessed tier: ").append(assessment.tier).append(" (score: ").append(assessment.score).append(")\n");
        text.append("Specs: ").append(specs.cpuCores).append(" cores, ").append((int) specs.refreshRate).append("Hz, ").append(specs.screenWidth).append("x").append(specs.screenHeight).append('\n');
        text.append("System power saver: ").append(PerformanceSettings.systemPowerSaver()).append('\n');
        text.append("Effective graphics: ").append(styleLabel()).append('\n');
        text.append("Reduce effects: ").append(PerformanceSettings.reduceEffects()).append('\n');
        AndroidUtilities.addToClipboard(text.toString());
        if (getParentActivity() != null) {
            org.telegram.ui.Components.BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.copy, LocaleController.getString(R.string.ForkPerformanceDiagnosticsCopied))
                    .show();
        }
    }

    private static String tierLabel(String tier) {
        if (PerformancePolicy.LEVEL_LOW.equals(tier) || DeviceAnalyzerPolicy.TIER_LOW.equals(tier)) {
            return LocaleController.getString(R.string.ForkPerformanceLevelLow);
        } else if (DeviceAnalyzerPolicy.TIER_LOW_MEDIUM.equals(tier)) {
            return LocaleController.getString(R.string.ForkPerformanceLevelLowMedium);
        } else if (PerformancePolicy.LEVEL_HIGH.equals(tier) || DeviceAnalyzerPolicy.TIER_HIGH.equals(tier)) {
            return LocaleController.getString(R.string.ForkPerformanceLevelHigh);
        }
        return LocaleController.getString(R.string.ForkPerformanceLevelMedium);
    }

    private static String styleLabel() {
        String style = ForkGraphicsSurface.effectiveStyle();
        if (PanelPolicy.STYLE_GLASS.equals(style) || "experimental".equals(style)) {
            return LocaleController.getString(R.string.ForkGraphicsGlass);
        } else if (PanelPolicy.STYLE_BLUR.equals(style)) {
            return LocaleController.getString(R.string.ForkGraphicsBlur);
        }
        return LocaleController.getString(R.string.ForkGraphicsNone);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
