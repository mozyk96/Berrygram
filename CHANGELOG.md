# Changelog

## 1.2.0-beta.5 — public testing

- Separate the normal chat-list action menu into a floating layout, retaining native contextual/search states and quick lock.
- Offer Blur and Liquid Glass, migrate Material preferences to Blur and synchronize native Liquid Glass settings.
- Add optional chat title/avatar composition and icon-only Mini Dock.
- Add optional anchored popup geometry reveals and select the round camera before recording starts.
- Pre-release: device layout, recording, Telegram account flows and update installation require manual testing.

## 1.2.0-beta.4 — public testing

- Remove the duplicate search-row space on the main chat list; size the search and menu controls equally and retain passcode locking in the menu.
- Open simple BerryGram settings choices in anchored Telegram popup menus.
- Consolidate the round-video camera setting into rear, front and choose during recording, migrating older preferences and using Telegram’s camera switch.
- Beta: real-device layout, popup positioning, camera recording, login, messaging, calls, media, notifications and update installation remain unverified.

## 1.2.0-beta.3 — public testing

- Connect the main chat-list search and menu controls to the selected graphics surfaces and remove the old Telegram branding from the normal header.
- Improve popup rendering, settings rounding, header accessibility and updater channel handling.
- Beta: layout, graphics, login, messaging, calls, media, notifications and installation are not yet device-verified. BerryGram settings headers still use a solid theme surface rather than a live blur.

## 1.2.0-beta.2 — public testing

- Correct rectangular bitmap recording dimensions and downscale geometry.
- Rebind the refraction shader when its source matrix changes.
- Resume downloads after a short response and check available storage, with localized errors.
- Main header and full glass integration remain unfinished; visual, device, account, call and media testing remain outstanding.

## 1.2.0-beta.1 — public testing

- Add selectable popup styles, graphic preferences and a color picker; hardware rendering and broad component coverage remain unverified.
- Add floating headers to BerryGram settings pages; layout, camera, account and media flows need device testing.
- Add Stable/Beta update channels and prerelease version ordering. Existing 1.1.4 installs require a one-time manual Beta download.
- Main chat-list header and full 1.2.0 visual overhaul remain in development.

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
