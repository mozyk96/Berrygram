import org.telegram.messenger.fork.updater.UpdateInstallIntent;
public class UpdateInstallIntentTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        UpdateInstallIntent intent = new UpdateInstallIntent();
        check(!intent.consume(true, true, true, true));
        intent.request(); intent.request();
        check(!intent.consume(true, false, true, true));
        check(!intent.consume(false, true, true, true));
        check(!intent.consume(true, true, false, true));
        check(!intent.consume(true, true, true, false));
        check(intent.consume(true, true, true, true));
        check(!intent.consume(true, true, true, true)); // Cancelled installer must not reopen.
        intent.request(); intent.cancel(); // Detach or failure revokes intent.
        check(!intent.consume(true, true, true, true));
        intent.request(); check(intent.consume(true, true, true, true)); // Explicit retry.
        check(!new UpdateInstallIntent().pending()); // Recreation never restores consent.
        System.out.println("UpdateInstallIntentTest passed");
    }
}
