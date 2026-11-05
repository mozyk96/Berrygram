# Telegram Internals

Scope: the upstream Telegram Android structures a BerryGram change most often touches.
Read this before editing `DialogsActivity`, `ChatActivity`, `ActionBar`, stories, or
theme-aware code.

## Screen anatomy

- `org.telegram.ui.DialogsActivity` — chat list, folders/archive, search, stories row,
  action mode. Public base uses upstream `org.telegram.ui.Stories.DialogStoriesCell`
  (`dialogStoriesCell`, `progressToDialogStoriesCell`,
  `DialogStoriesCell.HEIGHT_IN_DP`, `TYPE_ARCHIVE`/`TYPE_DIALOGS`).
- `org.telegram.ui.ChatActivity` — conversation, message list, composer, chat action bar.
  Fork hook: `ForkChatHeaderSettings.enabled()`.
- `org.telegram.ui.ActionBar.*` — `ActionBar`, `ActionBarMenuItem`, `ActionBarPopupWindow`,
  `BaseFragment`, `Theme`, `ActionBarLayout`.
- `org.telegram.ui.MainTabsActivity` — tab shell; fork hook: `ForkDockSettings`.
- `org.telegram.ui.Components.ItemOptions` — long-press/popup menus; fork hook: glass/blur
  decisions.

## ActionBar rules

1. Hiding the upstream ActionBar is not the same as removing its space. If you hide it,
   verify the layout height, padding, and inset consumption at the same time.
2. Do not draw new controls on top of the old panel. Either replace the panel or keep it.
3. Do not delete upstream actions (search, overflow, back, lock). Fork surfaces must
   preserve them.
4. Fork settings screens use `ForkFloatingHeader.wrap(context, actionBar, scroll, title, onBack)`.
   It returns the fragment root; call `ForkFloatingHeader.refresh(root)` after theme or
   preference changes (as `ForkSettingsActivity.onResume` does).
5. Height math must use the same source of truth as the list padding. For stories,
   `DialogStoriesCell.HEIGHT_IN_DP` is the unit used across `DialogsActivity`.

## Search and action-mode transitions

`DialogsActivity` animates between normal, search, and action modes using
`progressToActionMode`, `progressToDialogStoriesCell`, `scrollYOffset`, and
`getReservedSearchHeight()`. Fork surfaces that are positioned by these values must be
recomputed in the same pass; otherwise they disappear or lag after search.

Checklist for any surface crossing these modes:

- Alpha and translation are updated in the same layout pass as the upstream list.
- The surface is re-attached or re-measured after leaving search.
- Clipping matches the list clip; do not leave a stale clip rect.

## Stories

- Stories are an upstream feature. BerryGram should not reimplement the story viewer.
- The chat-list stories row is `DialogStoriesCell`; visibility flows from `hasStories`.
- Fork geometry helper: `HeaderGeometry.storiesWidth(int windowWidth, int controlsWidth, int insetsAndGap)`.
- Invisible stories usually mean: zero width/height, alpha left at 0, wrong z-order, or a
  parent clip. Confirm which before changing anything.

## Archive

- Archive is a folder in `DialogsActivity`; the archive holder participates in scroll and
  clipping.
- Common regressions: header height added twice, negative padding, stale
  `getMaxScrollYOffsetWithoutSearch()`, or a clip applied to the archive holder.
- Verify with both empty and non-empty archive, and with stories present and absent.

## Themes and resources

- Use existing theme keys and `Theme.ResourcesProvider`; do not hardcode colors.
- `ForkGlassSurface.provider(Theme.ResourcesProvider)` exists specifically to obtain a
  blur provider that follows the active theme.
- Fork strings live in fork resources and are resolved with `ForkLocale.get(...)`.
  Add a string for every locale you ship, or fall back deliberately.
- Night theme, day theme, and custom accent must all be checked for text contrast.
  `GraphicsColorPolicy.readableAccent(accent, background)` is the existing helper.

## Multi-account

- Account-specific data must use the current account, not a global singleton.
- Fork preferences that are device-local must say so in code comments and docs.
- When adding per-account behavior, confirm the switch-account path
  (`AccountInstance`/`UserConfig` recreation) does not keep stale UI state.

## Message and media pipeline

- Do not modify the media pipeline for a UI-only feature.
- Round video messages use the camera path; fork policies are
  `RoundCameraPolicy`, `RoundCameraSettings`, `RoundVideoQualityPolicy`.
- Voice quality policies live in `org.telegram.messenger.fork.voice`.
- Any change to capture or encoding needs a real device/emulator test; mark it
  `MANUAL TEST REQUIRED` otherwise.

## Anti-patterns seen in forks

- Duplicating an upstream mechanism "just for the fork" and letting both run.
- Adding a second settings entry that writes a key nobody reads.
- Replacing an upstream adapter instead of adding a decoration.
- Copying an upstream method into a fork class and letting the original drift.
