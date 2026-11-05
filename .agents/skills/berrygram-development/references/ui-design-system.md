# UI Design System

Scope: the fork's settings and surface visual language. Follow it instead of inventing a
new style per screen.

## Components

- `ForkSettingsLayout` — shared settings page scaffolding: cards, gaps, dividers, and the
  package-private `refreshPage(LinearLayout)` to re-apply colors after a theme or accent
  change.
- `ForkFloatingHeader` — in-flow header wrapper for fork settings screens (not an overlay).
- `ForkChoicePopup` — small selection popup.
- `ForkHsvPicker` — color picker for accent.
- `ForkLocale` — fork string resolution.
- `GraphicsColorPolicy.readableAccent(accent, background)` — ensures text contrast.

## Tokens

Use `PanelPolicy` for dimensions, not literals:

| Token | Source |
| --- | --- |
| Settings card radius | `PanelPolicy.settingsRadiusDp()` = 18dp |
| Settings card gap | `PanelPolicy.settingsGapDp()` = 6dp |
| Popup radius (experimental) | `PanelPolicy.popupRadiusDp(true)` = 26dp |
| Popup radius (standard) | `PanelPolicy.popupRadiusDp(false)` = 12dp |
| Surface radius | `PanelPolicy.surfaceRadiusDp(style, requested)` |
| Effect strength | `PanelPolicy.effectStrength(value)` clamped 0..100 |

Colors:

- Backgrounds, text, and dividers come from `Theme` keys and
  `Theme.ResourcesProvider`, never from hardcoded hex.
- Accent is user-selectable; run it through `GraphicsColorPolicy` before using it for text.
- Material fallback is `ForkGraphicsSurface.materialColor(context, themeFallback)`.

## Screen composition rules

- One fork header per screen, produced by `ForkFloatingHeader.wrap(...)`. It is a vertical
  in-flow layout: header row, then content. Content scrolls below it, not under it, and the
  class is package-private to `org.telegram.ui`.
- Cards are full-width with the standard gap; do not mix card and non-card rows in one
  group without a divider.
- Section titles use the upstream settings typography; do not invent a new text style.
- Empty states are explicit text, not a blank area.

## Controls

- Toggles must write a preference that a rendering path reads in the same feature.
- Sliders show a live preview; a slider whose value is only read on restart must say so.
- Destructive actions use the upstream confirmation pattern, not an immediate action.
- Long lists get a stable scroll position after a toggle; do not jump to top.
- Every control has a content description and a visible label.

## Theming

- Check day, night, and custom accent.
- Check that the floating header's text stays readable over the blurred/material surface.
- Check that disabled controls are visibly disabled in both themes.
- Do not assume the wallpaper is dark; blur output depends on content behind the surface.

## Accessibility

- 48dp minimum touch targets.
- Contrast: use `GraphicsColorPolicy` rather than eyeballing.
- Do not encode state by color alone; pair with text or an icon.
- Support font scaling at 1.3x without clipping labels.
- TalkBack: each control announces its label, state, and role.

## Density and insets

- Use `dp(...)` everywhere.
- Respect display cutouts; a floating header must not sit under the cutout.
- Landscape: the header must not cover the list's first row.
- Do not assume a fixed status-bar height.

## Anti-patterns

- A second visual language for one screen.
- A fork card style pasted as a copy of `ForkSettingsLayout`.
- A settings entry with no observable effect.
- A control that only changes a preference read by a debug screen.
- Hardcoded English strings when `ForkLocale` is available.
