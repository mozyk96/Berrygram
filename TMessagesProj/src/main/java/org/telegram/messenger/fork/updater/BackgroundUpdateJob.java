package org.telegram.messenger.fork.updater;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.File;

/** Periodically checks and verifies an official update on an allowed network. */
public final class BackgroundUpdateJob extends JobService {
    private static final int JOB_ID = 0x424752;
    private static final String PREFS = "fork_updater_panel";
    private static final String REPO = "mozyk96/Berrygram";
    private static final long INTERVAL = 6L * 60 * 60 * 1000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private static final Object STATE_LOCK = new Object();
    private static volatile boolean active;
    public static boolean isRunning() { return active; }
    private volatile JobParameters running;
    private volatile Thread worker;

    public static int mode(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getInt("background_mode", BackgroundUpdatePolicy.OFF);
    }

    public static boolean configure(Context context, int mode) {
        if (mode < BackgroundUpdatePolicy.OFF || mode > BackgroundUpdatePolicy.ANY) return false;
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        synchronized (STATE_LOCK) {
            prefs.edit().putInt("background_mode", mode).putBoolean("background_running", false).apply();
        }
        scheduler.cancel(JOB_ID);
        if (mode == BackgroundUpdatePolicy.OFF) return true;
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, new ComponentName(context, BackgroundUpdateJob.class))
                .setPeriodic(INTERVAL).setPersisted(true);
        if (Build.VERSION.SDK_INT >= 28 && (mode == BackgroundUpdatePolicy.WIFI || mode == BackgroundUpdatePolicy.MOBILE)) {
            int transport = mode == BackgroundUpdatePolicy.WIFI ? NetworkCapabilities.TRANSPORT_WIFI : NetworkCapabilities.TRANSPORT_CELLULAR;
            builder.setRequiredNetwork(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(transport).build());
        } else {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        }
        JobInfo job = builder.build();
        try {
            if (scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS) return true;
        } catch (RuntimeException ignored) { }
        prefs.edit().putInt("background_mode", BackgroundUpdatePolicy.OFF).apply();
        return false;
    }

    public static void ensureScheduled(Context context) {
        int selected = mode(context);
        if (selected == BackgroundUpdatePolicy.OFF || isRunning()) return;
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        for (JobInfo job : scheduler.getAllPendingJobs()) if (job.getId() == JOB_ID) return;
        configure(context, selected);
    }

    private Network allowedNetwork(JobParameters params) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return null;
        Network network = cm.getActiveNetwork();
        if (network == null || (Build.VERSION.SDK_INT >= 28 && !network.equals(params.getNetwork()))) return null;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return null;
        boolean wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean mobile = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        return BackgroundUpdatePolicy.allowed(mode(this), wifi, mobile) ? network : null;
    }

    @Override public boolean onStartJob(JobParameters params) {
        Network network = allowedNetwork(params);
        if (network == null || running != null) return false;
        running = params;
        active = true;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean("background_running", true).putLong("background_received", 0)
                .putLong("background_total", 0).putLong("background_error_at", 0).apply();
        worker = new Thread(() -> {
            boolean retry = false, failed = false;
            try {
                GithubUpdateClient.Release release = GithubUpdateClient.latest(REPO, network);
                String installed = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                if (UpdatePolicy.shouldShowRelease(release.version, installed)) {
                    File verified = GithubUpdateClient.downloadAndVerify(this, release, (received, total) -> {
                        if (running != params || allowedNetwork(params) == null || Thread.currentThread().isInterrupted())
                            throw new java.util.concurrent.CancellationException("Background download stopped");
                        prefs.edit().putLong("background_received", received).putLong("background_total", total).apply();
                    }, network);
                    synchronized (STATE_LOCK) {
                        if (running == params && allowedNetwork(params) != null && verified.isFile()) {
                            prefs.edit().putString("release_version", release.version).putString("release_url", release.url)
                                    .putString("release_digest", release.digest).putLong("release_size", release.size)
                                    .putString("release_notes", release.notes).putString("background_verified_digest", release.digest)
                                    .putLong("background_verified_at", System.currentTimeMillis()).apply();
                        }
                    }
                }
            } catch (java.util.concurrent.CancellationException ignored) {
                retry = true;
            } catch (java.io.IOException error) {
                retry = true; failed = true;
            } catch (Exception ignored) {
                // Verification and cancellation failures never produce a verified update.
                failed = true;
            }
            final boolean reschedule = retry, reportFailure = failed;
            main.post(() -> {
                if (running != params) return;
                running = null;
                active = false;
                worker = null;
                SharedPreferences.Editor update = prefs.edit().putBoolean("background_running", false);
                if (reportFailure) update.putLong("background_error_at", System.currentTimeMillis());
                update.apply();
                jobFinished(params, reschedule && mode(this) != BackgroundUpdatePolicy.OFF);
            });
        }, "BerrygramUpdateJob");
        worker.start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        Thread thread = null;
        synchronized (STATE_LOCK) {
            if (running == params) {
                running = null;
                active = false;
                thread = worker;
                worker = null;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("background_running", false).apply();
            }
        }
        if (thread != null) thread.interrupt();
        return mode(this) != BackgroundUpdatePolicy.OFF;
    }
}
