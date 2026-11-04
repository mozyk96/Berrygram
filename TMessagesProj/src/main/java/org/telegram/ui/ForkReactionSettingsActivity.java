package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.actions.ForkReactionSettings;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import java.util.ArrayList;
import java.util.List;

/** Ordered standard quick reactions; all sending restrictions remain native. */
public final class ForkReactionSettingsActivity extends BaseFragment {
    private LinearLayout rows;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkReactionTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout page = ForkSettingsLayout.page(context);
        rows = ForkSettingsLayout.section(context);
        ForkSettingsLayout.addSection(page, rows);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true); scroll.addView(page);
        fragmentView = scroll;
        render();
        return fragmentView;
    }
    private TextSettingsCell row(String title, String value, View.OnClickListener click) {
        TextSettingsCell cell = new TextSettingsCell(rows.getContext());
        cell.setTextAndValue(title, value, true);
        ForkSettingsLayout.interactive(cell);
        cell.setOnClickListener(click);
        rows.addView(cell);
        return cell;
    }
    private void render() {
        if (rows == null) return;
        rows.removeAllViews();
        boolean custom = ForkReactionSettings.customized(currentAccount);
        row(ForkLocale.get(R.string.ForkReactionQuick), ForkLocale.get(custom ? R.string.ForkReactionCustom : R.string.ForkReactionNative), anchor -> {
            CharSequence[] choices = {ForkLocale.get(R.string.ForkReactionNative), ForkLocale.get(R.string.ForkReactionCustom)};
            ForkChoicePopup.show(this, anchor, choices, null, custom ? 1 : 0, i -> {
                if (i == 0) ForkReactionSettings.useNativeFavorites(currentAccount);
                else ForkReactionSettings.setFavorites(currentAccount, ForkReactionSettings.favorites(currentAccount));
                render();
            });
        });
        if (custom) {
            List<String> favorites = ForkReactionSettings.favorites(currentAccount);
            for (int i = 0; i < favorites.size(); i++) {
                final int index = i;
                row(ForkLocale.get(R.string.ForkReactionPosition, i + 1), favorites.get(i), anchor -> editFavorite(anchor, index));
            }
            if (favorites.size() < 4) row(ForkLocale.get(R.string.ForkReactionAdd), "", anchor -> chooseEmoji(anchor, null, emoji -> {
                List<String> values = ForkReactionSettings.favorites(currentAccount);
                if (!values.contains(emoji)) values.add(emoji);
                ForkReactionSettings.setFavorites(currentAccount, values); render();
            }));
        }
        String doubleTap = ForkReactionSettings.doubleTapReaction(currentAccount, "");
        String binding = org.telegram.messenger.fork.actions.ActionSettings.gesture(currentAccount, "double_tap");
        String doubleTapLabel = !"native".equals(binding) && !ForkReactionSettings.ACTION_ID.equals(binding)
                ? ForkActionLabels.label(binding).toString()
                : doubleTap.isEmpty() ? ForkLocale.get(R.string.ForkReactionNative) : doubleTap;
        row(ForkLocale.get(R.string.ForkReactionDoubleTap), doubleTapLabel,
                anchor -> chooseEmoji(anchor, doubleTap, emoji -> { ForkReactionSettings.setDoubleTapReaction(currentAccount, emoji); render(); }));
        row(ForkLocale.get(R.string.ForkReactionReset), "", anchor -> { ForkReactionSettings.reset(currentAccount); render(); });
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(rows.getContext());
        info.setText(ForkLocale.get(R.string.ForkReactionInfo)); rows.addView(info);
    }
    private interface EmojiChoice { void selected(String emoji); }
    private void chooseEmoji(View anchor, String selected, EmojiChoice callback) {
        List<String> values = new ArrayList<>();
        // Only the native enabled catalog can offer emoji; custom/Premium documents are excluded.
        for (TLRPC.TL_availableReaction reaction : MediaDataController.getInstance(currentAccount).getEnabledReactionsList()) {
            String emoji = reaction.reaction;
            if (ForkReactionSettings.standardEmoji(emoji) && !values.contains(emoji)) values.add(emoji);
        }
        List<CharSequence> labels = new ArrayList<>();
        if (selected != null) { values.add(0, null); labels.add(ForkLocale.get(R.string.ForkReactionNative)); }
        for (String emoji : values) if (emoji != null) labels.add(emoji);
        if (values.isEmpty()) return;
        ForkChoicePopup.show(this, anchor, labels.toArray(new CharSequence[0]), null, values.indexOf(selected), i -> callback.selected(values.get(i)));
    }
    private void editFavorite(View anchor, int index) {
        List<String> values = ForkReactionSettings.favorites(currentAccount);
        List<CharSequence> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add(ForkLocale.get(R.string.ForkReactionReplace));
        actions.add(() -> chooseEmoji(anchor, null, emoji -> {
            List<String> updated = ForkReactionSettings.favorites(currentAccount);
            if (!updated.contains(emoji)) updated.set(index, emoji);
            ForkReactionSettings.setFavorites(currentAccount, updated); render();
        }));
        if (index > 0) { labels.add(ForkLocale.get(R.string.ForkReactionUp)); actions.add(() -> { ForkReactionSettings.move(currentAccount, index, index - 1); render(); }); }
        if (index + 1 < values.size()) { labels.add(ForkLocale.get(R.string.ForkReactionDown)); actions.add(() -> { ForkReactionSettings.move(currentAccount, index, index + 1); render(); }); }
        if (values.size() > 1) { labels.add(ForkLocale.get(R.string.ForkReactionRemove)); actions.add(() -> { values.remove(index); ForkReactionSettings.setFavorites(currentAccount, values); render(); }); }
        ForkChoicePopup.show(this, anchor, labels.toArray(new CharSequence[0]), null, -1, i -> actions.get(i).run());
    }
    @Override public void onFragmentDestroy() { rows = null; super.onFragmentDestroy(); }
}
