package org.telegram.messenger.fork.diagnostics;

/**
 * Pure mapping rules for BerryGram's notification diagnostics screen.
 *
 * <p>The screen must not invent a "working" state: every value here is derived
 * from a real Android or Telegram signal. Remote push is only reported as
 * registered when the FCM token exists and Telegram has accepted it.</p>
 */
public final class NotificationDiagnosticsPolicy {
    public static final String PUSH_UNAVAILABLE = "push_unavailable";
    public static final String PUSH_NOT_CONFIGURED = "push_not_configured";
    public static final String PUSH_UNREGISTERED = "push_unregistered";
    public static final String PUSH_TOKEN_READY = "push_token_ready";
    public static final String PUSH_REGISTERED = "push_registered";

    public static final String BACKGROUND_OK = "background_ok";
    public static final String BACKGROUND_RESTRICTED = "background_restricted";
    public static final String BACKGROUND_BATTERY = "background_battery";

    public static final String IMPORTANCE_NONE = "none";
    public static final String IMPORTANCE_MIN = "min";
    public static final String IMPORTANCE_LOW = "low";
    public static final String IMPORTANCE_DEFAULT = "default";
    public static final String IMPORTANCE_HIGH = "high";
    public static final String IMPORTANCE_UNKNOWN = "unknown";

    public static final String SUMMARY_READY = "ready";
    public static final String SUMMARY_PERMISSION = "permission";
    public static final String SUMMARY_APP_DISABLED = "app_disabled";
    public static final String SUMMARY_CHANNEL_BLOCKED = "channel_blocked";
    public static final String SUMMARY_PLAY_SERVICES = "play_services";
    public static final String SUMMARY_FIREBASE = "firebase";
    public static final String SUMMARY_TOKEN = "token";
    public static final String SUMMARY_BACKGROUND = "background";
    public static final String SUMMARY_BATTERY = "battery";

    private NotificationDiagnosticsPolicy() { }

    /** Local delivery requires both the Android permission and the app/channel switches. */
    public static boolean canDeliverLocally(boolean permissionGranted,
                                            boolean appNotificationsEnabled,
                                            boolean channelBlocked) {
        return permissionGranted && appNotificationsEnabled && !channelBlocked;
    }

    /** Maps Android's NotificationChannel importance without exposing platform values. */
    public static String channelImportance(int importance) {
        switch (importance) {
            case 0: return IMPORTANCE_NONE;
            case 1: return IMPORTANCE_MIN;
            case 2: return IMPORTANCE_LOW;
            case 3: return IMPORTANCE_DEFAULT;
            case 4: return IMPORTANCE_HIGH;
            default: return IMPORTANCE_UNKNOWN;
        }
    }

    public static boolean channelBlocked(int importance) {
        return importance == 0;
    }

    /**
     * @param playServicesAvailable Google Play Services is installed and usable
     * @param firebaseConfigured    this build contains a fork-owned Firebase app id
     * @param tokenPresent          an FCM token is stored locally
     * @param registeredForPush     Telegram accepted the token for the active account
     */
    public static String pushState(boolean playServicesAvailable,
                                   boolean firebaseConfigured,
                                   boolean tokenPresent,
                                   boolean registeredForPush) {
        if (!playServicesAvailable) return PUSH_UNAVAILABLE;
        if (!firebaseConfigured) return PUSH_NOT_CONFIGURED;
        if (!tokenPresent) return PUSH_UNREGISTERED;
        if (!registeredForPush) return PUSH_TOKEN_READY;
        return PUSH_REGISTERED;
    }

    /**
     * @param backgroundRestricted ActivityManager reports background execution limits
     * @param ignoringBatteryOptimizations app is exempt from Doze battery optimization
     */
    public static String backgroundState(boolean backgroundRestricted,
                                         boolean ignoringBatteryOptimizations) {
        if (backgroundRestricted) return BACKGROUND_RESTRICTED;
        if (!ignoringBatteryOptimizations) return BACKGROUND_BATTERY;
        return BACKGROUND_OK;
    }

    /** Returns the single most useful action to show first. */
    public static String summary(boolean permissionGranted,
                                 boolean appNotificationsEnabled,
                                 boolean channelBlocked,
                                 boolean playServicesAvailable,
                                 boolean firebaseConfigured,
                                 boolean tokenPresent,
                                 boolean registeredForPush,
                                 boolean backgroundRestricted,
                                 boolean ignoringBatteryOptimizations) {
        if (!permissionGranted) return SUMMARY_PERMISSION;
        if (!appNotificationsEnabled) return SUMMARY_APP_DISABLED;
        if (channelBlocked) return SUMMARY_CHANNEL_BLOCKED;
        if (!playServicesAvailable) return SUMMARY_PLAY_SERVICES;
        if (!firebaseConfigured) return SUMMARY_FIREBASE;
        if (!tokenPresent || !registeredForPush) return SUMMARY_TOKEN;
        if (backgroundRestricted) return SUMMARY_BACKGROUND;
        if (!ignoringBatteryOptimizations) return SUMMARY_BATTERY;
        return SUMMARY_READY;
    }

    /**
     * Background sync is a real fallback while a Firebase configuration is missing:
     * Telegram keeps its own connection and posts notifications locally.
     */
    public static boolean shouldRecommendBackgroundConnection(boolean firebaseConfigured,
                                                              boolean keepAliveEnabled) {
        return !firebaseConfigured && !keepAliveEnabled;
    }
}
