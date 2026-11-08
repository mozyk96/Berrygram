# Android 1.4.0 RC 1 — implemented, release candidate built (device test pending)

## Repository and branch

- Repository `C:\Users\Keoo2\Documents\BerryGram-1.3`, branch `feature/berrygram-1.4.0-stable`,
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
  `\'` inside unquoted Android strings; this breaks the release resource merge and was hidden by
  the build cache. A new `test_fork_string_escapes.py` prevents a recurrence.

## Verification

- `:TMessagesProj:compileDebugKotlin` and `compileDebugJavaWithJavac` — success.
- Full `fork-tests/smoke.ps1` — success, including the new `ForkArchivedRestorePolicyTest`, the
  extended `StealthPolicyTest` and the release candidate ordering checks in `UpdatePolicyTest`.
- `fork-tests/test_berrygram_resources.py`, `fork-tests/test_release_scope.py`,
  `fork-tests/test_fork_string_escapes.py` — success.
- Release arm64 build with R8 and locally injected credentials — see the artifact line below.

## Release candidate artifact

- APK `C:\Users\Keoo2\Documents\BerryGram-1.4.0-rc.1-arm64-v8a.apk`, 41918027 bytes,
  SHA-256 `9a0eaf3de94456947aabcabe334fde20b3736791d34460d3d0a4ac97768043e8`.
- Package `org.example.forkclient`, versionName `1.4.0-rc.1`, versionCode `72749`, minSdk 23,
  targetSdk 36, native-code arm64-v8a only, `POST_NOTIFICATIONS` declared, label BerryGram.
- Signed with the existing release key: `CN=BerryGram Release`, certificate SHA-256
  `b3ea9ca232ae03ebeab5bbc2729a306fb5eacd0520104fc52450a421efc722a5` (identical to the published
  Stable, so the candidate installs over Stable 1.3.0 and over Beta 1/Beta 2 without data loss).
  v1 and v2 signatures verified, `zipalign -c -P 16` passed, credential present in the release dex,
  no keystore packaged.
- The candidate is NOT published. The owner tests it with `docs/1.4.0-rc.1-device-checklist.md`.

## Known limitations

- Remote push still needs the owner's Firebase project; without it Telegram delivers notifications
  only while the application process is alive. The diagnostics screen reports
  `push_not_configured` and now contains the exact setup steps.
- No physical device test of this build yet: the emulator cannot start on this host and no device
  is connected over ADB. The owner receives the release candidate and the checklist
  `docs/1.4.0-rc.1-device-checklist.md`.
- The archive still only contains messages that were seen on this device; a full historical
  import is planned for 1.4.1.

# Android 1.4.0 Beta 2 — release prepared (published as v1.4.0-beta.2)

## Recovery

- The previous agent worked from the Stable 1.3.0 source and rebuilt a partial Berry Privacy system.
  The authoritative 1.4.0 line was recovered from the upstream repository: tag `v1.4.0-beta.1`
  → commit `7fd3022931020a0d187ea32dbbe22f3204e70069` (`2e99e6f` Beta 1 implementation, `4167fea` updater fix).
- The local copy `…\outputs\telegram-fork` contained additional unreleased 1.4.0 work that never reached
  the remote: notification diagnostics (`NotificationDiagnosticsPolicy`, `ForkNotificationDiagnosticsActivity`,
  `PushListenerController` Firebase state), `MenuSurfacePolicy` with the message-menu/reaction surfaces for
  No Effects / Blur / Liquid Glass, and the spring based bottom navigation indicator. All of it was restored.
- Branch `feature/berrygram-1.4.0-beta2` starts from `7fd3022`; the previous reconstruction is preserved in
  branch `feature/berrygram-1.4.0` and checkpoint commit `be8a751`.

## Beta 2 changes

- Ghost reading no longer suppresses an explicit user "Mark as read": `ReadTask.force`,
  `markDialogAsReadForced` (dialogs list and notification/Auto "heard" action) and
  `StealthPolicy.shouldSuppressRead(..., forced)` with regression tests.
- Privacy settings are isolated per account (`fork_stealth_<account>_<userId>`) with automatic migration from
  the legacy global `fork_stealth` file.
- Local archive, deleted-message markers and edit history are encrypted at rest with an account bound
  Android Keystore AES-GCM key (`ForkArchiveCrypto`); legacy plaintext rows are migrated on first open.
