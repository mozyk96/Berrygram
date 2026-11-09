package org.telegram.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.fork.diagnostics.NotificationDiagnosticsPolicy;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.PermissionRequest;

/**
 * Read-only notification diagnostics plus safe shortcuts to Android settings.
 * Tokens, credentials and account data are never displayed.
 */
public final class ForkNotificationDiagnosticsActivity extends ForkSettingsFragment {
    private static final String TEST_CHANNEL = "berrygram_notification_diagnostics";
    private static final int TEST_NOTIFICATION_ID = 0x4247;

    private TextSettingsCell permissionCell;
    private TextSettingsCell appCell;
    private TextSettingsCell channelCell;
    private TextSettingsCell pushCell;
    private TextSettingsCell backgroundCell;
    private TextSettingsCell summaryCell;
    private TextSettingsCell requestPermissionCell;
    private TextSettingsCell backgroundActionCell;
    private TextSettingsCell testCell;
    private TextInfoPrivacyCell firebaseInfo;
    private TextInfoPrivacyCell testInfo;

    @Override
    public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkNotificationTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        LinearLayout page = ForkSettingsLayout.page(context);
        LinearLayout summarySection = ForkSettingsLayout.section(context);
        summaryCell = new TextSettingsCell(context);
        summaryCell.setText(ForkLocale.get(R.string.ForkNotificationSummary), true);
        summarySection.addView(summaryCell);
        ForkSettingsLayout.addSection(page, summarySection);

        LinearLayout statusSection = ForkSettingsLayout.section(context);
        permissionCell = statusCell(context);
        appCell = statusCell(context);
        channelCell = statusCell(context);
        pushCell = statusCell(context);
        backgroundCell = statusCell(context);
        ForkSettingsLayout.addSection(page, statusSection);

        LinearLayout actions = ForkSettingsLayout.section(context);
        requestPermissionCell = new TextSettingsCell(context);
        requestPermissionCell.setText(ForkLocale.get(R.string.ForkNotificationRequestPermission), true);
        ForkSettingsLayout.interactive(requestPermissionCell);
        requestPermissionCell.setOnClickListener(v -> PermissionRequest.requestPermission(
                Manifest.permission.POST_NOTIFICATIONS, granted -> updateState()));
        actions.addView(requestPermissionCell);

        backgroundActionCell = new TextSettingsCell(context);
        backgroundActionCell.setText(ForkLocale.get(R.string.ForkNotificationEnableBackground), false);
        ForkSettingsLayout.interactive(backgroundActionCell);
        backgroundActionCell.setOnClickListener(v -> enableBackgroundConnection());
        actions.addView(backgroundActionCell);

        testCell = new TextSettingsCell(context);
        testCell.setText(ForkLocale.get(R.string.ForkNotificationTestAction), false);
        ForkSettingsLayout.interactive(testCell);
        testCell.setOnClickListener(v -> showTestNotification());
        actions.addView(testCell);

        TextSettingsCell openSettings = new TextSettingsCell(context);
        openSettings.setText(ForkLocale.get(R.string.ForkNotificationOpenSettings), false);
        ForkSettingsLayout.interactive(openSettings);
        openSettings.setOnClickListener(v -> openNotificationSettings());
        actions.addView(openSettings);
        ForkSettingsLayout.addSection(page, actions);

