import org.telegram.messenger.fork.panels.ForkChatHeaderSettings;
import org.telegram.messenger.fork.panels.ForkAnimationSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
public class GraphicsPreferencesPersistenceTest {
    public static void main(String[] args) throws Exception {
        ForkChatHeaderSettings.setEnabled(true);
        ForkAnimationSettings.setFluidPopupsEnabled(true);
        ForkPanelSettings.setStyle("blur");
        java.lang.reflect.Field cache = ForkChatHeaderSettings.class.getDeclaredField("cached");
        cache.setAccessible(true); cache.set(null, null);
        if (!ForkChatHeaderSettings.enabled() || !ForkAnimationSettings.fluidPopupsEnabled()) throw new AssertionError("Preferences did not reload");
        ForkPanelSettings.setStyle("liquid");
        if (!ForkChatHeaderSettings.enabled() || !ForkAnimationSettings.fluidPopupsEnabled()) throw new AssertionError("Style change reset independent preferences");
        ForkChatHeaderSettings.setEnabled(false); ForkAnimationSettings.setFluidPopupsEnabled(false);
        cache.set(null, null);
        if (ForkChatHeaderSettings.enabled() || ForkAnimationSettings.fluidPopupsEnabled()) throw new AssertionError("Disable did not persist");
        if (!"liquid".equals(ForkPanelSettings.style())) throw new AssertionError("Independent toggles changed graphics mode");
        System.out.println("GraphicsPreferencesPersistenceTest passed (mock Android preferences reload and setting independence)");
    }
}
