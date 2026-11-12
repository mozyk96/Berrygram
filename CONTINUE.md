# Berrygram 1.4.5 — release completion (2026-11-12)

- Berrygram 1.4.5 release finalized from branch feature/berrygram-1.4.5. Upstream Telegram 13.2.2 (base code 7303, arm64 code 72939) verified and integrated at commit a94c6d6e.
- Mandatory bug fixes implemented and verified:
  1. Screenshot restrictions completely removed across all windows and viewers (FlagSecureReason, PhotoViewer, SecretMediaViewer, SecretVoicePlayer, GroupCallActivity, PasscodeViewDialog, and default allowScreenCapture = true) to prevent Samsung One UI / Android OS screenshot blocking.
  2. Cancelled video phantom message resolved: unsent/upload-cancelled messages (id <= 0 or isSending / send_state != 0) are removed immediately rather than converted into tombstone deleted messages.
  3. "Delete locally" ("Видалити локально") action implemented in chat message context menu for tombstones and unsent/failed messages, allowing users to safely purge ghost messages locally without server interaction.
  4. Settings top item clipping resolved in BaseFragment: header height calculation accounts for glass pill extra height so first item in Power Saving / Settings is never hidden.
  5. Original Ghost Mode ghost silhouette icons (fork_ic_ghost_on, fork_ic_ghost_off) integrated and active, with unified fork icons (analyzer, archive, glass, notifications, performance, translate, update).
  6. Edge-to-edge navigation insets properly handle bottom system navigation and floating bottom tabs without letterboxing or black gaps.
- Full smoke test suite passed with 0 failures (`powershell -File .\fork-tests\smoke.ps1`).
- Release signed ARM64 APK built successfully via `./fork-tests/build-release.ps1`:
  - Package ID: `org.example.forkclient`
  - Version: `1.4.5` (versionCode `72939`, arm64-v8a)
  - Signature: CN=BerryGram Release, O=BerryGram (SHA-256 `b3ea9ca232ae03ebeab5bbc2729a306fb5eacd0520104fc52450a421efc722a5`), signature continuity verified.
  - APK SHA-256: `0e8ac6f34109927007043408ebbeac1788be7525d8af2968ee1942fef1480d65`
  - Size: 44,708,519 bytes.
  - Zipalign: verified (4-byte and 16KB-ready).
- Testing notice: Smoke tests, mock graphics, resource verification, and headless build tests passed. No real Telegram account automation or physical phone modifications performed.

# Berrygram 1.4.4 — historical integration notes

- The owner cancelled the 13.2.2 source requirement on 2026-11-11. Use the already integrated official Telegram 13.2.1 source at 7be672ead283d022812d75c729d3a04dd4c2a2c6 for 1.4.4; do not repeat its upstream check. Finish release validation and publish only after its real gates pass. For 1.4.5 and later, check official upstream once per new release cycle using tools/check-telegram-upstream.ps1 and record the result in tools/telegram-upstream.json.
- Android Keystore IV handling, SQLite direct-buffer binding and import failure propagation fixed; five emulator archive tests and full smoke passed. The earlier 13.2.1 ARM64 build is a preflight only, predates these fixes and must not be published. Authenticated messaging, archive and physical-device testing remain unavailable. Existing records previously erased by the old faulty migration may be unrecoverable.
- The archive restart test wrote encrypted synthetic content, stopped the isolated test process, and successfully read it in a new process on API 35. Logs: build-144-archive-process-write.log and build-144-archive-process-read.log.
- Emulator local-photo viewer test passed and its screenshot was inspected; video, sticker preview and authenticated media interaction were not verified.

- Integration worktree: `C:\Users\Keoo2\Documents\Codex\2026-11-10\files-pasted-by-the-user-berrygram\work\berrygram-upstream-144`, branch `integration/berrygram-1.4.4-upstream`; parent `31cff5d5`. Original checkout and its untracked native sources are preserved. The integration is staged and uncommitted.
- Telegram 13.2.1 source `7be672ead283d022812d75c729d3a04dd4c2a2c6` integrated using inferred closest 13.1.1 base `3de37c8cfb039452be7398909461da6288babf15`. All native gitlinks are checked out at their staged revisions. Berrygram settings, updater and glass hooks were retained.
- Version 1.4.4 uses base code 7292 (expected ARM64 code 72929). Progressive backdrop blur uses the upstream GPU blur source and a continuous fade; an emulator GPU pixel test passed. Viewer, preview and profile menus have theme-aware glass integration.
- Java/Kotlin compilation and smoke checks passed. On API 35 emulator, 15 of 16 targeted tests passed in the combined run; the remaining LiteMode test passed after fixing its test harness (`build-upstream-144-screens-retry.log`). No authenticated navigation, physical-device or full media interaction has been verified.
- Next: review and commit the staged integration, then run `./fork-tests/build-release.ps1 -EnvFile <private-env-file> -LogFile <release-log>` from the integration worktree. Verify signing, package, version, ABI, SHA-256 and upgrade from published 1.4.3; publish only after release gates pass. Continue 1.4.5 after publishing 1.4.4.

