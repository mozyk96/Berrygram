# Debugging

Scope: finding the real cause of a BerryGram defect. Diagnose from code paths and
observable behavior; never guess a cause and present it as confirmed.

## Ten-step method

1. **Restate the symptom** in observable terms: screen, action, expected vs actual.
2. **Find a reproduction path** (or state that none exists).
3. **Name the components** that must be involved.
4. **Trace the execution path** from the triggering event to the drawing/layout code.
5. **Confirm the cause** with a code fact (a call site, a missing call, a wrong value).
6. **Apply the minimal safe fix** at the correct seam.
7. **Add a targeted test** for the logic you changed.
8. **Check adjacent states** (search, action mode, archive, stories, theme, account).
9. **Run regression checks** proportional to the change.
10. **Document** cause, fix, evidence level, and residual risk.

If step 5 fails, you are still investigating. Do not ship a speculative change.

## Instrumentation

Prefer non-invasive evidence:

- Targeted search for the class/method and its call sites.
- `git log -p -- <path>` and `git diff` to see when a behavior changed.
- Temporary logging is acceptable, but remove it before commit; never log user content.
- Layout inspection with Android Studio's Layout Inspector on an emulator.
- `adb logcat` filtered to the app's process; do not capture unrelated personal data.

## Case templates

### 1. Glass/background disappears after search

Trace: `DialogsActivity` search mode → `progressToActionMode`, `progressToDialogStoriesCell`,
`scrollYOffset`, `getReservedSearchHeight()` → header/style application via
`ForkGraphicsSurface.effectiveStyle()`.

Check, in order:

- Is the style re-applied after leaving search, or only on create?
- Is the surface alpha/translation updated in the same pass as the upstream list?
- Was a clip applied for search and never removed?
- Did the view get re-attached without re-attaching its blur source?

Confirmed cause must name the specific missing call or stale value.

### 2. Wrong ActionBar/header height

Trace: the header wrapper → the list padding/top offset → `DialogStoriesCell.HEIGHT_IN_DP`
if stories are involved.

Check:

- Is the upstream ActionBar still measured while a fork header is shown?
- Is the top inset applied twice?
- Is a spacer left behind after hiding the ActionBar?
- Does the value use `dp(...)` rather than a pixel constant?

### 3. Stories invisible

Trace: `hasStories` → `DialogStoriesCell` creation
(`new DialogStoriesCell(context, this, currentAccount, TYPE_ARCHIVE|TYPE_DIALOGS)`) →
layout height `DialogStoriesCell.HEIGHT_IN_DP` → `HeaderGeometry.storiesWidth(...)`.

Check:

- zero width from the geometry helper,
- alpha still 0 after a transition,
- z-order (drawn under the list or under a fork surface),
- parent clip,
- empty story list for the account.

### 4. Archive clipped

Trace: archive holder layout and scroll offsets in `DialogsActivity`
(`getMaxScrollYOffsetWithoutSearch()`, `scrollYOffset`, `translateListHeight`).

Check:

- stories height added twice or subtracted,
- negative padding,
- clip rect left over from search,
- `progressToActionMode` math,
- archive with and without stories.

### 5. Settings toggle has no UI effect

Trace: the control's write → the preference key → every reader of that key.

Check:

- a reader exists at all (`grep` the key),
- the reader is reached in the current screen's lifecycle,
- a cached value is not stale,
- the setting is not gated by an SDK/power/capability condition that is always false,
- the change is not applied only on the next app start when the UI implies immediate effect.

### 6. Popup at wrong coordinates

Trace: anchor measurement → `PanelPolicy.clampPopupOffset(anchorX, offset, popupWidth,
left, right, margin)` → window bounds.

Check:

- clamp applied to the wrong reference point (absolute vs anchor-relative),
- popup width estimated before measurement,
- RTL / inset / cutout offsets,
- clamp margin using the wrong unit.

## Evidence discipline

- `TESTED` requires a named test that ran. Paste the command and result.
- `CODE VERIFIED` requires a cited call chain.
- UI rendering, touch, insets, animation smoothness, installer behavior, and GPU cost are
  `MANUAL TEST REQUIRED` unless actually exercised.
- If you cannot reproduce, say so and describe what evidence would confirm the cause.

## Anti-patterns

- Changing several unrelated things and declaring the bug fixed.
- Adding a null check that hides the real state bug.
- "Fixing" by disabling the feature.
- Copying a fix from another screen without tracing that screen's layout.
- Reporting success after a compile.
