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
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkSettingsTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout content = ForkSettingsLayout.page(context);
        TextDetailSettingsCell video = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(video);
        video.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkVideoTitle), ForkLocale.get(R.string.ForkVideoSubtitle), R.drawable.msg_video, false);
        video.setOnClickListener(v -> presentFragment(new ForkVideoSettingsActivity())); ForkSettingsLayout.addCard(content, video);
        TextDetailSettingsCell voice = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(voice);
        voice.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkVoiceTitle), ForkLocale.get(R.string.ForkVoiceSubtitle), R.drawable.msg_filled_data_voice, false);
        voice.setOnClickListener(v -> presentFragment(new ForkVoiceSettingsActivity())); ForkSettingsLayout.addCard(content, voice);
        TextCheckCell panels = new TextCheckCell(context, 71);
        ForkSettingsLayout.interactive(panels);
        ImageView panelIcon = new ImageView(context);
        panelIcon.setImageResource(R.drawable.msg_palette);
        panelIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        panels.addView(panelIcon, LayoutHelper.createFrame(24, 24, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 17, 0, 17, 0));
        panels.setTextAndCheck(ForkLocale.get(R.string.ForkPanelsTitle), ForkPanelSettings.enabled(), false);
        panels.setOnClickListener(v -> { boolean enabled = !ForkPanelSettings.enabled(); ForkPanelSettings.setEnabled(enabled); panels.setChecked(enabled); });
        ForkSettingsLayout.addCard(content, panels);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkPanelsInfo)); content.addView(info);
        TextDetailSettingsCell text = new TextDetailSettingsCell(context);
        ForkSettingsLayout.interactive(text);
        text.setTextAndValueAndIcon(ForkLocale.get(R.string.ForkCustomTextTitle), ForkLocale.get(R.string.ForkTextSubtitle), R.drawable.msg_edit, false);
        text.setOnClickListener(v -> presentFragment(new ForkCustomTextActivity())); ForkSettingsLayout.addCard(content, text);
        LinearLayout updates = ForkSettingsLayout.section(context);
        updater = new ForkUpdaterPanel(context, this); updates.addView(updater);
        ForkSettingsLayout.addSection(content, updates);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.addView(content);
        fragmentView = scroll; return scroll;
    }
    @Override public void onFragmentDestroy() { if (updater != null) updater.dispose(); updater = null; super.onFragmentDestroy(); }
}
