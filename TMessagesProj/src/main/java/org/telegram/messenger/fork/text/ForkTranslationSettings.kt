package org.telegram.messenger.fork.text

import android.content.Context
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.TranslateController
import org.telegram.messenger.UserConfig
import org.telegram.ui.ForkLocale

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

    /**
     * Human readable name of the configured target language, so the screen
     * shows "Ukrainian" instead of the technical language code.
     */
    fun targetLanguageName(): String {
        val code = targetLanguage
        if (code.isEmpty()) {
            return ForkLocale.get(R.string.ForkTranslationAutomatic)
        }
        for (language in supportedLanguages()) {
            if (language.first == code) {
                return language.second
            }
        }
        return code
    }

    /** Applies the configured target language to a chat. */
    fun applyToDialog(dialogId: Long): Boolean {
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return false
        }
        val language = ForkTranslationPolicy.sanitizeLanguage(targetLanguage)
        if (!ForkTranslationPolicy.shouldSetTarget(language)) {
            return false
        }
        return try {
            MessagesController.getInstance(account).translateController
                .setDialogTranslateTo(dialogId, language)
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

        @JvmStatic
        fun autoTranslateEnabled(account: Int): Boolean {
            return ForkTranslationSettings(account).autoTranslateEnabled
        }

        @JvmStatic
        fun configuredTargetLanguage(account: Int): String {
            return ForkTranslationSettings(account).targetLanguage
        }

        /**
         * Applies the Berrygram translation preferences when a chat is opened.
         *
         * <p>The configured target language is stored in Telegram's own
         * [TranslateController], so the translation request is performed by the
         * same service the user already uses. Automatic translation is only
         * switched on when the option is enabled, and it never touches secret
         * chats, Saved Messages or dialogs Telegram considers encrypted.</p>
         *
         * @return true when a preference was actually applied.
         */
        @JvmStatic
        fun applyOnChatOpen(account: Int, dialogId: Long, isSecretChat: Boolean): Boolean {
            val settings = ForkTranslationSettings(account)
            val self = try {
                UserConfig.getInstance(account).clientUserId == dialogId
            } catch (error: Throwable) {
                false
            }
            val eligible = ForkTranslationPolicy.isEligible(
                isSecretChat,
                DialogObject.isEncryptedDialog(dialogId),
                self
            )
            if (!eligible) {
                return false
            }
            val language = ForkTranslationPolicy.sanitizeLanguage(settings.targetLanguage)
            var applied = false
            try {
                val controller = MessagesController.getInstance(account).translateController
                if (ForkTranslationPolicy.shouldSetTarget(language)) {
                    controller.setDialogTranslateTo(dialogId, language)
                    applied = true
                }
                if (ForkTranslationPolicy.shouldTranslate(eligible, settings.autoTranslateEnabled)) {
                    controller.toggleTranslatingDialog(dialogId, true)
                    applied = true
                }
            } catch (error: Throwable) {
                return false
            }
            return applied
        }
    }
}
