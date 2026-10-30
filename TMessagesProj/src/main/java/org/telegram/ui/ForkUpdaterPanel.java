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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;

/** User-initiated updates from the owner's designated repository. */
public final class ForkUpdaterPanel extends LinearLayout {
    private static final String REPOSITORY = "mozyk96/Berrygram";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final BaseFragment parent;
    private final SharedPreferences preferences;
    private boolean expanded, busy, disposed;
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
            status = context.getString(R.string.ForkUpdateInterrupted);
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
        String headerValue = version + (release != null ? "  •  " + release.version + " ↑" : "") + (expanded ? "  ▴" : "  ▾");
        header.setTextAndValue(c.getString(R.string.ForkUpdatePanel), headerValue, expanded);
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
        if (busy) { progress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal); progress.setIndeterminate(true); addView(progress); return; }
        addAction(R.string.ForkUpdateCheck, this::check, release != null || verified != null);
        if (release != null && verified == null) addAction(R.string.ForkUpdateDownload, this::download, false);
        if (verified != null) addAction(R.string.ForkUpdateInstall, this::install, false);
    }
    private void addAction(int label, Runnable action, boolean divider) {
        TextSettingsCell cell = new TextSettingsCell(getContext()); cell.setText(getContext().getString(label), divider);
        cell.setOnClickListener(v -> action.run()); addView(cell);
    }
    private void updateDetails() {
        if (details == null || !expanded || disposed) return;
        StringBuilder text = new StringBuilder(getContext().getString(R.string.ForkUpdateCurrent, version));
        long lastChecked = preferences.getLong("last_checked", 0);
        if (lastChecked > 0) text.append("\n").append(getContext().getString(R.string.ForkUpdateLastChecked, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(lastChecked))));
        if (!status.isEmpty()) text.append("\n").append(status);
        if (release != null) {
            text.append("\n").append(getContext().getString(R.string.ForkUpdateAvailable, release.version, release.size / 1048576f));
            if (release.notes != null && !release.notes.isEmpty()) text.append("\n\n").append(release.notes.length() > 600 ? release.notes.substring(0, 600) + "…" : release.notes);
        }
        details.setText(text);
    }
    private GithubUpdateClient.Release restoreRelease() {
        String v = preferences.getString("release_version", "");
        String url = preferences.getString("release_url", "");
        String digest = preferences.getString("release_digest", "");
        long size = preferences.getLong("release_size", 0);
        if (!UpdatePolicy.validSavedRelease(REPOSITORY, v, version, url, digest, size)) return null;
        GithubUpdateClient.Release result = new GithubUpdateClient.Release();
        result.version = v; result.url = url; result.digest = digest; result.size = size;
        result.notes = preferences.getString("release_notes", ""); return result;
    }
    private void saveRelease(GithubUpdateClient.Release candidate) {
        SharedPreferences.Editor edit = preferences.edit();
        if (candidate == null) {
            edit.remove("release_version").remove("release_url").remove("release_digest").remove("release_size").remove("release_notes");
        } else {
            edit.putString("release_version", candidate.version).putString("release_url", candidate.url)
                .putString("release_digest", candidate.digest).putLong("release_size", candidate.size).putString("release_notes", candidate.notes);
        }
        edit.apply();
    }
    private void check() {
        busy = true; preferences.edit().putBoolean("interrupted", true).apply(); release = null; verified = null; saveRelease(null); status = getContext().getString(R.string.ForkUpdateChecking); render(false);
        worker.execute(() -> {
            try {
                GithubUpdateClient.Release found = GithubUpdateClient.latest(REPOSITORY);
                Integer comparison = UpdatePolicy.compareVersions(found.version, version);
                if (comparison == null) throw new SecurityException("Invalid release version");
                AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); release = comparison > 0 ? found : null;
                    status = comparison > 0 ? "" : getContext().getString(R.string.ForkUpdateLatest); saveRelease(release); preferences.edit().putLong("last_checked", System.currentTimeMillis()).apply(); render(false); });
            } catch (Exception e) { fail(e); }
        });
    }
    private void download() {
        GithubUpdateClient.Release candidate = release; if (candidate == null) return;
        Context app = getContext().getApplicationContext(); busy = true; preferences.edit().putBoolean("interrupted", true).apply(); status = getContext().getString(R.string.ForkUpdateDownloading, 0); render(false);
        worker.execute(() -> {
            try {
                File file = GithubUpdateClient.downloadAndVerify(app, candidate, (received, total) -> AndroidUtilities.runOnUIThread(() -> {
                    if (disposed) return; int percent = (int)(received * 100 / total);
                    status = getContext().getString(R.string.ForkUpdateDownloading, percent); updateDetails();
                    if (progress != null) { progress.setIndeterminate(false); progress.setProgress(percent); }
                }));
                AndroidUtilities.runOnUIThread(() -> { if (disposed) { file.delete(); return; } busy = false; preferences.edit().putBoolean("interrupted", false).apply(); verified = file;
                    status = getContext().getString(R.string.ForkUpdateVerified); render(false); });
            } catch (Exception e) { fail(e); }
        });
    }
    private void fail(Exception e) {
        String message = e.getMessage();
        final String detail = message == null || message.isEmpty() || message.length() > 160
                ? e.getClass().getSimpleName() : message;
        AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = getContext().getString(R.string.ForkUpdateFailure, detail); render(false); });
    }
    private void install() {
        Context c = parent.getParentActivity(); if (c == null || verified == null || !verified.isFile()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
                parent.showDialog(new AlertDialog.Builder(c).setTitle(c.getString(R.string.ForkUpdateInstall))
                    .setMessage(c.getString(R.string.ForkUpdatePermissionInfo))
                    .setPositiveButton(c.getString(android.R.string.ok), (dialog, which) -> {
                        try { c.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName()))); }
                        catch (Exception e) { fail(e); }
                    }).setNegativeButton(c.getString(android.R.string.cancel), null).create());
                return;
            }
            Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".provider", verified);
            c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) { fail(e); }
    }
    public void dispose() { if (busy) preferences.edit().putBoolean("interrupted", true).apply(); disposed = true; worker.shutdownNow(); details = null; progress = null; }
}
