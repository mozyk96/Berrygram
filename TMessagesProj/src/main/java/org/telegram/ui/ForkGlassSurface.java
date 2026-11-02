package org.telegram.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;

/** Bounded tint for fork-owned popup backdrops; Telegram-owned providers stay untouched. */
public final class ForkGlassSurface {
    private ForkGlassSurface() { }
    public static BlurredBackgroundProvider provider(Theme.ResourcesProvider resourcesProvider) {
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
                .setBackgroundColor((r, dark) -> {
                    float opacity = 0.94f - 0.16f * ForkPanelSettings.effectStrength() / 100f;
                    return Theme.multAlpha(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, r), opacity);
                })
                .setStrokeColorTop(0x44ffffff, 0x22ffffff)
                .setStrokeColorBottom(0x22ffffff, 0x11ffffff)
                .setShadowColor(0x26000000, 0x18000000)
                .setShadowLayer(AndroidUtilities.dpf2(4f), 0, 0)
                .setStrokeWidth(AndroidUtilities.dpf2(2f / 3f), AndroidUtilities.dpf2(2f / 3f))
                .build();
    }
}
