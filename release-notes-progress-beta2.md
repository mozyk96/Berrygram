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

