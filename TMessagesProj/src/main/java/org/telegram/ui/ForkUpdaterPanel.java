package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.text.DateFormat;
import java.util.Date;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.transition.TransitionManager;
import android.view.View;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.updater.GithubUpdateClient;
import org.telegram.messenger.fork.updater.UpdatePolicy;
import org.telegram.messenger.fork.updater.UpdateInstallIntent;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;

/** User-initiated updates from the owner's designated repository. */
public final class ForkUpdaterPanel extends LinearLayout {
    private static final String REPOSITORY = "mozyk96/Berrygram";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final BaseFragment parent;
    private final SharedPreferences preferences;
    private boolean expanded, busy, disposed, incompatible;
    private final UpdateInstallIntent installIntent = new UpdateInstallIntent();
    private boolean waitingPermission, permissionLeft, permissionSettingsOpened;
    private int downloadPercent;
    private final Runnable installWhenResumed = new Runnable() {
        @Override public void run() {
            if (disposed || !installIntent.pending() || !isAttachedToWindow()) return;
            boolean active = !parent.isPaused() && hasWindowFocus();
            if (waitingPermission && permissionSettingsOpened) {
                if (!active) permissionLeft = true;
                if (active && permissionLeft) {
                    waitingPermission = false;
                    if (Build.VERSION.SDK_INT >= 26 && !getContext().getPackageManager().canRequestPackageInstalls()) {
                        installIntent.cancel();
                        return;
                    }
                }
            }
            if (active && !waitingPermission && verified != null) {
                if (!verified.isFile()) { fail(new java.io.IOException("Verified update is no longer available; retry Update")); return; }
                install();
            }

        }
    };
    private String status = "";
    private String version = "";
    private GithubUpdateClient.Release release;
    private File verified;
    private TextView details;
    private ProgressBar progress;

