# Animation System

Scope: fork motion. Motion must be driven by policy, interruptible, and inert when the
system disables animations.

## Verified symbols

- `ForkAnimationSettings.fluidPopupsEnabled()` — master switch for fluid surfaces.
- `ForkSurfaceAnimation.enabled()` —
  `fluidPopupsEnabled() && animatorDurationScale > 0 && !effectsSuppressed()`.
- `ForkSurfaceAnimation.create(View view, Rect sourceOnScreen, boolean opening, boolean fromBottom)`
  — returns an `AnimatorSet`, or `null` when disabled or the view has no measured size.
  Captures original scale/translation/pivot and restores them on cancel.
- `PanelPolicy.popupAnimationDuration(String mode, int base, boolean systemAnimations)`.
- `PanelPolicy.popupAlpha(int alpha, boolean experimental)`.
- `ForkPanelSettings.animationMode()` and `setAnimationMode(String)`.

## Rules

1. **Null-check the animator.** `create(...)` returns `null` when disabled or unmeasured.
   `null` means "apply the final state immediately", not "crash".
2. **Two separate gates.** `fluidPopupsEnabled()` controls `ForkSurfaceAnimation`;
   `animationMode()` feeds `PanelPolicy.popupAnimationDuration` used by
   `ActionBarPopupWindow` and `ItemOptions`. Setting mode `"minimal"` does **not** disable
   the fluid surface animation. Do not conflate them.
3. **Durations are per-path.** `ForkSurfaceAnimation` uses its own fixed durations
   (~300/220/200/160 ms by case); popup duration comes from
   `PanelPolicy.popupAnimationDuration`. Read the path you are editing before claiming a
   policy controls it.
4. **Do not stack animators.** Cancel the previous animator on the same view before
   starting a new one.
5. **Restore final state.** After an interrupted or cancelled animation, the view must end
   at its target alpha, scale, and translation. `ForkSurfaceAnimation` restores originals
   on cancel; do not duplicate that logic in the caller.
6. **Respect the seed rect.** Growth starts from `sourceOnScreen`; passing the wrong rect
   produces an animation that appears to come from nowhere.
7. **No animation-only features.** If the code path that starts the animation is never
   reached, the feature does not exist. Verify with a call-site search.

## Transition coverage

Any surface that animates on open must have a defined behavior for:

- open,
- close,
- interrupted open (close during opening),
- theme change while open,
- configuration change,
- screen rotation,
- search/action-mode switch on `DialogsActivity`,
- back press.

If one of these is undefined, the surface will visually "stick" in a transitional state.

## ActionBar transitions

- The forward/back transition is owned by `ActionBarLayout`/`BaseFragment`. Do not add a
  second transition for the same navigation.
- A floating header must be present before the fragment transition ends, or it will pop in.
- When returning from search, restore the header style rather than re-creating the view.

## Popup coordinates

- Compute the anchor position, then clamp with
  `PanelPolicy.clampPopupOffset(anchorX, offset, popupWidth, left, right, margin)`.
- The returned value is relative to the anchor. Applying it as an absolute x is a common
  coordinate bug.
- Re-clamp after the popup measures its real width; an estimated width causes edge drift.

## Reduced motion

- System "remove animations" must produce the final state with no animator.
- Do not use `ValueAnimator` with a hardcoded duration for a policy-controlled element.
- Do not animate infinite loops for decoration in a chat list.

## Performance

- Animate `alpha`, `translation`, `scale`, and `elevation`; avoid layout-property animation
  in scrolling containers.
- Never allocate `Paint`/`Rect` inside `onAnimationUpdate`.
- Do not trigger blur capture per frame.
- Test on a low-end profile with effects disabled to confirm the fallback path is smooth.

## Verification levels

- Duration/alpha/clamp logic: covered by `PanelPolicyTest` in `fork-tests` (`TESTED` when run).
- Actual visual smoothness, interruption, and inset behavior: `MANUAL TEST REQUIRED`.
- An animation with no call site: `UNTESTED` and not implemented, regardless of compile success.
