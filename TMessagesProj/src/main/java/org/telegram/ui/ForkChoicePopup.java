package org.telegram.ui;

import android.view.Gravity;
import android.view.View;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ItemOptions;

/** Compact anchored choice menu using Telegram's popup positioning and dismissal. */
final class ForkChoicePopup {
    interface OnChoice { void selected(int index); }
    private ForkChoicePopup() { }

    static void show(BaseFragment parent, View anchor, CharSequence[] labels, int[] icons,
                     int selected, OnChoice callback) {
        if (anchor == null || !anchor.isAttachedToWindow() || parent.getParentActivity() == null) return;
        ItemOptions options = ItemOptions.makeOptions(parent, anchor)
                .setGravity(Gravity.RIGHT).setDimAlpha(0).setDrawScrim(false);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            CharSequence label = (i == selected ? "✓  " : "") + labels[i];
            int icon = icons != null && i < icons.length ? icons[i] : 0;
            options.add(icon, label, () -> callback.selected(index));
        }
        options.show();
    }
}