    public ForkUpdaterPanel(Context context, BaseFragment parent) {
        super(context); this.parent = parent; setOrientation(VERTICAL);
        preferences = context.getSharedPreferences("fork_updater_panel", Context.MODE_PRIVATE);
        expanded = preferences.getBoolean("expanded", false);
        if (preferences.getBoolean("interrupted", false)) {
            status = ForkLocale.get(R.string.ForkUpdateInterrupted);
            preferences.edit().putBoolean("interrupted", false).apply();
        }
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; } catch (Exception ignored) { }
        if (version == null) version = "";
        release = restoreRelease();
        render(false);
    }
    private void render(boolean animate) {
        if (disposed) return;
        if (animate && isAttachedToWindow()) TransitionManager.beginDelayedTransition(this);
        removeAllViews(); Context c = getContext();
        TextSettingsCell header = new TextSettingsCell(c);
        header.setBackground(Theme.getSelectorDrawable(false));
        String headerValue = (release != null ? release.version + (olderStable() ? " ↶" : " ↑") : version) + (expanded ? "  ▴" : "  ▾");
        header.setTextAndValue(ForkLocale.get(R.string.ForkUpdatePanel), headerValue, expanded);
        header.setIcon(R.drawable.msg_download);
        header.setOnClickListener(v -> { expanded = !expanded; preferences.edit().putBoolean("expanded", expanded).apply(); render(true); }); addView(header);
        if (!expanded) return;
        LinearLayout summary = new LinearLayout(c); summary.setOrientation(HORIZONTAL); summary.setGravity(Gravity.TOP);
        summary.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
        ImageView logo = new ImageView(c); logo.setImageResource(R.mipmap.berrygram_launcher);
        summary.addView(logo, new LinearLayout.LayoutParams(AndroidUtilities.dp(36), AndroidUtilities.dp(36)));
        details = new TextView(c); details.setTextSize(15); details.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(AndroidUtilities.dp(12)); summary.addView(details, textParams);
        addView(summary); updateDetails();
        TextSettingsCell channel = new TextSettingsCell(c);
        boolean beta = preferences.getBoolean("beta_channel", true);
        channel.setTextAndValue(ForkLocale.get(R.string.ForkUpdateChannel), ForkLocale.get(beta ? R.string.ForkUpdateChannelBeta : R.string.ForkUpdateChannelStable), false);
        channel.setEnabled(!busy);
        channel.setBackground(Theme.getSelectorDrawable(false));
        channel.setOnClickListener(v -> ForkChoicePopup.show(parent, v,
            new CharSequence[]{ForkLocale.get(R.string.ForkUpdateChannelStable), ForkLocale.get(R.string.ForkUpdateChannelBeta)}, null, beta ? 1 : 0, index -> {
                installIntent.cancel(); removeCallbacks(installWhenResumed);
                preferences.edit().putBoolean("beta_channel", index == 1).apply();
                release = null; verified = null; incompatible = false; saveRelease(null); status = ""; render(false);
            }));
        addView(channel);
        if (release != null && release.notes != null && !release.notes.isEmpty()) {
            addAction(R.string.ForkUpdateChangelog, () -> parent.showDialog(new AlertDialog.Builder(getContext())
                .setTitle(release.version).setMessage(release.notes)
                .setPositiveButton(getContext().getString(android.R.string.ok), null).create()), false);
        }
        if (busy) { progress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal); progress.setIndeterminate(verified == null && downloadPercent == 0); progress.setMax(100); progress.setProgress(downloadPercent); addView(progress); return; }
        addAction(R.string.ForkUpdateCheck, this::check, release != null || verified != null);
        if (release != null && verified == null && !incompatible) addAction(olderStable() ? R.string.ForkUpdateStableRecovery : R.string.ForkUpdateDownload, this::download, false);
        if (verified != null) addAction(R.string.ForkUpdateInstall, this::download, false);
    }
    private void addAction(int label, Runnable action, boolean divider) {
        TextSettingsCell cell = new TextSettingsCell(getContext()); cell.setText(ForkLocale.get(label), divider);
        cell.setIcon(label == R.string.ForkUpdateCheck || label == R.string.ForkUpdateStableRecovery ? R.drawable.msg_retry : label == R.string.ForkUpdateDownload ? R.drawable.msg_download : label == R.string.ForkUpdateChangelog ? R.drawable.msg_info : R.drawable.msg_check_s);
        cell.setBackground(Theme.getSelectorDrawable(false));
        cell.setOnClickListener(v -> action.run()); addView(cell);
    }
    private boolean olderStable() {
        if (release == null || preferences.getBoolean("beta_channel", true)) return false;
        Integer comparison = UpdatePolicy.compareVersions(release.version, version);
        return comparison != null && comparison < 0;
    }
    private void updateDetails() {
        if (details == null || !expanded || disposed) return;
        StringBuilder text = new StringBuilder(ForkLocale.get(R.string.ForkUpdateCurrent, version));
        long lastChecked = preferences.getLong("last_checked", 0);
        if (lastChecked > 0) text.append("\n").append(ForkLocale.get(R.string.ForkUpdateLastChecked, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(lastChecked))));
        if (!status.isEmpty()) text.append("\n").append(status);
        if (release != null) {
            text.append("\n").append(ForkLocale.get(R.string.ForkUpdateAvailable, release.version, release.size / 1048576f));
            if (olderStable()) text.append("\n").append(ForkLocale.get(R.string.ForkUpdateOlderStable));
        }
        details.setText(text);
    }
    private GithubUpdateClient.Release restoreRelease() {
        String v = preferences.getString("release_version", "");
        String url = preferences.getString("release_url", "");
        String digest = preferences.getString("release_digest", "");
        long size = preferences.getLong("release_size", 0);
        boolean prerelease = preferences.getBoolean("release_prerelease", true);
        if ((!preferences.getBoolean("beta_channel", true) && (prerelease || UpdatePolicy.isPrerelease(v))) || !UpdatePolicy.validSavedRelease(REPOSITORY, v, version, url, digest, size, preferences.getBoolean("beta_channel", true))) return null;
        GithubUpdateClient.Release result = new GithubUpdateClient.Release();
        result.version = v; result.url = url; result.digest = digest; result.size = size; result.prerelease = prerelease;
        result.notes = preferences.getString("release_notes", ""); return result;
    }
    private void saveRelease(GithubUpdateClient.Release candidate) {
        SharedPreferences.Editor edit = preferences.edit();
        if (candidate == null) {
            edit.remove("release_version").remove("release_url").remove("release_digest").remove("release_size").remove("release_notes").remove("release_prerelease");
        } else {
            edit.putString("release_version", candidate.version).putString("release_url", candidate.url)
                .putString("release_digest", candidate.digest).putLong("release_size", candidate.size).putString("release_notes", candidate.notes).putBoolean("release_prerelease", candidate.prerelease);
        }
        edit.apply();
    }
    private void check() {
        if (busy || disposed) return;
        installIntent.cancel(); downloadPercent = 0;
        busy = true; preferences.edit().putBoolean("interrupted", true).apply(); release = null; verified = null; incompatible = false; saveRelease(null); status = ForkLocale.get(R.string.ForkUpdateChecking); render(false);
        final boolean betaChannel = preferences.getBoolean("beta_channel", true);
        worker.execute(() -> {
            try {
                GithubUpdateClient.Release found = GithubUpdateClient.latest(REPOSITORY, betaChannel);
                Integer comparison = UpdatePolicy.compareVersions(found.version, version);
                if (comparison == null) throw new SecurityException("Invalid release version");
                boolean show = UpdatePolicy.shouldShowRelease(found.version, version, betaChannel);
                AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); release = show ? found : null;
                    status = show ? "" : ForkLocale.get(R.string.ForkUpdateLatest); saveRelease(release); preferences.edit().putLong("last_checked", System.currentTimeMillis()).apply(); render(false); });
            } catch (Exception e) { fail(e); }
        });
    }
    private void download() {
        GithubUpdateClient.Release candidate = release; if (candidate == null || busy || disposed || installIntent.pending()) return;
        candidate.stableRecovery = !preferences.getBoolean("beta_channel", true) && !candidate.prerelease && UpdatePolicy.isPrerelease(version);
        installIntent.request(); waitingPermission = false; permissionLeft = false; downloadPercent = 0; verified = null;
        Context app = getContext().getApplicationContext(); busy = true; preferences.edit().putBoolean("interrupted", true).apply(); status = ForkLocale.get(R.string.ForkUpdateDownloading, 0); render(false);
        worker.execute(() -> {
            try {
                final int[] reportedPercent = {-1};
                File file = GithubUpdateClient.downloadAndVerify(app, candidate, (received, total) -> {
                    final int percent = total > 0 ? (int) Math.min(100, received * 100 / total) : 0;
                    if (reportedPercent[0] == percent) return;
                    reportedPercent[0] = percent;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (disposed) return; downloadPercent = percent;
                        status = ForkLocale.get(R.string.ForkUpdateDownloading, percent); updateDetails();
                        if (progress != null) { progress.setIndeterminate(false); progress.setProgress(percent); }
                    });
                });
                AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); verified = file;
                    status = ForkLocale.get(R.string.ForkUpdateVerified); render(false);
                    scheduleInstallerCheck(); });
            } catch (Exception e) { fail(e); }
        });
    }
    private void fail(Exception e) {
        AndroidUtilities.runOnUIThread(() -> { installIntent.cancel(); waitingPermission = false; removeCallbacks(installWhenResumed); });
        if (e instanceof GithubUpdateClient.VersionNotNewerException) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; incompatible = true; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateIncompatibleVersion); render(false); });
            return;
        }
        if (e instanceof GithubUpdateClient.InsufficientStorageException) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateNoSpace); render(false); });
            return;
        }
        if (e instanceof GithubUpdateClient.NoStableReleaseException) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateNoStableRelease); render(false); });
            return;
        }
        String message = e.getMessage();
        final String detail = message == null || message.isEmpty() || message.length() > 160
                ? e.getClass().getSimpleName() : message;
        AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateFailure, detail); render(false); });
    }
    private void install() {
        Context c = parent.getParentActivity(); if (disposed || !installIntent.pending() || parent.isPaused() || !isAttachedToWindow() || !hasWindowFocus() || c == null || verified == null || !verified.isFile()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
                waitingPermission = true; permissionLeft = false; permissionSettingsOpened = false;
                if (parent.showDialog(new AlertDialog.Builder(c).setTitle(ForkLocale.get(R.string.ForkUpdateInstall))
                    .setMessage(ForkLocale.get(R.string.ForkUpdatePermissionInfo))
                    .setPositiveButton(c.getString(android.R.string.ok), (dialog, which) -> {
                        permissionSettingsOpened = true; permissionLeft = false;
                        try { c.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName()))); }
                        catch (Exception e) { fail(e); }
                    }).setNegativeButton(c.getString(android.R.string.cancel), (dialog, which) -> { installIntent.cancel(); waitingPermission = false; }).create(), dialog -> {
                        if (!permissionSettingsOpened) { installIntent.cancel(); waitingPermission = false; }
                    }) == null) { installIntent.cancel(); waitingPermission = false; }
                return;
            }
            Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".provider", verified);
            // Consume the one-tap intent before leaving the app: cancellation never reopens it.
            if (!installIntent.consume(isAttachedToWindow(), !parent.isPaused(), hasWindowFocus(), verified.isFile())) return;
            removeCallbacks(installWhenResumed);
            c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) { fail(e); }
    }
    private void scheduleInstallerCheck() {
        removeCallbacks(installWhenResumed);
        if (!disposed && installIntent.pending() && isAttachedToWindow()) post(installWhenResumed);
    }
    public void onParentResumed() { scheduleInstallerCheck(); }
    public void onParentPaused() {
        if (waitingPermission && permissionSettingsOpened) permissionLeft = true;
        removeCallbacks(installWhenResumed);
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && waitingPermission && permissionSettingsOpened) permissionLeft = true;
        if (hasFocus) scheduleInstallerCheck();
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        scheduleInstallerCheck();
    }
    @Override protected void onDetachedFromWindow() {
        installIntent.cancel(); waitingPermission = false;
        removeCallbacks(installWhenResumed);
        super.onDetachedFromWindow();
    }
    public void dispose() { installIntent.cancel(); removeCallbacks(installWhenResumed); if (busy) preferences.edit().putBoolean("interrupted", true).apply(); disposed = true; worker.shutdownNow(); details = null; progress = null; }
}
