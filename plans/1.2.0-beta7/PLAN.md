# Beta 7 engineering plan
Base: public-1.2.0 / 2e88e23; Beta6 APK source a30b0cf. No phone use. [ ] pending, [~] active, [x] implemented with available checks, [!] blocked. Real-account scenarios require owner testing.

## Floating Main Header [~]
File DialogsActivity.java; updateForkMainHeaderLayout/ContentView.onMeasure/onLayout. Controls currently sit too close to top/right. Adjust actual margins by4dp; reserve matching horizontal story area using measured control width. Retain48dp square hit targets, native search/selection/archive and real rails. Risk: rotation/insets and three-control overflow. Ready: geometry tests for two/three controls and narrow widths plus Android compile.

## Stories Integration [~]
File Stories/DialogStoriesCell.java; dispatchDraw/onMeasure/setFloatingCompactHeader, native recycler adapter/touch/StoryCell measurement. Existing collapsed mini strip is non-interactive itself and subtracts old menu width a second time inside an already constrained container, hiding stories. Use native full story recycler in compact mode with bounded icon-only cells and real story viewer/pagination callbacks; native expansion layout restored outside floating state. No fake peers. Risk: state changes, transition place provider, story updates and long press. Ready: width/padding geometry tests, compile and emulator harness where available; account stories MANUAL TEST REQUIRED.

## Chat Header [~]
Files ChatActivity.java, Components/ChatAvatarContainer.java, ActionBar/ActionBar.java; updateFloatingChatHeader/onMeasure/onLayout/dispatchDraw. Avatar currently precedes native action menu. Position actual actions to left of far-right avatar and bound title/status width; retain native action handlers and fallback states. Ellipsis rather than new endless marquee. Risk: action-mode restoration and small screen overlap. Ready: focused geometry and compilation; account actions manual.

## Custom Phrases UI [~]
File ForkCustomTextActivity.java; createView/rebuild/editor dialogs. Flat dense rows obscure changed/original text. Group sections using shared settings surfaces, multiline current/original display, clear changed state, empty state and correct editor focus. Preserve manager validation, aliases, packs, reset/export/import/locales. Risk: keyboard/save and long values. Ready: existing text-engine suite plus compiled UI paths; emulator no-account harness if possible.

## Global Graphics Engine [~]
Files ForkGraphicsSurface.java, ForkSettingsLayout.java, ForkFloatingHeader.java, shared popup drawable path. Use existing effective-style dispatch across supported surfaces; fix stale resolved state and live refresh. No Material selector. Backdrop-free components must use readable honest fallback, not claim transparency is blur. Risk: unsupported GPU/API and power changes. Ready: style/settings tests and compiled providers.

## Liquid Glass [~]
Files ForkGlassSurface.java and existing blur3 drawable/factory paths as needed. Preserve real captured backdrop/shader integration, improve common tint/stroke/radius only where supported. Do not add fullscreen shader or claim physical refraction. Risk: source lifecycle/recursive capture. Ready: existing bitmap/shader lifecycle tests and release compilation; GPU appearance manual.

## Blur [~]
Same shared providers; effectiveStyle dispatch. Ensure selected Blur does not retain expensive Liquid shader when state changes. Same bounded supported components and readable fallback. Ready: dispatch/persistence tests and compilation, emulator where possible.

## Advanced Animations [~]
Files ForkSurfaceAnimation.java, ActionBar/ActionBarPopupWindow.java. Continue Beta6 source morph, repair pending pre-draw and animation-owner lifecycle if reproduced. Avoid stale cancel callbacks dismissing reopened menus. Risk: rapid reopen, detach, system disabled animations and power saving. Ready: actual production animation mock lifecycle/geometry tests; UI harness where available.

## Regression Testing [ ]
fork-tests/smoke.ps1 and focused tests; actual settings persistence, geometry, animation completion/cancel, updater parsing. Use isolated work/avd/Fork_Test emulator only, always adb -s emulator-...; never enumerate or operate physical phone. Check emulator feasibility and record exact blocker if boot unavailable. No real account. Run Release/R8 only after source changes integrated. New findings limited to touched paths.

## Release [ ]
gradle.properties beta.7/code7254 ->72549. Existing local secure build injection and release key. Verify min23/package/ARM64/testOnly/debuggable/resources/ZIP/alignment/API23v1-v2/signer/SHA and embedded source revision. Scan new public files/history for secrets. Commit source before build; tag exact APK source revision v1.2.0-beta.7. Publish nonempty prerelease with APK/checksum/limitations; verify public download and Beta updater selection. No Stable/future features.

Implementation checkpoint: actual native full Stories recycler now renders compact48dp avatars in56dp cells, with bounded clipping and native click/longpress/viewer/pagination; old collapsed mini clipping bypassed only in floating state. Controls move4dp down/left using margins. Chat actions now precede far-right avatar and text uses production menu-width policy. Phrases grouped with current/original/changed display and editor focus. Shared popup style refresh and animation ownership fixed; settings without captured backdrop deliberately remain readable opaque fallback (not real glass). Mini Dock unchanged.
Checks passed: existing smoke policies/resources, actual production animation lifecycle, extracted popup scheduling lifecycle, compact story state/render branch, header width invariants, graphics preference reload/independence, chat text bounds80cases. These are JVM/mock checks, not device UI proof.
Emulator: API35x86 image and emulator exist; hypervisor driver unavailable. Isolated software boot on5580 remains offline; UI scenarios MANUAL TEST REQUIRED until usable Android boot. Physical phone never addressed. Next: Release/R8 compile, fix any errors, APK/signature/source audit and prerelease publication.

Pre-release correction: first Release/R8 passed in8m31s. Final source inspection found stock gradient truncation still enabled; floating title/status now request END ellipsis while stock retains its gradient. A new production-setter test reproduced cached child measurement after native action width changed; setter now forceLayout only on actual width changes, with no parent-layout loop.80 geometry cases plus width-change/unchanged/hidden-action invalidation pass on production source. Final rebuild pending. Emulator software retries remained offline; no Android UI execution possible on this host. MANUAL TEST REQUIRED remains explicit.
