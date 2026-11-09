# -*- coding: utf-8 -*-
"""Records the state of the 1.4.2 stabilization release in CONTINUE.md."""
import io
import os
import sys

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
path = os.path.join(root, 'CONTINUE.md')

apk = sys.argv[1] if len(sys.argv) > 1 else ''
sha = sys.argv[2] if len(sys.argv) > 2 else ''

ENTRY = u"""# BerryGram continuation \u2014 Android 1.4.2 stabilization release

## State

- Branch `feature/berrygram-1.4.1`, the tag `v1.4.1` is not moved. The stabilization
  release is published as `v1.4.2` from the same branch.
- Version `1.4.2`, versionCode `72789` (arm64). The published 1.4.1 is `72779`.
- Release APK `%s`, SHA-256 `%s`.
- Published as a normal GitHub Release (not a prerelease) with the ARM64 APK and
  `SHA256SUMS`: https://github.com/mozyk96/Berrygram/releases/tag/v1.4.2

## What the cycle fixed

- One settings system: every Berrygram feature is a block inside the Telegram screen
  that owns it (chat settings, privacy and security, notifications, data and storage,
  language, power saving). The duplicated rows are gone from the root, only the
  Update Center stays in the help section.
- `LOC_ERR:null`: the resolver of `LocaleController` falls back to the packaged
  resource, the Performance Center uses `ForkLocale` for its own labels.
- Navigation: the pager normalises the position with its own width, the indicator has
  no second frame loop, the tab selection only changes when the page changes and the
  anchor search stays inside the visible tabs.
- Archive: the database access is serialized, the instance is created atomically, the
  write of an incoming message and the clearing of the archive run on the archive
  executor and the screen releases its importer listener.
- Layout: the screens reserve the space of the floating bottom navigation and of the
  system navigation bar from the real window insets.

## Tests

- `fork-tests/smoke.ps1`: success, including the new localization regression test and
  the new settings inset regression test.
- `fork-tests/test_main_tabs_indicator.js`, `fork-tests/test_archive_import.js` and
  `fork-tests/ForkUnifiedSettingsMappingTest.java`: updated for the new architecture.
- Android instrumentation tests `ForkLocalizationInstrumentationTest` and
  `ForkSettingsInstrumentationTest` in `TMessagesProj_AppTests`: they compile, they
  need a device or an emulator and were not executed here.
- Release build: Kotlin/Java compilation and R8 arm64 release, package, versionCode,
  ABI, signer continuity and the resource locales were verified.

## Remaining

- Device checklist for the owner (`docs/1.4.2-device-checklist.md`): every settings
  screen, the `LOC_ERR` labels in three languages, the navigation, the insets, the
  archive import and the crash log instructions.
- Remote push still needs the owner's Firebase project, the diagnostics report
  `push_not_configured`.
- No physical-device testing was possible in this environment, the smoothness of the
  animations is therefore not verified.

"""

with io.open(path, 'r', encoding='utf-8') as handle:
    text = handle.read()

if text.startswith(u'# BerryGram continuation \u2014 Android 1.4.2'):
    print('CONTINUE.md already contains the 1.4.2 entry')
else:
    with io.open(path, 'w', encoding='utf-8', newline='') as handle:
        handle.write(ENTRY % (apk, sha) + text)
    print('CONTINUE.md updated for 1.4.2')
