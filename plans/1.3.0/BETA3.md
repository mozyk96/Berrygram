# BerryGram Android 1.3.0 Beta 3 — execution plan

Branch: `dev/1.3.0`. Baseline: Beta 2 `b1ce28e` (published). Owner device findings are P0. Preserve Beta 1/Beta 2 tags, Stable 1.2.0, signing identity, package ID, minSdk 23 and existing user data.

## P0 — Stability

### P0-1 Gesture engine / Edit crash
- Files: `ChatActivity.java` (`forkHandleGesture`, `forkPlainMessageBody`, `createMenu`, `processSelectedOption`, `startEditingMessageObject`, `fillMessageMenu`), `ActionRegistry`, `ActionSettings`, `ForkGestureSettingsActivity`, `fork-tests/test_gesture_dispatch.js`, `fork-tests/test_gesture_native_path.js`.
- Evidence to obtain: real Logcat stack on owner phone; reproduce tap→Edit on own editable text/media, received message, channel/group, saved messages, scheduled, old message; native/default.
- Suspected causes to verify: generic action-unavailable path for non-editable messages; stale/lost `selectedObjectToEditCaption`; `createMenu` being used as an action builder; synchronous `processSelectedOption` re-entry; activity/fragment lifecycle during edit; different routing for `tap` vs long press; menu editor hiding/pruning the edit tuple.
- Required: separate gesture dispatch from menu presentation; resolve the exact clicked `MessageObject`; use native menu option eligibility; execute with a scoped selected-object snapshot and restore/clear deterministically; never synthesize taps recursively; accurate unavailable explanation; no crash in native/default.
- Tests: production native-path harness for edit/reply/forward/copy/pin/delete/save/reaction/menu/none/native; account/message validation; repeated gestures; selection mode; editable and non-editable matrices.

### P0-2 Lock screen black background
- Files: `PasscodeView.java`, `PasscodeViewDialog.java`, `LaunchActivity.java`, `Theme` wallpaper selection, BerryGram graphics/performance surfaces.
- Evidence: lock from chat list, chat, settings, media; light/dark themes; wallpaper/gradient/pattern/default; app restart.
- Suspected: dialog transparent window + missing cached wallpaper/gradient; graphics fallback drawing opaque black; performance profile suppressing effect without theme-correct fallback.
- Required: theme-correct opaque/blurred fallback identical to Telegram's secure lock presentation; never show sensitive underlying chat content; preserve FLAG_SECURE and passcode security; no black fallback.

### P0-3 Other confirmed crashes
- Any stack traces from ADB; otherwise targeted native-path tests and static audit of changed code.

## P1 — Functional corrections

### P1-1 Remove unwanted Material appearance
- `ForkGraphicsSurface.materialColor` currently returns Android dynamic system neutral colors, causing gray/green surfaces. Replace with Telegram theme colors; remove user-facing Material residue and `ForkGraphicsMaterial`; migrate saved `style=material` to `blur`; keep blur/liquid. Verify light/dark/custom accent.
- Existing internal opaque fallback may remain only as a theme-derived, non-user-facing fallback.

### P1-2 Performance Center responsiveness and functionality
- Rewrite `ForkPerformanceSettingsActivity` to build rows once and update values in place; no `removeAllViews()` on every click; no full header refresh; cache resolved profile/device state; diagnostics noninteractive styling.
- Every control writes a preference with a real reader and observable effect: profile, custom reduce effects, optional animation/reduced-motion toggle, reset. Remove rows with no implementation.
- Add ability to navigate to native Telegram Lite Mode rather than duplicating it.

### P1-3 Theme integration audit
- Audit `ForkSettingsLayout`, `ForkFloatingHeader`, `ActionBarPopupWindow`, `ActionBarMenuItem`, `ItemOptions`, `PasscodeView`, `ForkPerformanceSettingsActivity` for hardcoded colors/black fallbacks/green tints.

## P2 — Performance architecture
- Research official Android/Telegram mechanisms (`LiteMode`, profile installer, baseline profiles, JankStats/FrameMetrics, thermal APIs).
- Measure startup, settings navigation, chat scroll and gesture dispatch with available device/emulator; no fabricated numbers.
- Implement safe, bounded caching/config reuse where a measured or clear redundant cost exists (gesture prefs already cached; add account-safe caches for derived graphics/profile/theme state).
- Defer nonessential startup work only where safe; never defer auth/database/messaging/security.
- Evaluate and document baseline/profile tooling; do not fake profiles.
- Adaptive modes must map to real optional effects/animations; system power saver and API-level guards.

## P3 — Experimental glass
- New optional style `experimental_glass` alongside `blur` and `liquid`.
- Use Telegram's existing blur/glass infrastructure where possible; no full-screen capture per frame, no synchronous heavy blur.
- API-gated quality: hardware-assisted on supported devices, reduced complexity, theme-aware opaque fallback. No crash/black on API 23.
- Apply to fork surfaces initially (Mini Dock, settings cards, popup panels). Clearly experimental; one-tap fallback.
- Test light/dark/custom accent, power saver, low-end, background/resume, orientation, profile switching.

## P4 — Verification
- Unit tests: profiles/persistence, gesture policy/native path, reaction filter, graphics fallback/migration, performance cache.
- Integration tests: gesture→native action, menu tuple preservation, reaction UI, theme/graphics state, passcode lifecycle.
- ADB on owner phone only after explicit authorization: install over existing app, collect filtered crash logs, frame timing where supported. No data clearing/logout/destructive actions.
- Full smoke suite and Release/R8.

## P5 — Beta 3
- Version `1.3.0-beta.3`, base code `7260` → ARM64 code `72609` (> Beta 2 `72599`).
- Verify package, certificate, minSdk 23, ARM64, non-debug/non-testOnly, source revision, SHA-256, secrets, updater continuity.
- Publish pre-release `v1.3.0-beta.3` only after valid signed artifact; owner testing instructions; no Stable/Desktop.
