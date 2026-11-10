## 1.4.3 — in development

- Moved the official updater into a dedicated screen with verified download state, byte progress and an explicit installation action; existing release verification is retained.
- Placed graphics controls beside theme and bubble settings, enabled wrapping for switch labels in unified sections, corrected two density-scaled text sizes and a Ukrainian storage label.
- Reject incompatible APK architectures. Added opt-in scheduled background downloading with network selection, selected-network requests and cached APK re-verification; Android runtime validation remains pending. Automated coverage includes updater state, routing and existing updater fixtures. Device and accessibility validation remain pending.

## 1.4.2 — real device stabilization: one settings system, no `LOC_ERR:null`, stable navigation

- One settings system: every Berrygram feature is now a block inside the Telegram screen that owns it. Chat Settings carries effects, reactions, message menu, gestures, custom text, media and input; Privacy and Security carries Ghost Mode, deleted message preservation, edit history, archive privacy and retention; Notifications carries the diagnostics; Data and Storage carries the archive browser and the import; Language carries the translation options; Power Saving carries the device analyzer and the performance profile. The duplicated Berrygram rows are gone from the root, only the Update Center stays in the help section. The shared blocks live in `ForkSettingsBlocks` and `ForkSettingsSection`.
- `LOC_ERR:null` is fixed at its root: the resolver of `LocaleController` only knows the upstream Telegram language packs, so every fork string ended up as an error label. The resolver now falls back to the packaged Android resource, the Performance Center uses the fork locale for its own labels and the strings are covered by a regression test that fails when a runtime resource is missing.
- Navigation: the pager normalises its position with its own measured width instead of the display width, the indicator follows the pager without a second frame loop, the tab selection only changes when the page really changes and the anchor search stays inside the visible tabs. A cancelled swipe and a rapid tab switch can no longer fight each other.
- Archive: the database access is serialized, the archive instance is created atomically, the write of an incoming message and the clearing of the archive run on the archive executor instead of the UI thread, and the archive screen releases its importer listener when it is closed.
- Layout: every settings screen reserves the space of the floating bottom navigation and of the system navigation bar from the real window insets, so the last row is never hidden behind the navigation. The Berrygram screens use rounded grouped surfaces with a matching pressed state, section headers and the shared floating navigation header.
- Tests: new localization regression test, settings inset regression test, updated unified settings and archive tests, and two Android instrumentation tests for the runtime resources and the settings destinations.
- Verified: Kotlin/Java compilation, the full smoke suite, the resource validation and a signed arm64 release with R8. No physical-device testing was possible in this environment, the owner checklist lists every screen and gesture that still needs a real phone.

## 1.4.1 — unified navigation, settings, archive import, translation and one official update stream

- Synchronised bottom navigation: the indicator renders the pager position directly (taps, drags and cancelled drags), the independent spring animations are removed, long press stays stable and reduced motion disables the stretch. The frame sync stops when the pager is idle.
- Unified settings: the separate top-level Berrygram entry is gone; Berrygram rows live directly after the matching Telegram sections (privacy, appearance, language, notifications, data, power) and About Berrygram sits above the version row. The old hub redirects into the unified screen.
- Quick Ghost Mode button in the chats toolbar toggles the per-account master switch, long press opens stealth settings; explicit "Mark as read", secret chats and TTL content keep their behaviour.
- Berry Archive history import: create/update with a chat picker (single, multiple, all), live progress with the current chat and Cancel, resumable paginated `messages.getHistory`, two durable cursors, forward gaps filled, service/secret/TTL content excluded, clearing an archive resets its cursors.
- Translation: the selected target language and the opt-in automatic mode are applied when a chat opens through Telegram's own translation service; the screen shows the real language name, empty selection keeps automatic detection, secret chats and Saved Messages are skipped.
- Performance/stability: idle indicator does not wake the main thread per frame, import cursors are saved after every page, FLOOD_WAIT is cancellable, large deletion updates are chunked in SQL.
- One official update stream: the Stable/Beta channel selector is removed. The updater reads normal GitHub Releases, ignores drafts and every prerelease (including historical Beta/RC tags), selects the newest official release by parsed version metadata and never offers an older version. The obsolete channel preference is removed safely; checksum, package, signer, progress and installer checks are unchanged.
- Verified: full smoke suite, updater fixtures, live GitHub API checks, Kotlin/Java compilation and a signed arm64 release with R8 (versionCode `72779`, SHA-256 `ac30bda056ef435924252eac4affcf3d9eeb25961de9d585ed208ba5cb93900a`). Remote FCM push is not configured and no physical-device testing was performed.

## 1.3.0-beta.5 — Glass 2.0, adaptive camera and voice engine

- Experimental Glass 2.0: independent transparency, blur, frost/matte, saturation, edge light, highlight, shadow depth, tint and corner radius, plus Clear/Frosted/Balanced/Deep presets and an in-place live preview. Existing Experimental Glass appearance is migrated, and popup, Mini Dock and reaction surfaces use the real Telegram blur path with safe fallbacks.
- Round-video settings now embed the requested panel: two circular icon-only controls on the left (preview power and camera switch) and a large circular live preview on the right, with strict pause/detach cleanup.
- Adaptive Camera Engine: real Camera2 capability discovery, Automatic/Quality/Balanced/Performance/Custom profiles, JPEG quality, round-video bitrate and FPS limits, validated Camera2 FPS ranges and advertised OIS/EIS stabilization.
- Berry Voice Engine: Standard/Automatic/Clear/Natural/Custom profiles, real platform noise suppression, AGC, echo cancellation on request, microphone source selection, profile-aware Opus bitrate and a local record/play/delete test tool.
- New pure-policy tests for glass, camera profiles and voice profiles; full smoke, Debug compile and Release/R8 passed. Beta 5 is a pre-release for owner acceptance; device camera/voice/glass behaviour still needs physical testing.