Historical notes below may describe earlier states.

# BerryGram 1.4.3 — work in progress

- Repository: `C:\Users\Keoo2\Documents\BerryGram-1.3`; branch `feature/berrygram-1.4.3`, based on `7f7d083`. Published version remains 1.4.2 (arm64 code 72789). No 1.4.3 release or APK has been produced.
- The updater now rejects an arm64-only release on a 32-bit device and checks APK native-library ABIs against the actual device. Existing settings and release metadata are unchanged.
- `fork-tests/smoke.ps1`, `updater-package-fixture.ps1`, `github-fixture.ps1` and `updater-live-github.ps1` passed; package and download fixtures use mocks. Live GitHub test discovered 1.4.2. ADB reported no devices; UI, installation and actual signature checks remain unverified.
- The original checkout contains uninitialized native submodules with untracked contents; preserve them. Do not initialize over or delete those contents without investigation and explicit deletion consent.
- Added dedicated update screen, explicit install action, real byte progress, graphics placement and typography fixes. Debug APK build passed with multiline switch changes; smoke tests and Android instrumentation compilation passed. A later divider-group change and its instrumentation test compiled, but runtime layout validation remains pending. Test module Maps placeholder is configured.
- Current work: shared setting titles can wrap beside values; navigation rows use icon and subtitle cells. Smoke and Android-test compilation passed, but neither the new row geometry tests nor the layout have run on a device.
- Remaining: finish the requested settings/UI overhaul, Update Center and background downloads, archive/privacy regression review, Android build and device/emulator checks, then full release verification. Delta updates remain deferred pending a complete verified design. Remote push requires the owner's Firebase configuration.
- Background update implementation: opt-in persisted JobScheduler, Wi-Fi/mobile/both restrictions, selected-network HTTP, progress, cancellation, verified APK caching and explicit installation. The existing updater fixtures and smoke passed, and Android instrumentation compiled. The scheduler and UI have not been executed on Android; do not claim them verified or publish on that basis.
- Debug APK rebuild passed (`build/berrygram-143-background-debug.log`); the APK is a debug build, not a release. Release version remains 1.4.2. Signing/API material has not been injected into a 1.4.3 build.
- An isolated API 35 emulator `emulator-5554` runs under the Codex workspace with WHPX. Seven settings and scheduler instrumentation tests passed (`build/berrygram-143-emulator-tests-online.log`); ARM64 debug APK launched to onboarding with no account. Full settings, actual background transfer and update installation remain untested.
- Next: run the scheduler and settings instrumentation on an authorized emulator, including network changes, cancellation, restart and APK installation. Then complete the remaining full settings/menu and archive/privacy/navigation review. Exact focused fixture command: `./fork-tests/updater-package-fixture.ps1 -JsonJar "C:\Users\Keoo2\Documents\Codex\2026-10-28\files-pasted-by-the-user-senior\work\test-deps\json-20240303.jar"`. Only after all required checks pass: assign 1.4.3/versionCode, build and verify signed ARM64 release, then publish.


# BerryGram continuation — Android 1.4.2 stabilization release

## State

- Branch `feature/berrygram-1.4.1`, the tag `v1.4.1` is not moved. The stabilization
  release is published as `v1.4.2` from the same branch.
- Version `1.4.2`, versionCode `72789` (arm64). The published 1.4.1 is `72779`.
- Release APK `C:\Users\Keoo2\Documents\BerryGram-1.4.2-arm64-v8a.apk`, SHA-256 `37b1e31f8ddaa7137880f8df1de2d23afb0e531d86a1ef0269d9f0e6f3f063d6`.
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

# BerryGram continuation — Android 1.4.1 official release

## State

- Branch `feature/berrygram-1.4.1` (from `530884f`); tag `v1.4.1` on release commit `e34245c`
  (published to `origin`).
