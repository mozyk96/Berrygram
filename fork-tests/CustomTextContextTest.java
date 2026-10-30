import org.telegram.messenger.fork.text.CustomTextContext;
public class CustomTextContextTest {
    public static void main(String[] args) throws Exception {
        check(CustomTextContext.current() == null);
        try (CustomTextContext.Scope a = CustomTextContext.open(1, 10)) {
            check(CustomTextContext.current().account == 1 && CustomTextContext.current().dialogId == 10);
            java.util.concurrent.atomic.AtomicBoolean isolated = new java.util.concurrent.atomic.AtomicBoolean(false);
            Thread other = new Thread(() -> isolated.set(CustomTextContext.current() == null)); other.start(); other.join();
            check(isolated.get());
            try (CustomTextContext.Scope b = CustomTextContext.open(2, -20)) {
                check(CustomTextContext.current().account == 2 && CustomTextContext.current().dialogId == -20);
            }
            check(CustomTextContext.current().account == 1);
        }
        check(CustomTextContext.current() == null);
        System.out.println("CustomTextContextTest passed");
    }
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
}
