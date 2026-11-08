import org.telegram.messenger.fork.diagnostics.NotificationDiagnosticsPolicy;

public class NotificationDiagnosticsPolicyTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    static void eq(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but was " + actual);
    }

    public static void main(String[] args) {
        check(NotificationDiagnosticsPolicy.canDeliverLocally(true, true, false));
        check(!NotificationDiagnosticsPolicy.canDeliverLocally(false, true, false));
        check(!NotificationDiagnosticsPolicy.canDeliverLocally(true, false, false));
        check(!NotificationDiagnosticsPolicy.canDeliverLocally(true, true, true));

        eq(NotificationDiagnosticsPolicy.IMPORTANCE_NONE, NotificationDiagnosticsPolicy.channelImportance(0));
        eq(NotificationDiagnosticsPolicy.IMPORTANCE_MIN, NotificationDiagnosticsPolicy.channelImportance(1));
        eq(NotificationDiagnosticsPolicy.IMPORTANCE_LOW, NotificationDiagnosticsPolicy.channelImportance(2));
        eq(NotificationDiagnosticsPolicy.IMPORTANCE_DEFAULT, NotificationDiagnosticsPolicy.channelImportance(3));
        eq(NotificationDiagnosticsPolicy.IMPORTANCE_HIGH, NotificationDiagnosticsPolicy.channelImportance(4));
        eq(NotificationDiagnosticsPolicy.IMPORTANCE_UNKNOWN, NotificationDiagnosticsPolicy.channelImportance(9));
        check(NotificationDiagnosticsPolicy.channelBlocked(0));
        check(!NotificationDiagnosticsPolicy.channelBlocked(2));

        eq(NotificationDiagnosticsPolicy.PUSH_UNAVAILABLE,
                NotificationDiagnosticsPolicy.pushState(false, false, false, false));
        eq(NotificationDiagnosticsPolicy.PUSH_NOT_CONFIGURED,
                NotificationDiagnosticsPolicy.pushState(true, false, false, false));
        eq(NotificationDiagnosticsPolicy.PUSH_UNREGISTERED,
                NotificationDiagnosticsPolicy.pushState(true, true, false, false));
        eq(NotificationDiagnosticsPolicy.PUSH_TOKEN_READY,
                NotificationDiagnosticsPolicy.pushState(true, true, true, false));
        eq(NotificationDiagnosticsPolicy.PUSH_REGISTERED,
                NotificationDiagnosticsPolicy.pushState(true, true, true, true));

        eq(NotificationDiagnosticsPolicy.BACKGROUND_RESTRICTED,
                NotificationDiagnosticsPolicy.backgroundState(true, true));
        eq(NotificationDiagnosticsPolicy.BACKGROUND_BATTERY,
                NotificationDiagnosticsPolicy.backgroundState(false, false));
        eq(NotificationDiagnosticsPolicy.BACKGROUND_OK,
                NotificationDiagnosticsPolicy.backgroundState(false, true));

        eq(NotificationDiagnosticsPolicy.SUMMARY_PERMISSION,
                NotificationDiagnosticsPolicy.summary(false, true, false, true, true, true, true, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_APP_DISABLED,
                NotificationDiagnosticsPolicy.summary(true, false, false, true, true, true, true, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_CHANNEL_BLOCKED,
                NotificationDiagnosticsPolicy.summary(true, true, true, true, true, true, true, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_PLAY_SERVICES,
                NotificationDiagnosticsPolicy.summary(true, true, false, false, false, false, false, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_FIREBASE,
                NotificationDiagnosticsPolicy.summary(true, true, false, true, false, false, false, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_TOKEN,
                NotificationDiagnosticsPolicy.summary(true, true, false, true, true, false, false, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_TOKEN,
                NotificationDiagnosticsPolicy.summary(true, true, false, true, true, true, false, false, true));
        eq(NotificationDiagnosticsPolicy.SUMMARY_BACKGROUND,
                NotificationDiagnosticsPolicy.summary(true, true, false, true, true, true, true, true, false));
        eq(NotificationDiagnosticsPolicy.SUMMARY_BATTERY,
                NotificationDiagnosticsPolicy.summary(true, true, false, true, true, true, true, false, false));
        eq(NotificationDiagnosticsPolicy.SUMMARY_READY,
                NotificationDiagnosticsPolicy.summary(true, true, false, true, true, true, true, false, true));

        check(NotificationDiagnosticsPolicy.shouldRecommendBackgroundConnection(false, false));
        check(!NotificationDiagnosticsPolicy.shouldRecommendBackgroundConnection(false, true));
        check(!NotificationDiagnosticsPolicy.shouldRecommendBackgroundConnection(true, false));

        System.out.println("NotificationDiagnosticsPolicyTest passed");
    }
}
