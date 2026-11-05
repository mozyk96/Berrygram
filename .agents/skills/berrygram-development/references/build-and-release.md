# Build and Release

Scope: producing and validating a BerryGram artifact. Publication requires explicit
authorization; a build is not a release.

## Toolchain (verified)

| Item | Value |
| --- | --- |
| JDK | 17 |
| Gradle | 8.13 wrapper (`gradlew`) |
| compileSdk / targetSdk | 36 |
| minSdk | 23 |
| NDK | 27.2.12479018 |
| Build tools | 36.0.0 |
| Release ABI | `arm64-v8a` |

Initialize submodules recursively (`TMessagesProj_Modules/media` and others) before
building. A missing submodule produces confusing native failures.

## Variants

`TMessagesProj_App` defines flavor dimension `minApi` with `afat` (split APK) and
`bundleAfat` (bundle), plus `debug` and `release` build types.

Verified tasks:

```bash
# Debug
./gradlew :TMessagesProj_App:assembleAfatDebug -PforkAbi=arm64-v8a

# Release (R8 enabled, signed) — only with authorization
./gradlew :TMessagesProj_App:assembleAfatRelease -PforkAbi=arm64-v8a
```

The public tree ships only the POSIX `gradlew`; there is no `gradlew.bat`. On Windows run
the wrapper JAR directly:

```powershell
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain `
  :TMessagesProj_App:assembleAfatDebug -PforkAbi=arm64-v8a
```

`-PforkAbi` accepts `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`; anything else throws a
`GradleException`. Without `-PforkAbi`, all four ABIs are included. The property is only
read by the `afat` flavor; `bundleAfat` ignores it and always includes all four ABIs.

## Versioning

- `gradle.properties`: `APP_VERSION_CODE`, `APP_VERSION_NAME`, `APP_PACKAGE`.
- `defaultConfig.versionCode = Integer.parseInt(APP_VERSION_CODE)`.
- The APK's effective version code is **overridden**:
  `output.versionCodeOverride = defaultConfig.versionCode * 10 + abiVersionCode`
  (`abiVersionCode` differs per flavor). Record the effective value from the built APK,
  not only the property.
- `versionName = APP_VERSION_NAME`.
- Do not bump a version as part of an unrelated change.
- The public repository ships a placeholder `APP_PACKAGE`; the real identity is supplied
  by the build environment. Verify the built APK's package instead of assuming.

## Application identity

- `debug` appends `.beta` as `applicationIdSuffix`. The `release` build type has **no**
  suffix, so `assembleAfatRelease` produces exactly `APP_PACKAGE`.
- `.web` belongs to the standalone variant module, not to `TMessagesProj_App` release.
- Debug and release are separate installations.
- Do not change the suffix or package casually: it breaks update continuity.
- Confirm the final package with `aapt dump badging` and `verify_apk.py`.

## Signing

Release signing reads, in order of preference, environment variables with fallback to a
local keystore path:

- `BERRYGRAM_RELEASE_KEYSTORE`
- `BERRYGRAM_RELEASE_STORE_PASSWORD`
- `BERRYGRAM_RELEASE_KEY_ALIAS`
- `BERRYGRAM_RELEASE_KEY_PASSWORD`

Rules:

- Never echo, log, commit, or screenshot these values.
- Never add a keystore to Git.
- Preserve the signing identity: a new key prevents normal Android updates of installed
  builds.
- Back up the key and credentials outside the repository.
- Verify the certificate fingerprint of the built APK and compare with the previously
  published release.

## R8

- `release` has `minifyEnabled true` with `proguard-android-optimize.txt` and
  `TMessagesProj/proguard-rules.pro`.
- New reflectively reached classes need keep rules. Do not disable R8 to silence a
  `ClassNotFoundException`.
- The release build is the only valid performance/size measurement; a debug build proves
  nothing about R8 behavior.

## Credentials in the app

BerryGram embeds the configured Telegram API ID/hash at build time. The owner permits this
in a distributable APK but not in Git files, logs, or release notes. Local builds use
`FORK_TELEGRAM_API_ID` / `FORK_TELEGRAM_API_HASH`. Optional `FORK_MAPS_API_KEY` and
`FORK_SAFETYNET_API_KEY` affect maps and some login-verification paths.

## APK verification

```powershell
python fork-tests/verify_apk.py <apk> --aapt <aapt.exe> --package <expected.id> --version <expected.version>
```

`verify_apk.py` reads `aapt dump badging` / `xmltree` / `configurations` and checks:
package name, `versionName` (not versionCode), app label `BerryGram`, that `arm64-v8a` is
present (it does **not** reject extra ABIs), `testOnly`/`debuggable` flags, and the `uk`/`ru`
locale resources. It does not install the APK and does not verify signatures.

Check separately, and record each result:

- `aapt dump badging` → package, **versionCode** (the effective overridden value), versionName,
  sdkVersion, targetSdkVersion, native-code ABIs. `verify_apk.py` does not check versionCode
  or ABI exclusivity, so this manual read is mandatory.
- `apksigner verify --print-certs` → signer certificate, v1/v2/v3 schemes.
- `unzip -l` (or `apktool`) → expected `lib/arm64-v8a/*.so`, and no unexpected ABIs.
- SHA-256 of the final file.
- No `android:testOnly="true"` in the merged manifest.
- No secrets, tokens, `.env`, or keystore material inside the APK.
- Source correspondence: the APK was built from the recorded commit.

Installation and update-over-previous-build behavior are `MANUAL TEST REQUIRED`; no script
in the repository performs them.

## Release safety gate

Do not publish unless all are true:

1. Release/R8 build succeeded from a clean, recorded commit.
2. Application ID matches the intended product identity.
3. ABI is `arm64-v8a` (unless another ABI is explicitly intended).
4. minSdk is 23 and target/compile SDK are 36.
5. versionCode/versionName are intended and the effective code was read from the APK.
6. Release signing certificate matches the previous release.
7. No `testOnly`, no debug suffix on a production artifact.
8. APK integrity checks pass; SHA-256 recorded.
9. No secrets in the artifact or the release notes.
10. Release asset name matches `UpdatePolicy.validAsset(tag, name)` and the published digest
    is `sha256:<64 hex>` matching the uploaded file; installed clients depend on both.
11. Known critical regressions are zero; remaining gaps are listed as
    `MANUAL TEST REQUIRED`.
12. GitHub release is marked prerelease when the build is not stable.
13. Explicit owner authorization for publication exists.

`build-with-credentials.ps1` can perform a credential/signing validation pass
(`-ValidateOnly`) before a build; use it rather than printing credentials to inspect them.

## Updater compatibility

- `GithubUpdateClient` fetches release metadata; `UpdatePolicy` validates version
  comparison, repository, asset name, digest (`sha256:<64 hex>`), saved release, and
  identity (package, installed version, signer set) via `validIdentity(...)`.
- `UpdatePolicy.enoughSpace(available, total, existing)` guards free space.
- `UpdatePolicy.shouldShowRelease(candidate, installed, betaChannel)` gates the beta
  channel.
- Changing asset naming, digest format, or signer handling breaks installed clients.
  Treat the updater contract as public API.
- An APK must be installable over the previous release: same package, signer, and a
  non-decreasing version code.

## Do not

- Build or publish an APK for a documentation task.
- Publish a Beta branch or APK without a separate explicit authorization.
- Push a branch, force push, delete a branch, or change Actions/settings as part of a docs
  change.
- Report "release ready" while any manual gate is unmet.
