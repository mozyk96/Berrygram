import io
import os
import re

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
java_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'java')
res_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'res')

pattern = re.compile(r'R\.string\.(Fork\w+)')
used = {}
for dirpath, _, files in os.walk(java_root):
    for name in files:
        if not (name.endswith('.java') or name.endswith('.kt')):
            continue
        path = os.path.join(dirpath, name)
        with io.open(path, 'r', encoding='utf-8', errors='replace') as handle:
            text = handle.read()
        for key in pattern.findall(text):
            used.setdefault(key, []).append(os.path.relpath(path, root))

print('distinct Fork* string ids used in code:', len(used))


def strings(folder, file_name):
    path = os.path.join(res_root, folder, file_name)
    if not os.path.exists(path):
        return None
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    return set(re.findall(r'<string name="([^"]+)"', text))


locales = {}
for folder in ('values', 'values-uk', 'values-ru'):
    merged = set()
    for name in os.listdir(os.path.join(res_root, folder)):
        if name.startswith('fork_') or name.startswith('strings_fork'):
            merged |= strings(folder, name)
    locales[folder] = merged

en = locales['values']
missing_uk = sorted(k for k in used if k not in locales['values-uk'])
missing_ru = sorted(k for k in used if k not in locales['values-ru'])
missing_en = sorted(k for k in used if k not in en)

print('used but absent in values (would crash/blank):', missing_en)
print('used but absent in values-uk:', len(missing_uk), missing_uk[:15])
print('used but absent in values-ru:', len(missing_ru), missing_ru[:15])

kt_used = sorted(k for k, v in used.items() if any(f.endswith('.kt') for f in v))
print('Fork* ids referenced from Kotlin files:', len(kt_used))
print('  of them missing in values-uk:', [k for k in kt_used if k not in locales['values-uk']])
print('  of them missing in values-ru:', [k for k in kt_used if k not in locales['values-ru']])
