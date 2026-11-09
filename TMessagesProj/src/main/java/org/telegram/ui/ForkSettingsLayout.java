package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.messenger.fork.panels.PanelPolicy;

/**
 * Shared spacing and theme aware surfaces for the BerryGram settings.
 *
 * <p>The screens use rounded grouped surfaces with a consistent radius, the
 * same spacing between the groups and a clear section header. The radius and
 * the colors follow the active graphical style, so the dark and the light
 * theme look the same.</p>
 */
public final class ForkSettingsLayout {
    private static final int MARGIN_DP = 12;
    private static final int HEADER_DP = 20;

    private ForkSettingsLayout() {}

    public static LinearLayout page(Context context) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, 0, 0, AndroidUtilities.dp(MARGIN_DP));
        page.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        return page;
    }

    public static LinearLayout section(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        refreshSection(section, false);
        section.setClipToOutline(true);
        return section;
    }

    public static void refreshSection(LinearLayout section, boolean card) {
        String style = ForkGraphicsSurface.effectiveStyle();
        // Settings sections have no captured backdrop; use an opaque theme fallback.
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        if ("material".equals(style)) background = ForkGraphicsSurface.materialColor(section.getContext(), background);
        int radius = radiusDp();
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(radius), background));
    }

    /** The corner radius of the grouped surfaces. */
    public static int radiusDp() {
        String style = ForkGraphicsSurface.effectiveStyle();
        return PanelPolicy.surfaceRadiusDp(style, ForkPanelSettings.radiusDp());
    }

    public static void addSection(LinearLayout page, View section) {
        int margin = AndroidUtilities.dp(MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        section.setTag(Boolean.FALSE);
        params.setMargins(margin, margin, margin, 0);
        page.addView(section, params);
    }

    public static void addCard(LinearLayout page, View child) {
        LinearLayout card = section(page.getContext());
        refreshSection(card, true);
        card.setTag(Boolean.TRUE);
        card.addView(child);
        int margin = AndroidUtilities.dp(MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, AndroidUtilities.dp(org.telegram.messenger.fork.panels.PanelPolicy.settingsGapDp()), margin, 0);
        page.addView(card, params);
    }

    public static void refreshPage(LinearLayout page) {
        page.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child instanceof LinearLayout && child.getTag() instanceof Boolean) refreshSection((LinearLayout) child, (Boolean) child.getTag());
        }
    }

    /**
     * Makes a row interactive. The pressed state uses the same rounded shape
     * as the grouped surface, a plain rectangle would break the rounded card.
     */
    public static void interactive(View view) {
        Drawable background = Theme.createSelectorDrawable(
                Theme.getColor(Theme.key_listSelector),
                Theme.RIPPLE_MASK_ROUNDRECT_6DP,
                AndroidUtilities.dp(radiusDp()));
        if (background != null) {
            view.setBackground(background);
        }
    }

    /**
     * A section header, the same style the Telegram settings use. It makes the
     * hierarchy of a screen readable without a heavy typeface.
     */
    public static TextView header(Context context, CharSequence title) {
        TextView view = new TextView(context);
        view.setText(title);
        view.setTextSize(AndroidUtilities.dp(13));
        view.setTypeface(AndroidUtilities.bold());
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        view.setAllCaps(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(28));
        params.setMargins(AndroidUtilities.dp(MARGIN_DP + 8), AndroidUtilities.dp(HEADER_DP), AndroidUtilities.dp(MARGIN_DP + 8), 0);
        view.setLayoutParams(params);
        return view;
    }
}
