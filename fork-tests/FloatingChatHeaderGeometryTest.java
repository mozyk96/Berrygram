import org.telegram.messenger.fork.panels.ForkChatHeaderSettings;

/** Legacy test entry point retained for the smoke suite. */
public class FloatingChatHeaderGeometryTest {
    public static void main(String[] args) {
        if (ForkChatHeaderSettings.enabled()) throw new AssertionError("Experimental header enabled by default");
        ForkChatHeaderSettings.setEnabled(true);
        if (ForkChatHeaderSettings.enabled()) throw new AssertionError("Legacy opt-in restored experimental header");
        ForkChatHeaderSettings.setEnabled(false);
        if (ForkChatHeaderSettings.enabled()) throw new AssertionError("Native header not retained");
        System.out.println("Native chat header: legacy opt-in is always ignored");
    }
}
