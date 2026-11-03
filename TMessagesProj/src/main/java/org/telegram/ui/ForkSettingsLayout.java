package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** Shared spacing and theme-aware surfaces for BerryGram settings. */
final class ForkSettingsLayout {
    private static final int MARGIN_DP = 12;
    private ForkSettingsLayout() {}

    static LinearLayout page(Context context) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, 0, 0, AndroidUtilities.dp(MARGIN_DP));
        page.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        return page;
    }
    static LinearLayout section(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        refreshSection(section, false);
        section.setClipToOutline(true);
        return section;
    }
    static void refreshSection(LinearLayout section, boolean card) {
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        if ("material".equals(org.telegram.messenger.fork.panels.ForkPanelSettings.style())) background = ForkGraphicsSurface.materialColor(section.getContext(), background);
        int radius = org.telegram.messenger.fork.panels.PanelPolicy.surfaceRadiusDp(
                org.telegram.messenger.fork.panels.ForkPanelSettings.style(),
                org.telegram.messenger.fork.panels.ForkPanelSettings.radiusDp());
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(radius), background));
    }
    static void addSection(LinearLayout page, View section) {
        int margin = AndroidUtilities.dp(MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        section.setTag(Boolean.FALSE);
        params.setMargins(margin, margin, margin, 0);
        page.addView(section, params);
    }
    static void addCard(LinearLayout page, View child) {
        LinearLayout card = section(page.getContext());
        refreshSection(card, true);
        card.setTag(Boolean.TRUE);
        card.addView(child);
        int margin = AndroidUtilities.dp(MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, AndroidUtilities.dp(org.telegram.messenger.fork.panels.PanelPolicy.settingsGapDp()), margin, 0);
        page.addView(card, params);
    }
    static void refreshPage(LinearLayout page) {
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child instanceof LinearLayout && child.getTag() instanceof Boolean) refreshSection((LinearLayout) child, (Boolean) child.getTag());
        }
    }
    static void interactive(View view) { view.setBackground(Theme.getSelectorDrawable(false)); }
}
