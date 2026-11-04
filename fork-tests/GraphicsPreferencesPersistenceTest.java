import android.content.Context;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.fork.panels.ForkChatHeaderSettings;
import org.telegram.messenger.fork.panels.ForkAnimationSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;

public class GraphicsPreferencesPersistenceTest {
    public static void main(String[] args) {
        // Simulate upgrading a device which previously opted in.
        ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .edit().putBoolean("floating_chat_header", true).apply();
        ForkChatHeaderSettings.setEnabled(true);
        ForkAnimationSettings.setFluidPopupsEnabled(true);
        ForkPanelSettings.setStyle("blur");
        if (ForkChatHeaderSettings.enabled() || !ForkAnimationSettings.fluidPopupsEnabled()) throw new AssertionError("Legacy preference enabled header or popup preference failed");
        ForkPanelSettings.setStyle("liquid");
        if (ForkChatHeaderSettings.enabled() || !ForkAnimationSettings.fluidPopupsEnabled()) throw new AssertionError("Style change affected independent preferences");
        ForkChatHeaderSettings.setEnabled(false);
        ForkAnimationSettings.setFluidPopupsEnabled(false);
        if (ForkChatHeaderSettings.enabled() || ForkAnimationSettings.fluidPopupsEnabled()) throw new AssertionError("Disable did not persist");
        if (!"liquid".equals(ForkPanelSettings.style())) throw new AssertionError("Independent toggles changed graphics mode");
        System.out.println("GraphicsPreferencesPersistenceTest passed (legacy header ignored; popup/style preferences independent)");
    }
}