- Archive retention 7/30/90 days or forever with cleanup on app start (`ForkStorageMaintenance`) and on demand.
- Version metadata: `1.4.0-beta.2`, `APP_VERSION_CODE=7273` → arm64 versionCode `72739`
  (published Beta 1 is `72719`, the earlier unpublished Beta 2 debug build was `72729`).

## Verification

- `:TMessagesProj:compileDebugKotlin` + `compileDebugJavaWithJavac` — success.
- `:TMessagesProj:compileReleaseKotlin` + `compileReleaseJavaWithJavac` — success.
- Full `fork-tests/smoke.ps1` — success (includes Stealth, Device Analyzer, Menu Surface,
  Notification Diagnostics, main-tabs indicator and message-menu integration checks).
- Release ARM64 build with R8 and locally injected credentials — see below.
- APK: `org.example.forkclient`, versionName `1.4.0-beta.2`, versionCode `72739`, minSdk 23, targetSdk 36,
  native-code arm64-v8a only, `CN=BerryGram Release` certificate SHA-256 `b3ea9ca2…c722a5` (identical to the
  published Stable, so in-place update works), v1+v2 signatures, `zipalign -c -P 16` passed,
  POST_NOTIFICATIONS declared, no keystore packaged.

## Known limitations (also in the release notes)

- Remote push (FCM) stays unavailable: there is no BerryGram-owned Firebase project
  (`google_app_id`, `gcm_defaultSenderId`, `project_id`, `google_api_key`). The diagnostics screen reports
  `push_not_configured` honestly; a local test notification is not proof of remote push.
- No physical-device test of this exact build: login, incoming messages, notifications, Ghost Mode,
  deleted-message rendering, edit history, archive, navigation, appearance modes and the in-app update
  still require owner verification.


# Android 1.4.0 Beta 1 completed — 2026-11-06

- Branch: feature/berrygram-1.4.0; version 1.4.0-beta.1, code 7271.
- Ghost Mode (Stealth Reading): read receipt suppression in MessagesController.completeReadTask, separate toggles for private chats and groups, strict preservation of secret chats / self-destructing media, dedicated ForkStealthSettingsActivity.
- Deleted Messages (AyuGram-style): message retained in-place in ChatActivity.processDeletedMessages, bubble alpha reduced to 38% in ChatMessageCell, crisp readable text, trash icon prepended to timestamp, SQLite storage in ForkDeletedMessagesStorage.kt.
- Message Edit History: local SQLite revisions in ForkEditHistoryStorage.kt, 'Історія змін' context menu item in ChatActivity, revision cards bottom sheet ForkEditHistoryBottomSheet.kt.
- Berry Archive: local SQLite archive in ForkArchiveManager.kt, full-text search, dialog browser ForkArchiveActivity.kt, message history ForkArchiveMessagesActivity.kt, survives Telegram cloud deletion.
- Message Translator: 1-tap translation enabled by default in TranslateController, native TranslateAlert2 integration, Ukrainian UI.
- Glass 2.0 & Unified 3 Visual Modes: Glass 2.0, Blur, No effects in ForkAppearanceSettingsActivity, obsolete intensity sliders, accent color pickers, and legacy device adaptation checkboxes removed.
- Device Analyzer: hardware detection in ForkDeviceAnalyzer.kt, corrected Samsung Galaxy A25 (SM-A256B) tier assessment to Medium / Low-Medium due to mid-tier Mali-G68 GPU under 120Hz display, interactive 'Сканувати пристрій' button in ForkPerformanceSettingsActivity.
- Performance Center 2.0: Auto vs Manual profiles (Low, Medium, High) independent of visual modes, manual selections strictly respected.
- Advanced Animations 2.0: continuous spring-animated shared tab indicator in MainTabsLayout, dynamic elongation, selector blinking eliminated.
- Kotlin Transition: all new fork features, storage, and settings written in Kotlin with clean Java interoperability.
- Built-in Updater: fixed install trigger in ForkUpdaterPanel (calling install() instead of re-downloading), safe intent consumption, ARM64 and Universal asset detection, distinct SignatureMismatchException for certificate conflicts, Stable channel as default.
- Verification: full smoke suite passed (smoke.ps1), test_berrygram_resources.py passed, test_release_scope.py passed, assembleAfatRelease ARM64 APK built and verified with permanent release key (code 72719, SHA-256 ccef7ca8ccc1efa7187aaeb90e251178a481b67e7a34fac26e89752baf632eee).

# Android 1.3.0 Beta 5 published — 2026-11-06

- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.5
- Source/tag `93794233dffb3ff53622792a023718124ee8e18d`; version `1.3.0-beta.5`, code `72629`.
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.5/BerryGram-1.3.0-beta.5-arm64-v8a.apk
- SHA-256: `dacc6aac28e214e14c9c0e31eb575f92069703a22c6e4d85eec44f57b0f81d6f`.
- Experimental Glass 2.0: independent transparency, blur, frost, saturation, edge/highlight, shadow, tint and radius; presets; live preview; popup/Mini Dock/reaction coverage with fallback.
- Embedded round-video preview with two circular icon-only controls and circular live preview; separate test-screen entry removed.
- Adaptive Camera Engine: Camera2 capability discovery, Automatic/Quality/Balanced/Performance/Custom profiles, JPEG quality, round-video bitrate/FPS limits, validated Camera2 FPS ranges and advertised OIS/EIS.
- Voice Engine: Standard/Automatic/Clear/Natural/Custom profiles, platform NS/AGC/AEC when available, source selection, profile-aware Opus bitrate and a local record/play/delete test tool.
- Verified: full smoke (including new Glass/Camera/Voice policy tests), Debug compile, Release/R8, APK identity/signature/ABI/minSdk/flags/alignment/source/API credentials, public download digest. Latest Stable remains 1.2.0.
- Not verified: physical-device behaviour of this exact APK. The owner's phone was not connected over ADB, and the local emulator could not start because the Windows Android Emulator hypervisor driver is not installed.
- Post-release branch commit `533d604` stops the embedded camera preview when scrolled out of view; it is not included in the published Beta 5 APK.

# Android 1.3.0 Beta 4 published — 2026-11-06

- Hotfix over Beta 3: version `1.3.0-beta.4`, code `72619`, source/tag `7bf93d3cff1490f93b9f67032a08819669974b55`.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.4
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.4/BerryGram-1.3.0-beta.4-arm64-v8a.apk
- SHA-256: `4e10c9f3a43604fe039f00205c77e817d85218867f50bfcf5e154bfb5659c604`.
- Reason: the Beta 3 phone test build already had version `1.3.0-beta.3`/code `72609`; the updater correctly saw the published Beta 3 as already installed. Beta 4 is a normal version bump and carries all Beta 3 fixes plus the restored API credentials.
- Verified: Debug/Release/R8, smoke, APK identity/signature/ABI/minSdk/flags/source/native-lib/alignment/API credentials, GitHub digest and independent public download. Updater release list now starts with Beta 4; latest Stable remains 1.2.0.
- Final Beta 4 APK was not installed before publication; owner should update from the in-app updater and verify network Edit, gestures, reactions, Experimental Glass and themes.

# Android 1.3.0 Beta 3 published — 2026-11-06

- Branch `dev/1.3.0`; source/tag `ec5517afa8147fd9688be2904b0bc19112ec8e0c`.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.3
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.3/BerryGram-1.3.0-beta.3-arm64-v8a.apk
- SHA-256: `5700cbd0c209645097089129af4bb7db516f7eccf40788a397b051b8b7d69323`.
- Device-confirmed: Beta 2 menu crash (`addViewToSwipeBack` NPE) fixed; lock-screen themed background normal; Performance Center responsive; Experimental Glass selectable. Final credential-bearing APK not installed before publication at owner request.
- Fixed empty Beta 2 API credentials (`CONNECTION_API_ID_INVALID`), removed dynamic-neutral Material tint, migrated `material`→Blur, fixed Ukrainian/Russian mojibake labels, added passcode wallpaper fallback, cached preferences, thermal/low-RAM awareness, and optional Experimental Glass.
- Smoke, Debug compile, Release/R8, APK identity/signature/ABI/minSdk/flags/source/native-lib/SHA/alignment checks passed. No Stable 1.3.0; no Desktop; no data clearing or destructive account actions.
- Owner manual acceptance remains pending, especially network Edit after credential restoration, all gestures, reactions, Experimental Glass visual quality, themes and in-place update.

# Android 1.3.0 Beta 2 published — 2026-11-05

