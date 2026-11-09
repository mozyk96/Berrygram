# -*- coding: utf-8 -*-
"""Adds the @Synchronized annotation to the public archive manager methods.

The archive database is used from the message updates of Telegram, from the
archive executor and from the import executor. The native SQLite handle is not
safe for concurrent use, so every access is serialized.
"""
import io
import os
import re

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
path = os.path.join(root, 'TMessagesProj', 'src', 'main', 'java', 'org', 'telegram', 'messenger', 'fork', 'storage', 'ForkArchiveManager.kt')

with io.open(path, 'r', encoding='utf-8') as handle:
    lines = handle.readlines()

out = []
count = 0
for line in lines:
    stripped = line.strip()
    if re.match(r'^fun (archiveMessage|markDeleted|markDeletedByIds|getDeletedMessages|getArchivedDialogs|'
                r'getArchivedMessages|search|cleanupOlderThan|deleteDialog|exists|importState|saveImportState|'
                r'newestArchivedMessageId|oldestArchivedMessageId|clearArchive)\b', stripped):
        indent = line[:len(line) - len(line.lstrip())]
        out.append(indent + '@Synchronized\n')
        count += 1
    out.append(line)

with io.open(path, 'w', encoding='utf-8') as handle:
    handle.writelines(out)

print('annotated %d archive methods' % count)
