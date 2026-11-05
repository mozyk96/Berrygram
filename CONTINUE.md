# BerryGram continuation — Android 1.3.0 Beta 3

## Published

- Branch `dev/1.3.0`; source/tag `ec5517afa8147fd9688be2904b0bc19112ec8e0c`.
- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.3
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.3/BerryGram-1.3.0-beta.3-arm64-v8a.apk
- SHA-256: `5700cbd0c209645097089129af4bb7db516f7eccf40788a397b051b8b7d69323`.

## Confirmed on physical SM-A256B

- Logcat captured the Beta 2 crash: `NullPointerException` in `ActionBarPopupWindowLayout.addViewToSwipeBack` from the custom More page. Beta 3 menu with `Ще` opens without crash.
- Lock screen background is normal after the passcode fallback.
- Performance Center opens and switches profiles without the previous full-page rebuild; Lite Mode and storage rows navigate.
- Experimental Glass is selectable alongside Blur/Liquid.
- Tap→Edit reached Telegram's native edit path and displayed a server edit refusal without crashing. The installed test build lacked API credentials; final APK restores them.

## Implemented

- Menu swipe-back flag + defensive fallback.
- Edit eligibility/account validation and accurate unavailable message.
- Restored approved fork API credentials from Stable 1.2.0 into the Beta 3 build (not committed/logged).
- Passcode wallpaper fallback (theme color; no opaque black from evicted/invalid bitmap).
- Removed dynamic-neutral Material tint, migrated `material` to Blur, removed Material resource.
- Fixed double-encoded Ukrainian/Russian `fork_custom_text.xml` labels.
- Rewrote Performance Center to update controls in place; real Lite Mode/cache/diagnostics actions; thermal and low-RAM-aware Automatic effects.
- Cached gesture/reaction/performance preferences.
- Added optional Experimental Glass with theme-aware translucent tint, stronger edge light, API/capability fallback.

## Verification

- Full `fork-tests/smoke.ps1` passed.
- Release/R8 passed; APK package/version code `72609`, minSdk 23, ARM64, non-debug/non-testOnly, permanent certificate, v1/v2 signatures, 16 KB alignment, source revision, native libs identical to Beta 2, SHA verified.
- Final credential-bearing APK was **not installed** before publication at the owner's request. Prior Beta 3 build (same code minus credentials) was installed over existing data for device checks.

## Pending

- Owner manual test of final credential build: Edit/network actions, every gesture, reactions/picker, Experimental Glass visual quality, themes, in-place update.
- No Stable 1.3.0, no Desktop, no data clearing, no destructive account actions.
- If final credential build has issues, fix in Beta 4; do not rewrite Beta 3 tag/assets.
