import java.util.Locale;
import org.telegram.messenger.fork.text.ForkLocalePolicy;

public final class ForkLocalePolicyTest {
    public static void main(String[] args) {
        Locale system = Locale.ENGLISH;
        Locale telegram = new Locale("uk", "UA");
        if (!telegram.equals(ForkLocalePolicy.select(telegram, system))) throw new AssertionError("Telegram language must win over system language");
        if (!system.equals(ForkLocalePolicy.select(null, system))) throw new AssertionError("Missing Telegram language must use system language");
        System.out.println("ForkLocalePolicyTest passed");
    }
}
