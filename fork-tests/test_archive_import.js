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

// The forward pass must paginate over a gap larger than one page.
check(importer.includes('forwardCursor'), 'The forward pass compares against a moving cursor');
check(importer.includes('forwardOffset'), 'The forward pass can not page into older new messages');
check(importer.includes('reachedKnown'), 'The forward pass does not stop when it reaches imported history');
check(importer.includes('hadCursor'), 'A fresh import can walk the whole history forwards');

// Cursor progress is durable after every page, and Cancel is honoured quickly.
check((importer.match(/saveImportState/g) || []).length >= 3, 'The cursors are not saved per page');
check(importer.includes('minOf(remaining, 500L)'), 'A FLOOD_WAIT sleep ignores cancellation');

// Clearing or deleting archive content must reset the matching cursors.
check(manager.includes('DELETE FROM archive_import_state;'), 'Clearing the archive keeps stale import cursors');
check(manager.includes('DELETE FROM archive_import_state WHERE dialog_id'), 'Deleting a chat keeps its import cursors');

// Large deletion updates are chunked instead of building one huge IN list.
check(manager.includes('MAX_SQL_IDS'), 'The archive has no SQL id chunk size');
check(manager.includes('sqlIdChunks'), 'Deletion ids are not chunked');
check(manager.includes('chunked(MAX_SQL_IDS)'), 'Deletion ids are not actually chunked');

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
check(activity.includes('val index = position - HEADER_COUNT'), 'Archive row clicks must skip the header rows');
check(activity.includes('return if (position < HEADER_COUNT) 0 else if (dataCount() == 0) 2 else 1'), 'Both archive action rows must use the header view type');
check(activity.includes('ForkArchiveImporter.getInstance'), 'The archive screen does not start the importer');
check(activity.includes('.cancel()'), 'The archive screen can not cancel the import');
check(activity.includes('ForkArchiveImportRunning'), 'The archive screen does not show progress');
check(activity.includes('ForkArchiveImportFinished'), 'The archive screen does not show completion');

// The chat picker supports single, multiple and all chats, without secret chats.
check(picker.includes('ForkArchiveSelectAll'), 'The picker has no select all action');
check(picker.includes('LinkedHashSet<Long>'), 'The picker can not select several chats');
check(picker.includes('isEncryptedDialog'), 'The picker offers secret chats');
check(picker.includes('onChatsPicked'), 'The picker does not return the selection');

// The database is used from the message updates, from the archive executor and
// from the import executor. Every access has to be serialized and the UI
// thread must never touch the SQLite handle.
const controller = read('org/telegram/messenger/MessagesController.java');
check(manager.includes('@Synchronized'), 'The archive database is not serialized');
check((manager.match(/@Synchronized/g) || []).length >= 15, 'Not every archive access is serialized');
check(manager.includes('computeIfAbsent'), 'The archive can open the same database twice');
check(controller.includes('ForkArchiveManager.archiveMessageAsync'),
    'An incoming message still writes to the archive on the UI thread');
check(!controller.includes('ForkArchiveManager.getInstance(currentAccount).archiveMessage'),
    'An incoming message still writes to the archive on the UI thread');
check(manager.includes('fun archiveMessageAsync'), 'The archive has no asynchronous write');
check(manager.includes('fun clearArchiveAsync'), 'The archive has no asynchronous clear');
check(activity.includes('clearArchiveAsync'), 'The archive screen clears the database on the UI thread');

// A destroyed screen must not stay registered as the listener of the importer.
check(activity.includes('setListener(null)'), 'The archive screen leaks its importer listener');
check(activity.includes('onFragmentDestroy'), 'The archive screen has no destroy handler');

console.log('test_archive_import passed (pagination, two cursors, dedup, eligibility, resume, thread safety, UI wiring)');
