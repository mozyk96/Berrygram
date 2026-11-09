# -*- coding: utf-8 -*-
"""Adds the storage usage label of the archive to the fork string resources."""
import io
import os

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
res_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'res')

STRINGS = {
    'values': [
        ('ForkArchiveStorage', 'Storage usage'),
        ('ForkArchiveStorageValue', '%1$s'),
    ],
    'values-uk': [
        ('ForkArchiveStorage', 'Використання пам\u2019ят\u2019'),
        ('ForkArchiveStorageValue', '%1$s'),
    ],
    'values-ru': [
        ('ForkArchiveStorage', '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u0430\u043c\u044f\u0442\u0438'),
        ('ForkArchiveStorageValue', '%1$s'),
    ],
}


def main():
    for folder, entries in STRINGS.items():
        path = os.path.join(res_root, folder, 'fork_custom_text.xml')
        with io.open(path, 'r', encoding='utf-8') as handle:
            text = handle.read()
        missing = []
        for name, value in entries:
            if ('name="%s"' % name) in text:
                continue
            missing.append('    <string name="%s">%s</string>' % (name, value))
        if not missing:
            continue
        index = text.rindex('</resources>')
        text = text[:index] + '\n'.join(missing) + '\n' + text[index:]
        with io.open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
        print('updated %s with %d strings' % (folder, len(missing)))


if __name__ == '__main__':
    main()
