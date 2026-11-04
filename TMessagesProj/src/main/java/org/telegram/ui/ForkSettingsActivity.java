package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.Gravity;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.R;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.messenger.fork.panels.ForkPanelSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;

/** BerryGram settings use Telegram's theme-aware, compact list components. */
public final class ForkSettingsActivity extends BaseFragment {
    private ForkUpdaterPanel updater;
    private TextDetailSettingsCell[] accentCells;
    private LinearLayout settingsPage;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkSettingsTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout content = ForkSettingsLayout.page(context);
        settingsPage = content;
        TextDetailSettingsCell appearance = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(appearance);
        appearance.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkAppearanceTitle), ForkLocale.get(R.string.ForkAppearanceSubtitle), R.drawable.msg_palette, false);
        appearance.setOnClickListener(v -> presentFragment(new ForkAppearanceSettingsActivity()));
        ForkSettingsLayout.addCard(content, appearance);
        TextDetailSettingsCell video = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(video);
        video.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkVideoTitle), ForkLocale.get(R.string.ForkVideoSubtitle), R.drawable.msg_video, false);
        video.setOnClickListener(v -> presentFragment(new ForkVideoSettingsActivity())); ForkSettingsLayout.addCard(content, video);
        TextDetailSettingsCell voice = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(voice);
        voice.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkVoiceTitle), ForkLocale.get(R.string.ForkVoiceSubtitle), R.drawable.msg_filled_data_voice, false);
        voice.setOnClickListener(v -> presentFragment(new ForkVoiceSettingsActivity())); ForkSettingsLayout.addCard(content, voice);
        TextDetailSettingsCell text = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(text);
        text.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkCustomTextTitle), ForkLocale.get(R.string.ForkTextSubtitle), R.drawable.msg_edit, false);
        text.setOnClickListener(v -> presentFragment(new ForkCustomTextActivity())); ForkSettingsLayout.addCard(content, text);
        TextDetailSettingsCell gestures = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(gestures);
        gestures.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkGestureTitle), ForkLocale.get(R.string.ForkGestureDoubleTap), R.drawable.menu_reply, false);
        gestures.setOnClickListener(v -> presentFragment(new ForkGestureSettingsActivity())); ForkSettingsLayout.addCard(content, gestures);
        TextDetailSettingsCell reactions = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(reactions);
        reactions.setTextAndValueAndIcon(LocaleController.getString(R.string.Reactions), ForkLocale.get(R.string.ForkGestureDoubleTap), R.drawable.msg_reactions, false);
        reactions.setOnClickListener(v -> presentFragment(new ForkReactionSettingsActivity())); ForkSettingsLayout.addCard(content, reactions);
        TextDetailSettingsCell messageMenu = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(messageMenu);
        messageMenu.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkMenuEditorTitle), ForkLocale.get(R.string.ForkMenuEditorPreview), R.drawable.msg_settings, false);
        messageMenu.setOnClickListener(v -> presentFragment(new ForkMessageMenuSettingsActivity())); ForkSettingsLayout.addCard(content, messageMenu);
        accentCells = new TextDetailSettingsCell[]{appearance, video, voice, text, gestures, reactions, messageMenu};
        updateAccent(context);
        LinearLayout updates = ForkSettingsLayout.section(context);
        updater = new ForkUpdaterPanel(context, this); updates.addView(updater);
        ForkSettingsLayout.addSection(content, updates);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.addView(content);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkSettingsTitle), this::finishFragment); return fragmentView;
    }
    private void updateAccent(Context context) {
        if (accentCells == null) return;
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        if ("material".equals(org.telegram.messenger.fork.panels.ForkPanelSettings.style())) background = ForkGraphicsSurface.materialColor(context, background);
        int color = org.telegram.messenger.fork.panels.GraphicsColorPolicy.readableAccent(ForkAppearanceSettingsActivity.accent(context), background);
        for (TextDetailSettingsCell cell : accentCells) cell.setIconColor(color);
    }
    @Override public void onPause() { if (updater != null) updater.onParentPaused(); super.onPause(); }
    @Override public void onResume() { super.onResume(); if (updater != null) updater.onParentResumed(); if (fragmentView != null) ForkFloatingHeader.refresh(fragmentView); if (settingsPage != null) { ForkSettingsLayout.refreshPage(settingsPage); updateAccent(settingsPage.getContext()); } }
    @Override public void onFragmentDestroy() { if (updater != null) updater.dispose(); updater = null; accentCells = null; settingsPage = null; super.onFragmentDestroy(); }
}
