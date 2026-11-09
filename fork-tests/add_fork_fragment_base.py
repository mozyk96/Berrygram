# -*- coding: utf-8 -*-
"""Makes the Berrygram screens use the shared bottom inset handling.

Every Berrygram screen reserves the space of the floating bottom navigation of
the main tabs, so the last row of a screen stays visible. The screens share
the behaviour through ForkSettingsFragment.
"""
import io
import os
import re

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
ui = os.path.join(root, 'TMessagesProj', 'src', 'main', 'java', 'org', 'telegram', 'ui')

JAVA = [
    'ForkAboutActivity.java',
    'ForkAppearanceSettingsActivity.java',
    'ForkCameraTestActivity.java',
    'ForkCustomTextActivity.java',
    'ForkExperimentalGlassActivity.java',
    'ForkGestureSettingsActivity.java',
    'ForkMessageMenuSettingsActivity.java',
    'ForkNotificationDiagnosticsActivity.java',
    'ForkPerformanceSettingsActivity.java',
    'ForkReactionSettingsActivity.java',
    'ForkSettingsActivity.java',
    'ForkVideoSettingsActivity.java',
    'ForkVoiceSettingsActivity.java',
]
KOTLIN = [
    'ForkArchiveActivity.kt',
    'ForkArchiveChatPickerActivity.kt',
    'ForkArchiveMessagesActivity.kt',
    'ForkMediaSettingsActivity.kt',
    'ForkStealthSettingsActivity.kt',
    'ForkTranslationSettingsActivity.kt',
]


def patch(path, pattern, replacement):
    with io.open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    if 'ForkSettingsFragment' in text:
        return False
    new_text, count = re.subn(pattern, replacement, text, count=1)
    if count == 0:
        return False
    with io.open(path, 'w', encoding='utf-8') as handle:
        handle.write(new_text)
    return True


changed = []
for name in JAVA:
    path = os.path.join(ui, name)
    if not os.path.exists(path):
        continue
    if patch(path, r'extends BaseFragment', 'extends ForkSettingsFragment'):
        changed.append(name)
for name in KOTLIN:
    path = os.path.join(ui, name)
    if not os.path.exists(path):
        continue
    if patch(path, r': BaseFragment\(\)', ': ForkSettingsFragment()'):
        changed.append(name)

print('patched %d screens' % len(changed))
for name in changed:
    print('  ' + name)
