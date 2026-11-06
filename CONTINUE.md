# BerryGram continuation — Android 1.3.0 Stable

## Published

- Branch `dev/1.3.0`; Stable source/tag `v1.3.0` at `dd0f71a04154dc39ee016a0e9ea80a6b7cf83738`.
- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0/BerryGram-1.3.0-arm64-v8a.apk
- SHA-256: `80e0be6a5aa1afc3f8251aedad686f9c42cb5e62c42c3c61c3582aa7ec7f882d`; versionName `1.3.0`, versionCode `72639`.
- Beta 5 remains published/immutable: tag `9379423`, code `72629`, SHA `dacc6aac28e214e14c9c0e31eb575f92069703a22c6e4d85eec44f57b0f81d6f`.
- Beta 4 remains published/immutable: `7bf93d3`, code `72619`, SHA `4e10c9f3a43604fe039f00205c77e817d85218867f50bfcf5e154bfb5659c604`.

## Stable 1.3.0 contents

- Everything in Beta 5: Experimental Glass 2.0, embedded round-video preview panel, Adaptive Camera Engine and Voice Engine.
- Includes post-Beta-5 commit `533d604` (preview stops when the panel scrolls out of view), plus an extra guard against late camera callbacks while off-screen or window-hidden.
- Removed the nonfunctional Appearance “preview” row that only showed a “Done” popup.
- Glass provider wrappers now re-evaluate style/config on every color refresh, so switching to/from Experimental Glass applies without recreating the host screen.
- Glass preview drawing no longer allocates paints/rectangles per frame; colors, radii, stroke widths and shadows are cached on config change.
- Appearance “Intensity” and the Experimental Glass “Blur” control now stay consistent for the selected style.
- Corrupt/mistyped preference values in Glass, camera and voice settings are repaired instead of crashing.
- Round-video power toggle turns off after denied permission or camera timeout.
- Voice settings preserve scroll position on refresh; stale record-permission results and pending test starts are cancelled on pause/destroy.
- Voice effect availability is cached (bounded, static) instead of re-querying platform effects repeatedly.

## Verification completed

- Full `fork-tests/smoke.ps1` including new `GlassSettingsTest` and all existing gesture/reaction/glass/camera/voice/updater/performance suites.
- Debug Java compilation and Release/R8 build with credentials injected at build time (values never printed).
- APK: `org.example.forkclient`, versionName `1.3.0`, code `72639` (> Beta 5 `72629`), minSdk 23, targetSdk 36, ARM64 only, v1+v2 signatures, permanent certificate `b3ea9ca2…c722a5`, not debuggable, not testOnly, 16 KB ZIP alignment, no keystore packaged, non-zero API credentials present, embedded revision `dd0f71a`.

## Open items

- No physical-device test of this exact Stable APK was possible: the owner's SM-A256B was not connected over ADB and the local emulator cannot start (Windows Android Emulator hypervisor driver missing). The owner explicitly chose to publish with this gap disclosed in the release notes.
- Device-only checks still to be reported by the owner: in-place upgrade from Beta 4/Beta 5, glass visuals per theme, Mini Dock/reaction panels, round-video start/stop/switch/scroll-away, photo/round-video capture, voice profiles/effects/local test, account switching, restart persistence, updater detection.
- Baseline/Startup Profiles and broader R8 rule pruning were investigated and deliberately deferred: real profiles require device/macrobenchmark generation, and hand-written profiles would be fabricated.
- Known feature limits: no manual ISO/exposure/focus UI, no physical-lens selection, no camera extensions/HDR/Night; BottomSheet/AlertDialog glass not converted; Desktop remains gated.

## Next

1. Owner installs Stable 1.3.0 and reports any reproducible issue; fixes go into 1.3.1 (do not rewrite the `v1.3.0` tag or assets).
2. Do not publish further Stable without device evidence or explicit owner direction.
3. Desktop work stays gated until the owner starts it explicitly.
