import org.telegram.messenger.fork.auth.SessionCredentials;
public final class SessionCredentialsTest {
    public static void main(String[] args) {
        char[] original = "0123456789abcdef0123456789abcdef".toCharArray();
        SessionCredentials c = SessionCredentials.create("123", original);
        original[0] = 'x';
        check(c.apiId() == 123 && c.apiHash().charAt(0) == '0', "defensive copy");
        c.clear();
        check(c.isCleared(), "clear");
        try { c.apiHash(); throw new AssertionError("cleared credential accessible"); } catch (IllegalStateException expected) { }
        for (String id : new String[] {"0", "-1", "2147483648", "1a"}) {
            try { SessionCredentials.create(id, "0123456789abcdef0123456789abcdef".toCharArray()); throw new AssertionError("invalid id accepted"); } catch (IllegalArgumentException expected) { }
        }
        try { SessionCredentials.create("12", "invalid".toCharArray()); throw new AssertionError("invalid hash accepted"); } catch (IllegalArgumentException expected) { }
        System.out.println("SessionCredentialsTest passed");
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
