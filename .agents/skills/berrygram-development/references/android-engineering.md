# Android Engineering

Scope: Android platform constraints for this fork. minSdk is 23, compile/target SDK 36,
JDK 17, NDK 27.2.12479018, build tools 36.0.0.

## Compatibility floor

- API 23 means no `RenderEffect` (API 31), no `RuntimeShader` (API 33), no
  `WindowInsets.Type` conveniences without guards.
- Guard every modern API with an explicit `Build.VERSION.SDK_INT` check and a fallback
  that still renders something usable.
- `PanelPolicy.useGlass(enabled, savingPower, sdk, capable)` and
  `PanelPolicy.effectiveStyle(requested, savingPower, sdk, capable)` are the existing
  gates. Route new visual features through policy, not raw SDK checks in views.

## Lifecycle

- `BaseFragment` screens: create views in `createView`, refresh in `onResume`, release
  workers/listeners in `onFragmentDestroy`.
- `ForkUpdaterPanel` owns a single-thread `ExecutorService`; `dispose()` shuts it down and
  if busy records `interrupted`. Any new panel with async work must do the same.
- Never hold an `Activity`/`View` reference in a static field or a long-lived singleton.
- Cancel animators on detach; a `ViewPropertyAnimator` on a detached view leaks and can
  fire on a stale window.
- Re-check state after configuration changes; the device-local preference cache
  (`ForkChatHeaderSettings.cached`, `ForkPanelSettings`) must not go stale.

## Insets and layout

- Use `ViewCompat.setOnApplyWindowInsetsListener` or upstream inset helpers; do not add
  fixed status-bar padding.
- When a fork header replaces an ActionBar, it must consume the same top inset the
  ActionBar consumed. Compare with the upstream screen before and after.
- Height must be derived from `dp(...)`, `DialogStoriesCell.HEIGHT_IN_DP`, or measured
  values, never from a hardcoded pixel constant.
- Test with: gesture navigation, 3-button navigation, display cutout, and landscape.

## Touch and accessibility

- Minimum touch target 48dp; do not shrink icon buttons below it.
- Every icon-only control needs a content description from resources.
- Do not intercept touches outside your surface. A floating header that covers the list
  must not swallow list scrolls.
- Long-press, swipe, and back gesture must keep working. Verify with TalkBack for new
  interactive surfaces.

## Canvas and drawing

- Prefer `View` composition over custom `onDraw` when possible.
- In `onDraw`, avoid allocations; reuse `Paint`, `Path`, `RectF`.
- Clip with `canvas.save()/restore()` in pairs, including on exception paths.
- For blur/glass, the supported path is the `blur3` engine; do not add a second blur
  implementation.
- Hardware layers and `setLayerType` are expensive; do not leave them enabled permanently.

## RenderEffect / RuntimeShader policy

- Allowed only behind an SDK and capability check; the fork's adaptive effects switch is
  `ForkPanelSettings.adaptiveEffects()` with
  `ForkPanelSettings.deviceAllowsEffects(deviceCapable)`.
- Provide a color/material fallback:
  `ForkGraphicsSurface.materialColor(context, fallback)`.
- Power saver and low-RAM devices suppress effects; `ForkGraphicsSurface.effectsSuppressed()`
  and `systemPowerSaver()` already encode this.

## JNI and native code

- JNI lives under `TMessagesProj/jni`. Fork features are Java/Kotlin-first; touch native
  only when there is no Java path.
- Changing native code requires a full native rebuild and an ABI check for the target
  (`arm64-v8a` for release).
- Never add a prebuilt `.so` of unknown origin. Verify source correspondence.

## Gradle and R8

- Build with the wrapper; do not use a system Gradle of a different version.
- R8 is enabled for release. New classes reached only by reflection need keep rules in
  `TMessagesProj/proguard-rules.pro`.
- Do not disable R8 to "make it work". Fix the keep rule or the reflection.
- Do not add dependencies casually; Telegram's build is large and each dependency affects
  method count, startup, and licensing.

## Kotlin

- The project is predominantly Java. Follow the file's existing language; do not convert
  files.
- If Kotlin is used, keep it in a new isolated file and confirm the Kotlin plugin is
  already applied to that module before adding a source set.
