# Fork architecture

The upstream Telegram Android project remains the source of MTProto, authentication, messaging and standard UI. Fork-specific code lives under `org.telegram.messenger.fork` with small integration points in upstream classes; fork screens live under `org.telegram.ui`.

Current components: ForkSettingsActivity links existing ThemeActivity appearance and ForkCustomTextActivity. AppearanceSettings adapts Telegram bubble-radius settings. CustomTextRegistry allowlists UI resource keys; CustomTextManager stores local values in `fork_custom_text`; CustomTextValidator and CustomTextPackValidator constrain templates and imports. LocalAliasPolicy isolates aliases by account and dialog. These components are partial and must not be mistaken for completed product requirements.

Text lookup currently passes through LocaleController.getStringV2, while local names are integrated narrowly in chat/list rendering. Selected plural forms, per-chat phrase precedence and locally named packs are implemented; broad resource coverage and Android behavior remain incomplete. Preserve original messages, server data and security prompts. Review accurate status accessibility against Telegram API Terms before changing status presentation.

Future modules should use a typed, versioned settings layer with account scope where appropriate, migrations and reset semantics. No translator, archive, plugin or alternate UI implementation is implied by this document.

CustomTextResolution defines personal → global → original fallback and stable account/dialog/resource keys. The personal editor and scoped rendering are implemented for activity and status keys in chat headers, profiles, and typing cache. The implementation is not yet validated on Android.
CustomTextContext provides a thread-local, nestable rendering scope and carries the factual status for accessibility and an explicit status detail view. Security-sensitive unscoped status labels are left unchanged.

Scoped Custom Text work adds per-account/dialog values and a thread-local rendering context in chat, profile and activity-cache paths. The factual-status detail and accessible original are being integrated; neither device behavior nor API Terms compliance has been established. Status overrides are restricted to explicit contexts to protect security-sensitive screens. See https://core.telegram.org/api/terms §1.4.

Privacy shortcuts expose existing Telegram settings and only show loaded rule counts; they do not modify network privacy. QuickReactionSettings stores per-account/dialog selection and routes double-tap through Telegram availability checks, retaining the standard reaction as fallback. Other gesture and privacy requirements remain open.

BerryGram launcher resources are derived from the owner-supplied image; the package ID and signing identity are unchanged. Phrase and alias preferences share fork_custom_text.xml and are excluded from Android full backup and device transfer via version-specific rules; key-value BackupAgent only handles Telegram saved-token preferences. Restore behavior has not yet been tested on devices.

The GitHub updater accepts an explicitly configured repository and stable ARM64 asset with a GitHub SHA-256 digest. It downloads to private cache, verifies size, hash, package name, version, non-debug status, ARM64 libraries, and exact installed signing certificates before offering the system installer. No repository is configured by default; Android integration and signed-release tests remain pending. Exact signer matching intentionally does not support signing-key rotation yet.
