# Graphics Engine

Scope: how BerryGram renders glass, blur, surfaces, popups, and fallbacks.
This is the highest-risk area for visual regressions. Read it before touching any
`Fork*Surface`, `blur3`, `ItemOptions`, `ActionBarPopupWindow`, or header drawing.

## Verified building blocks

| Symbol | Path | Role |
| --- | --- | --- |
| `ForkGraphicsSurface` | `TMessagesProj/src/main/java/org/telegram/ui/ForkGraphicsSurface.java` | Resolves the effective style and material fallback color. |
| `PanelPolicy` | `.../messenger/fork/panels/PanelPolicy.java` | Pure policy: eligibility, radius, alpha, duration, offset clamping. |
| `ForkGlassSurface` | `.../ui/ForkGlassSurface.java` | `provider(Theme.ResourcesProvider)` → `BlurredBackgroundProvider`. |
| `ForkSurfaceAnimation` | `.../ui/ForkSurfaceAnimation.java` | Surface open/close animation factory. |
| `LiquidGlassEffect` | `.../ui/Components/blur3/LiquidGlassEffect.java` | Liquid Glass rendering entry point. |
| `LiquidGlassBitmapSource` | `.../ui/Components/blur3/source/LiquidGlassBitmapSource.java` | Bitmap-backed glass source with lifecycle. |
| `ViewGroupPartRenderer` | `.../ui/Components/blur3/ViewGroupPartRenderer.java` | Captures a view subtree for blurring. |

## Style resolution (exact semantics)

`ForkGraphicsSurface.effectiveStyle()` calls
`PanelPolicy.effectiveStyle(ForkPanelSettings.style(), effectsSuppressed(), SDK_INT, deviceAllowsEffects(!SharedConfig.deviceIsLow()))`.

`PanelPolicy.effectiveStyle` returns the requested `"liquid"` or `"blur"` **only if**
`useGlass(true, savingPower, sdk, capable)` is true; otherwise it returns `"material"`.

`useGlass` requires **all** of:

- settings enabled,
- not power-saving,
- `sdk >= 33`,
- device capable (or adaptive effects disabled).

Consequences you must respect:

- On API 23–32 the effective style is always `material`. That is intentional, not a bug.
- Power saver forces `material`; `ForkGraphicsSurface.effectsSuppressed()` is
  `systemPowerSaver() || LiteMode.isPowerSaverApplied()`.
- Low-RAM devices can be excluded through `ForkPanelSettings.deviceAllowsEffects(...)`.

`ForkGraphicsSurface.materialColor(context, fallback)` returns an Android system neutral
color on API 31+ (`system_neutral1_900` in dark theme, `system_neutral1_50` in light) and
the given fallback otherwise. It reads `Theme.isCurrentThemeDark()`, not the passed
`ResourcesProvider`, so pass a theme-derived fallback and do not expect per-screen theme
overrides from this method.

## Geometry and radius

- `PanelPolicy.popupRadiusDp(experimental)` → 26 experimental, else 12.
- `PanelPolicy.popupRadiusDp(experimental, requested)` clamps requested to 12..32 when
  experimental.
- `PanelPolicy.surfaceRadiusDp(style, requested)` uses the experimental clamp for
  `material`/`blur`/`liquid`, otherwise 12.
- `PanelPolicy.settingsRadiusDp()` → 18, `settingsGapDp()` → 6.
- `PanelPolicy.effectStrength(v)` clamps 0..100.

Use a policy-derived radius for all new code so appearance settings keep working. One
existing legacy exception is `DialogsActivity`'s main-header radius (`dp(24)`); do not copy
that pattern and do not assume every upstream site is policy-driven.

## Blur and Liquid Glass application

- `ForkGlassSurface.provider(resourcesProvider)` is the supported way to obtain a
  `BlurredBackgroundProvider` that respects the current theme. Do not construct a raw
  `LiquidGlassEffect` in a screen.
- `ItemOptions` uses
  `PanelPolicy.useGlass(ForkPanelSettings.enabled(), ForkGraphicsSurface.effectsSuppressed(), SDK_INT, ForkPanelSettings.deviceAllowsEffects(!SharedConfig.deviceIsLow()))`
  to decide whether to attach glass.
- `ItemOptions` also computes `blurForMenu` and treats style `"liquid"` as refraction.
- `PanelPolicy.captureMenuGlass(enabled, savingPower, sdk, capable, hasFactory, eligible)`
  prevents double capture when a factory already produced a blurred drawable.

