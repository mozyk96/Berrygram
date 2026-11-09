# -*- coding: utf-8 -*-
"""Regression test for the layout of the Berrygram settings.

The screenshot of the owner showed the content of the settings below the
floating bottom navigation. The test verifies that every screen that can be
opened from the settings reserves the space of the navigation and that the
space is taken from the real window insets instead of a fixed value.
"""
import io
import os
import re
import sys

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
java_root = os.path.join(root, 'TMessagesProj', 'src', 'main', 'java')

failures = []


def fail(message):
    failures.append(message)
    print('FAIL: ' + message)


def read(path):
    with io.open(path, 'r', encoding='utf-8', errors='replace') as handle:
        return handle.read()


def source(*parts):
    return read(os.path.join(java_root, *parts))


HOST_SCREENS = [
    ('org', 'telegram', 'ui', 'ThemeActivity.java'),
    ('org', 'telegram', 'ui', 'PrivacySettingsActivity.java'),
    ('org', 'telegram', 'ui', 'NotificationsSettingsActivity.java'),
    ('org', 'telegram', 'ui', 'DataSettingsActivity.java'),
    ('org', 'telegram', 'ui', 'LanguageSelectActivity.java'),
    ('org', 'telegram', 'ui', 'LiteModeSettingsActivity.java'),
]

FORK_SCREENS = [
    ('org', 'telegram', 'ui', 'ForkAboutActivity.java'),
    ('org', 'telegram', 'ui', 'ForkAppearanceSettingsActivity.java'),
    ('org', 'telegram', 'ui', 'ForkArchiveActivity.kt'),
    ('org', 'telegram', 'ui', 'ForkMediaSettingsActivity.kt'),
    ('org', 'telegram', 'ui', 'ForkMessageMenuSettingsActivity.java'),
    ('org', 'telegram', 'ui', 'ForkNotificationDiagnosticsActivity.java'),
    ('org', 'telegram', 'ui', 'ForkPerformanceSettingsActivity.java'),
    ('org', 'telegram', 'ui', 'ForkStealthSettingsActivity.kt'),
    ('org', 'telegram', 'ui', 'ForkTranslationSettingsActivity.kt'),
]


def main():
    # The shared inset helper uses the real window insets.
    insets = source('org', 'telegram', 'ui', 'fork', 'settings', 'ForkInsets.java')
    if 'mainTabsBottomOffset' not in insets:
        fail('The shared inset helper does not reserve the bottom navigation')
    if 'getMeasuredWidth' in insets and 'navigationBarHeight' not in insets:
        fail('The shared inset helper ignores the system navigation bar')
    if 'MAIN_TABS_HEIGHT_WITH_MARGINS' not in insets:
        fail('The shared inset helper does not use the height of the bottom navigation')
    if re.search(r'setPadding\([^)]*\b\d{3,}\b', insets):
        fail('The shared inset helper uses a hard coded padding')

    # Every Telegram settings screen that hosts a Berrygram block reserves the
    # space of the bottom navigation.
    for parts in HOST_SCREENS:
        text = source(*parts)
        name = parts[-1]
        if 'ForkSettingsBlocks.' not in text:
            fail(name + ' has no Berrygram block')
        if 'ForkInsets.mainTabsBottomOffset()' not in text:
            fail(name + ' does not reserve the space of the bottom navigation')

    # Every Berrygram screen reserves the space as well.
    for parts in FORK_SCREENS:
        text = source(*parts)
        name = parts[-1]
        if 'ForkSettingsFragment' not in text:
            fail(name + ' does not share the inset handling')

    base = source('org', 'telegram', 'ui', 'ForkSettingsFragment.java')
    if 'onInsets' not in base or 'applyBottomInset' not in base:
        fail('The shared Berrygram fragment does not apply the window insets')

    # The bottom navigation of the main tabs reports its own height.
    tabs = source('org', 'telegram', 'ui', 'MainTabsActivity.java')
    if 'isMainTabsBarVisible' not in tabs:
        fail('The main tabs do not report the visibility of the bottom navigation')
    if 'getInstance' not in tabs:
        fail('The main tabs are not reachable for the inset calculation')

    print('checked %d settings screens' % (len(HOST_SCREENS) + len(FORK_SCREENS)))
    if failures:
        print('%d layout problems found' % len(failures))
        return 1
    print('settings inset regression test passed')
    return 0


if __name__ == '__main__':
    sys.exit(main())
