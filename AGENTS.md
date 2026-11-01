# BerryGram repository guidance

## Project and state

BerryGram is an unofficial Android Telegram client. `TMessagesProj` contains upstream core, UI and JNI; `TMessagesProj_App*` contain app variants; `TMessagesProj_Modules/media` and other native dependencies are Git submodules. Read `ROADMAP.md` for scope, `CONTINUE.md` and `PROGRESS.md` for current evidence, and Git status/log before working. For BerryGram tasks use the local `berrygram-maintainer` skill when available; these repository rules still apply if it is unavailable.

## Architecture and compatibility

Preserve Telegram behavior and existing user work. Prefer isolated fork additions and existing Telegram theme-aware components; do not introduce placeholder controls or implement deferred roadmap features without direction. Maintain minSdk 23, application ID, branding and release signing identity unless explicitly changed. The current stack includes Gradle 8.13, AGP 8.13.2, JDK 17, compile/target SDK 36 and NDK 27.2.12479018; verify actual configuration before changing it. Compare the upstream DrKLO/Telegram commit before updates and test controlled integrations; changing a version number is not an upstream update.

## Build and checks

Initialize recursive submodules where needed. On an appropriately configured host, run `./fork-tests/smoke.ps1`; use the updater fixtures for updater changes. Build with `./gradlew :TMessagesProj_App:assembleAfatDebug -PforkAbi=arm64-v8a` or the corresponding Release task when authorized and configured. Group changes and use incremental checks; perform relevant Android, UI and regression tests before release. Distinguish automated checks from untested device, account, media and UI behavior. Never automate a real Telegram account.

## Credentials, Git and release

Use only authorized fork-owned credentials supplied locally or through CI secrets. Do not log or commit credentials, keystores, passwords, sessions, generated APKs, personal data or service configuration. The owner permits the app's Telegram API hash inside a distributable APK, recognizing that it can be extracted, but does not permit publishing it in Git files or logs. Preserve the release key; verify application ID, version, ABI, signing continuity, SHA-256 and source correspondence. Publish only if authorized and appropriately tested; label any permitted prerelease and its gaps accurately. Never push the original history containing signing material or rewrite public history without explicit approval. Use logical commits, avoid mass staging and update the changelog and continuation state truthfully.

## Physical phone

Do not install, uninstall, push files, clear data, change settings or otherwise modify the owner's physical phone without a new explicit instruction. Use an emulator or another authorized test environment. Do not delete user data or unrelated files.