- Version `1.4.1`, versionCode `72779` (arm64). Previous official 1.4.0 is `72759`; the internal
  RC was `72769`.
- Release APK `C:\Users\Keoo2\Documents\BerryGram-1.4.1-arm64-v8a.apk`, 41933616 bytes,
  SHA-256 `ac30bda056ef435924252eac4affcf3d9eeb25961de9d585ed208ba5cb93900a`.
- Published as a normal GitHub Release (not a prerelease):
  https://github.com/mozyk96/Berrygram/releases/tag/v1.4.1 with the ARM64 APK and `SHA256SUMS`.

## Update channels

- Stable/Beta channels are discontinued. The selector, `beta_channel` preference and all channel
  branches are removed; obsolete preferences are cleaned once without touching other settings.
- The updater uses one official feed: normal GitHub Releases only, drafts and every prerelease are
  ignored, selection uses parsed version metadata and never offers a downgrade.
- Historical 1.4.0 Beta/RC installs still discover 1.4.1 through the normal versionCode comparison.

## Tests

- Release Kotlin/Java compilation: success.
- `fork-tests/smoke.ps1`: success, including the unified `UpdatePolicyTest`, resource validation
  and the release-scope checks that forbid channel controls.
- `updater-package-fixture.ps1` and `updater-live-github.ps1`: success; the live check discovers
  the published 1.4.1 official release.
- Release artifact checks: package, version, minSdk/targetSdk, ABI, signing continuity,
  v1/v2 schemes, zipalign, credential in dex, no keystore packaged.

## Remaining

- Device checklist for the owner (`docs/1.4.1-device-checklist.md`): navigation smoothness,
  settings layout, Ghost toggle, archive import/resume/cancel, translation on chat open,
  performance and the unified update panel.
- Remote push still needs the owner's Firebase project; diagnostics reports `push_not_configured`.
- 1.4.2 has not been started.

# BerryGram continuation — Android 1.4.0 Stable

## State

- Branch `feature/berrygram-1.4.0-stable`, tag `v1.4.0` points at the published release source.
- Version `1.4.0`, versionCode `72759` (arm64). Published before it: 1.4.0 Beta 1 `72719`,
  Beta 2 `72739`, Stable 1.3.0 `72639`.
- The release artifact is `BerryGram-1.4.0-arm64-v8a.apk`
  (SHA-256 `515d554ea740607d8b2672918afde6692e9ca7bd4ac6dabec951ec7c25dae8bf`), published as a normal GitHub Release with the Ukrainian changelog.
- The in-app updater was repaired in this cycle: no downgrade is offered any more, the channel
  follows the installed build, the checksum is resolved from the release `SHA256SUMS` file when the
  API has no digest, and the update panel strings are free of mojibake.

- Published release: https://github.com/mozyk96/Berrygram/releases/tag/v1.4.0
  (normal release, not a pre-release), asset
  `BerryGram-1.4.0-arm64-v8a.apk`, SHA-256
  `515d554ea740607d8b2672918afde6692e9ca7bd4ac6dabec951ec7c25dae8bf`, tag `v1.4.0`.
- The live updater test was executed after the publication and finds 1.4.0 on the stable and on
  the beta channel, so the in-app update centre discovers the published release.

## Tests

- Debug and release compilation: success.
- `fork-tests/smoke.ps1`: success.
- Updater fixtures (`github-fixture.ps1`, `updater-package-fixture.ps1`): success.
- Live GitHub validation (`updater-live-github.ps1`): success.

## Remaining

- Remote push needs the owner's Firebase project, the steps are in the release notes.
- No physical device test was possible on this host, the device checklist for the owner stays valid.
- 1.4.1 (quick Ghost Mode button, archive import, translation improvements) is not started and
  requires a separate instruction.

# BerryGram continuation — Android 1.4.0 RC 1

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
- Release candidate: `C:\Users\Keoo2\Documents\BerryGram-1.4.0-rc.1-arm64-v8a.apk`,
  SHA-256 `9a0eaf3de94456947aabcabe334fde20b3736791d34460d3d0a4ac97768043e8`, versionCode `72749`, signed with the published release certificate.

## Next

1. Owner installs the release candidate and works through the device checklist.
2. Owner creates the Firebase project and puts `google-services.json` into `.fork-local/`, then a
   new build enables remote push; this is the only remaining blocker for the notification goal.
