import io

progress = 'PROGRESS.md'
continue_file = 'CONTINUE.md'

block = """# Android 1.4.0 RC 1 — implemented, release candidate built (device test pending)

## Repository and branch

- Repository `C:\\Users\\Keoo2\\Documents\\BerryGram-1.3`, branch `feature/berrygram-1.4.0-stable`,
  based on the published Beta 2 (`5ed25a4`), checkpoint tag `checkpoint/pre-1.4.0-stable`.
- Version `1.4.0-rc.1`, `APP_VERSION_CODE=7274` → arm64 versionCode `72749`
  (higher than the published Beta 2 `72739` and Beta 1 `72719`, so the RC installs over them
  without losing data).

## What was fixed in this cycle

- **Deleted messages now survive a restart.** The archive keeps a deletion flag, the text of a
  deleted message is captured locally at the moment of deletion, delete updates mark archived
  messages as deleted, and the kept messages are restored into the open chat at their original
  position. The decision logic is in the pure `ForkArchivedRestorePolicy` (tested).
- **Edit history is deterministic.** A per-account text cache remembers the last known text of a
  message, so the previous version is recorded even when the storage queue already rewrote the
  database row. Identical versions are no longer stored twice.
- **Ghost Mode hides secondary read signals.** Mentions, reactions and read message contents are
  no longer reported to the server while ghost reading is active. An explicit "Mark as read" still
  works, secret chats and TTL content stay untouched.
- **Firebase integration without invented credentials.** The build reads an owner provided
  `google-services.json` from `.fork-local/` (or `FORK_GOOGLE_SERVICES_FILE`) and injects
  `google_app_id`, `gcm_defaultSenderId`, `project_id` and `google_api_key`. Without the file the
  build still succeeds and the notification diagnostics screen explains exactly what is missing.
- **Archive reads leave the UI thread.** Dialog list, message list and search run on the archive
  thread, the search is debounced by 250 ms.
- **Performance profile has more effect.** The low profile additionally stops the embedded round
  video preview, and the performance screen states which effects the profile really controls.
- **Invalid resource escapes removed.** Four Ukrainian and two fork action strings contained
  `\\'` inside unquoted Android strings; this breaks the release resource merge and was hidden by
  the build cache. A new `test_fork_string_escapes.py` prevents a recurrence.

## Verification

- `:TMessagesProj:compileDebugKotlin` and `compileDebugJavaWithJavac` — success.
- Full `fork-tests/smoke.ps1` — success, including the new `ForkArchivedRestorePolicyTest`, the
  extended `StealthPolicyTest` and the release candidate ordering checks in `UpdatePolicyTest`.
- `fork-tests/test_berrygram_resources.py`, `fork-tests/test_release_scope.py`,
  `fork-tests/test_fork_string_escapes.py` — success.
- Release arm64 build with R8 and locally injected credentials — see the artifact line below.

## Known limitations

- Remote push still needs the owner's Firebase project; without it Telegram delivers notifications
  only while the application process is alive. The diagnostics screen reports
  `push_not_configured` and now contains the exact setup steps.
- No physical device test of this build yet: the emulator cannot start on this host and no device
  is connected over ADB. The owner receives the release candidate and the checklist
  `docs/1.4.0-rc.1-device-checklist.md`.
- The archive still only contains messages that were seen on this device; a full historical
  import is planned for 1.4.1.

"""

with io.open(progress, 'r', encoding='utf-8') as handle:
    text = handle.read()
if 'Android 1.4.0 RC 1' not in text:
    with io.open(progress, 'w', encoding='utf-8', newline='') as handle:
        handle.write(block + text)

header = """# BerryGram continuation — Android 1.4.0 RC 1

## State

- Branch `feature/berrygram-1.4.0-stable` (from `feature/berrygram-1.4.0-beta2` @ `5ed25a4`),
  checkpoint tag `checkpoint/pre-1.4.0-stable`.
- Version `1.4.0-rc.1`, versionCode `72749` (arm64). Published Beta 2 is `72739`.
- Release candidate APK is built and verified; it is NOT published. The owner tests it on the
  phone with `docs/1.4.0-rc.1-device-checklist.md`.

## Milestones completed in this cycle

1. Deleted messages are captured with their text and restored into the chat after a restart.
2. Edit history records versions deterministically through a per-account text cache.
3. Ghost reading also hides mentions, reactions and read message contents.
4. Owner provided Firebase configuration can be injected at build time; diagnostics explains the
   missing configuration.
5. Archive reads and search run off the UI thread.
6. The performance profile also stops the embedded round video preview.
7. Invalid resource escapes in the fork strings are fixed and covered by a regression test.

## Tests

- Debug Kotlin/Java compilation: success.
- `fork-tests/smoke.ps1`: success (incl. `ForkArchivedRestorePolicyTest`, extended
  `StealthPolicyTest`, `UpdatePolicyTest` release candidate ordering).
- `fork-tests/test_berrygram_resources.py`, `test_release_scope.py`,
  `test_fork_string_escapes.py`: success.
- Release build with R8 and injected credentials: success, APK verified.

## Next

1. Owner installs the release candidate and works through the device checklist.
2. Owner creates the Firebase project and puts `google-services.json` into `.fork-local/`, then a
   new build enables remote push; this is the only remaining blocker for the notification goal.
3. After the device feedback, decide whether 1.4.0 Stable can be published; the Stable release
   needs its own version code (`7275` → `72759`) and a normal (not pre-release) GitHub release.
4. 1.4.1 starts only after 1.4.0 is finished: quick Ghost Mode button, full archive import and
   update, improved translation, targeted performance work.

"""

with io.open(continue_file, 'r', encoding='utf-8') as handle:
    text = handle.read()
marker = '# BerryGram continuation — Android 1.4.0 Beta 2'
if marker in text and 'Android 1.4.0 RC 1' not in text.split(marker)[0]:
    text = text.replace(marker, header + marker, 1)
    with io.open(continue_file, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)

print('documentation updated')
