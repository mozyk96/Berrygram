import io
import os
import re

base = os.path.join('TMessagesProj', 'src', 'main', 'res')
pattern = re.compile(r'(<string name="[^"]+">)(.*?)(</string>)', re.S)


def repair(value):
    """Reverses a CP1252 mojibake of UTF-8 text, for example 'â€™' -> '’'."""
    try:
        repaired = value.encode('cp1252').decode('utf-8')
    except (UnicodeEncodeError, UnicodeDecodeError):
        return value
    return repaired if repaired != value else value


changed = []
for folder in ('values', 'values-uk', 'values-ru'):
    folder_path = os.path.join(base, folder)
    for name in sorted(os.listdir(folder_path)):
        if 'fork' not in name.lower() or not name.endswith('.xml'):
            continue
        path = os.path.join(folder_path, name)
        with io.open(path, 'r', encoding='utf-8') as handle:
            text = handle.read()
        counter = {'count': 0}

        def replace(match):
            value = match.group(2)
            repaired = repair(value)
            if repaired != value:
                counter['count'] += 1
            return match.group(1) + repaired + match.group(3)

        updated = pattern.sub(replace, text)
        if updated != text:
            with io.open(path, 'w', encoding='utf-8', newline='') as handle:
                handle.write(updated)
            changed.append('%s/%s (%d strings)' % (folder, name, counter['count']))

print('files repaired:', len(changed))
for item in changed:
    print('  ', item)
