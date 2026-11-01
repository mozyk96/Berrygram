# BerryGram 1.0.1 modernization audit

This is an engineering assessment, not evidence of device acceptance or a completed release.

## Baseline and build

- The original upstream base is DrKLO/Telegram `3de37c8cfb039452be7398909461da6288babf15` (13.1.1 / 7237). On 2026-11-01 `git ls-remote` returned that same commit for upstream `master`. Release tags are not a substitute for checking the branch. No later upstream commit was identified by that check; no security or functional audit of upstream is implied.
- Gradle wrapper 8.13, Android Gradle Plugin 8.13.2, JDK 17 for Gradle, Kotlin plugin 2.1.0, compile/target SDK 36 and NDK 27.2.12479018 are configured. Java source/target 8 is distinct from the Gradle JDK. AGP 8.13 supports API 36.1 and requires Gradle 8.13; AGP 8 requires JDK 17. Retain this previously compiled combination for 1.0.1. A change to AGP 9 and dependency upgrades require a separate compatibility and native-build investigation.
- The app and core modules currently specify minSdk 23. Android 12/API 31 would allow RenderEffect, but setting a view's render effect does not by itself establish a backdrop blur. Raising minSdk removes users and has not been checked across the native, camera, notification, installer and background paths on real Android 12. Keep 23 in 1.0.1 until an isolated migration passes those checks; API 33 adds no demonstrated benefit sufficient to justify excluding Android 12 users.
- Release resource shrinking remains disabled because it previously produced invalid stable-ID resources; code minification remains enabled. Locally built signed candidates are not production acceptance evidence.

## BerryGram components and risk

- Settings use Telegram's theme-aware cells. The hub, video/voice pages, custom text editor and updater need visual checks for dark/light themes, small screens, large fonts, keyboard and RTL. No screenshot-based conclusion is available.
- Video camera selection and quality integrate existing Telegram camera code; voice quality reaches both native Opus recorder paths. Policy tests pass, but actual recording and hardware capabilities have not been verified on Android.
- Custom text is keyed and persisted separately from localization; language changes, actual statuses and per-user behavior need device tests. Avoid broad interception of LocaleController and preserve factual status semantics.
- Experimental styling affects eligible `ActionBarPopupWindow` surfaces, action-bar popup blur and the message context-menu blur paths. Other menus including particular profile/popup variants have not been individually verified. Telegram's existing LiteMode governs parts of its blur implementation. Do not claim full coverage or GPU performance without visual and performance testing.
- Updater uses a fixed GitHub repository, background worker, validated release metadata and APK digest, package, version, ABI and signer checks. Rotation interrupts the active operation and restores metadata; it does not resume a download. A real release download, Android installer, offline and lifecycle behavior are not accepted as tested by fixture tests.
- The full Telegram modules and native libraries remain upstream-owned. Avoid invasive refactoring or replacing the native media stack before targeted regression coverage exists.

## Release and CI constraints

Sanitized source has been published on separate Git history. Actions could not start because GitHub reported an account billing lock. The configured Telegram API hash was found inside local APKs and the owner prohibits its publication; CI secrets only protect input storage and do not make a hash embedded in an APK secret. No APK containing it may be uploaded. The emulator cannot currently start because host hardware acceleration is unavailable; the owner's phone must not be touched. Authentication, calls, notifications, media, update installation and UI acceptance remain unverified.

## References

- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://developer.android.com/build/releases/about-agp
- https://developer.android.com/build/jdks
- https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing
