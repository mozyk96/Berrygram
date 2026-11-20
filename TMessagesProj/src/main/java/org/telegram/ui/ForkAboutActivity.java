package org.telegram.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.pm.PackageInfoCompat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.fork.settings.ForkSettingsSection;

/** Installed build information and project attribution. */
public final class ForkAboutActivity extends ForkSettingsFragment {

    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkAboutTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        LinearLayout content = ForkSettingsLayout.page(context);
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(32), AndroidUtilities.dp(24), AndroidUtilities.dp(18));
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.mipmap.berrygram_launcher);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        hero.addView(icon, new LinearLayout.LayoutParams(AndroidUtilities.dp(76), AndroidUtilities.dp(76)));
        TextView title = addCenteredText(hero, ForkLocale.get(R.string.ForkAppName), 28, Theme.key_windowBackgroundWhiteBlackText, 12);
        title.setTypeface(AndroidUtilities.bold());
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String build = info.versionName + " (" + PackageInfoCompat.getLongVersionCode(info) + ")";
            addCenteredText(hero, ForkLocale.get(R.string.ForkAboutVersion, build), 14, Theme.key_windowBackgroundWhiteGrayText2, 4);
        }
        catch (PackageManager.NameNotFoundException ignored) { }
        addCenteredText(hero, ForkLocale.get(R.string.ForkIntroDisclosure), 16, Theme.key_windowBackgroundWhiteBlackText, 16);
        content.addView(hero);

        ForkSettingsSection links = new ForkSettingsSection(context);
        links.addRow(R.string.ForkAboutSource, "GitHub", R.drawable.msg_link, v -> {
            Browser.openUrl(context, "https://github.com/mozyk96/Berrygram");
            return kotlin.Unit.INSTANCE;
        });
        links.addRow(R.string.ForkAboutUpstream, null, R.drawable.msg_link, v -> {
            Browser.openUrl(context, "https://github.com/DrKLO/Telegram");
            return kotlin.Unit.INSTANCE;
        });
        links.addRow(R.string.ForkAboutLicense, "GNU GPL", R.drawable.msg_info, v -> {
            Browser.openUrl(context, "https://github.com/mozyk96/Berrygram/blob/HEAD/LICENSE");
            return kotlin.Unit.INSTANCE;
        });
        links.addInfo(R.string.ForkAboutAttribution);
        content.addView(links);

        scroll.addView(content);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkAboutTitle), this::finishFragment);
        return fragmentView;
    }

    private static TextView addCenteredText(LinearLayout parent, CharSequence value, int size, int colorKey, int top) {
        TextView text = new TextView(parent.getContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(Theme.getColor(colorKey));
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = AndroidUtilities.dp(top);
        parent.addView(text, params);
        return text;
    }
}
