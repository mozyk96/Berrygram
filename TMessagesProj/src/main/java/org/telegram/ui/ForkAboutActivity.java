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

/** Basic attribution and installed-version information. */
public final class ForkAboutActivity extends BaseFragment {
    @Override public View createView(Context context) {
        actionBar.setTitle(context.getString(R.string.ForkAboutTitle));
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
        title.setText(context.getString(R.string.ForkAppName));
        title.setTextSize(22);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        content.addView(title);
        TextView details = new TextView(context);
        String version = "";
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException ignored) { }
        details.setText(context.getString(R.string.ForkIntroDisclosure) + "\n\n" +
            context.getString(R.string.ForkAboutVersion, version == null ? "" : version) + "\n\n" +
            context.getString(R.string.ForkAboutAttribution));
        details.setTextSize(16);
        details.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        details.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        content.addView(details);
        scroll.addView(content);
        fragmentView = scroll;
        return fragmentView;
    }
}
