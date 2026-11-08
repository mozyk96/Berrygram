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
