# Architecture

Scope: how the BerryGram repository is laid out, where fork code lives, and how the
app boots. All paths are repository-relative and describe commit `68081a6`, the last
published commit on branch `public-1.2.0` (version `1.2.0-beta.7`). The branch tip may be
ahead with unpublished work: check `git log --oneline 68081a6..<branch>` and never describe
unpublished commits as public behavior.

## Modules

| Module | Purpose |
| --- | --- |
| `TMessagesProj` | Core Android app: Activities, Fragments, UI components, Telegram logic, JNI, resources. |
| `TMessagesProj_App` | Main installable variant (`afat`, `bundleAfat`) with debug/release build types. |
| `TMessagesProj_AppStandalone` | Standalone variant module. |
| `TMessagesProj_AppHockeyApp` | Legacy distribution variant. |
| `TMessagesProj_AppTests` | Android test module. |
| `TMessagesProj_Modules/media` | Git submodule for AndroidX Media3 sources. |
| `TMessagesProj/lib/jlatexmath` | Math rendering dependency included from `settings.gradle`. |
| `fork-tests` | Plain JVM tests, PowerShell fixtures, Python checks, Node checks. |
| `plans/` | Per-release plans with checkpoints. |
| `docs/` | Build, upstream, publication, and modernization notes. |

`settings.gradle` conditionally includes `TMessagesProj_AppHuawei` only when
`TMessagesProj_AppHuawei/agconnect-services.json` exists. Never add that file to Git.

## Where fork code lives

Initialization and settings:

- `TMessagesProj/src/main/java/org/telegram/messenger/fork/auth/AuthDiagnostics.java`
- `.../fork/auth/BackendPolicy.java`
- `.../fork/auth/SessionCredentials.java`

Panel and graphics policy:

- `.../fork/panels/ForkPanelSettings.java` — global panel enable/style/radius/strength/animation/adaptive flags.
- `.../fork/panels/PanelPolicy.java` — pure policy: `useGlass`, `effectiveStyle`, `popupRadiusDp`, `surfaceRadiusDp`, `popupAnimationDuration`, `clampPopupOffset`, `captureMenuGlass`.
- `.../fork/panels/GraphicsColorPolicy.java` — accent readability against a background.
- `.../fork/panels/HeaderGeometry.java` — `storiesWidth(windowWidth, controlsWidth, insetsAndGap)`.
- `.../fork/panels/ForkChatHeaderSettings.java` — `floating_chat_header` opt-in (`fork_panels` preferences).
- `.../fork/panels/ForkDockSettings.java` — `mini_dock` preference and `miniDockEnabled()`.
- `.../fork/panels/ForkAnimationSettings.java` — animation preferences.

Text overrides:

- `.../fork/text/CustomTextManager.java`, `CustomTextRegistry.java`, `CustomTextResolution.java`
- `.../fork/text/CustomTextValidator.java`, `CustomTextPackValidator.java`, `CustomTextPackKeys.java`
- `.../fork/text/CustomTextContext.java`, `CustomTextScopePolicy.java`
- `.../fork/text/LocalAliasPolicy.java`, `ForkLocalePolicy.java`

Camera and voice:

- `.../fork/camera/RoundCameraSettings.java`, `RoundCameraPolicy.java`, `RoundVideoQualityPolicy.java`
- `.../fork/voice/VoiceQualitySettings.java`, `VoiceQualityPolicy.java`

Updater:

- `.../fork/updater/GithubUpdateClient.java`, `UpdatePolicy.java`

UI shell and screens:

- `TMessagesProj/src/main/java/org/telegram/ui/ForkFloatingHeader.java` — header wrapper for
  fork settings screens. `wrap(Context, ActionBar, View content, CharSequence, Runnable)`
  and the overload with a trailing action `View` return a vertical `LinearLayout` (header
  row + weight-1 content); `refresh(View)` re-applies styling. It is a normal in-flow
  header, not an overlay: content scrolls below it, and the class is package-private to
  `org.telegram.ui`.
- `ForkSettingsLayout.java` — shared card/gap layout for fork settings.
- `ForkSettingsActivity.java` — entry screen; hosts `ForkUpdaterPanel`.
- `ForkAppearanceSettingsActivity.java`, `ForkVideoSettingsActivity.java`,
  `ForkVoiceSettingsActivity.java`, `ForkCustomTextActivity.java`,
  `ForkCameraTestActivity.java`, `ForkAboutActivity.java`.
- `ForkChoicePopup.java`, `ForkHsvPicker.java`, `ForkLocale.java`,
  `ForkRoundCameraShortcuts.java`.

Graphics engine:

- `TMessagesProj/src/main/java/org/telegram/ui/ForkGraphicsSurface.java` — style resolution.
- `.../ui/ForkGlassSurface.java` — `provider(Theme.ResourcesProvider)` returning a `BlurredBackgroundProvider`.
- `.../ui/ForkSurfaceAnimation.java` — `enabled()` and `create(View, Rect, boolean opening, boolean fromBottom)`.
- `.../ui/Components/blur3/**` — blur/Liquid Glass runtime:
  `LiquidGlassEffect`, `LiquidGlassBitmapSource`, `BlurredBackgroundProvider`,
  `BlurredBackgroundDrawableSource`, `BlurredBackgroundSourceRenderNode`,
  `ViewGroupPartRenderer`, `DownscaleScrollableNoiseSuppressor`.

## Boot and initialization

- `ApplicationLoader` creates the application context used by fork preference helpers
  (for example `ForkChatHeaderSettings` reads `ApplicationLoader.applicationContext`).
- Account-scoped state is reached through `UserConfig`/`AccountInstance` patterns in
  upstream code. Fork preferences are device-local unless a feature explicitly stores
  per-account data.
- Fork preferences are stored in the `fork_panels` `SharedPreferences` file and in
  per-screen preference files. Read the owning class before writing a new key.

## Navigation

- `MainTabsActivity` owns the tabbed shell and references `ForkDockSettings`.
- `DialogsActivity` is the chat-list screen; `ChatActivity` is the conversation screen.
- Fork settings screens are `BaseFragment` subclasses presented with `presentFragment(...)`.
- `ForkUpdaterPanel` is a `View` hosted inside `ForkSettingsActivity`, constructed as
  `new ForkUpdaterPanel(context, this)` where `this` is the parent `BaseFragment`.

## Integration points in upstream files

Keep this list short when adding a feature — one hook is better than a refactor.

- `DialogsActivity` — `ForkGraphicsSurface.effectiveStyle()` and `materialColor(...)` around
  the main-header background (search for `isForkMainHeader` and `effectiveStyle`).
- `ChatActivity` — `ForkChatHeaderSettings.enabled()` gates the floating chat header path.
- `ActionBarMenuItem`, `ActionBarPopupWindow`, `ItemOptions` — glass/blur decisions via
  `PanelPolicy.useGlass(...)`, `PanelPolicy.effectiveStyle(...)`, and
  `ForkSurfaceAnimation`.
- `MainTabsActivity` — mini dock preference.
- `TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java` — upstream
  stories cell used by `DialogsActivity`; fork geometry is `HeaderGeometry.storiesWidth`.

## Unconfirmed areas

Do not document or "fix" these without reading the code first:

- Exact per-account persistence rules for every fork setting.
- Liquid Glass shader math and per-frame cost on low-end GPUs.
- Updater behavior when the GitHub API is rate-limited or returns an unexpected schema.
- Any behavior only present in unpublished local commits. Public docs must describe the
  public branch, not a private Beta branch.
