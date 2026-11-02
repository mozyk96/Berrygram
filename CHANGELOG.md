# Changelog

## 1.0.4 — in development

- Keep the Ukrainian BerryGram resources in release APKs and follow the language selected in Telegram.
- Add a scoped reset icon with confirmation to the custom-text editor and simplify phrase labels.
- Extend updated menus with rounded surfaces and opt-in blurred backdrops on supported devices; visual/device validation is pending.
- Restrict single-ABI native builds to the requested architecture and verify Ukrainian APK resources.

## 1.0.3 — in development

- Correct the test-only build configuration that caused normal Android installers to reject 1.0.2.
- Verify release APKs are neither test-only nor debuggable before distribution.

## 1.0.2 — in development

- Added resumable updater downloads with range validation and full-file digest verification; Android installer and real release tests remain pending.
- Display the selected camera and video quality in settings; visual and device validation remain pending.
- Keep Android 6.0 / API 23 as the minimum supported version.

## 1.0.1 — in development

- Began an expandable GitHub updater; its device, network and installation scenarios remain unverified.
- Extended experimental popup styling and refined the BerryGram settings screen. Visual and accessibility validation are pending.
- Prepared a draft Android CI workflow. It has not yet run on GitHub.

## 1.0.0 — release candidate

- Added video-message camera and quality preferences, voice quality modes, experimental panel styling and a custom text editor.
- The Debug APK builds and launches on an ARM64 device. Manual account and feature acceptance tests are pending; Firebase push is not configured.
