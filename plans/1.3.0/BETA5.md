# BerryGram Android 1.3.0 Beta 5

Branch: `dev/1.3.0`
Baseline: Beta 4 `7bf93d3` (code `72619`)
Target: `1.3.0-beta.5`, APK `BerryGram-1.3.0-beta.5-arm64-v8a.apk`

## P0 — Recovery and baseline

- Preserved all Beta 3/Beta 4 fixes: menu swipe-back crash, Edit validation, API credentials, themed passcode fallback, Material tint removal, localisation, Performance Center, preference caches, Experimental Glass.
- No published tag or asset is rewritten. Beta 4 remains immutable.

## P1 — Experimental Glass 2.0

Implemented in production code:

- `ForkGlassSettings` / `GlassPolicy`: persisted transparency, blur, frost, saturation, edge light, highlight, shadow depth, tint mode, adaptive quality and presets.
- Migration from the legacy effect-strength value keeps the previous Experimental Glass appearance.
- `ForkGlassSurface` now derives actual tint opacity, stroke colours/width and shadow from the new controls. Telegram-owned providers are untouched.
- Popup capture blur and backdrop saturation now follow the blur/frost/saturation controls.
- Coverage extended to BerryGram popup menus, the Mini Dock and the reaction panel through a non-destructive provider wrapper.
- `ForkGlassPreview` renders a live synthetic wallpaper, blurred backdrop and glass surface; sliders update it in place.
- Presets: Clear, Frosted, Balanced, Deep, Custom. Manual edits switch to Custom without resetting other preferences.
- Safe fallback when Experimental Glass is unavailable (power saving, unsupported API, low-end policy): existing opaque/Telegram surface path.

Boundary: `BottomSheet` and every `AlertDialog` are not converted to blur3 surfaces in this beta; doing so would require changing Telegram-owned window composition. Privacy-sensitive overlays keep opaque backgrounds.

## P2 — Round Video embedded preview

- `ForkRoundVideoPreviewPanel`: two circular icon-only controls (power, switch camera) in a vertical column on the left; large circular live preview on the right.
- No visible button labels; accessibility descriptions are provided.
- Preview starts only on request, stops on pause/detach/invisible, releases `CameraView` and the camera controller callback, and handles permission denial/timeout.
- Integrated at the bottom of `ForkVideoSettingsActivity`; the separate test screen entry point was removed.
- Front-camera preview keeps Telegram's mirroring and aspect handling through the existing `CameraView`.

## P3 — Adaptive Camera Engine

- `CameraCapabilities`: real Camera2 characteristics discovery per camera id (facing, hardware level, output sizes, FPS ranges, focus modes, OIS/EIS, zoom, flash, sensor orientation, exposure compensation). Cached, read-only, never opens a camera.
- `CameraProfilePolicy` / `CameraProfileSettings`: Automatic, Quality, Balanced, Performance, Custom profiles with validated fallbacks.
- Integrated behavior:
  - JPEG quality for ordinary still capture (`CameraSession`).
  - Round-video bitrate scaling and codec-range clamping (`RoundVideoCodecRecorder`, `InstantCameraView`).
  - Round-video FPS cap for Performance and low-end Balanced devices (`RoundVideoCameraController`).
  - Camera2 video FPS range validation against advertised ranges instead of the previous hard-coded 30–60 (`Camera2Session`).
  - Video/OIS stabilization only when advertised and supported by the profile.
- Settings show the selected profile and a read-only capability report. No fake HDR/Night/lens controls are exposed.

Boundary: ordinary chat-attachment capture still uses Telegram's Camera1 path; manual ISO/exposure/focus UI, physical-lens selection and Android camera extensions are not enabled in this beta. They require changes to the active capture pipeline that were not safe to introduce here.

## P4 — Berry Voice Engine

- `VoiceProfilePolicy` / `VoiceProfileSettings`: Standard, Automatic, Clear voice, Natural voice, Custom.
- Profiles change real capture behavior: Opus target bitrate, audio source and platform effects. Explicit custom bitrate remains authoritative.
- `VoiceProcessing`: attaches `NoiseSuppressor`, `AutomaticGainControl` and `AcousticEchoCanceler` only when available. System noise suppression is not combined with RNNoise.
- Integrated into the default V2 recorder (`AudioCapture`) and the legacy `MediaController` recorder, with release on every stop/error path.
- Settings expose only supported effects, source choice for Custom, and a local test recorder (start, stop, play, delete). Test files stay in the app cache, are never sent to Telegram, and are deleted on dialog dismissal/destruction.

## P5 — Verification

Completed:

- `fork-tests/smoke.ps1` including new pure tests: `GlassPolicyTest`, `CameraProfilePolicyTest`, `VoiceProfilePolicyTest`; existing policy/resource/release tests unchanged and passing.
- Debug Java compilation with `:TMessagesProj_App:compileAfatDebugJavaWithJavac`.
- Release/R8 build with the approved fork API credentials injected at build time (values not printed).
- APK identity, version code, minSdk, ARM64, non-debug/non-testOnly, permanent certificate, v1/v2 signatures, 16 KB alignment, embedded source revision, API credential presence and SHA-256.

Not completed at the time of writing: physical-device verification of the exact Beta 5 APK. The owner's phone was not available over ADB. Camera preview, voice effects and glass visuals therefore remain owner-acceptance items.

## P6 — Beta 5 release

Published as a GitHub pre-release after the checks above. Reports only what was actually verified; no Stable or Desktop release.
