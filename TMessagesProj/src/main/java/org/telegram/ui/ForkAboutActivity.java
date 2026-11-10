package org.telegram.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

/** Basic attribution, installed-version information and the Update Center. */
public final class ForkAboutActivity extends ForkSettingsFragment {

    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkAboutTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        TextView title = new TextView(context);
        title.setText(ForkLocale.get(R.string.ForkAppName));
        title.setTextSize(22);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        content.addView(title);
        TextView details = new TextView(context);
        String version = "";
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException ignored) { }
        details.setText(ForkLocale.get(R.string.ForkIntroDisclosure) + "\n\n" +
            ForkLocale.get(R.string.ForkAboutVersion, version == null ? "" : version) + "\n\n" +
            ForkLocale.get(R.string.ForkAboutAttribution));
        details.setTextSize(16);
        details.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        details.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        content.addView(details);

        org.telegram.ui.Cells.TextSettingsCell updates = new org.telegram.ui.Cells.TextSettingsCell(context);
        updates.setText(ForkLocale.get(R.string.ForkUpdatePanel), false);
        updates.setIcon(R.drawable.msg_download);
        updates.setBackground(Theme.getSelectorDrawable(false));
        updates.setOnClickListener(v -> presentFragment(new ForkUpdateActivity()));
        content.addView(updates);

        scroll.addView(content);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkAboutTitle), this::finishFragment);
        return fragmentView;
    }

}
