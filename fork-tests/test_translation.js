const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..');
const javaRoot = path.join(root, 'TMessagesProj/src/main/java');

function read(file) {
    return fs.readFileSync(path.join(javaRoot, file), 'utf8');
}

function check(condition, message) {
    if (!condition) throw new Error(message);
}

const settings = read('org/telegram/messenger/fork/text/ForkTranslationSettings.kt');
const policy = read('org/telegram/messenger/fork/text/ForkTranslationPolicy.java');
const activity = read('org/telegram/ui/ForkTranslationSettingsActivity.kt');
const chat = read('org/telegram/ui/ChatActivity.java');

// Translation must reuse Telegram's own controller, never an external provider.
check(settings.includes('TranslateController'), 'The settings do not use Telegram translation');
check(settings.includes('setDialogTranslateTo'), 'The target language is not stored in Telegram');
check(settings.includes('toggleTranslatingDialog'), 'Automatic translation is not wired to Telegram');
check(!/okhttp|api\.deepl|translate\.googleapis|yandex/i.test(settings), 'An external provider is used');

// Secret chats and Saved Messages must be excluded twice: by the caller and by the policy.
check(settings.includes('DialogObject.isEncryptedDialog'), 'Encrypted dialogs are not excluded');
check(settings.includes('clientUserId == dialogId'), 'Saved Messages are not excluded');
check(settings.includes('applyOnChatOpen'), 'There is no chat open hook');
check(settings.includes('@JvmStatic'), 'Java can not call the translation helper');
check(policy.includes('isEligible'), 'The policy has no eligibility rule');
check(policy.includes('sanitizeLanguage'), 'The policy does not normalise the language');
check(policy.includes('shouldSetTarget'), 'The policy can apply an empty language');
check(policy.includes('shouldTranslate'), 'The policy has no auto translation rule');

// The empty target language must never be written to the controller.
check(settings.includes('shouldSetTarget(language)'), 'An empty language can reach Telegram');
check(settings.includes('shouldTranslate(eligible, settings.autoTranslateEnabled)'),
    'Auto translation is not gated by the policy');

// The chat activity must invoke the hook once per open.
check(chat.includes('ForkTranslationSettings.applyOnChatOpen('),
    'ChatActivity does not apply the translation preferences');
check(chat.includes('encId != 0 || DialogObject.isEncryptedDialog(dialog_id)'),
    'ChatActivity does not treat secret chats as ineligible');

// The screen shows the real language name, not a technical code.
check(activity.includes('targetLanguageName()'), 'The screen does not show the language name');
check(settings.includes('fun targetLanguageName()'), 'The settings have no language name helper');
check(!activity.includes('targetLanguageLabel()'), 'The screen still uses the placeholder label');

console.log('test_translation passed (Telegram provider, eligibility, empty language, chat hook, language name)');
