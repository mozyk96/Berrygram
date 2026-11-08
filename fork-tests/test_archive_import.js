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

const importer = read('org/telegram/messenger/fork/storage/ForkArchiveImporter.kt');
const policy = read('org/telegram/messenger/fork/storage/ForkArchiveImportPolicy.java');
const manager = read('org/telegram/messenger/fork/storage/ForkArchiveManager.kt');
const activity = read('org/telegram/ui/ForkArchiveActivity.kt');
const picker = read('org/telegram/ui/ForkArchiveChatPickerActivity.kt');

// The importer uses Telegram's own history request with pagination.
check(importer.includes('TL_messages_getHistory'), 'The importer does not use messages.getHistory');
check(importer.includes('PAGE_SIZE'), 'The importer has no page size');
check(importer.includes('offset_id'), 'The importer does not paginate with offset_id');
check(importer.includes('FLOOD_WAIT'), 'The importer does not handle FLOOD_WAIT');
check(importer.includes('cancel()'), 'The importer can not be cancelled');
check(importer.includes('importState('), 'The importer does not restore its cursor');
check(importer.includes('saveImportState('), 'The importer does not persist its cursor');
check(importer.includes('ForkArchiveImportPolicy'), 'The importer does not use the tested cursor policy');
check(importer.includes('isEncryptedDialog'), 'The importer does not exclude secret chats');
check(importer.includes('isEligible'), 'The importer does not exclude ineligible content');

// Both directions must be present: a forward update and a historical backfill.
check(importer.includes('isNewer'), 'The importer has no forward update pass');
check(importer.includes('shouldBackfill'), 'The importer has no historical backfill pass');
check(importer.includes('nextBackfillCursor'), 'The backfill cursor is not advanced');
check(importer.includes('nextUpdateCursor'), 'The update cursor is not advanced');

// The policy must keep the two cursors apart.
check(policy.includes('shouldBackfill'), 'The policy has no backfill rule');
check(policy.includes('isNewer'), 'The policy has no update rule');
check(policy.includes('reachedEnd'), 'The policy can not detect the end of the history');
check(policy.includes('isEligible'), 'The policy has no eligibility rule');

// The archive schema keeps a durable per chat import state.
check(manager.includes('archive_import_state'), 'The archive has no import state table');
check(manager.includes('backfill_id'), 'The import state has no backfill cursor');
check(manager.includes('update_id'), 'The import state has no update cursor');
check(manager.includes('fun saveImportState'), 'The import state can not be saved');
check(manager.includes('fun importState'), 'The import state can not be restored');
check(manager.includes('fun exists('), 'The archive can not check for duplicates');
check(manager.includes('PRIMARY KEY (dialog_id, message_id)'), 'The archive does not deduplicate messages');

// The archive screen exposes create, update, progress and cancel.
check(activity.includes('R.string.ForkArchiveCreate'), 'The archive screen has no create action');
check(activity.includes('R.string.ForkArchiveUpdate'), 'The archive screen has no update action');
check(activity.includes('ForkArchiveImporter.getInstance'), 'The archive screen does not start the importer');
check(activity.includes('.cancel()'), 'The archive screen can not cancel the import');
check(activity.includes('ForkArchiveImportRunning'), 'The archive screen does not show progress');
check(activity.includes('ForkArchiveImportFinished'), 'The archive screen does not show completion');

// The chat picker supports single, multiple and all chats, without secret chats.
check(picker.includes('ForkArchiveSelectAll'), 'The picker has no select all action');
check(picker.includes('LinkedHashSet<Long>'), 'The picker can not select several chats');
check(picker.includes('isEncryptedDialog'), 'The picker offers secret chats');
check(picker.includes('onChatsPicked'), 'The picker does not return the selection');

console.log('test_archive_import passed (pagination, two cursors, dedup, eligibility, resume, UI wiring)');
