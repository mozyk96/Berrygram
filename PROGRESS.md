# Android 1.3.0 Stable published — 2026-11-06

- Release (not pre-release): https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0
- Source/tag `dd0f71a04154dc39ee016a0e9ea80a6b7cf83738`; version `1.3.0`, code `72639` (> Beta 5 `72629`).
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0/BerryGram-1.3.0-arm64-v8a.apk
- SHA-256: `80e0be6a5aa1afc3f8251aedad686f9c42cb5e62c42c3c61c3582aa7ec7f882d`; 41874894 bytes.
- Stable audit fixes: removed the decorative Appearance preview row; glass provider wrappers re-evaluate style/config on each color refresh; glass preview no longer allocates paint/rect objects per frame (cached colors, radii, strokes, shadow); Appearance intensity stays consistent with the Experimental Glass blur value; corrupt preference values in Glass/camera/voice settings are repaired instead of throwing; round-video power state turns off on denied permission or timeout; preview also refuses to start camera while off-screen or window-hidden; voice settings keep scroll position, and stale record-permission results/pending starts are cancelled; platform voice-effect availability cached.
- Performance work: bounded existing caches retained, per-frame allocations removed from the glass preview, header drawables no longer rebuilt on every slider tick, round-video camera/GL work stops when off-screen. Baseline/Startup Profiles and R8 rule pruning were evaluated and deliberately deferred (no fabricated profiles; reflection/JNI risk).
- Verified: full `fork-tests/smoke.ps1` (new `GlassSettingsTest` plus gesture/reaction/glass/camera/voice/updater/performance suites), Debug Java compile, Release/R8 with injected credentials, APK identity/version/minSdk/ABI/v1+v2 signature/permanent certificate/non-debug/non-testOnly/16 KB alignment/no keystore/API credentials/embedded revision.
- Not verified: physical-device behaviour of this exact APK — owner's phone was not connected over ADB and the local emulator cannot start (hypervisor driver missing). Published with this gap explicitly disclosed at the owner's direction.
- Previous tags/assets (v1.2.0, v1.3.0-beta.1…beta.5) unchanged. Desktop remains gated.

# Android 1.3.0 Beta 5 published — 2026-11-06

- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.5
- Source/tag `93794233dffb3ff53622792a023718124ee8e18d`; version `1.3.0-beta.5`, code `72629`.
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.5/BerryGram-1.3.0-beta.5-arm64-v8a.apk
- SHA-256: `dacc6aac28e214e14c9c0e31eb575f92069703a22c6e4d85eec44f57b0f81d6f`.
- Experimental Glass 2.0: independent transparency, blur, frost, saturation, edge/highlight, shadow, tint and radius; presets; live preview; popup/Mini Dock/reaction coverage with fallback.
- Embedded round-video preview with two circular icon-only controls and circular live preview; separate test-screen entry removed.
- Adaptive Camera Engine: Camera2 capability discovery, Automatic/Quality/Balanced/Performance/Custom profiles, JPEG quality, round-video bitrate/FPS limits, validated Camera2 FPS ranges and advertised OIS/EIS.
- Voice Engine: Standard/Automatic/Clear/Natural/Custom profiles, platform NS/AGC/AEC when available, source selection, profile-aware Opus bitrate and a local record/play/delete test tool.
- Verified: full smoke (including new Glass/Camera/Voice policy tests), Debug compile, Release/R8, APK identity/signature/ABI/minSdk/flags/alignment/source/API credentials, public download digest. Latest Stable remains 1.2.0.
- Not verified: physical-device behaviour of this exact APK. The owner's phone was not connected over ADB, and the local emulator could not start because the Windows Android Emulator hypervisor driver is not installed.
- Post-release branch commit `533d604` stops the embedded camera preview when scrolled out of view; it is not included in the published Beta 5 APK.

# Android 1.3.0 Beta 4 published — 2026-11-06

- Hotfix over Beta 3: version `1.3.0-beta.4`, code `72619`, source/tag `7bf93d3cff1490f93b9f67032a08819669974b55`.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.4
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.4/BerryGram-1.3.0-beta.4-arm64-v8a.apk
- SHA-256: `4e10c9f3a43604fe039f00205c77e817d85218867f50bfcf5e154bfb5659c604`.
- Reason: the Beta 3 phone test build already had version `1.3.0-beta.3`/code `72609`; the updater correctly saw the published Beta 3 as already installed. Beta 4 is a normal version bump and carries all Beta 3 fixes plus the restored API credentials.
- Verified: Debug/Release/R8, smoke, APK identity/signature/ABI/minSdk/flags/source/native-lib/alignment/API credentials, GitHub digest and independent public download. Updater release list now starts with Beta 4; latest Stable remains 1.2.0.
- Final Beta 4 APK was not installed before publication; owner should update from the in-app updater and verify network Edit, gestures, reactions, Experimental Glass and themes.

# Android 1.3.0 Beta 3 published — 2026-11-06

- Branch `dev/1.3.0`; source/tag `ec5517afa8147fd9688be2904b0bc19112ec8e0c`.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.3
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.3/BerryGram-1.3.0-beta.3-arm64-v8a.apk
- SHA-256: `5700cbd0c209645097089129af4bb7db516f7eccf40788a397b051b8b7d69323`.
- Device-confirmed: Beta 2 menu crash (`addViewToSwipeBack` NPE) fixed; lock-screen themed background normal; Performance Center responsive; Experimental Glass selectable. Final credential-bearing APK not installed before publication at owner request.
- Fixed empty Beta 2 API credentials (`CONNECTION_API_ID_INVALID`), removed dynamic-neutral Material tint, migrated `material`→Blur, fixed Ukrainian/Russian mojibake labels, added passcode wallpaper fallback, cached preferences, thermal/low-RAM awareness, and optional Experimental Glass.
- Smoke, Debug compile, Release/R8, APK identity/signature/ABI/minSdk/flags/source/native-lib/SHA/alignment checks passed. No Stable 1.3.0; no Desktop; no data clearing or destructive account actions.
- Owner manual acceptance remains pending, especially network Edit after credential restoration, all gestures, reactions, Experimental Glass visual quality, themes and in-place update.

# Android 1.3.0 Beta 2 published — 2026-11-05

- Branch: `dev/1.3.0`. Source/tag commit: `b1ce28e55e9755b95631cfe009e21f01156aea3b`.
- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.3.0-beta.2
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.3.0-beta.2/BerryGram-1.3.0-beta.2-arm64-v8a.apk
- SHA-256: `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`. Public unauthenticated download and GitHub asset digest matched.
- Fixed gesture dispatch (native option gate for media types, account/message validation, single execution, no recursive reaction callback, unavailable feedback, selected-state cleanup) and quick-reaction filtering (native-index policy, order/dedup, empty fallback, defensive state). Added performance profiles/Performance Center and gesture preference cache.
- Full smoke suite passed; debug Java compilation and Release/R8 passed (8m28s). Package `org.example.forkclient`, code `72599` > Beta 1 `72589`, minSdk 23, ARM64, non-debug/non-testOnly, existing permanent certificate, v1/v2 signatures, 16 KB ZIP alignment, source revision and native-library identity to Beta 1 verified.
- Live GitHub data: Beta 2 is a pre-release; `releases/latest` remains Stable 1.2.0. No Stable 1.3.0 published; Desktop remains gated.
- No physical phone, real Telegram account or crash log was used. Device touch/reaction/menu/account/restart behavior and in-place installation remain owner manual tests. No measured FPS/battery/thermal claim.

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
