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
import org.telegram.ui.Cells.TextSettingsCell;

/** The four BerryGram 1.0 settings, using Telegram's theme-aware cells. */
public final class ForkSettingsActivity extends BaseFragment {
    @Override public View createView(Context context) {
        actionBar.setTitle(context.getString(R.string.ForkSettingsTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        TextSettingsCell video = new TextSettingsCell(context);
        video.setText(context.getString(R.string.ForkVideoTitle), true); video.setIcon(R.drawable.msg_video);
        video.setOnClickListener(v -> presentFragment(new ForkVideoSettingsActivity())); rows.addView(video);
        TextSettingsCell voice = new TextSettingsCell(context);
        voice.setText(context.getString(R.string.ForkVoiceTitle), true); voice.setIcon(R.drawable.msg_filled_data_voice);
        voice.setOnClickListener(v -> presentFragment(new ForkVoiceSettingsActivity())); rows.addView(voice);
        TextCheckCell panels = new TextCheckCell(context, 58);
        ImageView panelIcon = new ImageView(context);
        panelIcon.setImageResource(R.drawable.msg_palette);
        panelIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        panels.addView(panelIcon, LayoutHelper.createFrame(24, 24, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 17, 0, 17, 0));
        panels.setTextAndCheck(context.getString(R.string.ForkPanelsTitle), ForkPanelSettings.enabled(), true);
        panels.setOnClickListener(v -> { boolean enabled = !ForkPanelSettings.enabled(); ForkPanelSettings.setEnabled(enabled); panels.setChecked(enabled); });
        rows.addView(panels);
        TextSettingsCell text = new TextSettingsCell(context);
        text.setText(context.getString(R.string.ForkCustomTextTitle), false); text.setIcon(R.drawable.msg_edit);
        text.setOnClickListener(v -> presentFragment(new ForkCustomTextActivity())); rows.addView(text);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(context.getString(R.string.ForkPanelsInfo)); rows.addView(info);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.addView(rows);
        fragmentView = scroll; return scroll;
    }
}
