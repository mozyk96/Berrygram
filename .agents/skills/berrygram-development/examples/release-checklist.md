# Example: Release Checklist

Use only with explicit authorization to build and publish. A documentation-only change
must not run this checklist.

## A. Source state

- [ ] Working tree clean except known submodule data noise.
- [ ] Branch and commit recorded.
- [ ] No unpublished private work is being published accidentally.
- [ ] Submodules initialized recursively.
- [ ] Changelog and version are intentional for this build.

## B. Environment

- [ ] JDK 17 active.
- [ ] `ANDROID_HOME` points at the SDK with platform 36 and build tools 36.0.0.
- [ ] NDK 27.2.12479018 available.
- [ ] Gradle wrapper 8.13 used.
- [ ] Release signing variables provided locally (never printed):
      `BERRYGRAM_RELEASE_KEYSTORE`, `BERRYGRAM_RELEASE_STORE_PASSWORD`,
      `BERRYGRAM_RELEASE_KEY_ALIAS`, `BERRYGRAM_RELEASE_KEY_PASSWORD`.
- [ ] Telegram credentials provided via `FORK_TELEGRAM_API_ID` / `FORK_TELEGRAM_API_HASH`
      when required.

## C. Tests before build

- [ ] `pwsh -NoProfile -File fork-tests/smoke.ps1` → pass, output recorded.
- [ ] Updater/package changes: `fork-tests/updater-package-fixture.ps1` run.
- [ ] Release metadata changes: `fork-tests/github-fixture.ps1` run.
- [ ] Source checks relevant to the change run (`test_auth_privacy.py`,
      `test_berrygram_resources.py`, `test_release_scope.py`, Node checks).
- [ ] Known regressions listed; critical ones are zero.

## D. Build

- [ ] `:TMessagesProj_App:assembleAfatRelease -PforkAbi=arm64-v8a` from the recorded
      commit.
- [ ] Build log reviewed for R8 warnings about fork classes.
- [ ] No `minifyEnabled false` workaround.

## E. APK verification

- [ ] `python fork-tests/verify_apk.py <apk> --aapt <aapt> --package <id> --version <ver>`
      → pass. (It checks versionName only, not versionCode or ABI exclusivity.)
- [ ] `aapt dump badging`: package, versionCode (effective, after the ×10 + abi offset),
      versionName, sdk 23, target 36, native ABI exactly `arm64-v8a`.
- [ ] `apksigner verify --print-certs`: certificate matches the previous release.
- [ ] No `testOnly="true"`.
- [ ] Native libs present for the intended ABI only.
- [ ] APK signature schemes valid.
- [ ] SHA-256 computed and recorded.

## F. Security

- [ ] No keystore, `.env`, service JSON, or token in the tree or APK.
- [ ] No credentials in release notes or commit messages.
- [ ] No user data or personal paths in artifacts.
- [ ] Updater trust checks unchanged or intentionally versioned.

## G. Installability

- [ ] Fresh install works on an emulator (MANUAL/emulator).
- [ ] Update over the previously published build works (same package + signer,
      non-decreasing version code).
- [ ] Downgrade is rejected as expected.
- [ ] App launches and reaches the main screen.
- [ ] No crash on first run.

## H. Publication

- [ ] Owner authorization for publication exists.
- [ ] Release tagged immutably (`vX.Y.Z[-beta.N]`).
- [ ] Marked prerelease unless stable and authorized as stable.
- [ ] Release notes list changes, evidence, and `MANUAL TEST REQUIRED` gaps.
- [ ] Assets and SHA-256 attached.
- [ ] Asset name matches `UpdatePolicy.validAsset(tag, name)` and the release digest metadata
      is `sha256:<64 hex>` matching the uploaded file (installed updaters depend on both).
- [ ] No force push, no branch deletion, no settings/Actions change.

## I. Post-publication

- [ ] Verify the public tag resolves to the intended commit.
- [ ] Verify the asset downloads and its SHA-256 matches.
- [ ] Update the state/handoff file (`CONTINUE.md` or equivalent) with the published state and
      remaining gaps.
- [ ] Record any incident or rollback decision.

## Reporting

State the outcome per gate: pass, fail, or not run. Never write "release ready" while any
gate is "not run" without labeling it a gap.
