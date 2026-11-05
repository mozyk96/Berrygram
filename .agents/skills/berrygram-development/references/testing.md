# Testing and Verification

Scope: what can actually be run in this repository, and how to label the result honestly.
A successful compile is not evidence that a feature works.

## Real commands (verified against commit `68081a6`)

Prerequisites: `JAVA_HOME` = JDK 17; a Python 3 executable (`FORK_PYTHON` if not on
`PATH`); Node.js for the JS checks; `ANDROID_HOME` and an `org.json` test JAR for the
fixtures. The JS checks also spawn `javac`/`java`.

```powershell
# Android-independent JVM + source checks (auth, text, aliases, package policy,
# graphics color, updater policy, bitmap geometry, source greps)
pwsh -NoProfile -File fork-tests/smoke.ps1

# If PowerShell 7 is not installed, Windows PowerShell 5.1 works for this script:
powershell -NoProfile -ExecutionPolicy Bypass -File fork-tests/smoke.ps1
```

Other real checks:

```powershell
# GitHub release metadata parser. Requires JAVA_HOME and ANDROID_HOME
# (it uses platforms/android-36/android.jar) plus a test-only org.json JAR.
pwsh -NoProfile -File fork-tests/github-fixture.ps1 -JsonJar <path-to-org.json.jar>

# Updater package/installer simulation. Requires JAVA_HOME and ANDROID_HOME.
pwsh -NoProfile -File fork-tests/updater-package-fixture.ps1 -JsonJar <path-to-org.json.jar>

# Plain Python source/resource checks. Run from the repository root:
# test_release_scope.py resolves paths relative to the current directory.
python fork-tests/test_auth_privacy.py
python fork-tests/test_berrygram_resources.py
python fork-tests/test_release_scope.py

# Plain Node checks over real production source (also need javac/java)
node fork-tests/test_compact_stories.js
node fork-tests/test_popup_animation_lifecycle.js
```

CI: `.github/workflows/berrygram-smoke.yml` runs account-free JVM and source checks only.
It does not build an APK, verify native submodules, sign, or publish. Do not claim CI
coverage you have not seen run.

APK verification (see `build-and-release.md`):

```powershell
python fork-tests/verify_apk.py <apk> --aapt <aapt.exe> --package <id> --version <version>
```

## Test inventory (public base)

JVM tests in `fork-tests/` include `AuthDiagnosticsTest`, `BackendPolicyTest`,
`SessionCredentialsTest`, `CustomText*Test`, `LocalAliasPolicyTest`,
`ForkLocalePolicyTest`, `RoundCameraPolicyTest`, `RoundVideoQualityPolicyTest`,
`VoiceQualityPolicyTest`, `PanelPolicyTest`, `GraphicsColorPolicyTest`,
`UpdatePolicyTest`, `GraphicsPreferencesPersistenceTest`, `HeaderGeometryTest`,
`FloatingChatHeaderGeometryTest`, `BitmapSourceGeometryTest`,
`LiquidGlassSourceLifecycleTest`, `GithubReleaseFixtureTest`.

The directory also contains older tests for retired fork packages (`AppearanceSettingsTest`,
`BatteryTrendTest`, `CaptionTextPolicyTest`, `MessageMenu*Test`, `FavoriteReaction*Test`,
`MessageSwipe*Test`). Those packages are gone, several of these files cannot compile, and
`smoke.ps1` does not register them. Do not cite them as passing evidence and do not use
them as templates.

`fork-tests/smoke.ps1` compiles an explicit subset and registers new tests manually. If
you add a policy test, add it there only if it belongs to the smoke set; otherwise document
the separate command.

`fork-tests/test_release_scope.py` also asserts that retired fork packages and classes do
not come back (`fork.gestures`, `fork.menu`, `fork.player`, `fork.performance`,
`fork.privacy`, `fork.appearance`, `ForkMessageMenu`, `ForkUpdaterActivity`,
`ForkQuickReactionShortcuts`). Do not reintroduce them.

## What each layer proves

| Layer | Proves | Does not prove |
| --- | --- | --- |
| JVM unit tests | Pure policy, geometry, parsing, validation | Rendering, insets, lifecycle on device |
| Source checks (Python/Node) | Required symbols and wiring exist | Behavior at runtime |
| Fixtures (GitHub/updater) | Parser, package/signer and download logic against synthetic input | Real TLS, real APK signatures, real installer |
| Release build | Compiles and packages with R8 | UI correctness, performance |
| `verify_apk.py` | Package, versionName, label, arm64 presence, testOnly/debuggable, locales | versionCode, ABI exclusivity, signatures, installation, behavior |
| Device/emulator | Actual rendering, touch, install | Long-term reliability |

## Acceptance levels

- `TESTED` — named command executed and passed; record it.
- `CODE VERIFIED` — call chain read and consistent; no runtime test.
- `MANUAL TEST REQUIRED` — needs emulator/device.
- `UNTESTED` — no verification. Never described as done.

## Regression matrix

Automated and manual columns are separate. `A` = automatable, `M` = manual.

| Area | Automated | Manual scenarios |
| --- | --- | --- |
| Authorization | `AuthDiagnosticsTest`, `BackendPolicyTest`, `test_auth_privacy.py` (A) | Real login, code entry, 2FA (M) |
| Main screen | `test_release_scope.py`, `HeaderGeometryTest` (A) | Header layout, scroll, tabs (M) |
| Search | — | Open/close search, restore header/glass, back (M) |
| Stories | `test_compact_stories.js` (A) | Row visible, open story, collapse, archive stories (M) |
| Archive | — | Empty/non-empty, with/without stories, clipping (M) |
| Chat header | `FloatingChatHeaderGeometryTest` (A) | Floating header, back, insets, rotation (M) |
| Mini dock | — | Appearance, overlap with input, hide on scroll (M) |
| Graphics modes | `PanelPolicyTest`, `GraphicsColorPolicyTest`, `BitmapSourceGeometryTest`, `LiquidGlassSourceLifecycleTest` (A) | Blur/Liquid/material, power saver, low-end device, theme switch (M) |
| Custom phrases | `CustomText*Test` (A) | Editor, import/export, aliases, locale change, theme (M) |
| Camera settings | `RoundCamera*Test` (A) | Round video record, quality, shortcuts (M) |
| Passcode lock | — | Set/change/remove, background lock, wrong code (M) |
| Update Center | `UpdatePolicyTest`, `GithubReleaseFixtureTest`, `updater-package-fixture.ps1` (A) | Real release check, download, digest, install, interruption (M) |
| APK installation | `verify_apk.py` metadata only (A, partial) | Fresh install, update over previous build, downgrade rejection (M) |

## Rules

1. Run the narrowest test that covers the change; widen only across boundaries.
2. Do not re-run an unchanged suite repeatedly to "look thorough".
3. Do not run R8, a full APK build, an emulator, or FPS benchmarks for a documentation or
   string change.
4. Record the exact command and result for every `TESTED` claim.
5. Any UI, inset, animation, installer, or GPU claim without a device run is
   `MANUAL TEST REQUIRED`.
6. Never automate a real Telegram account or handle a user's codes/passwords.
7. If a test is skipped, say it was skipped and why.
