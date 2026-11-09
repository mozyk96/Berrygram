package org.telegram.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.voice.VoiceProcessing;
import org.telegram.messenger.fork.voice.VoiceProfilePolicy;
import org.telegram.messenger.fork.voice.VoiceProfileSettings;
import org.telegram.messenger.fork.voice.VoiceQualitySettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.io.File;

/** Functional voice profile, platform effects and a local test recorder. */
public final class ForkVoiceSettingsActivity extends ForkSettingsFragment {
    private static final int RECORD_PERMISSION = 7206;
    private LinearLayout rows;
    private boolean pendingTestStart;
    private File testFile;
    private MediaRecorder testRecorder;
    private MediaPlayer testPlayer;
    private TextView testStatus;

    @Override
    public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkVoiceTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        LinearLayout page = ForkSettingsLayout.page(context);
        rows = ForkSettingsLayout.section(context);
        ForkSettingsLayout.addSection(page, rows);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll,
                ForkLocale.get(R.string.ForkVoiceTitle), this::finishFragment);
        render();
        return fragmentView;
    }

    private void render() {
        if (rows == null) return;
        Context context = rows.getContext();
        final int previousScroll = currentScroll();
        rows.removeAllViews();

        TextSettingsCell profile = new TextSettingsCell(context);
        profile.setTextAndValue(ForkLocale.get(R.string.ForkVoiceProfile),
                ForkLocale.get(profileLabel(VoiceProfileSettings.profile())), true);
        ForkSettingsLayout.interactive(profile);
        profile.setOnClickListener(this::chooseProfile);
        rows.addView(profile);

        String mode = VoiceQualitySettings.mode();
        int label = "auto".equals(mode) ? R.string.ForkVoiceAuto
                : "custom".equals(mode) ? R.string.ForkVoiceCustom : R.string.ForkVoiceStandard;
        TextSettingsCell modeCell = new TextSettingsCell(context);
        modeCell.setTextAndValue(ForkLocale.get(R.string.ForkVoiceMode),
                ForkLocale.get(label), !"custom".equals(mode));
        ForkSettingsLayout.interactive(modeCell);
        modeCell.setOnClickListener(this::chooseMode);
        rows.addView(modeCell);
        if ("custom".equals(mode)) {
            TextSettingsCell rate = new TextSettingsCell(context);
            rate.setTextAndValue(ForkLocale.get(R.string.ForkVoiceBitrate),
                    (VoiceQualitySettings.customBitrate() / 1000) + " kbps", false);
            ForkSettingsLayout.interactive(rate);
            rate.setOnClickListener(this::chooseBitrate);
            rows.addView(rate);
        }

        if (VoiceProfilePolicy.CUSTOM.equals(VoiceProfileSettings.profile())) {
            addEffectToggle(context, R.string.ForkVoiceNoiseSuppression,
                    VoiceProfileSettings.customNoiseSuppression(),
                    VoiceProcessing.noiseSuppressorAvailable(),
                    VoiceProfileSettings::setCustomNoiseSuppression);
            addEffectToggle(context, R.string.ForkVoiceAgc,
                    VoiceProfileSettings.customGain(), VoiceProcessing.gainAvailable(),
                    VoiceProfileSettings::setCustomGain);
            addEffectToggle(context, R.string.ForkVoiceAec,
                    VoiceProfileSettings.customEcho(), VoiceProcessing.echoAvailable(),
                    VoiceProfileSettings::setCustomEcho);
            TextSettingsCell source = new TextSettingsCell(context);
            source.setTextAndValue(ForkLocale.get(R.string.ForkVoiceSource),
                    ForkLocale.get(sourceLabel(VoiceProfileSettings.customSource())), false);
            ForkSettingsLayout.interactive(source);
            source.setOnClickListener(this::chooseSource);
            rows.addView(source);
        }

        TextSettingsCell test = new TextSettingsCell(context);
        test.setTextAndValue(ForkLocale.get(R.string.ForkVoiceTest),
                ForkLocale.get(R.string.ForkVoiceTestOpen), false);
        test.setIcon(R.drawable.msg_filled_data_voice);
        ForkSettingsLayout.interactive(test);
        test.setOnClickListener(v -> openTestDialog());
        rows.addView(test);

        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkVoiceInfo) + "\n\n"
                + ForkLocale.get(R.string.ForkVoiceProfileInfo) + "\n\n"
                + VoiceProcessing.availabilitySummary());
        rows.addView(info);
        if (previousScroll > 0) {
            rows.post(() -> {
                ScrollView scroll = findScroll();
                if (scroll != null) scroll.scrollTo(0, previousScroll);
            });
        }
    }

    private int currentScroll() {
        ScrollView scroll = findScroll();
        return scroll == null ? 0 : scroll.getScrollY();
    }

    private ScrollView findScroll() {
        View parent = rows;
        while (parent != null) {
            if (parent instanceof ScrollView) return (ScrollView) parent;
            if (!(parent.getParent() instanceof View)) return null;
            parent = (View) parent.getParent();
        }
        return null;
    }

    private void addEffectToggle(Context context, int title, boolean checked,
                                 boolean supported, Setter setter) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setTextAndValueAndCheck(ForkLocale.get(title),
                ForkLocale.get(supported ? R.string.ForkVoiceEffectAvailable
                        : R.string.ForkVoiceEffectUnsupported),
                supported && checked, false, false);
        cell.setEnabled(supported);
        if (supported) {
            ForkSettingsLayout.interactive(cell);
            cell.setOnClickListener(v -> {
                boolean value = !checked;
                setter.set(value);
                render();
            });
        }
        rows.addView(cell);
    }

    private void chooseProfile(View anchor) {
        String[] modes = {VoiceProfilePolicy.STANDARD, VoiceProfilePolicy.AUTOMATIC,
                VoiceProfilePolicy.CLEAR, VoiceProfilePolicy.NATURAL, VoiceProfilePolicy.CUSTOM};
        CharSequence[] labels = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) labels[i] = ForkLocale.get(profileLabel(modes[i]));
        int selected = java.util.Arrays.asList(modes).indexOf(VoiceProfileSettings.profile());
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            VoiceProfileSettings.setProfile(modes[index]);
            render();
        });
    }

    private static int profileLabel(String profile) {
        if (VoiceProfilePolicy.AUTOMATIC.equals(profile)) return R.string.ForkVoiceProfileAutomatic;
        if (VoiceProfilePolicy.CLEAR.equals(profile)) return R.string.ForkVoiceProfileClear;
        if (VoiceProfilePolicy.NATURAL.equals(profile)) return R.string.ForkVoiceProfileNatural;
        if (VoiceProfilePolicy.CUSTOM.equals(profile)) return R.string.ForkVoiceCustom;
        return R.string.ForkVoiceStandard;
    }

    private void chooseMode(View anchor) {
        String[] modes = {"standard", "auto", "custom"};
        int[] names = {R.string.ForkVoiceStandard, R.string.ForkVoiceAuto, R.string.ForkVoiceCustom};
        CharSequence[] labels = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) labels[i] = ForkLocale.get(names[i]);
        ForkChoicePopup.show(this, anchor, labels, null,
                java.util.Arrays.asList(modes).indexOf(VoiceQualitySettings.mode()),
                index -> {
                    VoiceQualitySettings.setMode(modes[index]);
                    render();
                });
    }

    private void chooseBitrate(View anchor) {
        int[] rates = {24000, 32000, 48000, 64000};
        CharSequence[] labels = {"24 kbps", "32 kbps", "48 kbps", "64 kbps"};
        int selected = -1;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] == VoiceQualitySettings.customBitrate()) selected = i;
        }
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            VoiceQualitySettings.setCustomBitrate(rates[index]);
            render();
        });
    }

    private void chooseSource(View anchor) {
        int[] sources = {VoiceProfilePolicy.SOURCE_DEFAULT, VoiceProfilePolicy.SOURCE_MIC,
                VoiceProfilePolicy.SOURCE_VOICE_RECOGNITION, VoiceProfilePolicy.SOURCE_CAMCORDER};
        CharSequence[] labels = new CharSequence[sources.length];
        for (int i = 0; i < sources.length; i++) labels[i] = ForkLocale.get(sourceLabel(sources[i]));
        int selected = java.util.Arrays.asList(sources).indexOf(VoiceProfileSettings.customSource());
        ForkChoicePopup.show(this, anchor, labels, null, selected, index -> {
            VoiceProfileSettings.setCustomSource(sources[index]);
            render();
        });
    }

    private static int sourceLabel(int source) {
        switch (source) {
            case VoiceProfilePolicy.SOURCE_MIC:
                return R.string.ForkVoiceSourceMic;
            case VoiceProfilePolicy.SOURCE_VOICE_RECOGNITION:
                return R.string.ForkVoiceSourceRecognition;
            case VoiceProfilePolicy.SOURCE_CAMCORDER:
                return R.string.ForkVoiceSourceCamcorder;
            case VoiceProfilePolicy.SOURCE_DEFAULT:
            default:
                return R.string.ForkVoiceSourceDefault;
        }
    }

    private void openTestDialog() {
        Context context = getParentActivity();
        if (context == null) return;
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = AndroidUtilities.dp(20);
        panel.setPadding(padding, AndroidUtilities.dp(8), padding, 0);
        testStatus = new TextView(context);
        testStatus.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        testStatus.setText(ForkLocale.get(R.string.ForkVoiceTestIdle));
        testStatus.setGravity(Gravity.START);
        testStatus.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        panel.addView(testStatus);

        addTestAction(panel, R.string.ForkVoiceTestStart, this::startTestRecording);
        addTestAction(panel, R.string.ForkVoiceTestStop, this::stopTestRecording);
        addTestAction(panel, R.string.ForkVoiceTestPlay, this::playTestRecording);
        addTestAction(panel, R.string.ForkVoiceTestDelete, this::deleteTestRecording);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(ForkLocale.get(R.string.ForkVoiceTest))
                .setView(panel)
                .setNegativeButton(LocaleController.getString(R.string.Close), null)
                .create();
        dialog.setOnDismissListener(d -> {
            stopTestRecording();
            releaseTestPlayer();
            deleteTestFile();
            testStatus = null;
        });
        showDialog(dialog);
    }

    private void addTestAction(LinearLayout panel, int title, Runnable action) {
        TextSettingsCell cell = new TextSettingsCell(panel.getContext());
        cell.setText(ForkLocale.get(title), false);
        ForkSettingsLayout.interactive(cell);
        cell.setOnClickListener(v -> action.run());
        panel.addView(cell);
    }

    private void startTestRecording() {
        Context context = getParentActivity();
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingTestStart = true;
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION);
            }
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestPermission));
            return;
        }
        if (testRecorder != null) {
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestRecording));
            return;
        }
        stopTestPlayback();
        deleteTestFile();
        try {
            File directory = context.getCacheDir();
            testFile = new File(directory, "berrygram_voice_test_"
                    + System.currentTimeMillis() + ".m4a");
            testRecorder = new MediaRecorder();
            testRecorder.setAudioSource(VoiceProcessing.source());
            testRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            testRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            testRecorder.setAudioChannels(1);
            testRecorder.setAudioSamplingRate(48_000);
            testRecorder.setAudioEncodingBitRate(64_000);
            testRecorder.setOutputFile(testFile.getAbsolutePath());
            testRecorder.prepare();
            testRecorder.start();
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestRecording));
        } catch (Throwable error) {
            releaseTestRecorder();
            deleteTestFile();
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestError, String.valueOf(error.getMessage())));
        }
    }

    private void stopTestRecording() {
        if (testRecorder == null) return;
        boolean ok = true;
        try {
            testRecorder.stop();
        } catch (Throwable ignored) {
            ok = false;
        } finally {
            releaseTestRecorder();
        }
        if (!ok || testFile == null || !testFile.isFile() || testFile.length() <= 0) {
            deleteTestFile();
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestError, "no audio"));
            return;
        }
        setTestStatus(ForkLocale.get(R.string.ForkVoiceTestReady,
                Math.max(1, testFile.length() / 1024)));
    }

    private void playTestRecording() {
        if (testFile == null || !testFile.isFile()) {
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestEmpty));
            return;
        }
        stopTestPlayback();
        try {
            testPlayer = new MediaPlayer();
            testPlayer.setDataSource(testFile.getAbsolutePath());
            testPlayer.setOnCompletionListener(mp -> setTestStatus(
                    ForkLocale.get(R.string.ForkVoiceTestReady,
                            Math.max(1, testFile.length() / 1024))));
            testPlayer.prepare();
            testPlayer.start();
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestPlaying));
        } catch (Throwable error) {
            releaseTestPlayer();
            setTestStatus(ForkLocale.get(R.string.ForkVoiceTestError,
                    String.valueOf(error.getMessage())));
        }
    }

    private void deleteTestRecording() {
        stopTestPlayback();
        deleteTestFile();
        setTestStatus(ForkLocale.get(R.string.ForkVoiceTestIdle));
    }

    private void stopTestPlayback() {
        releaseTestPlayer();
    }

    private void releaseTestPlayer() {
        if (testPlayer != null) {
            try {
                testPlayer.stop();
            } catch (Throwable ignored) { }
            try {
                testPlayer.release();
            } catch (Throwable ignored) { }
            testPlayer = null;
        }
    }

    private void releaseTestRecorder() {
        if (testRecorder != null) {
            try {
                testRecorder.reset();
            } catch (Throwable ignored) { }
            try {
                testRecorder.release();
            } catch (Throwable ignored) { }
            testRecorder = null;
        }
    }

    private void deleteTestFile() {
        if (testFile != null) {
            try {
                //noinspection ResultOfMethodCallIgnored
                testFile.delete();
            } catch (Throwable ignored) { }
            testFile = null;
        }
    }

    private void setTestStatus(String value) {
        if (testStatus != null) testStatus.setText(value);
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] results) {
        if (rows == null) return;
        if (requestCode == RECORD_PERMISSION && pendingTestStart) {
            pendingTestStart = false;
            if (isPaused()) return;
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startTestRecording();
            } else {
                setTestStatus(ForkLocale.get(R.string.ForkVoiceTestPermission));
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        pendingTestStart = false;
        stopTestRecording();
        releaseTestPlayer();
        deleteTestFile();
        rows = null;
        testStatus = null;
        super.onFragmentDestroy();
    }

    private interface Setter {
        void set(boolean value);
    }
}
