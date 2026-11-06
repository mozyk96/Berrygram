# BerryGram Android 1.3.0 Stable

Branch: `dev/1.3.0`
Beta 5 baseline: `9379423` (code `72629`)
Release source: `dd0f71a04154dc39ee016a0e9ea80a6b7cf83738`
Published tag: `v1.3.0`; versionName `1.3.0`, versionCode `72639`
APK: `BerryGram-1.3.0-arm64-v8a.apk`, 41874894 bytes
SHA-256: `80e0be6a5aa1afc3f8251aedad686f9c42cb5e62c42c3c61c3582aa7ec7f882d`

## P0 — Correctness and preservation

- Preserved Beta 5 and every earlier tag/artifact; no tag or asset is rewritten.
- Included the post-Beta-5 preview scroll fix (`533d604`) in Stable: the embedded round-video camera now stops when its panel is scrolled out of view, in addition to pause/detach/window invisibility.
- Removed the nonfunctional Appearance “preview” row that only opened a “Done” popup. Experimental Glass has the real live preview screen instead.
- Tightened round-preview state: denied permission and camera timeout now switch the power state off instead of leaving a misleading active toggle; the preview also refuses to start the camera while off-screen or window-hidden, so late callbacks cannot reopen it.
- Made the Glass provider wrapper dynamic: it re-checks style/config on every color refresh, so switching to or from Experimental Glass applies without recreating the host view, and its shadow/stroke/alpha values no longer freeze at wrapper creation.
- Unified the Appearance “Intensity” slider with the Experimental Glass blur value, so the row never shows a stale number for the active style.
- Repaired corrupt preference values: mistyped Glass/camera/voice keys are removed and replaced with validated defaults instead of throwing `ClassCastException` during settings or recorder initialization.
- Cancelled stale voice-test state: pending record-permission starts are cleared on pause/destroy and late permission results no longer start or update a destroyed screen.
- Preserved Telegram data, account sessions, signing identity, minSdk 23, ARM64 and updater policy.

## P1 — Regression and settings audit

Audited the BerryGram-only production paths:

- Gesture dispatch/edit validation/menu More fallback (existing tests retained).
- Message menu and reaction panel provider coverage.
- Passcode fallback and theme-aware surfaces.
- Performance Center (no `removeAllViews` rebuild; real Lite Mode/cache/diagnostics/reset actions).
- Glass settings migration and persistence (new `GlassSettingsTest`).
- Camera capability/profile policy and fallback (existing `CameraProfilePolicyTest`).
- Voice profile/effect/bitrate policy (existing `VoiceProfilePolicyTest`).
- Round preview lifecycle, permissions and camera-owner handoff.
- Voice test file cleanup and audio-effect release paths.

Known limitations are recorded in `CONTINUE.md` rather than presented as verified features.

## P2 — Performance and memory work

Implemented, low-risk changes only:

- `ForkGlassPreview` no longer allocates `Paint`/`RectF` objects inside `onDraw`; colors, radii, stroke widths and shadow parameters are cached when settings change.
- Glass preview slider updates no longer recreate the floating-header selector drawables on every progress event.
- Glass provider wrappers evaluate configuration lazily instead of capturing a stale snapshot, which also removes repeated per-refresh allocations.
- Voice effect availability (`NoiseSuppressor`, `AutomaticGainControl`, `AcousticEchoCanceler`) is resolved once and cached in bounded static fields instead of being re-queried on every recorder start.
- Appearance screen has one less redundant interactive row.
- Voice settings preserve scroll position across its configuration refresh instead of jumping to the top.
- Round-video preview stops when off-screen; camera and GL resources are released rather than kept alive while the panel is invisible.
- Existing bounded caches remain authoritative: `ForkPanelSettings`, `ForkGlassSettings`, `CameraProfileSettings`, `CameraCapabilities` (read-only descriptors), performance thermal/power checks (15 s bound), gesture/reaction preference caches.

Investigated but not enabled:

- **Baseline Profiles / Startup Profiles**: the repository currently has no profile installer dependency or profile-generation module. Generating real profiles requires device/macrobenchmark runs. Hand-written profiles would be fabricated, so none were added. This is documented as a tooling limitation, not a claimed optimization.
- **R8 rule changes**: release already uses `proguard-android-optimize.txt` with Telegram's reflective/JNI rules. No broad rule removal was attempted before Stable because the risk outweighs a secondary APK-size benefit.
- **Additional media caches**: Telegram already owns media/thumbnail/avatar caches; no competing cache was introduced.

No FPS, startup-time or battery percentages are claimed because no physical device or profiler-capable emulator was available during this pass.

## P3 — Verification

Completed:

- Full `fork-tests/smoke.ps1`, including `GlassSettingsTest` (migration, clamping, preset application, tint persistence and cache invalidation).
- Debug Java compilation.
- Release/R8 build with approved fork API credentials injected at build time (values not printed).
- APK identity/version/minSdk/ABI/signing/debuggable/testOnly/alignment/source-revision/API-credential checks.
- Public download SHA-256 verification.

Not completed:

- Physical-device testing of the exact Stable APK. ADB reported no connected device, and the local emulator cannot start because the Windows Android Emulator hypervisor driver is not installed. Stable publication must not claim device-passed camera, voice, glass or upgrade behavior without this evidence.

## P4 — Stable release gates

Stable is published only if the build/identity/security/updater checks pass and no known critical failure remains. Device-only checks are labelled as unverified in the release notes instead of being replaced with assumptions.

## P5 — Documentation

`CONTINUE.md`, `PROGRESS.md`, `plans/1.3.0/RELEASE.md` and `plans/1.3.0/TASKS.md` record actual release metadata, tests and remaining gaps. Existing Beta tags remain immutable.
