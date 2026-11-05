# Example: Feature Plan

Scenario: add an opt-in setting that makes the fork settings cards use a denser gap.
This is a small feature, but the shape applies to any BerryGram addition.

## 1. Frame

- **Outcome:** when "Compact cards" is on, fork settings cards use `gap / 2`.
- **Seam:** `ForkPanelSettings` (preference) + `PanelPolicy` (pure math) +
  `ForkSettingsLayout` (apply).
- **Hook:** `ForkSettingsLayout.refreshPage(...)` / card construction.
- **Target evidence:** `PanelPolicyTest` for the math (`TESTED`); layout density
  `MANUAL TEST REQUIRED`.

## 2. Search before building

```text
grep "settingsGapDp"      → PanelPolicy, ForkSettingsLayout
grep "fork_panels"        → ForkPanelSettings, ForkChatHeaderSettings, ForkDockSettings
grep "ForkSettingsLayout" → settings screens
```

If `settingsGapDp()` already has a caller that can vary, extend it instead of adding a
parallel path.

## 3. Preference

In `ForkPanelSettings`:

- key: `compact_cards`
- default: `false` (preserves upstream/fork default)
- device-local, cached with invalidation on write, following the
  `ForkChatHeaderSettings.cached` pattern.

## 4. Policy

```java
public static int settingsGapDp(boolean compact) {
    return compact ? 3 : 6;
}
```

Keep it pure so it is JVM-testable. Do not read Android preferences inside the policy.

## 5. Apply

In `ForkSettingsLayout`, read the preference once per page build and use the policy value
for padding. After a toggle, call `ForkSettingsLayout.refreshPage(page)` — the same call
`ForkSettingsActivity.onResume` already makes.

Do not:

- add a second layout class,
- hardcode `3`,
- apply the gap only on the next app start if the toggle implies immediate effect.

## 6. Test

Add to `fork-tests/PanelPolicyTest.java`:

```java
check(PanelPolicy.settingsGapDp(false) == 6);
check(PanelPolicy.settingsGapDp(true) == 3);
```

Run:

```powershell
pwsh -NoProfile -File fork-tests/smoke.ps1
```

Result: `TESTED` for policy; `CODE VERIFIED` for the call site; `MANUAL TEST REQUIRED` for
visual density, night theme, and font scaling.

## 7. Manual checklist

- Toggle on → cards visibly tighten without clipping text.
- Toggle off → returns to the original spacing.
- Rotate → spacing preserved.
- Night theme + 1.3x font scale → no overlap.
- TalkBack → toggles still announce state.

## 8. Review

- Diff touches `ForkPanelSettings`, `PanelPolicy`, `ForkSettingsLayout`, one test.
- No upstream file changed.
- No secrets, no absolute paths, no build artifacts.

## 9. Handoff

```md
- Branch: feat/compact-cards
- Last commit: <sha> feat(settings): add compact card spacing
- Tests: smoke.ps1 → pass (PanelPolicyTest included)
- Manual: density/theme/font-scale MANUAL TEST REQUIRED
- Next: emulator visual check, then review
```