3. After the device feedback, decide whether 1.4.0 Stable can be published; the Stable release
   needs its own version code (`7275` → `72759`) and a normal (not pre-release) GitHub release.
4. 1.4.1 starts only after 1.4.0 is finished: quick Ghost Mode button, full archive import and
   update, improved translation, targeted performance work.

# BerryGram continuation — Android 1.4.0 Beta 2

## State
- Branch: feature/berrygram-1.4.0-beta2, base `7fd3022` (tag `v1.4.0-beta.1`).
- Recovered from the wrong base: the authoritative 1.4.0 history was restored from the upstream
  repository and the unreleased local 1.4.0 work (notification diagnostics, menu surfaces, spring
  tab indicator) was re-applied. The previous reconstruction stays in `feature/berrygram-1.4.0`
  (checkpoint `be8a751`).
- Version: 1.4.0-beta.2, versionCode 72739 (arm64). Published Beta 1 is 72719, the earlier
  unpublished Beta 2 debug build was 72729.

## Beta 2 scope completed
- Ghost reading no longer suppresses explicit "Mark as read" (force path + regression tests).
- Per-account privacy settings with legacy migration.
- Keystore AES-GCM encryption for archive, deleted markers and edit history, with plaintext migration.
- Archive retention 7/30/90 days or forever, cleanup on app start and on demand.
- Notification diagnostics, three appearance modes, message menu surfaces and the spring tab
  indicator recovered from the 1.4.0 development history.

## Verification
- Debug and release Kotlin/Java compilation: success.
- Full `fork-tests/smoke.ps1`: success.
- Release ARM64 APK built, signed with the existing release key (certificate SHA-256 `b3ea9ca2…c722a5`,
  identical to the published Stable) and published as the GitHub pre-release `v1.4.0-beta.2`:
  `https://github.com/mozyk96/Berrygram/releases/tag/v1.4.0-beta.2`, APK `BerryGram-1.4.0-beta.2-arm64-v8a.apk`,
  SHA-256 `d8e1787a3ab2ead530fcff1453b1b22db8d595fa51fca2260c67fc9933369322`, versionCode `72739`.
  Branches `feature/berrygram-1.4.0-beta2` and `feature/berrygram-1.4.0` plus tag `v1.4.0-beta.2` point at `c35dfd8`.
  The in-app updater acceptance rules were verified against the live release.

## Open items
- Owner device test: login, incoming messages, notifications, Ghost Mode vs explicit Mark as read,
  deleted-message bubbles, edit history, archive search/retention, navigation indicator, the three
  appearance modes and the in-app update.
- Remote push still needs a BerryGram-owned Firebase project; steps are in the release notes.
- Do not publish further releases without device evidence or explicit owner direction.

# BerryGram continuation — Android 1.4.0 Beta 1

## State
- Branch: feature/berrygram-1.4.0
- Checkpoints saved: backup-initial-state-9365c4c, backup-initial-state-130-stable
- Version: 1.4.0-beta.1, versionCode: 7271

## Implemented in 1.4.0 Beta 1
- Ghost Mode: read receipt suppression for private chats & groups, secret chats preserved.
- Deleted Messages: AyuGram-style preservation, 38% bubble alpha, readable text, deletion mark.
- Message Edit History: local SQLite storage, revision viewer dialog, context menu item.
- Berry Archive: local SQLite storage, dialog & message browsers, full-text search.
- 1-Tap Translator: enabled by default, native alert integration.
- Glass 2.0 & 3 Visual Modes: Glass 2.0, Blur, No effects; obsolete controls removed.
- Device Analyzer: hardware assessment, Galaxy A25 tier correction, scan dialog.
- Performance Center 2.0: Auto vs Manual profiles (Low, Medium, High).
- Advanced Animations 2.0: continuous spring tab indicator on MainTabsLayout.
- Kotlin Transition: all new fork modules written in Kotlin.
- Built-in Updater: fixed install invocation, permission flow, ARM64/Universal support, signing key continuity check, default Stable channel.

## Verification
- All smoke tests passed (fork-tests/smoke.ps1).
- Resource and scope checks passed.
- APK assembleAfatDebug succeeded.

## Next Steps
- Owner testing on device.
- Beta 2 phase will focus exclusively on bug fixes and stabilization based on owner feedback.

# BerryGram continuation — Android 1.3.0 Beta 5

## Published