## Surfaces and z-order

- `ForkFloatingHeader.wrap(context, actionBar, content, title, onBack[, action])` builds the
  in-flow header for fork settings screens: a vertical layout with a header row and the
  content below. It is not an overlay; content scrolls below it, not under it. It is
  package-private, so it cannot be reused from another package without changing visibility.
- `ForkSurfaceAnimation.create(...)` may return `null` when animation is disabled or the
  view has no size; always null-check before starting it (every existing caller does).
- Surfaces must be added after the content they float above, or raised with
  `bringToFront()`/elevation. A surface drawn before the list will be covered.
- Do not let a floating surface participate in the list's scroll clip; otherwise it is
  clipped away when the list scrolls.
- Keep the upstream ActionBar's inset consumption. If the fork header replaces it, the
  same top inset must be applied once, not twice.

## Popups

- Use `PanelPolicy.clampPopupOffset(anchorX, offset, popupWidth, left, right, margin)`.
  It returns the offset relative to the anchor, already clamped to the window with margin.
  Wrong popup coordinates are almost always a missing clamp or a clamp applied to the
  wrong reference point.
- `PanelPolicy.popupAlpha(alpha, experimental)` caps experimental opacity at 240/255.
- `PanelPolicy.popupAnimationDuration(mode, base, systemAnimations)` returns 0 when system
  animations are off or mode is `"minimal"`, and `min(300, base + 40)` for `"smooth"`.
  Respect 0 as "no animation", not "use default".
- `ForkSurfaceAnimation.create(View, Rect sourceOnScreen, boolean opening, boolean fromBottom)`
  returns an `AnimatorSet`, or `null` when `ForkSurfaceAnimation.enabled()` is false or the
  view has no measured size. `enabled()` is
  `ForkAnimationSettings.fluidPopupsEnabled() && animatorDurationScale > 0 && !effectsSuppressed()`.
  The implementation restores original scale, translation, and pivot on cancel, so callers
  must not also restore them, and must null-check the result.

## Popup and surface animation lifecycle

- Cancel any running animator before starting a new one on the same view.
- On detach, cancel; on re-attach, re-apply the final state.
- Do not leave a view at `alpha = 0` after an interrupted close.
- If an animation is only invoked from a code path that never runs, the feature is not
  implemented. Verify the call site exists (grep the class name).

## Stories

- The stories row is upstream `DialogStoriesCell`; fork code must not re-implement it.
- `HeaderGeometry.storiesWidth(windowWidth, controlsWidth, insetsAndGap)` computes the
  available width. Invisible stories usually come from zero width, alpha 0, clip, or
  z-order — check all four.
- Keep `DialogStoriesCell.HEIGHT_IN_DP` as the single height source in `DialogsActivity`.

## Clipping and archive

- Pair every `canvas.save()` with `restore()`.
- Do not clip the archive holder or apply a clip rect inherited from the stories row.
- After search/action-mode transitions, recompute clip bounds; a stale clip is a common
  cause of "the list disappeared".
- Verify archive with and without stories, empty and non-empty.

## Insets

- Apply the top inset once. If both the floating header and the upstream ActionBar apply
  it, the list shifts by double the status bar.
- Use the same insets listener style as the surrounding screen; do not mix
  `fitsSystemWindows` with manual padding.
- Test cutout and gesture navigation.

## Fallback rules

1. If glass is unavailable, render `material` with `ForkGraphicsSurface.materialColor(...)`.
   Gate the branch on `ForkGraphicsSurface.effectiveStyle()`, never on
   `ForkPanelSettings.style()`: `style()` normalizes a requested `"material"` to `"blur"`,
   so `style().equals("material")` branches never run. Several existing legacy call sites
   still have this dead branch — do not copy it.
2. If blur capture fails, keep the surface opaque; never render transparent-on-transparent.
3. If animations are disabled, apply the final state immediately (and handle a `null`
   `AnimatorSet`).
4. If the device is in power saver, do not start capture work.

## Prohibited shortcuts

- Hiding the old ActionBar while leaving its layout height.
- Drawing new buttons over the old panel.
- Removing upstream actions (search, overflow, back, lock).
- A settings toggle that changes no rendered property.
- Shipping an animation with no call site.
- Treating a successful compile as proof the UI is correct.
