from pathlib import Path
import re, sqlite3
root=Path(__file__).resolve().parent.parent
source=(root/'TMessagesProj/src/main/java/org/telegram/messenger/fork/storage/ForkArchiveManager.kt').read_text(encoding='utf-8')
ui=(root/'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java').read_text(encoding='utf-8')
def sql(fragment):
 return re.search(r'"('+fragment+r'[^"\n]*)"',source).group(1)
def render(query,dialog=101,mid=7,observed=1000):
 return query.replace('${org.telegram.messenger.UserConfig.getInstance(currentAccount).clientUserId}','100').replace('$owner','100').replace('$dialogId',str(dialog)).replace('$messageId',str(mid)).replace('$observedAt',str(observed)).replace('$chunk',str(mid)).replace('$id',str(mid)).replace('$limit','100')
def database():
 db=sqlite3.connect(':memory:')
 db.execute('CREATE TABLE archived_messages(dialog_id INTEGER,message_id INTEGER,date INTEGER,sender_id INTEGER,sender_name TEXT,message_text TEXT,message_text_enc BLOB,deleted INTEGER,PRIMARY KEY(dialog_id,message_id))')
 db.execute(sql('CREATE TABLE IF NOT EXISTS local_deletions'))
 db.execute(sql('CREATE TABLE IF NOT EXISTS archived_message_state'))
 return db
db=database()
for dialog in (101,-101): db.execute('INSERT INTO archived_messages VALUES(?,?,123,44,NULL,\'\',?,1)',(dialog,7,b'encrypted-fixture'))
body=source.split('fun getDeletedMessages(',1)[1].split('private fun readEntry',1)[0]
query=''.join(re.findall(r'"([^"\n]*)"',body))
assert len(db.execute(render(query)).fetchall())==1
# Execute the production durable-marker statement, then simulate a late archive write.
db.execute(render(sql('INSERT OR IGNORE INTO local_deletions VALUES')))
assert db.execute(render(query)).fetchall()==[], 'late archive result restored a locally deleted record'
assert len(db.execute(render(query,dialog=-101)).fetchall())==1, 'another dialog was suppressed'
other=database();other.execute('INSERT INTO archived_messages VALUES(101,7,123,44,NULL,\'\',?,1)',(b'cipher',))
assert len(other.execute(render(query)).fetchall())==1, 'account storage isolation broken'
# Production metadata update keeps the first observed time and the latest known edit.
db.execute('INSERT INTO archived_message_state(dialog_id,message_id) VALUES(101,7)')
update=sql('UPDATE archived_message_state SET observed_at = CASE')
for observed,edit in [(1000,200),(2000,100)]: db.execute(update,(observed,edit,b'cipher',101,7))
assert db.execute('SELECT observed_at,edit_date FROM archived_message_state WHERE dialog_id=101').fetchone()==(1000,200)
assert db.execute('SELECT date FROM archived_messages WHERE dialog_id=101').fetchone()[0]==123
# Execute both production local record deletions and retain the suppression tombstone.
db.execute(sql('DELETE FROM archived_messages WHERE dialog_id = \?'),(101,7))
db.execute(render(sql('DELETE FROM archived_message_state WHERE dialog_id = \$dialogId')))
assert db.execute('SELECT 1 FROM archived_messages WHERE dialog_id=101').fetchone() is None
assert db.execute('SELECT 1 FROM archived_message_state WHERE dialog_id=101').fetchone() is None
assert db.execute('SELECT 1 FROM local_deletions WHERE dialog_id=101').fetchone() is not None
# Verify the actual integration points, rather than treating the SQLite fixture as an Android test.
assert 'isLocallyDeleted(dialogId, messageId)' in source
process=ui.split('boolean sent, boolean thanos)',1)[1].split('private final BotForumHelper',1)[0]
assert process.index('clearForkDeletedSelection(')<process.index('obj.deleted = true')<process.index('obj.isForkDeleted = true')
assert 'ForkDeletionPolicy.isLocal(currentAccount, dialogId, entry.messageId)' in ui
assert 'messagesDict[0].put(obj.getId(), obj)' in ui
menu=ui.split('private boolean showForkPreservedMenu',1)[1].split('private void processDeletedMessages',1)[0]
assert 'getString(R.string.Delete)' in menu and 'deleteForkPreservedMessage' in menu
assert 'getMessagesController().deleteMessages' not in menu
print('Deletion persistence passed (production SQL, replay suppression, account/dialog isolation, original dates, first observation, local purge; no server/UI claims)')
