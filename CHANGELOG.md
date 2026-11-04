## 1.2.0-beta.9 â€” Release candidate

- Restore Telegram native chat-list ActionBar, title, search and native Stories/search collapse and scroll reservation. Remove floating overlay and compact Stories overrides.
- Keep quick lock in the native ActionBar; preserve Mini Dock, camera, updater, phrases and graphic popup effects.
- Restore Telegram native open-chat header and contextual states. Remove experimental chat-header toggle and ignore old saved opt-in.
- Add bounded native toolbar hide/reveal after Stories/search collapse; restore toolbar for search, selection and top of list. No new header morphing or redesign. Device layout/account/installer acceptance remains manual; this is not Stable.

# Changelog

## 1.2.0-beta.8 â€” public testing

- Recompose the real compact Stories/control row and restore owned surfaces after native search/background resets.
- Remove obsolete81dp compact scroll/collapse offsets and retain the genuine self-story recorder entry.
- Correct translated archive clipping and protect initial first-row anchors.
- Refresh popup tint/ownership and Liquid strength/fallback state consistently.
- One Update action downloads, verifies and requests the system installer; preserve explicit Stable/Beta recovery safeguards and full changelog.
- Expanded source-method regression and updater validation coverage. Device/account/rendering/installer scenarios remain manual.

## 1.2.0-beta.7 â€” public testing

- Restore native scrollable Stories in the compact floating header and move round controls4dp inward/down.
- Place native chat actions before the far-right avatar and bound long title/status text.
- Group Custom Phrases controls and show current/original/changed values with improved editor focus.
- Refresh shared popup graphic policy and fix canceled-close/pre-draw lifecycle conflicts.
- Expand executable geometry, preference-reload and popup/story state regression tests.
- Backdrop-free settings surfaces retain opaque fallback; visual/account/device scenarios need manual testing.

## 1.2.0-beta.6 â€” public testing

- Separate floating main-header drawing bounds from reserved list height and measure compact stories independently.
- Enable the optional floating chat header in ordinary chats with call/search actions; keep native contextual states.
- Move and expand popup surfaces from their source anchor; restore all transient transforms on completion or cancellation.
- Add mocked animation geometry/lifecycle regression coverage, including anchors outside popup bounds and animation-disable policies.
- Keep Mini Dock unchanged. Device appearance, touch/animation behavior, account flows and installation remain for manual testing.

## 1.2.0-beta.5 â€” public testing

- Separate the normal chat-list action menu into a floating layout, retaining native contextual/search states and quick lock.
- Offer Blur and Liquid Glass, migrate Material preferences to Blur and synchronize native Liquid Glass settings.
- Add optional chat title/avatar composition and icon-only Mini Dock.
- Add optional anchored popup geometry reveals and select the round camera before recording starts.
- Pre-release: device layout, recording, Telegram account flows and update installation require manual testing.

## 1.2.0-beta.4 â€” public testing

- Remove the duplicate search-row space on the main chat list; size the search and menu controls equally and retain passcode locking in the menu.
- Open simple BerryGram settings choices in anchored Telegram popup menus.
- Consolidate the round-video camera setting into rear, front and choose during recording, migrating older preferences and using Telegramâ€™s camera switch.
- Beta: real-device layout, popup positioning, camera recording, login, messaging, calls, media, notifications and update installation remain unverified.

## 1.2.0-beta.3 â€” public testing

- Connect the main chat-list search and menu controls to the selected graphics surfaces and remove the old Telegram branding from the normal header.
- Improve popup rendering, settings rounding, header accessibility and updater channel handling.
- Beta: layout, graphics, login, messaging, calls, media, notifications and installation are not yet device-verified. BerryGram settings headers still use a solid theme surface rather than a live blur.

## 1.2.0-beta.2 â€” public testing

- Correct rectangular bitmap recording dimensions and downscale geometry.
- Rebind the refraction shader when its source matrix changes.
- Resume downloads after a short response and check available storage, with localized errors.
- Main header and full glass integration remain unfinished; visual, device, account, call and media testing remain outstanding.

## 1.2.0-beta.1 â€” public testing

- Add selectable popup styles, graphic preferences and a color picker; hardware rendering and broad component coverage remain unverified.
- Add floating headers to BerryGram settings pages; layout, camera, account and media flows need device testing.
- Add Stable/Beta update channels and prerelease version ordering. Existing 1.1.4 installs require a one-time manual Beta download.
- Main chat-list header and full 1.2.0 visual overhaul remain in development.

## 1.0.4 â€” in development

- Keep the Ukrainian BerryGram resources in release APKs and follow the language selected in Telegram.
- Add a scoped reset icon with confirmation to the custom-text editor and simplify phrase labels.
- Extend updated menus with rounded surfaces and opt-in blurred backdrops on supported devices; visual/device validation is pending.
- Restrict single-ABI native builds to the requested architecture and verify Ukrainian APK resources.

## 1.0.3 â€” in development

- Correct the test-only build configuration that caused normal Android installers to reject 1.0.2.
- Verify release APKs are neither test-only nor debuggable before distribution.

## 1.0.2 â€” in development

- Added resumable updater downloads with range validation and full-file digest verification; Android installer and real release tests remain pending.
- Display the selected camera and video quality in settings; visual and device validation remain pending.
- Keep Android 6.0 / API 23 as the minimum supported version.

## 1.0.1 â€” in development

- Began an expandable GitHub updater; its device, network and installation scenarios remain unverified.
- Extended experimental popup styling and refined the BerryGram settings screen. Visual and accessibility validation are pending.
- Prepared a draft Android CI workflow. It has not yet run on GitHub.

## 1.0.0 â€” release candidate

- Added video-message camera and quality preferences, voice quality modes, experimental panel styling and a custom text editor.
- The Debug APK builds and launches on an ARM64 device. Manual account and feature acceptance tests are pending; Firebase push is not configured.
