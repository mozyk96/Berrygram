package org.telegram.messenger.fork.text;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/** Local, language-independent overrides; never alters server or message data. */
public final class CustomTextManager {
    private static final String PREFS = "fork_custom_text";
    private CustomTextManager() {}
    private static SharedPreferences prefs() { return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static String activePack() {
        String id = prefs().getString("__active_pack", "custom");
        if (!CustomTextPackKeys.validId(id) || (!"custom".equals(id) && !prefs().contains("__pack_name:" + id))) return "custom";
        return id;
    }
    public static String get(String key) { return prefs().getString(CustomTextPackKeys.key(activePack(), key), null); }
    public static ArrayList<String> packIds() {
        ArrayList<String> ids = new ArrayList<>();
        for (String key : prefs().getAll().keySet()) {
            if (key.startsWith("__pack_name:")) {
                String id = key.substring("__pack_name:".length());
                if (CustomTextPackKeys.validId(id) && !"custom".equals(id)) ids.add(id);
            }
        }
        Collections.sort(ids);
        ids.add(0, "custom");
        return ids;
    }
    public static String packName(String id) {
        return "custom".equals(id) ? null : prefs().getString("__pack_name:" + id, null);
    }
    public static void activatePack(String id) {
        if (!CustomTextPackKeys.validId(id) || (!"custom".equals(id) && !prefs().contains("__pack_name:" + id))) throw new IllegalArgumentException("Unknown pack");
        prefs().edit().putString("__active_pack", id).putBoolean("__enabled", true).apply();
    }
    public static String createPack(String name) {
        if (!LocalAliasPolicy.valid(name) || name.length() > 40) throw new IllegalArgumentException("Invalid pack name");
        String id = "pack-" + java.util.UUID.randomUUID().toString();
        prefs().edit().putString("__pack_name:" + id, name).putString("__active_pack", id).putBoolean("__enabled", true).apply();
        return id;
    }
    public static void deletePack(String id) {
        if (!CustomTextPackKeys.validId(id) || "custom".equals(id) || !prefs().contains("__pack_name:" + id)) throw new IllegalArgumentException("Unknown pack");
        SharedPreferences.Editor editor = prefs().edit().remove("__pack_name:" + id);
        String prefix = "pack:" + id + ":";
        for (String key : prefs().getAll().keySet()) if (key.startsWith(prefix)) editor.remove(key);
        if (id.equals(activePack())) editor.putString("__active_pack", "custom");
        editor.apply();
    }
    public static boolean isEnabled() { return prefs().getBoolean("__enabled", true); }
    public static void setEnabled(boolean enabled) { prefs().edit().putBoolean("__enabled", enabled).apply(); }
    public static void resetCategory(boolean status) {
        SharedPreferences.Editor editor = prefs().edit();
        for (CustomTextRegistry.Entry e : CustomTextRegistry.ENTRIES) if (e.status == status) editor.remove(CustomTextPackKeys.key(activePack(), e.key));
        editor.apply();
    }
    public static void replaceAll(Map<String, String> values) {
        SharedPreferences.Editor editor = prefs().edit().putBoolean("__enabled", true);
        String pack = activePack();
        for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) editor.remove(CustomTextPackKeys.key(pack, entry.key));
        for (Map.Entry<String, String> e : values.entrySet()) editor.putString(CustomTextPackKeys.key(pack, e.getKey()), e.getValue());
        editor.apply();
    }
    public static void set(String key, String value) {
        if (CustomTextRegistry.find(key, 0) == null) throw new IllegalArgumentException("Unknown key");
        String storageKey = CustomTextPackKeys.key(activePack(), key);
        if (value == null) prefs().edit().remove(storageKey).apply(); else prefs().edit().putString(storageKey, value).putBoolean("__enabled", true).apply();
    }
    public static void resetAll() {
        SharedPreferences.Editor editor = prefs().edit();
        for (CustomTextRegistry.Entry e : CustomTextRegistry.ENTRIES) editor.remove(CustomTextPackKeys.key(activePack(), e.key));
        editor.apply();
    }
    /** Per-dialog values are keyed by the stable Telegram account and peer IDs. */
    public static String getPersonal(int account, long dialogId, String resourceKey) {
        CustomTextRegistry.Entry entry = CustomTextRegistry.find(resourceKey, 0);
        long userId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (entry == null || userId <= 0 || dialogId == 0 || org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId) || org.telegram.messenger.DialogObject.isFolderDialogId(dialogId)) return null;
        return prefs().getString(CustomTextResolution.key(userId, dialogId, entry.key), null);
    }
    public static void setPersonal(int account, long dialogId, String resourceKey, String value) {
        CustomTextRegistry.Entry entry = CustomTextRegistry.find(resourceKey, 0);
        long userId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (entry == null || userId <= 0 || dialogId == 0 || org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId) || org.telegram.messenger.DialogObject.isFolderDialogId(dialogId)) {
            throw new IllegalArgumentException("Invalid personal text scope");
        }
        String key = CustomTextResolution.key(userId, dialogId, entry.key);
        if (value == null) {
            prefs().edit().remove(key).apply();
        } else {
            String original = org.telegram.messenger.LocaleController.getOriginalForkString(entry.key, entry.fallback, entry.resource);
            if (!CustomTextValidator.valid(original, value)) throw new IllegalArgumentException("Invalid personal text template");
            prefs().edit().putString(key, value).putBoolean("__enabled", true).apply();
        }
    }
    public static ArrayList<Long> personalDialogs(int account) {
        long userId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        ArrayList<Long> result = new ArrayList<>();
        if (userId <= 0) return result;
        String prefix = "chat:" + userId + ":";
        HashSet<Long> ids = new HashSet<>();
        for (String key : prefs().getAll().keySet()) {
            if (!key.startsWith(prefix)) continue;
            int end = key.indexOf(':', prefix.length());
            if (end < 0) continue;
            try {
                long id = Long.parseLong(key.substring(prefix.length(), end));
                if (CustomTextRegistry.find(key.substring(end + 1), 0) != null) ids.add(id);
            } catch (NumberFormatException ignored) { }
        }
        result.addAll(ids);
        Collections.sort(result);
        return result;
    }
    public static void resetPersonal(int account, long dialogId, Boolean status) {
        long userId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (userId <= 0 || dialogId == 0) return;
        SharedPreferences.Editor editor = prefs().edit();
        for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) {
            if (status == null || entry.status == status) editor.remove(CustomTextResolution.key(userId, dialogId, entry.key));
        }
        editor.apply();
    }
    public static String alias(int account, long dialogId) {
        long userId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (userId == 0 || dialogId == 0 || org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId) || org.telegram.messenger.DialogObject.isFolderDialogId(dialogId)) return null;
        return prefs().getString(LocalAliasPolicy.key(userId, dialogId), null);
    }
    public static void setAlias(int account, long dialogId, String value) {
        long userId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (userId == 0 || dialogId == 0 || org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId) || org.telegram.messenger.DialogObject.isFolderDialogId(dialogId)) return;
        String key = LocalAliasPolicy.key(userId, dialogId);
        if (value == null) prefs().edit().remove(key).apply();
        else if (LocalAliasPolicy.valid(value)) prefs().edit().putString(key, value).apply();
    }
    public static String aliasDisplay(int account, long dialogId, String original) {
        return LocalAliasPolicy.display(original, alias(account, dialogId));
    }
    public static String resolve(String key, int res, String original) {
        if (original == null || ApplicationLoader.applicationContext == null) return original;
        CustomTextRegistry.Entry entry = CustomTextRegistry.find(key, res);
        if (entry == null || !isEnabled()) return original;
        CustomTextContext.Value context = CustomTextContext.current();
        if (!CustomTextScopePolicy.allowed(entry.status, context != null)) return original;
        String personal = context == null ? null : getPersonal(context.account, context.dialogId, entry.key);
        String value = CustomTextResolution.select(original, get(entry.key), personal);
        if (original.equals(value)) return original;
        if (entry.status) CustomTextContext.recordOriginal(original);
        // The original status is recorded in the rendering context for accessibility and the status detail view.
        return value;
    }
}
