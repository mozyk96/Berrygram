# BerryGram 1.2.0 Beta 6 implementation

Status: [ ] pending; [~] implementation; [x] implemented with relevant automated checks; [!] blocker. Device appearance and interaction remain UNTESTED until owner testing.

## 1. Floating main header [x]
File: TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java.
Classes/methods: ContentView.getActionBarFullHeight/onMeasure/onLayout/dispatchDraw/drawChild, DialogsRecyclerView.onMeasure, getMainHeaderHeight/updateForkMainHeaderLayout/usesFloatingMainHeader, updateStoriesViewAlpha.
Problem: the normal floating state still returns the full toolbar height and the recycler independently reserves ActionBar height. Header drawing geometry and content reservation are conflated.
Change: introduce separate overlay drawing height and reserved content height. Normal list reserves only the system-safe inset; controls remain in the overlay. Route recycler padding, clip bounds, progress/empty views and story origins through the appropriate quantity. Preserve normal toolbar reservation for search, archive, selection and right-sliding contexts. Keep stories clickable and avoid drawing any full-width toolbar scrim in overlay mode. Do not change Mini Dock.
Dependencies: stories collapsing/overscroll, list scroll anchoring, filter tabs/top panels, archive transitions, quick lock and menu actions.
Expected: list can scroll under floating controls; safe status inset remains; native contextual states retain their geometry.
Checks: focused source/path review; policy tests for reserved vs overlay heights and context transitions; Release compilation. Owner visual/scroll/action tests UNTESTED.

## 2. Real chat header [x]
Files: TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java and Components/ChatAvatarContainer.java; settings remain in messenger/fork/panels/ForkChatHeaderSettings.java.
Methods: updateFloatingChatHeader, ChatAvatarContainer.onMeasure/onLayout/setupFloatingChatHeader/setFloatingChatHeader.
Problem: availability guards exclude ordinary chats with native call/search icons, so the switch frequently does nothing. Header may still inherit a full scrim.
Change: enable normal supported chats, adapt title width to native action space instead of excluding call/search icons, retain avatar profile action and back/menu. Restore stock for selection/search/special states. Ensure header background drawing follows the toggle and existing glass/blur providers; refresh on resume and measurement.
Dependencies: title/status measurement, calls, avatar clicks, pinned/context overlays, font size and small widths.
Checks: inspect both toggle states and action guards; targeted policy checks and Release compile. Device visual/call/profile checks UNTESTED.

## 3. Actual popup morph [~]
Files: TMessagesProj/src/main/java/org/telegram/ui/ForkSurfaceAnimation.java, ActionBar/ActionBarPopupWindow.java, Components/ItemOptions.java, ForkChoicePopup.java.
Methods: create, startForkFluidAnimation/prepareForkFluidAnimation/postForkFluidAnimation/startAnimation/dismiss, ItemOptions.show.
Problem: outline-only clipping can be hidden by child/render-node clipping or lost in concurrent native animation; source outside popup bounds is clamped to a corner. Need prove actual paths and measurement timing.
Change: animate actual visible popup surface and content from measured anchor geometry; delay item reveal, distinguish Blur/Liquid, install after measurement/pre-draw, suppress competing native transforms, restore every property on cancel/end and protect rapid reopen generations. Pass real anchor for anchored choices and supported contextual menus. Respect system animation disable and preference.
Dependencies: popup wrappers, blur source offsets, native exit timing, notification locks, outside-touch dismissal.
Checks: automated geometry/lifecycle policy tests, source-path checks, Release compile. Real-device animation remains UNTESTED.

## 4. Integration and publication [~]
Keep Mini Dock sources unchanged. Keep applicationId, minSdk23, ARM64 and permanent signer. Set beta.6 and increasing code only after integration. Run changed-code tests, one principal Release/R8 build (retry only for concrete errors), APK manifest/testOnly/debuggable/resources/ABI/ZIP/signature/API23/alignment/SHA checks and secret/history scan. Commit source, push exact tag v1.2.0-beta.6, upload nonempty prerelease, verify asset digest/public download and updater selection. Record known device gaps. No phone ADB, Stable or future feature development.

Implementation checkpoint: main content reservation/overlay heights separated; compact stories measured independently. Normal chats no longer excluded by call/search icon preferences; title/avatar measured in remaining native action space and stock title translation reset on toggle. Popup surface now transforms from real screen anchor, caps source seed to48dp, restores render properties, starts after layout; message scrim popup paths pass their real source. Mini Dock untouched. Smoke suite and diff whitespace passed. Release compilation pending; all device appearance/gesture checks UNTESTED.

Validation checkpoint: first Release/R8 build passed (9m21s). New lifecycle/geometry test reproduced anchor drift, then passed with seed-centered uniform transform; production fix applied. Test checks both styles, opening/closing, finish/cancel, restored transforms/clip/provider and disabled-system/preference/fallback gates. Mock rendering only. Final rebuilt APK pending; no publication yet.
