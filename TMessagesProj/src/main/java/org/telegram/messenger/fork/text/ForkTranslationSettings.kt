package org.telegram.messenger.fork.text

import android.content.Context
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.TranslateController

/**
 * Berrygram translation preferences.
 *
 * The configuration only uses Telegram's own translation infrastructure: the
 * supported languages come from [TranslateController.getLanguages] and the
 * automatic mode reuses the per dialog translation state that Telegram already
 * maintains. No external translation service is used.
 */
class ForkTranslationSettings(private val account: Int) {

    private val preferences =
        ApplicationLoader.applicationContext.getSharedPreferences("fork_translation_$account", Context.MODE_PRIVATE)

    /** Automatically translate incoming messages of the chats where it is enabled. */
    var autoTranslateEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) {
            preferences.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()
        }

    /** Target language code, empty means that Telegram detects the language. */
    var targetLanguage: String
        get() = preferences.getString(KEY_TARGET_LANGUAGE, "") ?: ""
        set(value) {
            preferences.edit().putString(KEY_TARGET_LANGUAGE, value ?: "").apply()
        }

    /** The languages the existing translation provider actually supports. */
    fun supportedLanguages(): List<Pair<String, String>> {
        val result = ArrayList<Pair<String, String>>()
        try {
            val languages = TranslateController.getLanguages()
            for (language in languages) {
                if (language == null || language.code == null) continue
                val name = language.ownDisplayName ?: language.displayName ?: continue
                result.add(Pair(language.code, name))
            }
        } catch (error: Throwable) {
            // The translation service is not available, the screen stays empty
            // instead of offering languages that can not work.
        }
        return result
    }

    /** Resource identifier of the label that describes the current target language. */
    fun targetLanguageLabel(): Int {
        val code = targetLanguage
        if (code.isEmpty()) {
            return org.telegram.messenger.R.string.ForkTranslationAutomatic
        }
        for (language in supportedLanguages()) {
            if (language.first == code) {
                return org.telegram.messenger.R.string.ForkTranslationTargetSelected
            }
        }
        return org.telegram.messenger.R.string.ForkTranslationAutomatic
    }

    /** Applies the configured target language to a chat. */
    fun applyToDialog(dialogId: Long): Boolean {
        return try {
            val controller = MessagesController.getInstance(account).translateController
            controller.setDialogTranslateTo(dialogId, targetLanguage)
            true
        } catch (error: Throwable) {
            false
        }
    }

    fun save() {
        preferences.edit().apply()
    }

    fun reset() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_TARGET_LANGUAGE = "target_language"
    }
}