Beta for owner acceptance, not Stable.

## 1.3.0-beta.4 — updater/API hotfix

- Version/code bump so the in-app updater can move from the installed Beta 3 test build (`72609`) to the credential-bearing release.
- Includes all Beta 3 fixes: message-menu swipe-back crash, Edit validation, restored Telegram API credentials, themed passcode fallback, Material tint removal, Ukrainian/Russian label repair, responsive Performance Center, thermal/low-RAM awareness, preference caches and optional Experimental Glass.
- Beta 3's published final APK had the same version as the installed device test build, so the updater correctly treated it as already current. No updater logic was weakened; this is a standard version increment.

Beta for owner acceptance, not Stable.

## 1.3.0-beta.3 — Android stability, performance and glass

- Fixed the message-menu crash caused by the custom More page being added to a popup without a swipe-back container (NPE in `addViewToSwipeBack`). Hidden actions remain reachable; the popup requests the container whenever hidden menu actions exist and degrades safely otherwise.
- Gesture actions now validate message/account/edit eligibility before native dispatch, use a single scoped execution path, show an accurate edit-unavailable message, and never recurse through the double-tap callback. Verified on device: tap→Edit opens the native edit flow and reports a server-side edit refusal without crashing.
- Removed the unwanted dynamic-neutral "Material" surface tint. Settings now derive from Telegram theme colors; legacy `style=material` migrates to Blur. Fixed double-encoded Ukrainian/Russian BerryGram graphics labels.
- Lock screen: evicted/missing wallpapers or invalid bitmaps fall back to the Telegram theme wallpaper color instead of an opaque black frame; darkening overlay is applied only after a wallpaper was actually drawn.
- Performance Center rebuilt to update rows in place instead of recreating the page. Removed nonfunctional controls; profile, custom effects, Lite Mode, storage/cache and privacy-safe diagnostics copy are wired to real actions. Added thermal/low-RAM-aware effect reduction and cached gesture/reaction/performance preferences.
- Added optional Experimental Glass style alongside Blur and Liquid Glass: higher translucency, stronger edge light and softer shadows using the existing Telegram blur source, with API/capability fallback to theme colours.
- Existing 1.2.0 headers, Mini Dock, camera, voice, custom text, updater and account isolation retained.

Beta for owner acceptance, not Stable. Device lock-screen and Experimental Glass visual quality still require owner confirmation; no Stable or Desktop release.

## 1.3.0-beta.2 — Android stability and performance

- Gesture dispatch now uses one re-entrant-safe path with native tap/long-press eligibility, correct unavailable-action feedback, and a dedicated double-tap reaction helper instead of a recursive callback.
- Quick-reaction filtering is policy-driven over the native allowed list: ordered favorites, duplicate suppression, and a native fallback when no favorite is allowed. The full picker and server-side restrictions remain native.
- Added the BerryGram Performance Engine profile policy (Automatic, Maximum smoothness, Battery saver, Custom) with a Performance Center, non-sensitive diagnostics and device-local persistence. Battery/automatic/custom reduction affects only optional BerryGram surfaces and reveal animations; Telegram Lite Mode settings are not overwritten.
- Gesture preferences are cached per account/user so swipe tracking no longer reads SharedPreferences on every touch move.
- New regression coverage for gesture re-entry/flags, reaction filtering/fallback and performance profile resolution.
- Existing 1.2.0 headers, Mini Dock, graphics, camera, voice, custom text, updater, menus and per-account isolation retained.

Beta for owner acceptance, not Stable. Physical touch timing, reaction sending, menus, account switching and restart behavior still require owner testing. No Desktop implementation until Android 1.3.0 Stable is approved and published.

## 1.3.0-beta.1 — Android Total Customization

- Shared native-action registry and preferences isolated by account slot plus signed-in user identity.
- Gesture editor for confirmed taps, double taps, long press and horizontal swipes. Plain-text tap overrides preserve interactive media/links/bot controls; native selection and edge navigation retain priority. Native menu permissions and destructive confirmations remain authoritative.
- Opt-in 1..4 ordered quick standard reactions from the native enabled catalog, native allowed-list intersection and full-picker access; double-tap reaction shares the gesture binding.
- Message-menu drag ordering, visibility, pinning, structure preview/reset. Hidden supported actions remain in scrollable More; native unknown/special actions and native Delete warnings/suppression are retained.
- Localized en/uk/ru editors; existing1.2.0 headers, MiniDock, graphics/camera/voice/text/updater retained.

Beta for owner acceptance, not Stable. Actual gesture timing, touch, accessibility, reaction sending, menus and restart/account scenarios require device testing. Tap overrides intentionally apply only to noninteractive plain text; paid/tag/effect reaction panels remain native. No Desktop implementation until Android1.3.0 Stable is approved and published.

## 1.2.0 Stable

Promotes the owner-tested Beta9 code without functional or design changes.

- Native Telegram main/chat headers with bounded toolbar scrolling.
- Mini Dock, video-message camera/quality selection and voice quality settings.
- Blur and Liquid Glass on supported surfaces, with device/power fallbacks.
- Custom phrases/names/statuses and verified GitHub Updater (Stable/Beta channels).
- Android6+ (API23), ARM64, same app identity and permanent release certificate; code72579 > Beta9 72569.

Owner tested Beta9 on a physical phone and approved Stable. This Stable APK's physical installation is not automated; package/version/signer compatibility is checked separately. Backdrop-free surfaces retain opaque fallback. Graphics depend on device/power capabilities.

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
