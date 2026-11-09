package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.actions.ActionMenuPolicy;
import org.telegram.messenger.fork.actions.ActionMenuSettings;
import org.telegram.messenger.fork.actions.ActionRegistry;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

/** Edits presentation only. Native Telegram still decides which actions a message allows. */
public final class ForkMessageMenuSettingsActivity extends ForkSettingsFragment {
    private List<String> order;
    private MenuAdapter adapter;
    private ItemTouchHelper touchHelper;

    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkMenuEditorTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        order = ActionMenuSettings.order(currentAccount);
        LinearLayout page = ForkSettingsLayout.page(context);
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(ForkLocale.get(R.string.ForkMenuEditorInfo));
        page.addView(info);
        LinearLayout controls = ForkSettingsLayout.section(context);
        TextSettingsCell preview = new TextSettingsCell(context);
        preview.setText(ForkLocale.get(R.string.ForkMenuEditorPreview), true);
        ForkSettingsLayout.interactive(preview);
        preview.setOnClickListener(v -> showPreview(context));
        controls.addView(preview);
        TextSettingsCell reset = new TextSettingsCell(context);
        reset.setText(ForkLocale.get(R.string.ForkMenuEditorReset), false);
        ForkSettingsLayout.interactive(reset);
        reset.setOnClickListener(v -> {
            ActionMenuSettings.reset(currentAccount);
            order = ActionMenuSettings.order(currentAccount);
            adapter.notifyDataSetChanged();
        });
        controls.addView(reset);
        ForkSettingsLayout.addSection(page, controls);
        RecyclerView list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        adapter = new MenuAdapter();
        list.setAdapter(adapter);
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean isLongPressDragEnabled() { return false; }
            @Override public boolean onMove(RecyclerView recycler, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
                int from = source.getAdapterPosition(), to = target.getAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
                ActionMenuSettings.move(currentAccount, from, to);
                order.add(to, order.remove(from));
                adapter.notifyItemMoved(from, to);
                return true;
            }
            @Override public void onSwiped(RecyclerView.ViewHolder holder, int direction) { }
        });
        touchHelper.attachToRecyclerView(list);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, 0, 1);
        int margin = AndroidUtilities.dp(12);
        params.setMargins(margin, margin, margin, 0);
        page.addView(list, params);
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, page,
                ForkLocale.get(R.string.ForkMenuEditorTitle), this::finishFragment);
        return fragmentView;
    }

    private void showPreview(Context context) {
        List<Integer> options = new ArrayList<>();
        for (String id : ActionMenuSettings.editableIds()) options.add(ActionRegistry.nativeOptions(id)[0]);
        ActionMenuPolicy.Plan plan = ActionMenuSettings.plan(currentAccount, options);
        StringBuilder text = new StringBuilder(ForkLocale.get(R.string.ForkMenuEditorPreviewInfo));
        text.append("\n\n").append(ForkLocale.get(R.string.ForkMenuEditorMain));
        appendPreview(text, options, plan.mainIndices);
        showDialog(new AlertDialog.Builder(context).setTitle(ForkLocale.get(R.string.ForkMenuEditorPreview))
                .setMessage(text).setPositiveButton(ForkLocale.get(R.string.OK), null).create());
    }
    private void appendPreview(StringBuilder text, List<Integer> options, List<Integer> indices) {
        if (indices.isEmpty()) text.append("\n—");
        for (int index : indices) text.append("\n• ").append(ForkActionLabels.label(ActionRegistry.forNativeOption(options.get(index))));
    }
    private TextView button(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        ForkSettingsLayout.interactive(view);
        return view;
    }
    private final class MenuAdapter extends RecyclerView.Adapter<MenuHolder> {
        @Override public int getItemCount() { return order.size(); }
        @Override public MenuHolder onCreateViewHolder(ViewGroup parent, int type) {
            Context context = parent.getContext();
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextCheckCell visible = new TextCheckCell(context);
            row.addView(visible, new LinearLayout.LayoutParams(0, -2, 1));
            ImageView pin = new ImageView(context);
            pin.setScaleType(ImageView.ScaleType.CENTER);
            ForkSettingsLayout.interactive(pin);
            row.addView(pin, new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(50)));
            TextView drag = button(context, "↕");
            drag.setTextSize(24);
            row.addView(drag, new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(50)));
            row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
            return new MenuHolder(row, visible, pin, drag);
        }
        @Override public void onBindViewHolder(MenuHolder holder, int position) {
            String id = order.get(position);
            CharSequence label = ForkActionLabels.label(id);
            holder.visible.setTextAndCheck(label, ActionMenuSettings.visible(currentAccount, id), true);
            holder.visible.setColorfullIcon(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), ForkActionLabels.icon(id));
            holder.visible.setContentDescription(label + ", " + ForkLocale.get(R.string.ForkMenuEditorVisible));
            holder.visible.setOnClickListener(v -> {
                ActionMenuSettings.setVisible(currentAccount, id, !ActionMenuSettings.visible(currentAccount, id));
                holder.visible.setChecked(ActionMenuSettings.visible(currentAccount, id));
            });
            boolean pinned = ActionMenuSettings.pinned(currentAccount, id);
            holder.pin.setImageResource(pinned ? R.drawable.msg_unpin : R.drawable.msg_pin);
            holder.pin.setColorFilter(Theme.getColor(pinned ? Theme.key_windowBackgroundWhiteBlueText : Theme.key_windowBackgroundWhiteGrayIcon));
            holder.pin.setContentDescription(ForkLocale.get(pinned ? R.string.ForkMenuEditorUnpin : R.string.ForkMenuEditorPin) + " " + label);
            holder.pin.setOnClickListener(v -> {
                ActionMenuSettings.setPinned(currentAccount, id, !ActionMenuSettings.pinned(currentAccount, id));
                notifyItemChanged(holder.getAdapterPosition());
            });
            holder.drag.setContentDescription(ForkLocale.get(R.string.ForkMenuEditorReorder) + " " + label);
            holder.drag.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder);
                return false;
            });
            holder.drag.setOnClickListener(v -> showDialog(new AlertDialog.Builder(v.getContext())
                    .setTitle(label).setItems(new CharSequence[]{ForkLocale.get(R.string.ForkMenuEditorUp), ForkLocale.get(R.string.ForkMenuEditorDown)},
                            (dialog, which) -> {
                                int from = holder.getAdapterPosition();
                                if (from == RecyclerView.NO_POSITION) return;
                                ActionMenuSettings.move(currentAccount, from, from + (which == 0 ? -1 : 1));
                                order = ActionMenuSettings.order(currentAccount);
                                notifyDataSetChanged();
                            }).create()));
        }
    }
    private static final class MenuHolder extends RecyclerView.ViewHolder {
        final TextCheckCell visible;
        final ImageView pin;
        final TextView drag;
        MenuHolder(View row, TextCheckCell visible, ImageView pin, TextView drag) {
            super(row); this.visible = visible; this.pin = pin; this.drag = drag;
        }
    }
}
