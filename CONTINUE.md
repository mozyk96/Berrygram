# BerryGram continuation — Android 1.3.0 Beta 2

## Published

- Branch: `dev/1.3.0`; source/tag commit `b1ce28e55e9755b95631cfe009e21f01156aea3b`.
- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.2
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.2/BerryGram-1.3.0-beta.2-arm64-v8a.apk
- SHA-256: `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`.

## Completed

- Gesture dispatch: native option gate for media-type messages, active-account/message/owner validation, one-shot execution, re-entry guard, no recursive reaction callback, unavailable-action bulletin, selected-state cleanup, per-account gesture preference cache.
- Quick reactions: native-index filter policy with configured order, duplicate suppression, empty-result native fallback, defensive list copy, layout-params null guard, full native picker retained.
- Performance Engine: Automatic/Smooth/Battery/Custom profiles, Performance Center, non-sensitive diagnostics, runtime effect reduction integrated into existing BerryGram surfaces; Telegram Lite Mode storage not overwritten.
- Prior unpublished Beta2 work from `work/public-candidate` was recovered and integrated; backup at `work/public-candidate-beta2-prior`.
- Tests added: gesture native-path, reaction filter policy, performance policy/settings, updated smoke/resource/release-scope fixtures.

## Verified

- `fork-tests/smoke.ps1`: full pass (Python 3.12 installed for fixtures).
- `:TMessagesProj_App:compileAfatDebugJavaWithJavac`: pass.
- Release/R8: BUILD SUCCESSFUL in 8m28s.
- APK: package `org.example.forkclient`, version `1.3.0-beta.2`, code `72599`, minSdk 23, ARM64 only, non-debug/non-testOnly; v1/v2 signatures; permanent certificate `b3ea9ca2…`; 16 KB alignment; embedded revision matches commit; native libraries byte-identical to Beta 1; public download SHA matches.
- GitHub latest Stable remains 1.2.0; Beta 2 is a pre-release. No Stable 1.3.0 or Desktop work started.

## Pending

- Owner physical-device acceptance: gestures on text/media/groups/channels, unavailable actions, quick reactions and full picker, reaction permissions, message menu/More, account switching, restart persistence, accessibility, in-place install/update.
- No crash log, real account or phone was used. Fix further owner findings before any Stable 1.3.0.
- Artifacts/logs: `work/release-130-beta2`; build workspace `work/public-candidate` (detached at b1ce28e with native cache).
