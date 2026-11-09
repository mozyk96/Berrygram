# -*- coding: utf-8 -*-
"""Adds the 1.4.2 entry to the changelog of the fork."""
import io
import os

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
path = os.path.join(root, 'CHANGELOG.md')

ENTRY = u"""## 1.4.2 \u2014 real device stabilization: one settings system, no `LOC_ERR:null`, stable navigation

- One settings system: every Berrygram feature is now a block inside the Telegram screen that owns it. Chat Settings carries effects, reactions, message menu, gestures, custom text, media and input; Privacy and Security carries Ghost Mode, deleted message preservation, edit history, archive privacy and retention; Notifications carries the diagnostics; Data and Storage carries the archive browser and the import; Language carries the translation options; Power Saving carries the device analyzer and the performance profile. The duplicated Berrygram rows are gone from the root, only the Update Center stays in the help section. The shared blocks live in `ForkSettingsBlocks` and `ForkSettingsSection`.
- `LOC_ERR:null` is fixed at its root: the resolver of `LocaleController` only knows the upstream Telegram language packs, so every fork string ended up as an error label. The resolver now falls back to the packaged Android resource, the Performance Center uses the fork locale for its own labels and the strings are covered by a regression test that fails when a runtime resource is missing.
- Navigation: the pager normalises its position with its own measured width instead of the display width, the indicator follows the pager without a second frame loop, the tab selection only changes when the page really changes and the anchor search stays inside the visible tabs. A cancelled swipe and a rapid tab switch can no longer fight each other.
- Archive: the database access is serialized, the archive instance is created atomically, the write of an incoming message and the clearing of the archive run on the archive executor instead of the UI thread, and the archive screen releases its importer listener when it is closed.
- Layout: every settings screen reserves the space of the floating bottom navigation and of the system navigation bar from the real window insets, so the last row is never hidden behind the navigation. The Berrygram screens use rounded grouped surfaces with a matching pressed state, section headers and the shared floating navigation header.
- Tests: new localization regression test, settings inset regression test, updated unified settings and archive tests, and two Android instrumentation tests for the runtime resources and the settings destinations.
- Verified: Kotlin/Java compilation, the full smoke suite, the resource validation and a signed arm64 release with R8. No physical-device testing was possible in this environment, the owner checklist lists every screen and gesture that still needs a real phone.

"""

with io.open(path, 'r', encoding='utf-8') as handle:
    text = handle.read()

if text.startswith('## 1.4.2'):
    print('the changelog already contains the 1.4.2 entry')
else:
    with io.open(path, 'w', encoding='utf-8', newline='') as handle:
        handle.write(ENTRY + text)
    print('the 1.4.2 changelog entry was added')
