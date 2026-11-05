# BerryGram continuation — Android 1.3.0 Beta 5

## Published

- Branch `dev/1.3.0`; Beta 5 source/tag `93794233dffb3ff53622792a023718124ee8e18d`.
- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.5
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.5/BerryGram-1.3.0-beta.5-arm64-v8a.apk
- SHA-256: `dacc6aac28e214e14c9c0e31eb575f92069703a22c6e4d85eec44f57b0f81d6f`; code `72629`.
- Beta 4 remains published/immutable: `7bf93d3`, code `72619`, SHA `4e10c9f3a43604fe039f00205c77e817d85218867f50bfcf5e154bfb5659c604`.

## Beta 5 implemented

- Experimental Glass 2.0 with persisted transparency, blur, frost, saturation, edge light, highlight, shadow depth, tint and radius; Clear/Frosted/Balanced/Deep presets and a live preview. Legacy effect strength is migrated.
- Real blur-path coverage for BerryGram popups, Mini Dock and reaction panel; opaque/Telegram fallback under power saving or unsupported devices.
- Embedded round-video panel at the bottom of Video messages: circular power and switch-camera buttons on the left, large circular live preview on the right; stops on pause/detach/window invisibility and releases camera resources.
- Adaptive Camera Engine with Camera2 capability discovery, five profiles, JPEG quality, round-video bitrate/FPS limits, validated FPS ranges and advertised OIS/EIS.
- Voice profiles with real platform noise suppression/AGC/AEC where available, source selection, profile-aware Opus bitrate and a local test recorder.
- Post-release branch commit `533d604` adds stopping the embedded preview when scrolled off-screen. It is **not** in the published Beta 5 APK and should be included in the next build.

## Open items

- Owner must install the published Beta 5 APK and verify: glass controls/presets/themes, Mini Dock/reaction panels, round-video preview lifecycle and rotation, photo/ordinary/round capture, voice profiles/effects/test recorder, account switching, restart persistence and in-place update.
- Beta 5 was not device-tested before publication. The owner's phone was not connected over ADB; the emulator failed due to a missing Windows Android Emulator hypervisor driver.
- Manual ISO/exposure/focus UI, physical-lens selection, camera extensions/HDR/Night and full BottomSheet/AlertDialog glass remain out of scope for this beta.
- Do not publish Stable 1.3.0 until owner acceptance; Desktop remains gated.

## Confirmed on physical SM-A256B

- Logcat captured the Beta 2 crash: `NullPointerException` in `ActionBarPopupWindowLayout.addViewToSwipeBack` from the custom More page. Beta 3 menu with `Ще` opens without crash.
- Lock screen background is normal after the passcode fallback.
- Performance Center opens and switches profiles without the previous full-page rebuild; Lite Mode and storage rows navigate.
- Experimental Glass is selectable alongside Blur/Liquid.
- Tap→Edit reached Telegram's native edit path and displayed a server edit refusal without crashing. The installed test build lacked API credentials; final APK restores them.

## Implemented

- Menu swipe-back flag + defensive fallback.
- Edit eligibility/account validation and accurate unavailable message.
- Restored approved fork API credentials from Stable 1.2.0 into the Beta 3/Beta 4 builds (not committed/logged).
- Passcode wallpaper fallback (theme color; no opaque black from evicted/invalid bitmap).
- Removed dynamic-neutral Material tint, migrated `material` to Blur, removed Material resource.
- Fixed double-encoded Ukrainian/Russian `fork_custom_text.xml` labels.
- Rewrote Performance Center to update controls in place; real Lite Mode/cache/diagnostics actions; thermal and low-RAM-aware Automatic effects.
- Cached gesture/reaction/performance preferences.
- Added optional Experimental Glass with theme-aware translucent tint, stronger edge light, API/capability fallback.

## Verification

- Full `fork-tests/smoke.ps1` passed.
- Release/R8 passed; Beta 4 APK version code `72619`, minSdk 23, ARM64, non-debug/non-testOnly, permanent certificate, v1/v2 signatures, 16 KB alignment, source revision, native libs identical to Beta 2, API credentials present, SHA verified.
- Final Beta 3/Beta 4 credential-bearing APK was **not installed** before publication at the owner's request. Prior Beta 3 test build (same code minus credentials) was installed over existing data for device checks.

## Pending

- Owner manual test of final credential build: Edit/network actions, every gesture, reactions/picker, Experimental Glass visual quality, themes, in-place update.
- No Stable 1.3.0, no Desktop, no data clearing, no destructive account actions.
- Beta 4 is the updater/credential hotfix. If the new build has issues, fix in Beta 5; do not rewrite Beta 3/Beta 4 tags/assets.
