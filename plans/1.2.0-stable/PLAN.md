# Stable1.2.0 promotion

- Freeze runtime at owner-tested a41d059 (Beta9). Only gradle version/name, docs and test expectations change. Base7257 -> APK72579.
- Run smoke and updater package/download/channel fixtures: Stable/Beta selection and Beta9->Stable version/code/signer compatibility. No physical installation claim or phone access.
- Commit, Release/R8 build, verify embedded source, manifest/resources/ABI/API23/signature/alignment/ZIP/SHA, package/signer continuity, changed-source/history secrets.
- Push sanitized source/exact immutable v1.2.0 tag. Upload APK/checksum to draft normal release, verify digest, publish Stable, verify public download/actual updater channels. Save final progress and stop. No1.3.0.

Owner explicitly tested Beta9 on a physical phone and approved Stable. Stable upgrade installation remains distinct from static package compatibility evidence.

## Completed release evidence

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
