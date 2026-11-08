import io
import os
import re

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
res = os.path.join(root, 'TMessagesProj', 'src', 'main', 'res')

# Android resource strings only accept \' inside a double quoted string.
# A bare \' in an unquoted string breaks the release resource merge, which is
# why the problem can stay hidden until the resources are rebuilt.
pattern = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.S)
problems = []
checked = 0
for folder in ('values', 'values-uk', 'values-ru'):
    folder_path = os.path.join(res, folder)
    for name in sorted(os.listdir(folder_path)):
        if 'fork' not in name.lower():
            continue
        path = os.path.join(folder_path, name)
        with io.open(path, 'r', encoding='utf-8') as handle:
            text = handle.read()
        checked += 1
        for key, value in pattern.findall(text):
            stripped = value.strip()
            if stripped.startswith('"') and stripped.endswith('"') and len(stripped) >= 2:
                continue
            if "\\'" in value:
                problems.append('%s/%s:%s' % (folder, name, key))

print('fork string files checked:', checked)
if problems:
    raise SystemExit('Invalid unicode escape sequence in fork string resources: ' + ', '.join(problems))
print('fork string resources contain no invalid escape sequences')
