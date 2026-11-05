# BerryGram Android 1.3.0 Beta 2 — result

Branch: `dev/1.3.0`. Source/tag: `b1ce28e55e9755b95631cfe009e21f01156aea3b`. Published pre-release `v1.3.0-beta.2`.

## P0-1 Gesture engine — fixed and verified

- Files: `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`, `fork/actions/ActionSettings.java`, `fork-tests/test_gesture_dispatch.js`, `fork-tests/test_gesture_native_path.js`.
- Root causes found: explicit actions on message types ≥2 never reached native option construction (`createMenu` gate); reaction gestures re-entered the double-tap callback; missing message/owner/account validation could act on a recycled/mismatched cell; unavailable actions could be swallowed without feedback; selected state could leak; swipe tracking read SharedPreferences on every move.
- Fix: native option gate includes `forkRequestedAction != null`; message/owner/account/date/sending/editing validation; one-shot re-entry guard; direct double-tap reaction helper; unavailable bulletin; selected-state cleanup; per-account/user gesture cache.
- Verification: production `forkHandleGesture` fake-host test and extracted native gate/initialization/reply/forward/copy-path test passed. Device touch timing remains owner-tested.

## P0-2 Reaction engine — fixed and verified

- Files: `org/telegram/ui/Components/ReactionsContainerLayout.java`, `fork/actions/ReactionFilterPolicy.java`, tests.
- Root causes found: favorites were rebuilt through a nested scan, could yield an empty panel, and could alias the mutable visible list; layout params were assumed non-null.
- Fix: pure native-index policy, stable order, duplicate suppression, empty-result fallback to the native allowed list, defensive copy, null layout-params guard, expander only when genuinely narrowed. Full native picker retained.
- Verification: 0..4 favorites, disallowed/all-disallowed, duplicates, reorder, empty native list, fallback, narrowed flag, source integration checks.

## P1 Performance Engine — implemented

- `PerformancePolicy`/`PerformanceSettings` profiles: Automatic, Maximum smoothness, Battery saver, Custom.
- Performance Center in BerryGram Settings with non-sensitive diagnostics.
- Runtime reduction feeds `ForkGraphicsSurface`, which already gates optional glass and reveal animations. Telegram Lite Mode storage and native rendering are not replaced.
- No FPS, thermal or battery benchmark claims.

## Release verification

- Full `fork-tests/smoke.ps1` passed.
- Debug Java compile and Release/R8 passed (8m28s).
- APK `BerryGram-1.3.0-beta.2-arm64-v8a.apk`, SHA-256 `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`.
- Package `org.example.forkclient`; version `1.3.0-beta.2`; code `72599`; minSdk 23; ARM64; non-debug/non-testOnly; permanent certificate; v1/v2 signatures; 16 KB alignment; source revision embedded; native libs identical to Beta 1; public unauthenticated download matched.
- GitHub latest Stable remains 1.2.0; Beta 2 is a pre-release. No Stable 1.3.0 or Desktop work started.

## Remaining

Owner manual device acceptance and subsequent fixes. No physical phone, real account or crash log was used.
