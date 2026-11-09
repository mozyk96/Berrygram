# -*- coding: utf-8 -*-
"""Regression test for the Berrygram localization.

The previous static test only checked that a string key exists in the XML
files. It reported a missing runtime resource as a successful check, so the
application showed "LOC_ERR:null" on the device. The test therefore verifies

  * every fork string that is used in the code exists in the English, the
    Ukrainian and the Russian resources,
  * no fork string is looked up through LocaleController, that resolver only
    knows the upstream Telegram language packs,
  * every fork string that is used with arguments has a matching number of
    format placeholders, a wrong count crashes the screen at runtime,
  * the resolver of LocaleController falls back to the packaged resource, so
    a fork string can never end up as "LOC_ERR:null".
"""
import io
import os
import re
import sys

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
java_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'java')
res_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'res')

FORK_FILES = ('fork_custom_text.xml', 'fork_beta5.xml', 'fork_reactions.xml',
              'fork_menu_editor.xml', 'strings_fork_actions.xml', 'strings.xml')
LOCALES = ('values', 'values-uk', 'values-ru')

failures = []


def fail(message):
    failures.append(message)
    print('FAIL: ' + message)


def read(path):
    with io.open(path, 'r', encoding='utf-8', errors='replace') as handle:
        return handle.read()


def strings_of(folder, file_name):
    path = os.path.join(res_root, folder, file_name)
    if not os.path.exists(path):
        return {}
    result = {}
    for match in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', read(path), re.S):
        result[match.group(1)] = match.group(2)
    return result


def collect_sources():
    sources = []
    for dirpath, _, files in os.walk(java_root):
        for name in files:
            if name.endswith('.java') or name.endswith('.kt'):
                sources.append(os.path.join(dirpath, name))
    return sources


def main():
    locale_strings = {}
    for folder in LOCALES:
        merged = {}
        for file_name in FORK_FILES:
            merged.update(strings_of(folder, file_name))
        locale_strings[folder] = merged
        if not merged:
            fail('no fork strings found in ' + folder)

    sources = collect_sources()

    # 1) every used fork string exists in every locale
    used = {}
    for path in sources:
        text = read(path)
        for name in re.findall(r'R\.string\.(Fork\w+)', text):
            used.setdefault(name, set()).add(os.path.relpath(path, root))

    for name, files in sorted(used.items()):
        for folder in LOCALES:
            if name not in locale_strings[folder]:
                fail('%s is used in %s but missing in %s' %
                     (name, ', '.join(sorted(files)), folder))

    # 2) no fork string may be resolved through LocaleController
    pattern = re.compile(r'LocaleController\.getString\([^)]*R\.string\.(Fork\w+)')
    for path in sources:
        text = read(path)
        for match in pattern.finditer(text):
            fail('%s uses LocaleController.getString for the fork string %s, '
                 'the fork strings are not part of the Telegram language packs' %
                 (os.path.relpath(path, root), match.group(1)))

    # 3) the number of format placeholders has to match the argument count
    call = re.compile(r'ForkLocale\.get\(\s*R\.string\.(Fork\w+)\s*((?:,[^;()]*?)?)\)')
    for path in sources:
        text = read(path)
        for match in call.finditer(text):
            name = match.group(1)
            arguments = match.group(2).strip()
            if not arguments.startswith(','):
                continue
            arguments = arguments[1:].strip()
            depth = 0
            count = 0
            current = ''
            parts = []
            for char in arguments:
                if char in '([{':
                    depth += 1
                elif char in ')]}':
                    depth -= 1
                if char == ',' and depth == 0:
                    parts.append(current)
                    current = ''
                else:
                    current += char
            parts.append(current)
            parts = [part for part in parts if part.strip()]
            value = locale_strings['values'].get(name)
            if value is None:
                continue
            placeholders = len(re.findall(r'%\d+\$', value))
            if placeholders != len(parts):
                fail('%s has %d format placeholders but %d arguments are used (%s)' %
                     (name, placeholders, len(parts), os.path.relpath(path, root)))

    # 4) the resolver falls back to the packaged resource
    controller = os.path.join(java_root, 'org', 'telegram', 'messenger', 'LocaleController.java')
    text = read(controller)
    if 'getStringV2Original' not in text:
        fail('LocaleController has no getStringV2Original resolver')
    fallback = re.search(r'getStringV2Original[\s\S]{0,4000}?context\.getString\(stringRes\)', text)
    if not fallback:
        fail('LocaleController does not fall back to the packaged resource, '
             'a fork string would end up as LOC_ERR:null')

    # 5) the shared fork locale helper reads the packaged resource
    helper = os.path.join(java_root, 'org', 'telegram', 'ui', 'ForkLocale.java')
    if 'getString' not in read(helper):
        fail('ForkLocale does not read the packaged resource')

    print('distinct fork strings used in the code: %d' % len(used))
    if failures:
        print('%d localization problems found' % len(failures))
        return 1
    print('fork localization regression test passed')
    return 0


if __name__ == '__main__':
    sys.exit(main())