- Branch: `dev/1.3.0`. Source/tag commit: `b1ce28e55e9755b95631cfe009e21f01156aea3b`.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.2
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.2/BerryGram-1.3.0-beta.2-arm64-v8a.apk
- SHA-256: `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`. Public unauthenticated download and GitHub asset digest matched.
- Fixed gesture dispatch (native option gate for media types, account/message validation, single execution, no recursive reaction callback, unavailable feedback, selected-state cleanup) and quick-reaction filtering (native-index policy, order/dedup, empty fallback, defensive state). Added performance profiles/Performance Center and gesture preference cache.
- Full smoke suite passed; debug Java compilation and Release/R8 passed (8m28s). Package `org.example.forkclient`, code `72599` > Beta 1 `72589`, minSdk 23, ARM64, non-debug/non-testOnly, existing permanent certificate, v1/v2 signatures, 16 KB ZIP alignment, source revision and native-library identity to Beta 1 verified.
- Live GitHub data: Beta 2 is a pre-release; `releases/latest` remains Stable 1.2.0. No Stable 1.3.0 published; Desktop remains gated.
- No physical phone, real Telegram account or crash log was used. Device touch/reaction/menu/account/restart behavior and in-place installation remain owner manual tests. No measured FPS/battery/thermal claim.

# Android 1.3.0 Beta 1 published — 2026-11-04

- Branch: dev/1.3.0. Built and tagged source: 0ce1f164567809050a30f7ec132752b90c5f11f3.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.1
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.1/BerryGram-1.3.0-beta.1-arm64-v8a.apk
- SHA-256: 0a31242c7df6466cfe1408a0f3a3106a26721b174eba9849b2c3077090146c07.
- Implemented shared action registry, account/user-isolated gestures, reaction editor and message-menu editor. Hidden menu actions retain native cells, warnings and confirmations in scrollable More.
- Smoke tests passed; Release/R8 passed in 7m45s. Final APK identity, code72589, min23, ARM64, flags, ZIP/alignment, permanent signer/API23, source revision, resources and public checksum verified.
- Git packed refs caused AGP NO_VALID_GIT_FOUND. Re-extraction from the same detached commit and incremental re-signing fixed metadata (39s; no runtime change). Development branch restored.
- Live current/legacy Updater checks: Beta selects1.3.0-beta.1; Stable and Latest remain1.2.0. No older artifact overwritten.
- Manual device/account/touch/reaction/menu/restart/accessibility/installation acceptance remains pending. Plain-text tap overrides exclude interactive/formatted targets; special reaction panels stay native. No phone or account automation performed.
- No active build. Next: owner tests Beta1, then reproduce/fix findings. Android Stable1.3.0 requires owner approval; Desktop implementation remains gated until its publication. Plans: plans/1.3.0.

# BerryGram1.2.0 Stable completed — 2026-11-04

- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.2.0 (published, non-draft, non-prerelease, Latest).
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.2.0/BerryGram-1.2.0-arm64-v8a.apk
- Exact source/tag: 60ea4ddeb16e7119c310f8beb4e29d0b7348fbb6. Runtime trees are identical to owner-tested Beta9 a41d059; only version metadata/docs/tests changed.
- versionName1.2.0; code72579 > Beta9 72569; org.example.forkclient; Android6+/minSdk23; ARM64-only. Permanent certificate unchanged: b3ea9ca232ae03ebeab5bbc2729a306fb5eacd0520104fc52450a421efc722a5.
- SHA256 e0d57f92947b52d9eb9fc4771f29afb32697c6afd91597d8cbd4d7a8b438fe25. Attached checksum, GitHub asset digest and independent unauthenticated public download matched.
- Release/R8 succeeded5m42s; manifest/resources/non-debug/non-testOnly/version/identity/API23 v1-v2 signing/ZIP/ARM64/16KB ZIP alignment/embedded source checks passed. Native libraries byte-identical to Beta9; no keystore packaged.
- Smoke and updater policy/package/download fixtures passed. Live published GitHub JSON selected Stable1.2.0 in both current channels; Beta1..9 promotion and already-installed Stable suppression passed. Older parser channel selection/semver also passed with a harness adapted to its older API (initial harness API mismatch did not require app changes).
- Changed public source/history scan0 findings; configured private values absent. No full repeated repository audit or performance benchmark.
- Owner physically tested Beta9 and approved Stable. This Stable artifact was NOT physically installed by automation. In-place update compatibility verified by identical package/cert and higher versionCode, not an actual phone install. No owner phone accessed or user data changed.
- Graphics/device capabilities and opaque fallback limits are in release notes. Existing v1 metadata warnings remain; v2 whole-APK check passes.
- No active build. Version1.2.0 is complete and published. Do not rewrite its tag/assets. Do not begin1.3.0 or other development without a new owner instruction. Logs/artifacts: work/release-120-stable and work/berrygram-120-stable.log.
