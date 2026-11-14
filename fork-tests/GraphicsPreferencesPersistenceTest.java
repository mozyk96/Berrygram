import android.content.Context;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.fork.panels.ForkChatHeaderSettings;
import org.telegram.messenger.fork.panels.ForkAnimationSettings;
import org.telegram.messenger.fork.panels.ForkPanelSettings;

public class GraphicsPreferencesPersistenceTest {
    public static void main(String[] args) {
        if (ForkChatHeaderSettings.isIosStyle()) throw new AssertionError("Telegram header must be the default");
        ForkChatHeaderSettings.setIosStyle(true);
        ForkPanelSettings.setStyle("none");
        if (!ForkChatHeaderSettings.isIosStyle()) throw new AssertionError("Header style was not persisted independently");
        if (!ApplicationLoader.applicationContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .getBoolean("ios_chat_header", false)) throw new AssertionError("Header style missing from storage");
        ForkChatHeaderSettings.setIosStyle(false);
        if (ForkChatHeaderSettings.isIosStyle()) throw new AssertionError("Cannot restore Telegram header");
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
