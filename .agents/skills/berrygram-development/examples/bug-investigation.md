# Example: Bug Investigation

Scenario: "After closing search, the main-header glass disappears and the header looks
flat until the app is restarted."

This example demonstrates the required evidence discipline. It does **not** assert a
confirmed cause — the cause must be proven in the actual checkout.

## 1. Symptom

- Screen: `DialogsActivity` chat list.
- Action: open search, then close it.
- Expected: header returns to glass/liquid style.
- Actual: flat material header; restart restores it.
- Reproduction: consistent on the affected device/build. Record the build commit and
  graphics mode (blur vs liquid), power-saver state, and SDK level.

## 2. Components

- `DialogsActivity` search/action-mode state.
- `ForkGraphicsSurface.effectiveStyle()`, `materialColor(...)`.
- `ForkGlassSurface.provider(...)`.
- `PanelPolicy.effectiveStyle(...)` and `ForkPanelSettings`.

## 3. Trace

1. Find where the header background is applied: search `effectiveStyle` and
   `materialColor` in `DialogsActivity`.
2. Find the search exit path: `progressToActionMode`, `progressToDialogStoriesCell`,
   `scrollYOffset`, `getReservedSearchHeight()`.
3. Determine whether style is re-applied on mode change or only on create.
4. Check whether the blur provider/drawable is detached during search and whether it is
   re-attached.
5. Check whether power saver or `LiteMode.isPowerSaverApplied()` changed during the flow.

## 4. Candidate causes (must be discriminated)

| Candidate | How to confirm |
| --- | --- |
| Style applied only at creation | No `effectiveStyle()` call on the search-exit path. |
| Blur source not re-attached | Provider returns a source whose owner was disposed during search. |
| Stale clip/alpha | View remains at post-search alpha or clip. |
| Capability gate now false | `effectsSuppressed()` true at exit, or `sdk < 33`. |
| Theme provider stale | Old `Theme.ResourcesProvider` used after theme change. |

Do not pick one because it "sounds likely". Confirm with a code fact or a runtime log.

## 5. Minimal fix

Examples of minimal, seam-correct fixes:

- call the existing style-application method from the search-exit path,
- invalidate the cached provider,
- re-attach the existing source instead of creating a second renderer.

Not acceptable:

- forcing `"liquid"` regardless of capability,
- disabling search,
- recreating the whole screen,
- adding a delayed re-apply "to make it stick" without understanding the lifecycle.

## 6. Test

- If the fix is policy logic: extend `PanelPolicyTest` (or
  `GraphicsPreferencesPersistenceTest`) and run `fork-tests/smoke.ps1`.
- If the fix is lifecycle: check `LiquidGlassSourceLifecycleTest` coverage; add a focused
  test if the class is JVM-testable.
- Visual restoration is `MANUAL TEST REQUIRED`.

## 7. Adjacent states

- Search open → close → reopen.
- Action mode enter/exit.
- Archive ↔ chats.
- Stories present/absent.
- Power saver toggled while the screen is open.
- Theme switch while the screen is open.
- Rotation.

## 8. Regression checks

Run `smoke.ps1` if any policy changed. Do not run a full APK build for a Java-only,
JVM-testable policy change unless the fix touches Android rendering.

## 9. Report

```md
- Cause: <confirmed code fact, file:line or method>
- Fix: <what changed and why it is minimal>
- Evidence: <command → result> / CODE VERIFIED / MANUAL TEST REQUIRED
- Residual risk: <states not exercised>
```

If the cause is not confirmed, report the investigation state and the next discriminating
check instead of claiming a fix.
