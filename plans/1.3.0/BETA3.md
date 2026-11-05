# BerryGram Android 1.3.0 Beta 3 — result

Branch: `dev/1.3.0`. Source: `ec5517afa8147fd9688be2904b0bc19112ec8e0c`. Published pre-release `v1.3.0-beta.3`.

## P0 — device-confirmed stability

- **Message-menu crash (Beta 2)**: Logcat on the physical SM-A256B showed `NullPointerException` in `ActionBarPopupWindowLayout.addViewToSwipeBack` from the custom More page. The popup requested a swipe-back container only for reactions/seen/sponsor. Fixed by requesting `FLAG_USE_SWIPEBACK` whenever hidden menu actions exist, plus a defensive no-container fallback. Verified on device: message menu with `Ще` opens without crash.
- **Edit gesture**: Gesture dispatch validates the message/account and edit eligibility, uses a scoped one-shot native dispatch, and shows a specific edit-unavailable message. Device test: tap→Edit reached Telegram's native edit request and displayed the server's edit refusal without crashing (the installed test build still lacked API credentials, see below).
- **API credential regression**: Beta 2 APK was built with empty `FORK_TELEGRAM_API_ID/HASH`, causing `CONNECTION_API_ID_INVALID` on edit/network actions. Beta 3 restores the approved fork credentials already present in Stable 1.2.0. Values are injected at build time and are not committed/logged.
- **Lock screen**: Device-confirmed normal themed background after the passcode fallback fix. Missing/invalid wallpaper bitmaps now fall back to a Telegram theme color; the darkening overlay is applied only after a wallpaper was drawn.

## P1 — functional corrections

- Removed Android dynamic-neutral "Material" surface tint; settings use Telegram theme colors. Legacy `style=material` migrates to Blur; the Material option/resource is gone.
- Fixed double-encoded Ukrainian/Russian labels in `fork_custom_text.xml`.
- Performance Center rebuilt to update controls in place; profile, custom reduction, Lite Mode shortcut, storage/cache shortcut, reset and privacy-safe diagnostics copy are real actions. Device-confirmed responsive, no page rebuild.
- Added optional Experimental Glass alongside Blur/Liquid Glass using the existing Telegram blur source with a distinct translucent tint/edge light and safe API/capability fallback.

## P2 — performance

- Cached gesture, reaction and performance preferences (account/user-aware).
- Thermal status (API 29+, 15s sampling) and low-RAM state influence Automatic effect reduction; no per-frame polling.
- Telegram Lite Mode remains the native mechanism; no competing render engine.
- No baseline profile data was fabricated; no FPS/battery/thermal improvement is claimed.

## Verification

- Full `fork-tests/smoke.ps1` passed.
- Release/R8 passed (7m23s final credential build; 7m26s prior build).
- APK: `org.example.forkclient`, `1.3.0-beta.3`, code `72609` > Beta 2 `72599`, minSdk 23, ARM64, non-debug/non-testOnly, permanent certificate, v1/v2 signatures, 16 KB alignment, source revision embedded, native libs identical to Beta 2.
- SHA-256: `5700cbd0c209645097089129af4bb7db516f7eccf40788a397b051b8b7d69323`.

## Boundaries

- Final credential-bearing APK was not installed before publication at the owner's request. Prior Beta 3 build (same code except API credentials) was installed over existing data and used for lock-screen, Performance Center, Experimental Glass selection and Edit/menu checks.
- No Stable 1.3.0, no Desktop, no destructive account operations, no data clearing.
- Owner manual testing remains required for network Edit after the credential fix, all gesture actions, reactions/picker, Experimental Glass visual quality, themes, and in-place update.
