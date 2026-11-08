import io
import os
import re
import shutil
import subprocess
import sys
import tempfile

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
res = os.path.join(root, 'TMessagesProj', 'src', 'main', 'res')

pattern = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.S)


def check_file(path):
    """Returns the list of unquoted strings with an unescaped apostrophe."""
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    problems = []
    for name, value in pattern.findall(text):
        stripped = value.strip()
        if stripped.startswith('"') and stripped.endswith('"') and len(stripped) >= 2:
            continue
        if re.search(r"(?<!\\)'", value):
            problems.append(name)
    return problems


files = []
for folder in ('values', 'values-uk', 'values-ru'):
    folder_path = os.path.join(res, folder)
    for name in sorted(os.listdir(folder_path)):
        if 'fork' in name.lower() and name.endswith('.xml'):
            files.append(os.path.join(folder_path, name))

problems = []
for path in files:
    for key in check_file(path):
        problems.append('%s:%s' % (os.path.relpath(path, res), key))

print('fork string files checked:', len(files))
if problems:
    raise SystemExit('Unescaped apostrophe in fork string resources: ' + ', '.join(problems))

# Validate the real tool chain: the aapt2 of the Android Gradle Plugin rejects
# unescaped apostrophes in unquoted strings even though plain javac does not.
aapt2 = None
candidates = []
gradle_home = os.environ.get('GRADLE_USER_HOME')
if gradle_home:
    candidates.append(gradle_home)
candidates.append(os.path.join(os.path.expanduser('~'), '.gradle'))
for home in candidates:
    if not home or not os.path.isdir(home):
        continue
    for dirpath, _, filenames in os.walk(home):
        for name in filenames:
            if name.startswith('aapt2') and name.endswith('.exe') and 'windows' in dirpath:
                aapt2 = os.path.join(dirpath, name)
                break
        if aapt2:
            break
    if aapt2:
        break

if aapt2:
    for folder in ('values', 'values-uk', 'values-ru'):
        for name in sorted(os.listdir(os.path.join(res, folder))):
            if 'fork' not in name.lower() or not name.endswith('.xml'):
                continue
            temp = tempfile.mkdtemp()
            try:
                target = os.path.join(temp, folder)
                os.makedirs(target)
                shutil.copy(os.path.join(res, folder, name), os.path.join(target, name))
                result = subprocess.run([aapt2, 'compile', '--dir', temp, '-o', os.path.join(temp, 'out.zip')],
                                        capture_output=True, text=True)
                if result.returncode != 0:
                    raise SystemExit('aapt2 rejected %s/%s: %s' % (folder, name, result.stdout.strip()[:400]))
            finally:
                shutil.rmtree(temp, ignore_errors=True)
    print('aapt2 accepted every fork string resource')
else:
    print('aapt2 of the Android Gradle Plugin not found, skipped the tool chain check')

print('fork string resources use valid escaping')
