package org.telegram.ui;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.text.CustomTextManager;
import org.telegram.messenger.fork.text.CustomTextRegistry;
import org.telegram.messenger.fork.text.CustomTextValidator;
import org.telegram.ui.ActionBar.ActionBar;
import android.widget.ImageView;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.telegram.messenger.fork.text.CustomTextPackValidator;
import org.telegram.messenger.fork.text.LocalAliasPolicy;
import static org.telegram.messenger.LocaleController.getString;

public class ForkCustomTextActivity extends ForkSettingsFragment {
    private LinearLayout rows;
    private LinearLayout controls;
    private EditText search;
    private boolean personalMode;
    private boolean addingPhrase;
    private boolean changedOnly;
    private long selectedDialogId;
    private TextView globalTab;
    private TextView personalTab;
    private ImageView resetButton;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkCustomTextTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1 && onBackPressed(true)) finishFragment();
                else if (id == 1) confirmReset();
            }
        });
        resetButton = new ImageView(context);
        resetButton.setImageResource(R.drawable.msg_reset);
        resetButton.setScaleType(ImageView.ScaleType.CENTER);
        resetButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.MULTIPLY));
        resetButton.setOnClickListener(v -> confirmReset());
        LinearLayout root = ForkSettingsLayout.page(context);
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        globalTab = new TextView(context);
        personalTab = new TextView(context);
        for (TextView tab : new TextView[] { globalTab, personalTab }) {
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setTextSize(15);
            ForkSettingsLayout.interactive(tab);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(48), 1));
        }
        globalTab.setText(ForkLocale.get(R.string.ForkTextGlobalTab));
        personalTab.setText(ForkLocale.get(R.string.ForkTextPersonalTab));
        globalTab.setOnClickListener(v -> { personalMode = false; selectedDialogId = 0; addingPhrase = false; search.setText(""); renderRows(); });
        personalTab.setOnClickListener(v -> { personalMode = true; selectedDialogId = 0; addingPhrase = false; search.setText(""); renderRows(); });
        ForkSettingsLayout.addSection(root, tabs);
        search = new EditText(context);
        search.setSingleLine(true);
        search.setHint(ForkLocale.get(R.string.ForkCustomTextSearch));
        search.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        search.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        int pad = AndroidUtilities.dp(16);
        search.setPadding(pad, 0, pad, 0);
        search.setTextSize(16);
        LinearLayout searchSurface = ForkSettingsLayout.section(context);
        searchSurface.addView(search, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(48)));
        ForkSettingsLayout.addSection(root, searchSurface);
        ScrollView scroll = new ScrollView(context);
        rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, AndroidUtilities.dp(12));
        scroll.addView(rows);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { renderRows(); }
            public void afterTextChanged(Editable e) {}
        });
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, root, ForkLocale.get(R.string.ForkCustomTextTitle), () -> {
            if (onBackPressed(true)) finishFragment();
        }, resetButton);
        renderRows();
        return fragmentView;
    }
    private void renderRows() {
        if (rows == null) return;
        Context context = rows.getContext();
        rows.removeAllViews();
        controls = null;
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        globalTab.setSelected(!personalMode);
        personalTab.setSelected(personalMode);
        boolean canReset = !personalMode || selectedDialogId != 0;
        resetButton.setVisibility(canReset ? View.VISIBLE : View.GONE);
        resetButton.setContentDescription(ForkLocale.get(personalMode ? R.string.ForkTextResetChat : R.string.ForkCustomTextResetAll));
        globalTab.setTextColor(Theme.getColor(personalMode ? Theme.key_windowBackgroundWhiteGrayText : Theme.key_windowBackgroundWhiteBlueHeader));
        personalTab.setTextColor(Theme.getColor(personalMode ? Theme.key_windowBackgroundWhiteBlueHeader : Theme.key_windowBackgroundWhiteGrayText));
        if (personalMode && selectedDialogId == 0) {
            if (query.isEmpty()) {
                TextSettingsCell add = new TextSettingsCell(context);
                add.setText(ForkLocale.get(R.string.ForkTextAddChat), true);
                add.setOnClickListener(v -> choosePersonalChat());
                addControl(add);
            }
            java.util.ArrayList<Long> ids = CustomTextManager.personalDialogs(currentAccount);
            for (long id : ids) {
                String name = personalChatName(id);
                if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) continue;
                TextSettingsCell chat = new TextSettingsCell(context);
                chat.setText(name, true);
                chat.setOnClickListener(v -> { selectedDialogId = id; addingPhrase = false; search.setText(""); renderRows(); });
                addControl(chat);
            }
            if (ids.isEmpty() && query.isEmpty()) {
                TextView empty = new TextView(context);
                empty.setText(ForkLocale.get(R.string.ForkTextEmptyChats));
                empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                empty.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0);
                rows.addView(empty);
            }
            return;
        }
        if (personalMode && query.isEmpty()) {
            TextSettingsCell title = new TextSettingsCell(context);
            title.setText(personalChatName(selectedDialogId), true);
            title.setOnClickListener(v -> { selectedDialogId = 0; addingPhrase = false; renderRows(); });
            addControl(title);
            TextSettingsCell add = new TextSettingsCell(context);
            add.setText(ForkLocale.get(R.string.ForkTextAddPhrase), true);
            add.setOnClickListener(v -> { addingPhrase = true; renderRows(); search.requestFocus(); AndroidUtilities.showKeyboard(search); });
            addControl(add);
        }
        if (!personalMode && query.isEmpty()) {
            TextSettingsCell filter = new TextSettingsCell(context);
            filter.setText(ForkLocale.get(changedOnly ? R.string.ForkTextShowAll : R.string.ForkTextChangedOnly), true);
            filter.setOnClickListener(v -> { changedOnly = !changedOnly; renderRows(); });
            addControl(filter);
            controls = null;
            TextSettingsCell activePack = new TextSettingsCell(context);
            String packId = CustomTextManager.activePack();
            String packName = "custom".equals(packId) ? ForkLocale.get(R.string.ForkTextDefaultPack) : CustomTextManager.packName(packId);
            activePack.setTextAndValue(ForkLocale.get(R.string.ForkTextPacks), packName, true);
            activePack.setOnClickListener(this::selectPack);
            addControl(activePack);
            TextSettingsCell newPack = new TextSettingsCell(context);
            newPack.setText(ForkLocale.get(R.string.ForkTextNewPack), true);
            newPack.setOnClickListener(v -> createPack());
            addControl(newPack);
            if (!"custom".equals(packId)) {
                TextSettingsCell deletePack = new TextSettingsCell(context);
                deletePack.setText(ForkLocale.get(R.string.ForkTextDeletePack), true);
                deletePack.setOnClickListener(v -> new AlertDialog.Builder(getParentActivity())
                    .setMessage(ForkLocale.get(R.string.ForkTextDeletePackConfirm))
                    .setPositiveButton(getString(R.string.Delete), (d, w) -> { CustomTextManager.deletePack(packId); changed(); })
                    .setNegativeButton(getString(R.string.Cancel), null).show());
                addControl(deletePack);
            }
            TextSettingsCell pack = new TextSettingsCell(context);
            pack.setText(ForkLocale.get(CustomTextManager.isEnabled() ? R.string.ForkTextUseStandard : R.string.ForkTextUseCustom), true);
            pack.setOnClickListener(v -> { CustomTextManager.setEnabled(!CustomTextManager.isEnabled()); changed(); });
            addControl(pack);
            TextSettingsCell importCell = new TextSettingsCell(context);
            importCell.setText(ForkLocale.get(R.string.ForkTextImport), true);
            importCell.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i, 701);
            });
            addControl(importCell);
            TextSettingsCell exportCell = new TextSettingsCell(context);
            exportCell.setText(ForkLocale.get(R.string.ForkTextExport), true);
            exportCell.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "fork-phrases.json");
                startActivityForResult(i, 702);
            });
            addControl(exportCell);
            controls = null;
            TextSettingsCell aliases = new TextSettingsCell(context);
            aliases.setText(ForkLocale.get(R.string.ForkTextAliases), true);
            aliases.setOnClickListener(v -> chooseAliasDialog());
            addControl(aliases);
        }
        int activeCount = 0;
        if (CustomTextManager.isEnabled()) {
            for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) {
                if (personalMode && !entry.status) continue;
                String value = personalMode ? CustomTextManager.getPersonal(currentAccount, selectedDialogId, entry.key) : CustomTextManager.get(entry.key);
                String original = LocaleController.getOriginalForkString(entry.key, entry.fallback, entry.resource);
                if (value != null && !value.equals(original) && CustomTextValidator.valid(original, value)) activeCount++;
            }
        }
        TextView counter = new TextView(context);
        counter.setText(ForkLocale.get(R.string.ForkTextActiveCount, activeCount));
        counter.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        counter.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), 0);
        rows.addView(counter);
        int visiblePhrases = 0;
        for (CustomTextRegistry.Category category : CustomTextRegistry.Category.values()) {
            boolean header = false;
            LinearLayout categoryRows = null;
            for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) {
                if (entry.category != category || (personalMode && !entry.status)) continue;
                String original = LocaleController.getOriginalForkString(entry.key, entry.fallback, entry.resource);
                if (original == null) continue;
                String custom = personalMode ? CustomTextManager.getPersonal(currentAccount, selectedDialogId, entry.key) : CustomTextManager.get(entry.key);
                if (personalMode && !addingPhrase && custom == null) continue;
                if (!personalMode && changedOnly && (custom == null || custom.equals(original))) continue;
                if (!query.isEmpty() && !(entry.key + " " + original + " " + (custom == null ? "" : custom)).toLowerCase(Locale.ROOT).contains(query)) continue;
                if (!header) {
                    TextView title = new TextView(context);
                    title.setText(categoryTitle(context, category));
                    title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                    title.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), 0, AndroidUtilities.dp(8));
                    title.setTextSize(14);
                    rows.addView(title);
                    categoryRows = ForkSettingsLayout.section(context);
                    ForkSettingsLayout.addSection(rows, categoryRows);
                    header = true;
                }
                LinearLayout cell = new LinearLayout(context);
                cell.setOrientation(LinearLayout.VERTICAL);
                int horizontal = AndroidUtilities.dp(16);
                cell.setPadding(horizontal, AndroidUtilities.dp(12), horizontal, AndroidUtilities.dp(12));
                TextView current = new TextView(context);
                current.setTextSize(16);
                current.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                current.setText(custom == null ? original : custom);
                cell.addView(current);
                TextView source = new TextView(context);
                source.setTextSize(13);
                source.setPadding(0, AndroidUtilities.dp(4), 0, 0);
                boolean modified = custom != null && !custom.equals(original);
                source.setTextColor(Theme.getColor(modified ? Theme.key_windowBackgroundWhiteBlueHeader : Theme.key_windowBackgroundWhiteGrayText));
                source.setText((modified ? ForkLocale.get(R.string.ForkCustomTextChanged) + " · " : "")
                        + ForkLocale.get(R.string.ForkCustomTextOriginal) + ": " + original);
                cell.addView(source);
                cell.setFocusable(true);
                ForkSettingsLayout.interactive(cell);
                cell.setOnClickListener(v -> edit(entry, original));
                categoryRows.addView(cell);
                visiblePhrases++;
            }
            if (header && query.isEmpty()) {
                TextSettingsCell resetCategory = new TextSettingsCell(context);
                resetCategory.setText(ForkLocale.get(R.string.ForkTextResetCategory), false);
                resetCategory.setOnClickListener(v -> new AlertDialog.Builder(getParentActivity())
                    .setMessage(ForkLocale.get(R.string.ForkTextResetCategoryConfirm))
                    .setPositiveButton(getString(R.string.Reset), (d, w) -> { resetWithUndo(personalMode ? selectedDialogId : 0, category, null); })
                    .setNegativeButton(getString(R.string.Cancel), null).show());
                ForkSettingsLayout.interactive(resetCategory);
                categoryRows.addView(resetCategory);
            }
        }
        if (visiblePhrases == 0) {
            TextView empty = new TextView(context);
            empty.setText(getString(R.string.NoResult));
            empty.setTextSize(15);
            empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, AndroidUtilities.dp(24), 0, AndroidUtilities.dp(24));
            rows.addView(empty);
        }
        if (query.isEmpty()) {
            TextSettingsCell reset = new TextSettingsCell(context);
            reset.setText(ForkLocale.get(personalMode ? R.string.ForkTextResetChat : R.string.ForkCustomTextResetAll), false);
            reset.setOnClickListener(v -> confirmReset());
            ForkSettingsLayout.interactive(reset);
            ForkSettingsLayout.addCard(rows, reset);
        }
    }
    private void addControl(View cell) {
        if (controls == null) {
            controls = ForkSettingsLayout.section(rows.getContext());
            ForkSettingsLayout.addSection(rows, controls);
        }
        ForkSettingsLayout.interactive(cell);
        controls.addView(cell);
    }

    private void focusInput(AlertDialog dialog, EditText input) {
        input.requestFocus();
        input.setSelection(input.length());
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            if (dialog.isShowing()) AndroidUtilities.showKeyboard(input);
        }, 100));
    }

    private void confirmReset() {
        if (getParentActivity() == null || personalMode && selectedDialogId == 0) return;
        final long dialogId = personalMode ? selectedDialogId : 0;
        showDialog(new AlertDialog.Builder(getParentActivity())
            .setMessage(ForkLocale.get(personalMode ? R.string.ForkTextResetChatConfirm : R.string.ForkCustomTextResetConfirm))
            .setPositiveButton(getString(R.string.Reset), (d, w) -> resetWithUndo(dialogId, null, null))
            .setNegativeButton(getString(R.string.Cancel), null).create());
    }
    private String categoryTitle(Context context, CustomTextRegistry.Category category) {
        switch (category) {
            case ONLINE: return ForkLocale.get(R.string.ForkTextCategoryOnline);
            case LAST_SEEN: return ForkLocale.get(R.string.ForkTextCategoryLastSeen);
            case TYPING: return ForkLocale.get(R.string.ForkTextCategoryTyping);
            case RECORDING: return ForkLocale.get(R.string.ForkTextCategoryRecording);
            case SENDING: return ForkLocale.get(R.string.ForkTextCategorySending);
            case MEDIA: return ForkLocale.get(R.string.ForkTextCategoryMedia);
            default: return ForkLocale.get(R.string.ForkTextCategoryNavigation);
        }
    }
    private void edit(CustomTextRegistry.Entry entry, String original) {
        Context context = getParentActivity();
        if (context == null) return;
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        content.setPadding(pad, 0, pad, 0);
        TextView originalView = new TextView(context);
        originalView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        originalView.setText(ForkLocale.get(R.string.ForkCustomTextOriginal) + ": " + original);
        content.addView(originalView);
        if (entry.status) {
            TextView hint = new TextView(context);
            hint.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            hint.setText(ForkLocale.get(R.string.ForkTextActualHint));
            content.addView(hint);
        }
        EditTextBoldCursor input = new EditTextBoldCursor(context);
        input.setTextSize(16);
        input.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        input.setCursorSize(AndroidUtilities.dp(20));
        input.setCursorWidth(1.5f);
        input.setSingleLine(false);
        input.setHint(ForkLocale.get(R.string.ForkCustomTextHint));
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        final long editDialogId = personalMode ? selectedDialogId : 0;
        input.setText(editDialogId == 0 ? CustomTextManager.get(entry.key) : CustomTextManager.getPersonal(currentAccount, editDialogId, entry.key));
        content.addView(input);
        TextView preview = new TextView(context);
        preview.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        content.addView(preview);
        Runnable update = () -> preview.setText(ForkLocale.get(R.string.ForkCustomTextPreview) + ": " + input.getText());
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
            public void afterTextChanged(Editable e) {}
        });
        update.run();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(original);
        builder.setView(content);
        builder.setPositiveButton(getString(R.string.Save), null);
        builder.setNegativeButton(getString(R.string.Cancel), null);
        builder.setNeutralButton(ForkLocale.get(R.string.ForkCustomTextReset), (d, w) -> resetWithUndo(editDialogId, null, entry.key));
        AlertDialog dialog = builder.create();
        focusInput(dialog, input);
        if (showDialog(dialog) != null) {
            View save = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (save != null) save.setOnClickListener(v -> {
                String value = input.getText().toString();
                if (!CustomTextValidator.valid(original, value)) {
                    input.setError(ForkLocale.get(R.string.ForkCustomTextInvalid));
                    return;
                }
                if (editDialogId == 0) CustomTextManager.set(entry.key, value);
                else { CustomTextManager.setPersonal(currentAccount, editDialogId, entry.key, value); addingPhrase = false; }
                dialog.dismiss();
                changed();
            });
        }
    }
    @Override public boolean onBackPressed(boolean invoked) {
        if (personalMode && selectedDialogId != 0) {
            if (invoked) {
                if (addingPhrase) addingPhrase = false;
                else selectedDialogId = 0;
                search.setText("");
                renderRows();
            }
            return false;
        }
        return super.onBackPressed(invoked);
    }
    private String personalChatName(long dialogId) {
        if (dialogId > 0) {
            org.telegram.tgnet.TLRPC.User user = getMessagesController().getUser(dialogId);
            if (user != null) return org.telegram.messenger.UserObject.getUserName(user);
        } else {
            org.telegram.tgnet.TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
            if (chat != null && chat.title != null) return chat.title;
        }
        return ForkLocale.get(R.string.ForkTextUnknownChat);
    }
    private void choosePersonalChat() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        DialogsActivity picker = new DialogsActivity(args);
        picker.setDelegate((fragment, selected, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (selected == null || selected.isEmpty()) return false;
            long id = selected.get(0).dialogId;
            if (id == 0 || id == getUserConfig().getClientUserId() || org.telegram.messenger.DialogObject.isEncryptedDialog(id) || org.telegram.messenger.DialogObject.isFolderDialogId(id)) return false;
            selectedDialogId = id; addingPhrase = false;
            fragment.finishFragment();
            AndroidUtilities.runOnUIThread(this::renderRows, 250);
            return true;
        });
        presentFragment(picker);
    }
    private void selectPack(View anchor) {
        Context context = getParentActivity();
        if (context == null) return;
        java.util.ArrayList<String> ids = CustomTextManager.packIds();
        CharSequence[] names = new CharSequence[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            names[i] = "custom".equals(id) ? ForkLocale.get(R.string.ForkTextDefaultPack) : CustomTextManager.packName(id);
        }
        ForkChoicePopup.show(this, anchor, names, null, ids.indexOf(CustomTextManager.activePack()), which -> { CustomTextManager.activatePack(ids.get(which)); changed(); });
    }
    private void createPack() {
        Context context = getParentActivity();
        if (context == null) return;
        EditText name = new EditText(context);
        name.setSingleLine(true); name.setHint(ForkLocale.get(R.string.ForkTextPackName));
        name.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        LinearLayout container = new LinearLayout(context);
        int pad = AndroidUtilities.dp(20); container.setPadding(pad, 0, pad, 0); container.addView(name);
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(ForkLocale.get(R.string.ForkTextNewPack))
            .setView(container).setPositiveButton(getString(R.string.Save), null)
            .setNegativeButton(getString(R.string.Cancel), null).create();
        focusInput(dialog, name);
        if (showDialog(dialog) != null) {
            View save = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (save != null) save.setOnClickListener(v -> {
                String value = name.getText().toString().trim();
                if (!LocalAliasPolicy.valid(value) || value.length() > 40) { name.setError(ForkLocale.get(R.string.ForkTextInvalidPackName)); return; }
                CustomTextManager.createPack(value); dialog.dismiss(); changed();
            });
        }
    }
    private void resetWithUndo(long dialogId, CustomTextRegistry.Category category, String onlyKey) {
        final Map<String, String> previous = new HashMap<>();
        final boolean enabled = CustomTextManager.isEnabled();
        final String packId = CustomTextManager.activePack();
        final long ownerId = getUserConfig().getClientUserId();
        for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) {
            if (onlyKey != null && !onlyKey.equals(entry.key) || category != null && entry.category != category) continue;
            String value = dialogId == 0 ? CustomTextManager.get(entry.key) : CustomTextManager.getPersonal(currentAccount, dialogId, entry.key);
            if (value == null) continue;
            previous.put(entry.key, value);
            if (dialogId == 0) CustomTextManager.set(entry.key, null);
            else CustomTextManager.setPersonal(currentAccount, dialogId, entry.key, null);
        }
        changed();
        if (!previous.isEmpty()) AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() == null) return;
            org.telegram.ui.Components.BulletinFactory.of(this).createUndoBulletin(
                ForkLocale.get(R.string.ForkTextResetDone), () -> {
                    if (!packId.equals(CustomTextManager.activePack()) || dialogId != 0 && ownerId != getUserConfig().getClientUserId()) return;
                    for (Map.Entry<String, String> entry : previous.entrySet()) {
                        if (dialogId == 0) CustomTextManager.set(entry.getKey(), entry.getValue());
                        else CustomTextManager.setPersonal(currentAccount, dialogId, entry.getKey(), entry.getValue());
                    }
                    CustomTextManager.setEnabled(enabled);
                    changed();
                }, null).show();
        }, 350);
    }
    private void chooseAliasDialog() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        DialogsActivity picker = new DialogsActivity(args);
        picker.setDelegate((fragment, selected, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (selected == null || selected.isEmpty()) return false;
            long dialogId = selected.get(0).dialogId;
            if (org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId) || org.telegram.messenger.DialogObject.isFolderDialogId(dialogId) || dialogId == 0 || dialogId == getUserConfig().getClientUserId()) return false;
            fragment.finishFragment();
            AndroidUtilities.runOnUIThread(() -> editAlias(dialogId), 250);
            return true;
        });
        presentFragment(picker);
    }
    private void editAlias(long dialogId) {
        Context context = getParentActivity();
        if (context == null) return;
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20); content.setPadding(pad, 0, pad, 0);
        TextView info = new TextView(context);
        info.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        info.setText(ForkLocale.get(R.string.ForkTextAliasInfo));
        content.addView(info);
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHint(ForkLocale.get(R.string.ForkTextAliasHint));
        input.setText(CustomTextManager.alias(currentAccount, dialogId));
        content.addView(input);
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(ForkLocale.get(R.string.ForkTextAliases)); b.setView(content);
        b.setPositiveButton(getString(R.string.Save), null);
        b.setNegativeButton(getString(R.string.Cancel), null);
        b.setNeutralButton(ForkLocale.get(R.string.ForkCustomTextReset), (d, w) -> { CustomTextManager.setAlias(currentAccount, dialogId, null); changed(); });
        AlertDialog dialog = b.create();
        focusInput(dialog, input);
        if (showDialog(dialog) != null) {
            View save = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (save != null) save.setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (!LocalAliasPolicy.valid(value)) {
                    input.setError(ForkLocale.get(R.string.ForkTextAliasInvalid));
                    return;
                }
                CustomTextManager.setAlias(currentAccount, dialogId, value);
                dialog.dismiss();
                changed();
            });
        }
    }

    @Override public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null || getParentActivity() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == 701) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = getParentActivity().getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IllegalArgumentException();
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        if (bytes.size() + n > 65536) throw new IllegalArgumentException();
                        bytes.write(buffer, 0, n);
                    }
                }
                String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString();
                JSONObject json = new JSONObject(decoded);
                if (json.getInt("version") != 1 || json.length() != 2) throw new IllegalArgumentException();
                JSONObject phrases = json.getJSONObject("phrases");
                Map<String, String> originals = new HashMap<>(), values = new HashMap<>();
                for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) originals.put(entry.key, LocaleController.getOriginalForkString(entry.key, entry.fallback, entry.resource));
                Iterator<String> keys = phrases.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); Object value = phrases.get(key);
                    if (!(value instanceof String)) throw new IllegalArgumentException();
                    values.put(key, (String) value);
                }
                if (!CustomTextPackValidator.valid(originals, values)) throw new IllegalArgumentException();
                // Import into a new pack so an existing pack is never silently replaced.
                CustomTextManager.createPack(ForkLocale.get(R.string.ForkTextImportedPack));
                CustomTextManager.replaceAll(values);
                changed();
            } else if (requestCode == 702) {
                JSONObject phrases = new JSONObject();
                for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) {
                    String value = CustomTextManager.get(entry.key);
                    if (value != null) phrases.put(entry.key, value);
                }
                JSONObject json = new JSONObject(); json.put("version", 1); json.put("phrases", phrases);
                try (OutputStream out = getParentActivity().getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new IllegalArgumentException();
                    out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            showDialog(new AlertDialog.Builder(getParentActivity()).setMessage(ForkLocale.get(R.string.ForkTextPackError)).setPositiveButton(getString(R.string.OK), null).create());
        }
    }

    private void changed() {
        renderRows();
        for (int account = 0; account < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (org.telegram.messenger.UserConfig.getInstance(account).isClientActivated()) {
                org.telegram.messenger.MessagesController.getInstance(account).refreshForkCustomText();
            }
        }
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface), 150);
    }
}
