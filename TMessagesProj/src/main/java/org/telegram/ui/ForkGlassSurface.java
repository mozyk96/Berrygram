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
        String style = ForkPanelSettings.style();
        float strength = ForkPanelSettings.effectStrength() / 100f;
        final float opacity;
        final int strokeTopLight, strokeTopDark, strokeBottomLight, strokeBottomDark, shadowLight, shadowDark;
        final float strokeWidth;
        if ("experimental".equals(style)) {
            // Original high-translucency glass: quieter tint, stronger edge light, soft shadow.
            opacity = 0.82f - 0.24f * strength;
            strokeTopLight = 0x66ffffff;
            strokeTopDark = 0x33ffffff;
            strokeBottomLight = 0x2affffff;
            strokeBottomDark = 0x16ffffff;
            shadowLight = 0x30000000;
            shadowDark = 0x22000000;
            strokeWidth = AndroidUtilities.dpf2(1f);
        } else {
            opacity = 0.94f - 0.16f * strength;
            strokeTopLight = 0x44ffffff;
            strokeTopDark = 0x22ffffff;
            strokeBottomLight = 0x22ffffff;
            strokeBottomDark = 0x11ffffff;
            shadowLight = 0x26000000;
            shadowDark = 0x18000000;
            strokeWidth = AndroidUtilities.dpf2(2f / 3f);
        }
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
                .setBackgroundColor((r, dark) -> Theme.multAlpha(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, r), opacity))
                .setStrokeColorTop(strokeTopLight, strokeTopDark)
                .setStrokeColorBottom(strokeBottomLight, strokeBottomDark)
                .setShadowColor(shadowLight, shadowDark)
                .setShadowLayer(AndroidUtilities.dpf2(5f), 0, 0)
                .setStrokeWidth(strokeWidth, strokeWidth)
                .build();
    }
}
