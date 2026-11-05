# BerryGram Android 1.3.0 Beta 2 — actionable plan

Branch: `dev/1.3.0`. Baseline: `9118a39` (Beta 1 published). Owner findings are P0 and override cosmetic work.

## P0-1 Gesture engine

- Files: `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`, `fork/actions/ActionSettings.java`, `fork-tests/test_gesture_dispatch.js`.
- Evidence: dispatching a gesture re-entered `createMenu`; a reaction gesture re-entered `onDoubleTap`; unavailable actions could consume the event without feedback; tap used the wrong `single` menu flag. Re-entry and stale `forkRequestedAction` could dispatch twice or execute against a second, incomplete menu build.
- Fix: one dispatch path with an explicit re-entry guard, native tap/long-press flags, capture-and-clear of `forkRequestedAction` before `processSelectedOption`, direct double-tap reaction helper (no recursive callback), visible unavailable bulletin when menu construction exits early.
- Verify: production `forkHandleGesture` compiled against a fake Android host; native option IDs; re-entry; local coordinates; action available/unavailable; long-press flags. Device timing remains owner-tested.

## P0-2 Reaction engine

- Files: `org/telegram/ui/Components/ReactionsContainerLayout.java`, `fork/actions/ReactionFilterPolicy.java`, `fork-tests/ReactionFilterPolicyTest.java`, `fork-tests/test_reaction_integration.js`.
- Evidence: filtered favorites were rebuilt with a nested scan and could produce an empty/short panel while `showExpandableReactions`/full-picker state was mutated late; callers could alias the mutable `visibleReactionsList`.
- Fix: pure index policy over the native allowed list, stable order, duplicate suppression, empty-result fallback to native, defensive copy, expander state only when the panel is genuinely narrowed. Native list stays authoritative for the full picker.
- Verify: 0..4 favorites, disallowed/all-disallowed, duplicates, reorder, empty native list, fallback, narrowed flag; source integration checks.

## P0-3 Regression checks

- Message menu tuples/More/delete suppression; per-account/user settings; settings persistence; native fallbacks.
- Smoke tests that do not require a real Telegram account. Python fixtures need a local Python interpreter; report the gap if unavailable.

## P1 Performance engine (only after P0 verification)

- Keep Telegram Lite Mode as the storage/behavior base; do not build a parallel renderer.
- Implement a device-local performance profile with real effects on supported flags, an explicit opt-in for changing Telegram Lite Mode presets, safe defaults, and a Performance Center entry that exposes only working controls.
- No forced FPS, no thermal polling loops, no telemetry, no permanent preference changes for temporary battery-saver state.

## Release gate

Version `1.3.0-beta.2`, code `72599` (Beta 1 was 72589), ARM64, minSdk 23, same package and signing identity. Run smoke + Release/R8, verify APK metadata/signature/checksum, then publish a GitHub pre-release only if valid. Owner device acceptance remains mandatory.
