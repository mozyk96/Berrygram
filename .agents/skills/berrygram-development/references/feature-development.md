# Feature Development

Scope: how to add a BerryGram feature end to end. Use this as the default workflow for
any new setting, surface, or behavior.

## Step 0 — Frame the feature

Write four lines before touching code:

1. **User-visible outcome.** What changes on screen or in behavior.
2. **Isolation seam.** Which `Fork*` class will own it.
3. **Upstream hook.** The smallest file(s) that must call the fork code.
4. **Acceptance level target.** What can be `TESTED`, and what will remain
   `MANUAL TEST REQUIRED`.

If you cannot name the hook, the feature is not ready.

## Step 1 — Prove there is no existing mechanism

Search before building:

- `ForkSettingsActivity`, `ForkAppearanceSettingsActivity`, `ForkSettingsLayout` for an
  existing entry.
- `org/telegram/messenger/fork/panels` for an existing policy/preference.
- Upstream `Theme`, `SharedConfig`, `MessagesController` for an existing supported path.

Duplicating an upstream mechanism is a defect. BerryGram wraps or gates; it does not
fork the chat engine for a UI preference.

## Step 2 — Choose the seam

Preferred order:

1. Pure policy class in `org/telegram/messenger/fork/...` (unit-testable without Android).
2. Fork `View`/helper in `org/telegram/ui/Fork*.java`.
3. Narrow hook in an upstream file (a few lines, guarded by a fork check).
4. Last resort: a branch inside an upstream method. Justify it in the commit message.

Never copy an upstream class into the fork to modify it.

## Step 3 — Preference design

- Add the key to the owning settings class, not to an unrelated one.
- Cache only if the read is hot, and invalidate on write (see
  `ForkChatHeaderSettings.cached`).
- Default must preserve upstream behavior: the feature is opt-in unless product says
  otherwise.
- Device-local vs per-account must be explicit in a comment.
- A preference without a reader is incomplete.

## Step 4 — Implementation checklist

- Rendering reads policy, not raw constants.
- Every new dimension goes through `PanelPolicy`.
- Every new color goes through `Theme`/`GraphicsColorPolicy`.
- Lifecycle: create in `createView`/constructor, release in destroy/dispose.
- Insets applied once.
- Accessibility labels present.
- Reduced-motion and power-saver paths defined.
- Fallback defined for API < 33 and incapable devices.

## Step 5 — Tests

Add the narrowest meaningful test:

- Pure logic → JVM test in `fork-tests` and register it in `fork-tests/smoke.ps1` if it is
  part of the smoke set.
- Geometry → test in `fork-tests` (see `HeaderGeometryTest`, `FloatingChatHeaderGeometryTest`,
  `BitmapSourceGeometryTest`).
- Lifecycle → test alongside `LiquidGlassSourceLifecycleTest` if it touches sources.
- Rendering/insets/touch/installer → manual checklist, marked `MANUAL TEST REQUIRED`.

## Step 6 — Regression surface

Ask what the hook can break:

- Search on/off, action mode, folder switch, archive.
- Stories present/absent.
- Multi-account switch.
- Day/night theme, custom accent.
- Power saver on/off, low-RAM device.
- Rotation and cutout.
- Back navigation and fragment recreation.

Run only the relevant subset; do not run a full matrix for a string change.

## Step 7 — Review your own diff

- Only intended files changed.
- No formatting-only churn in upstream files.
- No debug logs with user data.
- No secrets, no absolute local paths, no generated artifacts.
- Commit message states behavior and evidence level.

## Step 8 — Handoff

Record: branch, commit, files, tests run and their result, manual gaps, next action.

## Worked example

`examples/feature-plan.md` shows this applied to a new settings toggle.
