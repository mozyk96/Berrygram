package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** Shared spacing and theme-aware surfaces for BerryGram settings. */
final class ForkSettingsLayout {
    private static final int MARGIN_DP = 12;
    private static final int RADIUS_DP = 14;
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
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(RADIUS_DP), Theme.getColor(Theme.key_windowBackgroundWhite)));
        section.setClipToOutline(true);
        return section;
    }
    static void addSection(LinearLayout page, View section) {
        int margin = AndroidUtilities.dp(MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, margin, margin, 0);
        page.addView(section, params);
    }
    static void interactive(View view) { view.setBackground(Theme.getSelectorDrawable(false)); }
}
