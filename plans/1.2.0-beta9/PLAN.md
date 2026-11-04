# Beta 9 release candidate: native headers

1. DialogsActivity.java / ContentView measure/layout/draw, stories visibility, header helpers: remove floating overlay/reparent/surface ownership and compact-story geometry; restore native ActionBar and native story/search reservation. Keep quick lock in native menu, Mini Dock and existing handlers. Verify no overlay parent, native search/selection/title routes, production scroll budget and source regression tests.
2. ChatActivity.java / ChatAvatarContainer.java / ActionBar.java: remove experimental floating chat layout/calls; preserve native back/avatar/title/status/menu/call geometry. ForkAppearanceSettingsActivity.java/ForkChatHeaderSettings.java: remove toggle and ignore legacy preference. Verify saved opt-in cannot activate retired geometry, native methods retained, compile.
3. DialogStoriesCell.java: remove compact-only experimental mode without changing native Stories feed/interaction. Check native story collapse/scroll restoration. Use native scrollYOffset/stories/search collapsing rather than introducing a second header animation system. Stable native behavior takes priority; full toolbar hide only where native supports it.
4. Keep shared Blur/Liquid popup rendering, menu morph policy, Mini Dock, camera, updater and phrase code intact. Remove retired floating-specific tests from active smoke, replace with native header regression tests. Run affected tests and smoke once as appropriate; no FPS/account/device automation.
5. gradle.properties/CHANGELOG: beta.9, base code7256 => ARM64 code72569, higher than72559. One principal Release/R8 build after source commit. Verify package/min23/ARM64/testOnly/debug flags, permanent cert/API23 signature, ZIP/alignment/SHA/embedded source and changed-source secrets. Publish immutable v1.2.0-beta.9 prerelease+APK+checksum; verify public download and Beta updater ordering. No Stable. Manual layout/account acceptance remains owner testing.

Status: implemented and published as Pre-release; owner manual acceptance pending. Physical phone untouched. Existing Beta8 preserved in immutable Git tag.

## Implementation and technical checkpoint
- Native main/chat headers restored in149cbff; root source comparison confirms no changes to MiniDock/camera/updater/phrase integration. Retired overlay, compact Story mode and chat wrappers removed. Legacy preference always ignored; toggle removed.
- Smoke passed after fixing an existing test invocation cwd dependency. Native geometry/search/selection/sliding-parent, preference compatibility, popup/shader and lifecycle checks passed. Changed-source secret scanner:0 findings.
- Native Stories/search collapse exists, but whole toolbar did not hide. Added a bounded translation of the native ActionBar after native rows collapse, with one shared offset for toolbar, native clipping, Stories/tabs and capture. No overlay, remeasure, new animator or saved scroll-position mutation. Scroll-up reveals; search/selection/preview/pan/right-sliding/top restores. Honors disableActionBarScrolling. Pending input applied after native geometry update so a single cross-boundary swipe is not lost. Production-method fake-host regression covers one200px event, no-scroll redraw, upward reveal and context/top resets. Self-only Stories use the same translation.
- First native-header Release/R8 succeeded in9m2s. Final Release/R8 repeat is necessary to include the required whole-toolbar scroll behavior; no per-file APK builds. Manual device rendering/scroll/touch/account/installer tests remain owner acceptance, not automated passes.

## Final verification and handoff

# Beta 9 Release Candidate published — 2026-11-04

- Pre-release: https://github.com/mozyk96/Berrygram/releases/tag/v1.2.0-beta.9
- APK: https://github.com/mozyk96/Berrygram/releases/download/v1.2.0-beta.9/BerryGram-1.2.0-beta.9-arm64-v8a.apk
- Exact built/tagged source: a41d059234afe97e338d35a4fd00f44d44dc62a7. Source commits149cbff (native headers), efdc9b5 (bounded toolbar scrolling), a41d059 (correct native fragment invalidation).
- Version1.2.0-beta.9, ARM64 versionCode72569 > Beta8 72559, org.example.forkclient, minSdk23. Permanent signer retained: b3ea9ca232ae03ebeab5bbc2729a306fb5eacd0520104fc52450a421efc722a5.
- SHA256: 89a03aff257ec4b443a20b3d60c4b286059cc4c33d50fe7d3ca0a6eb7b2fd6ff. Public unauthenticated download and GitHub asset digest matched.
- Final Release/R8 passed7m16s. ZIP integrity, manifest resources/identity/version, non-debug/non-testOnly, min23, ARM64-only,16KB ZIP alignment, API23 v1/v2 signature, embedded exact source revision passed. Baseline v1 metadata warnings remain; v2 verification passed.
- Restored native main/chat headers, Stories/search reservation and native control parents. Removed chat-header toggle and ignored old opt-in. Bounded toolbar translation after native row collapse; upward scroll reveals and search/context/top resets. No overlay/remeasure/morph. MiniDock/camera/updater/phrases/graphic popup implementation retained.
- Smoke passed for native restoration; subsequent changed scroll production-method tests passed (cross-boundary200px swipe, no-scroll redraw, upward reveal, context/top reset, translated clipping). Final Android compilation passed. Source/commit secret scanner had0 findings; configured private values absent from reviewed history.
- Actual published GitHub JSON selects Beta9 via older Beta1 updater parser and orders it above Beta1..8. Installer interaction/update acceptance NOT device tested.
- Device graphics/scroll smoothness/touch, authenticated chats/Stories/archive, calls/media/notifications and actual installation remain OWNER MANUAL TEST REQUIRED. No physical phone accessed. No FPS or battery claims.
- No build remains running. Await owner testing; do not publish Stable or start1.2.1/1.3.0. Keep published tag/APK immutable. logs/artifacts: work/release-120-beta9 and work/berrygram-120-beta9.log.

