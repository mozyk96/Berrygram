import org.telegram.messenger.fork.auth.AuthDiagnostics;
public final class AuthDiagnosticsTest {
    public static void main(String[] args) {
        check(AuthDiagnostics.errorCategory("PHONE_NUMBER_FLOOD").equals("PHONE_NUMBER_FLOOD"));
        check(AuthDiagnostics.errorCategory("FLOOD_WAIT_42").equals("FLOOD_WAIT"));
        check(AuthDiagnostics.errorCategory("SESSION_PASSWORD_NEEDED").equals("SESSION_PASSWORD_NEEDED"));
        check(AuthDiagnostics.errorCategory("PRIVATE_PHONE_OR_TOKEN").equals("OTHER"));
        check(AuthDiagnostics.errorCategory(null).equals("UNKNOWN"));
        System.out.println("AuthDiagnosticsTest passed");
    }
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
}
