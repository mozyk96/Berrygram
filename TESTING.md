# Testing

## Local smoke tests

Set `JAVA_HOME` to JDK 17 and `FORK_PYTHON` to a Python 3 executable if Python is not on PATH, then run `pwsh -NoProfile -File fork-tests/smoke.ps1`. It compiles and runs Android-independent checks for appearance, backend policy, credentials, text validation, pack validation and aliases, scoped status and plural behavior, actual text-manager persistence against local stubs, privacy shortcut and per-chat reaction policies, plus a source check for sensitive auth logging. It does not prove Android UI, Telegram authentication, network delivery or full privacy.

## Build and device

Use the isolated Android SDK/NDK and Gradle setup and build `:TMessagesProj_App:assembleAfatDebug` with `forkAbi=arm64-v8a` and the configured fork credentials. Never print credentials. Verify the package, ABI, installation and process separately. APKs contain extractable app credentials.

Use a controlled test account or owner-operated real device for functional checks. Do not automate access to personal chats, codes, passwords or tokens. Verify Custom Text editor, appearance, persistence, language changes, import/export, aliases, light/dark themes and accessibility on Android. Keep production and test backends isolated. Record failures and untested scenarios; a compilation success is not device QA.

GitHub release metadata parser: with JDK 17, Android SDK platform 36 and a test-only org.json JAR, run `fork-tests/github-fixture.ps1 -JsonJar <path>`. The fixture checks stable metadata and rejects draft, invalid digest, untrusted URL and unpublished assets; it does not test HTTPS, Android APK signature parsing, installation or the real GitHub service.

`.github/workflows/berrygram-smoke.yml` runs the account-free JVM and source checks only. It does not build an APK, verify native submodules, sign artifacts or publish releases. Android build and release CI require separately configured credentials and signing.

Updater package fixture: set JAVA_HOME and ANDROID_HOME, then run `fork-tests/updater-package-fixture.ps1 -JsonJar <path-to-org.json-jar>`. This uses a mock PackageManager and a synthetic ZIP to exercise package name, version, debug flag, ARM64 entries and signer-set matching for pre-28 and 28+ branches. It also runs the download, SHA-256, progress, truncation, interruption, signer-mismatch and redirect-rejection paths with an in-process HTTPS handler. It does not verify real TLS, Android package parsing, actual APK signatures or installer behavior.