        firebaseInfo = new TextInfoPrivacyCell(context);
        page.addView(firebaseInfo);
        testInfo = new TextInfoPrivacyCell(context);
        page.addView(testInfo);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(page);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll,
                ForkLocale.get(R.string.ForkNotificationTitle), this::finishFragment);
        updateState();
        return fragmentView;
    }

    private TextSettingsCell statusCell(Context context) {
        TextSettingsCell cell = new TextSettingsCell(context);
        cell.setTextAndValue("", "", true);
        // Status rows are informational only; no click listener is attached.
        return cell;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void updateState() {
        Context context = getContext();
        if (context == null || permissionCell == null) return;

        boolean permissionGranted = Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();

        int channelCount = 0;
        int lowestImportance = Integer.MAX_VALUE;
        boolean channelBlocked = false;
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            for (NotificationChannel channel : manager.getNotificationChannels()) {
                if (TEST_CHANNEL.equals(channel.getId())) continue;
                channelCount++;
                lowestImportance = Math.min(lowestImportance, channel.getImportance());
                if (NotificationDiagnosticsPolicy.channelBlocked(channel.getImportance())) channelBlocked = true;
            }
        }

        boolean playServices = false;
        try {
            playServices = ApplicationLoader.getPushProvider().hasServices();
        } catch (Throwable ignore) {
            playServices = false;
        }
        boolean firebaseConfigured = PushListenerController.isFirebaseConfigured(context);
        boolean tokenPresent = SharedConfig.pushString != null && !SharedConfig.pushString.isEmpty();
        boolean registered = UserConfig.getInstance(currentAccount).registeredForPush;

        boolean backgroundRestricted = false;
        boolean ignoringBattery = true;
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= 28 && am != null) backgroundRestricted = am.isBackgroundRestricted();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= 23 && pm != null) {
                ignoringBattery = pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        } catch (Throwable ignore) {
        }

        String permissionValue = permissionGranted
                ? ForkLocale.get(R.string.ForkNotificationPermissionGranted)
                : ForkLocale.get(R.string.ForkNotificationPermissionDenied);
        permissionCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationPermission), permissionValue, false);

        appCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationApp),
                ForkLocale.get(appEnabled ? R.string.ForkNotificationEnabled : R.string.ForkNotificationDisabled), false);

        String channelValue;
        if (Build.VERSION.SDK_INT < 26) {
            channelValue = ForkLocale.get(R.string.ForkNotificationChannelNotRequired);
        } else if (channelCount == 0) {
            channelValue = ForkLocale.get(R.string.ForkNotificationChannelMissing);
        } else {
            String importance = ForkLocale.get(importanceString(lowestImportance));
            channelValue = ForkLocale.get(R.string.ForkNotificationChannelValue, channelCount, importance);
        }
        channelCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationChannel), channelValue, true);

        String push = NotificationDiagnosticsPolicy.pushState(playServices, firebaseConfigured, tokenPresent, registered);
        pushCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationPush), ForkLocale.get(pushString(push)), true);

        String background = NotificationDiagnosticsPolicy.backgroundState(backgroundRestricted, ignoringBattery);
        backgroundCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationBackground),
                ForkLocale.get(backgroundString(background)), false);

        String summary = NotificationDiagnosticsPolicy.summary(permissionGranted, appEnabled, channelBlocked,
                playServices, firebaseConfigured, tokenPresent, registered, backgroundRestricted, ignoringBattery);
        summaryCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationSummary),
                ForkLocale.get(summaryString(summary)), true);

        requestPermissionCell.setVisibility(Build.VERSION.SDK_INT >= 33 && !permissionGranted ? View.VISIBLE : View.GONE);
        SharedPreferences notificationSettings = MessagesController.getGlobalNotificationsSettings();
        boolean keepAlive = notificationSettings.contains("pushService")
                ? notificationSettings.getBoolean("pushService", true)
                : MessagesController.getMainSettings(currentAccount).getBoolean("keepAliveService", false);
        backgroundActionCell.setVisibility(
                NotificationDiagnosticsPolicy.shouldRecommendBackgroundConnection(firebaseConfigured, keepAlive)
                        ? View.VISIBLE : View.GONE);

        firebaseInfo.setText(ForkLocale.get(firebaseConfigured
                ? R.string.ForkNotificationPushOkInfo : R.string.ForkNotificationFirebaseInfo));
        if (!firebaseConfigured) {
            firebaseInfo.setText(firebaseInfo.getText() + "\n\n"
                    + ForkLocale.get(R.string.ForkNotificationFirebaseSetupHint));
        }
        testInfo.setText(ForkLocale.get(R.string.ForkNotificationTestInfo));
    }

    private void enableBackgroundConnection() {
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        preferences.edit().putBoolean("pushService", true).apply();
        ApplicationLoader.startPushService();
        updateState();
    }

    private void showTestNotification() {
        Context context = getContext();
        if (context == null) return;
        boolean permissionGranted = Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            testCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationTestAction),
                    ForkLocale.get(R.string.ForkNotificationTestBlocked), true);
            return;
        }
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && manager != null) {
                NotificationChannel channel = new NotificationChannel(TEST_CHANNEL,
                        ForkLocale.get(R.string.ForkNotificationTestChannel), NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(ForkLocale.get(R.string.ForkNotificationTestChannelInfo));
                manager.createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TEST_CHANNEL)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(ForkLocale.get(R.string.ForkNotificationTestTitle))
                    .setContentText(ForkLocale.get(R.string.ForkNotificationTestBody))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, builder.build());
            testCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationTestAction),
                    ForkLocale.get(R.string.ForkNotificationTestDone), true);
        } catch (Throwable error) {
            testCell.setTextAndValue(ForkLocale.get(R.string.ForkNotificationTestAction),
                    ForkLocale.get(R.string.ForkNotificationTestBlocked), true);
        }
    }

    private void openNotificationSettings() {
        Context context = getContext();
        if (context == null) return;
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 26) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + context.getPackageName()));
            }
            context.startActivity(intent);
        } catch (Throwable error) {
            try {
                context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + context.getPackageName())));
            } catch (Throwable ignored) {
            }
        }
    }

    private static int pushString(String state) {
        switch (state) {
            case NotificationDiagnosticsPolicy.PUSH_UNAVAILABLE: return R.string.ForkNotificationPushUnavailable;
            case NotificationDiagnosticsPolicy.PUSH_NOT_CONFIGURED: return R.string.ForkNotificationPushNotConfigured;
            case NotificationDiagnosticsPolicy.PUSH_TOKEN_READY: return R.string.ForkNotificationPushTokenReady;
            case NotificationDiagnosticsPolicy.PUSH_REGISTERED: return R.string.ForkNotificationPushRegistered;
            default: return R.string.ForkNotificationPushUnregistered;
        }
    }

    private static int backgroundString(String state) {
        switch (state) {
            case NotificationDiagnosticsPolicy.BACKGROUND_RESTRICTED: return R.string.ForkNotificationBackgroundRestricted;
            case NotificationDiagnosticsPolicy.BACKGROUND_BATTERY: return R.string.ForkNotificationBackgroundBattery;
            default: return R.string.ForkNotificationBackgroundOk;
        }
    }

    private static int summaryString(String state) {
        switch (state) {
            case NotificationDiagnosticsPolicy.SUMMARY_PERMISSION: return R.string.ForkNotificationSummaryPermission;
            case NotificationDiagnosticsPolicy.SUMMARY_APP_DISABLED: return R.string.ForkNotificationSummaryAppDisabled;
            case NotificationDiagnosticsPolicy.SUMMARY_CHANNEL_BLOCKED: return R.string.ForkNotificationSummaryChannelBlocked;
            case NotificationDiagnosticsPolicy.SUMMARY_PLAY_SERVICES: return R.string.ForkNotificationSummaryPlayServices;
            case NotificationDiagnosticsPolicy.SUMMARY_FIREBASE: return R.string.ForkNotificationSummaryFirebase;
            case NotificationDiagnosticsPolicy.SUMMARY_TOKEN: return R.string.ForkNotificationSummaryToken;
            case NotificationDiagnosticsPolicy.SUMMARY_BACKGROUND: return R.string.ForkNotificationSummaryBackground;
            case NotificationDiagnosticsPolicy.SUMMARY_BATTERY: return R.string.ForkNotificationSummaryBattery;
            default: return R.string.ForkNotificationSummaryReady;
        }
    }

    private static int importanceString(int importance) {
        String value = NotificationDiagnosticsPolicy.channelImportance(importance);
        switch (value) {
            case NotificationDiagnosticsPolicy.IMPORTANCE_NONE: return R.string.ForkNotificationImportanceNone;
            case NotificationDiagnosticsPolicy.IMPORTANCE_MIN: return R.string.ForkNotificationImportanceMin;
            case NotificationDiagnosticsPolicy.IMPORTANCE_LOW: return R.string.ForkNotificationImportanceLow;
            case NotificationDiagnosticsPolicy.IMPORTANCE_DEFAULT: return R.string.ForkNotificationImportanceDefault;
            case NotificationDiagnosticsPolicy.IMPORTANCE_HIGH: return R.string.ForkNotificationImportanceHigh;
            default: return R.string.ForkNotificationImportanceUnknown;
        }
    }
}
