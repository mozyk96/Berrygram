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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.text.format.Formatter;
import org.telegram.messenger.fork.updater.UpdateUiPolicy;
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
    private boolean expanded, busy, disposed, incompatible, failed, downloading;
    private long downloadedBytes, totalBytes;
    private TextView progressText, progressBytes;
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
    private long versionCode;
    private GithubUpdateClient.Release release;
    private File verified;
    private TextView details;
    private ProgressBar progress;

    public ForkUpdaterPanel(Context context, BaseFragment parent) {
        super(context); this.parent = parent; setOrientation(VERTICAL);
        preferences = context.getSharedPreferences("fork_updater_panel", Context.MODE_PRIVATE);
        expanded = true;
        if (preferences.getBoolean("interrupted", false)) {
            status = ForkLocale.get(R.string.ForkUpdateInterrupted);
            preferences.edit().putBoolean("interrupted", false).apply();
        }
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; } catch (Exception ignored) { }
        if (version == null) version = "";
        try {
            android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        } catch (Exception ignored) { }
        // Update channels are obsolete. Drop their stored preference once and
        // discard a saved prerelease, without touching unrelated updater state.
        String savedVersion = preferences.getString("release_version", "");
        if (preferences.contains("beta_channel") || preferences.contains("release_prerelease")
                || UpdatePolicy.isPrerelease(savedVersion)) {
            SharedPreferences.Editor edit = preferences.edit()
                    .remove("beta_channel").remove("release_prerelease");
            if (UpdatePolicy.isPrerelease(savedVersion)) {
                edit.remove("release_version").remove("release_url").remove("release_digest")
                        .remove("release_size").remove("release_notes");
            }
            edit.apply();
        }
        release = restoreRelease();
        render(false);
    }
    private void render(boolean animate) {
        if (disposed) return;
        removeAllViews();
        Context c = getContext();
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        int padding = AndroidUtilities.dp(24);
        setPadding(padding, 0, padding, Math.max(getPaddingBottom(), AndroidUtilities.dp(16)));
        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(c);
        content.setOrientation(VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(0, AndroidUtilities.dp(48), 0, AndroidUtilities.dp(24));
        ImageView logo = new ImageView(c);
        logo.setImageResource(R.mipmap.berrygram_launcher);
        content.addView(logo, new LinearLayout.LayoutParams(AndroidUtilities.dp(80), AndroidUtilities.dp(80)));
        TextView title = new TextView(c);
        title.setText("Berrygram"); title.setTextSize(28); title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = AndroidUtilities.dp(16); content.addView(title, titleParams);
        details = new TextView(c); details.setTextSize(15); details.setGravity(Gravity.CENTER);
        details.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = AndroidUtilities.dp(8); content.addView(details, detailParams); updateDetails();
        if (release != null && release.notes != null && !release.notes.isEmpty()) {
            TextView notes = new TextView(c); notes.setText(release.notes); notes.setTextSize(15);
            notes.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            notesParams.topMargin = AndroidUtilities.dp(28); content.addView(notes, notesParams);
        }
        scroll.addView(content);
        addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        progress = null; progressText = null; progressBytes = null;
        if (busy && downloading) {
            progress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100); progress.setProgress(downloadPercent);
            addView(progress, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtilities.dp(6)));
            LinearLayout progressLabels = new LinearLayout(c);
            progressLabels.setOrientation(HORIZONTAL);
            progressText = new TextView(c); progressText.setTextSize(13);
            progressBytes = new TextView(c); progressBytes.setTextSize(13); progressBytes.setGravity(Gravity.END);
            int secondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
            progressText.setTextColor(secondary); progressBytes.setTextColor(secondary);
            progressLabels.addView(progressText, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
            progressLabels.addView(progressBytes, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            progressParams.topMargin = AndroidUtilities.dp(6); addView(progressLabels, progressParams); updateProgressText();
        }
        Button button = new Button(c); button.setAllCaps(false); button.setGravity(Gravity.CENTER); button.setTextSize(16);
        button.setTypeface(AndroidUtilities.bold()); button.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        button.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), Theme.getColor(Theme.key_featuredStickers_addButton)));
        int action = UpdateUiPolicy.action(busy, release != null && !incompatible, verified != null, failed);
        int label = action == UpdateUiPolicy.BUSY ? (downloading ? R.string.ForkUpdateDownloadingButton : R.string.ForkUpdateCheckingButton)
                : action == UpdateUiPolicy.INSTALL ? R.string.ForkUpdateInstall : action == UpdateUiPolicy.DOWNLOAD ? R.string.ForkUpdateDownload
                : action == UpdateUiPolicy.RETRY ? R.string.ForkUpdateRetry : R.string.ForkUpdateCheck;
        button.setText(ForkLocale.get(label)); button.setEnabled(!busy);
        button.setOnClickListener(v -> { if (action == UpdateUiPolicy.INSTALL) startInstall(); else if (action == UpdateUiPolicy.DOWNLOAD || (action == UpdateUiPolicy.RETRY && release != null && !incompatible)) download(); else check(); });
        button.setMinHeight(AndroidUtilities.dp(52));
        button.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = AndroidUtilities.dp(16); addView(button, buttonParams);
    }
    private void updateProgressText() {
        if (progressText == null) return;
        progressText.setText(downloadPercent + "%");
        progressBytes.setText(Formatter.formatFileSize(getContext(), downloadedBytes) + " / " + Formatter.formatFileSize(getContext(), totalBytes));
    }
    private void updateDetails() {
        if (details == null || !expanded || disposed) return;
        StringBuilder text = new StringBuilder(ForkLocale.get(R.string.ForkUpdateCurrent, version));
        if (versionCode > 0) {
            text.append(" (").append(versionCode).append(')');
        }
        long lastChecked = preferences.getLong("last_checked", 0);
        if (lastChecked > 0) text.append("\n").append(ForkLocale.get(R.string.ForkUpdateLastChecked, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(lastChecked))));
        if (!status.isEmpty()) text.append("\n").append(status);
        if (release != null) {
            text.append("\n").append(ForkLocale.get(R.string.ForkUpdateAvailable, release.version, release.size / 1048576f));
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
            edit.remove("release_version").remove("release_url").remove("release_digest").remove("release_size").remove("release_notes").remove("release_prerelease");
        } else {
            edit.putString("release_version", candidate.version).putString("release_url", candidate.url)
                .putString("release_digest", candidate.digest).putLong("release_size", candidate.size).putString("release_notes", candidate.notes);
        }
        edit.apply();
    }
    private void check() {
        if (busy || disposed) return;
        installIntent.cancel(); downloadPercent = 0; failed = false; downloading = false;
        busy = true; release = null; verified = null; incompatible = false; saveRelease(null); status = ForkLocale.get(R.string.ForkUpdateChecking); render(false);
        worker.execute(() -> {
            try {
                GithubUpdateClient.Release found = GithubUpdateClient.latest(REPOSITORY);
                Integer comparison = UpdatePolicy.compareVersions(found.version, version);
                if (comparison == null) throw new SecurityException("Invalid release version");
                boolean show = UpdatePolicy.shouldShowRelease(found.version, version);
                AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); release = show ? found : null;
                    status = show ? "" : ForkLocale.get(R.string.ForkUpdateLatest); saveRelease(release); preferences.edit().putLong("last_checked", System.currentTimeMillis()).apply(); render(false); });
            } catch (Exception e) { fail(e); }
        });
    }
    private void download() {
        GithubUpdateClient.Release candidate = release; if (candidate == null || busy || disposed || installIntent.pending()) return;
        installIntent.cancel(); waitingPermission = false; permissionLeft = false; downloadPercent = 0; downloadedBytes = 0; totalBytes = candidate.size; verified = null; failed = false; downloading = true;
        Context app = getContext().getApplicationContext(); busy = true; preferences.edit().putBoolean("interrupted", true).apply(); status = ForkLocale.get(R.string.ForkUpdateDownloading, 0); render(false);
        worker.execute(() -> {
            try {
                File file = GithubUpdateClient.downloadAndVerify(app, candidate, (received, total) -> {
                    final int percent = total > 0 ? (int) Math.min(100, received * 100 / total) : 0;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (disposed) return; downloadPercent = percent; downloadedBytes = received; totalBytes = total; updateProgressText();
                        status = ForkLocale.get(R.string.ForkUpdateDownloading, percent); updateDetails();
                        if (progress != null) { progress.setIndeterminate(false); progress.setProgress(percent); }
                    });
                });
                AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); verified = file;
                    status = ForkLocale.get(R.string.ForkUpdateVerified); downloading = false; render(false); });
            } catch (Exception e) { fail(e); }
        });
    }
    private void fail(Exception e) {
        AndroidUtilities.runOnUIThread(() -> { failed = true; downloading = false; });
        AndroidUtilities.runOnUIThread(() -> { installIntent.cancel(); waitingPermission = false; removeCallbacks(installWhenResumed); });
        if (e instanceof GithubUpdateClient.VersionNotNewerException) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; incompatible = true; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateIncompatibleVersion); render(false); });
            return;
        }
        if (e instanceof GithubUpdateClient.InsufficientStorageException) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateNoSpace); render(false); });
            return;
        }
        if (e instanceof GithubUpdateClient.NoReleaseException) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateNoRelease); render(false); });
            return;
        }
        if (e instanceof GithubUpdateClient.SignatureMismatchException || (e instanceof SecurityException && "Update signing certificate mismatch".equals(e.getMessage()))) {
            AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; incompatible = true; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateSignatureMismatch); render(false); });
            return;
        }
        String message = e.getMessage();
        final String detail = message == null || message.isEmpty() || message.length() > 160
                ? e.getClass().getSimpleName() : message;
        AndroidUtilities.runOnUIThread(() -> { if (disposed) return; busy = false; preferences.edit().putBoolean("interrupted", false).apply(); status = ForkLocale.get(R.string.ForkUpdateFailure, detail); render(false); });
    }
    private void startInstall() {
        if (verified == null || !verified.isFile()) {
            download();
            return;
        }
        installIntent.request();
        waitingPermission = false;
        permissionLeft = false;
        install();
    }

    private void install() {
        Context c = parent.getParentActivity();
        if (disposed || !installIntent.pending() || parent.isPaused() || !isAttachedToWindow() || c == null || verified == null || !verified.isFile()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
                waitingPermission = true;
                permissionLeft = false;
                permissionSettingsOpened = false;
                if (parent.showDialog(new AlertDialog.Builder(c).setTitle(ForkLocale.get(R.string.ForkUpdateInstall))
                    .setMessage(ForkLocale.get(R.string.ForkUpdatePermissionInfo))
                    .setPositiveButton(c.getString(android.R.string.ok), (dialog, which) -> {
                        permissionSettingsOpened = true;
                        permissionLeft = false;
                        try {
                            c.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName())));
                        } catch (Exception e) {
                            fail(e);
                        }
                    }).setNegativeButton(c.getString(android.R.string.cancel), (dialog, which) -> {
                        installIntent.cancel();
                        waitingPermission = false;
                    }).create(), dialog -> {
                        if (!permissionSettingsOpened) {
                            installIntent.cancel();
                            waitingPermission = false;
                        }
                    }) == null) {
                    installIntent.cancel();
                    waitingPermission = false;
                }
                return;
            }
            Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".provider", verified);
            if (!installIntent.consume(isAttachedToWindow(), !parent.isPaused(), true, verified.isFile())) return;
            removeCallbacks(installWhenResumed);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(intent);
        } catch (Exception e) {
            fail(e);
        }
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
    public void dispose() { installIntent.cancel(); removeCallbacks(installWhenResumed); if (busy) preferences.edit().putBoolean("interrupted", true).apply(); disposed = true; worker.shutdownNow(); details = null; progress = null; progressText = null; progressBytes = null; }
}
