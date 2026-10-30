import org.telegram.messenger.fork.auth.BackendPolicy;
public final class BackendPolicyTest {
    public static void main(String[] args) {
        check(BackendPolicy.shouldRestoreProduction(true, false), "unconfigured test backend must restore production");
        check(!BackendPolicy.shouldRestoreProduction(false, false), "production must not be toggled");
        check(!BackendPolicy.shouldRestoreProduction(true, true), "do not destroy an existing authorized session");
        check(!BackendPolicy.shouldRestoreProduction(false, true), "keep authorized production session");
        System.out.println("BackendPolicyTest passed");
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
