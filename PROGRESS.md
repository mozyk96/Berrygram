# Android 1.3.0 Beta 1 published — 2026-11-04

- Branch: dev/1.3.0. Built and tagged source: 0ce1f164567809050a30f7ec132752b90c5f11f3.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.1
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.1/BerryGram-1.3.0-beta.1-arm64-v8a.apk
- SHA-256: 0a31242c7df6466cfe1408a0f3a3106a26721b174eba9849b2c3077090146c07.
- Implemented shared action registry, account/user-isolated gestures, reaction editor and message-menu editor. Hidden menu actions retain native cells, warnings and confirmations in scrollable More.
- Smoke tests passed; Release/R8 passed in 7m45s. Final APK identity, code72589, min23, ARM64, flags, ZIP/alignment, permanent signer/API23, source revision, resources and public checksum verified.
- Git packed refs caused AGP NO_VALID_GIT_FOUND. Re-extraction from the same detached commit and incremental re-signing fixed metadata (39s; no runtime change). Development branch restored.
- Live current/legacy Updater checks: Beta selects1.3.0-beta.1; Stable and Latest remain1.2.0. No older artifact overwritten.
- Manual device/account/touch/reaction/menu/restart/accessibility/installation acceptance remains pending. Plain-text tap overrides exclude interactive/formatted targets; special reaction panels stay native. No phone or account automation performed.
- No active build. Next: owner tests Beta1, then reproduce/fix findings. Android Stable1.3.0 requires owner approval; Desktop implementation remains gated until its publication. Plans: plans/1.3.0.

# BerryGram1.2.0 Stable completed — 2026-11-04

- Release: https://github.com/mozyk96/Berrygram/releases/tag/v1.2.0 (published, non-draft, non-prerelease, Latest).
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.2.0/BerryGram-1.2.0-arm64-v8a.apk
- Exact source/tag: 60ea4ddeb16e7119c310f8beb4e29d0b7348fbb6. Runtime trees are identical to owner-tested Beta9 a41d059; only version metadata/docs/tests changed.
- versionName1.2.0; code72579 > Beta9 72569; org.example.forkclient; Android6+/minSdk23; ARM64-only. Permanent certificate unchanged: b3ea9ca232ae03ebeab5bbc2729a306fb5eacd0520104fc52450a421efc722a5.
- SHA256 e0d57f92947b52d9eb9fc4771f29afb32697c6afd91597d8cbd4d7a8b438fe25. Attached checksum, GitHub asset digest and independent unauthenticated public download matched.
- Release/R8 succeeded5m42s; manifest/resources/non-debug/non-testOnly/version/identity/API23 v1-v2 signing/ZIP/ARM64/16KB ZIP alignment/embedded source checks passed. Native libraries byte-identical to Beta9; no keystore packaged.
- Smoke and updater policy/package/download fixtures passed. Live published GitHub JSON selected Stable1.2.0 in both current channels; Beta1..9 promotion and already-installed Stable suppression passed. Older parser channel selection/semver also passed with a harness adapted to its older API (initial harness API mismatch did not require app changes).
- Changed public source/history scan0 findings; configured private values absent. No full repeated repository audit or performance benchmark.
- Owner physically tested Beta9 and approved Stable. This Stable artifact was NOT physically installed by automation. In-place update compatibility verified by identical package/cert and higher versionCode, not an actual phone install. No owner phone accessed or user data changed.
- Graphics/device capabilities and opaque fallback limits are in release notes. Existing v1 metadata warnings remain; v2 whole-APK check passes.
- No active build. Version1.2.0 is complete and published. Do not rewrite its tag/assets. Do not begin1.3.0 or other development without a new owner instruction. Logs/artifacts: work/release-120-stable and work/berrygram-120-stable.log.