- Branch `dev/1.3.0`; Beta 5 source/tag `93794233dffb3ff53622792a023718124ee8e18d`.
- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.5
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.5/BerryGram-1.3.0-beta.5-arm64-v8a.apk
- SHA-256: `dacc6aac28e214e14c9c0e31eb575f92069703a22c6e4d85eec44f57b0f81d6f`; code `72629`.
- Beta 4 remains published/immutable: `7bf93d3`, code `72619`, SHA `4e10c9f3a43604fe039f00205c77e817d85218867f50bfcf5e154bfb5659c604`.

## Beta 5 implemented

- Experimental Glass 2.0 with persisted transparency, blur, frost, saturation, edge light, highlight, shadow depth, tint and radius; Clear/Frosted/Balanced/Deep presets and a live preview. Legacy effect strength is migrated.
- Real blur-path coverage for BerryGram popups, Mini Dock and reaction panel; opaque/Telegram fallback under power saving or unsupported devices.
- Embedded round-video panel at the bottom of Video messages: circular power and switch-camera buttons on the left, large circular live preview on the right; stops on pause/detach/window invisibility and releases camera resources.
- Adaptive Camera Engine with Camera2 capability discovery, five profiles, JPEG quality, round-video bitrate/FPS limits, validated FPS ranges and advertised OIS/EIS.
- Voice profiles with real platform noise suppression/AGC/AEC where available, source selection, profile-aware Opus bitrate and a local test recorder.
- Post-release branch commit `533d604` adds stopping the embedded preview when scrolled off-screen. It is **not** in the published Beta 5 APK and should be included in the next build.

## Open items

- Owner must install the published Beta 5 APK and verify: glass controls/presets/themes, Mini Dock/reaction panels, round-video preview lifecycle and rotation, photo/ordinary/round capture, voice profiles/effects/test recorder, account switching, restart persistence and in-place update.
- Beta 5 was not device-tested before publication. The owner's phone was not connected over ADB; the emulator failed due to a missing Windows Android Emulator hypervisor driver.
- Manual ISO/exposure/focus UI, physical-lens selection, camera extensions/HDR/Night and full BottomSheet/AlertDialog glass remain out of scope for this beta.
- Do not publish Stable 1.3.0 until owner acceptance; Desktop remains gated.

## Confirmed on physical SM-A256B

- Logcat captured the Beta 2 crash: `NullPointerException` in `ActionBarPopupWindowLayout.addViewToSwipeBack` from the custom More page. Beta 3 menu with `Ще` opens without crash.
- Lock screen background is normal after the passcode fallback.
- Performance Center opens and switches profiles without the previous full-page rebuild; Lite Mode and storage rows navigate.
- Experimental Glass is selectable alongside Blur/Liquid.
- Tap→Edit reached Telegram's native edit path and displayed a server edit refusal without crashing. The installed test build lacked API credentials; final APK restores them.

## Implemented

- Menu swipe-back flag + defensive fallback.
- Edit eligibility/account validation and accurate unavailable message.
- Restored approved fork API credentials from Stable 1.2.0 into the Beta 3/Beta 4 builds (not committed/logged).
- Passcode wallpaper fallback (theme color; no opaque black from evicted/invalid bitmap).
- Removed dynamic-neutral Material tint, migrated `material` to Blur, removed Material resource.
- Fixed double-encoded Ukrainian/Russian `fork_custom_text.xml` labels.
- Rewrote Performance Center to update controls in place; real Lite Mode/cache/diagnostics actions; thermal and low-RAM-aware Automatic effects.
- Cached gesture/reaction/performance preferences.
- Added optional Experimental Glass with theme-aware translucent tint, stronger edge light, API/capability fallback.

## Verification

- Full `fork-tests/smoke.ps1` passed.
- Release/R8 passed; Beta 4 APK version code `72619`, minSdk 23, ARM64, non-debug/non-testOnly, permanent certificate, v1/v2 signatures, 16 KB alignment, source revision, native libs identical to Beta 2, API credentials present, SHA verified.
- Final Beta 3/Beta 4 credential-bearing APK was **not installed** before publication at the owner's request. Prior Beta 3 test build (same code minus credentials) was installed over existing data for device checks.

## Pending

- Owner manual test of final credential build: Edit/network actions, every gesture, reactions/picker, Experimental Glass visual quality, themes, in-place update.
- No Stable 1.3.0, no Desktop, no data clearing, no destructive account actions.
- Beta 4 is the updater/credential hotfix. If the new build has issues, fix in Beta 5; do not rewrite Beta 3/Beta 4 tags/assets.
