# Security

Scope: protecting credentials, users, and the update channel. Applies to code changes,
documentation, commits, and releases.

## Secrets

Never commit, print, log, or paste:

- Telegram API ID/hash values (`FORK_TELEGRAM_API_ID`, `FORK_TELEGRAM_API_HASH`). Supplying
  them as local/CI environment variables for an authorized build is required and allowed;
  what is forbidden is committing, logging, echoing, or publishing the values.
- Release keystore or its passwords/alias (`BERRYGRAM_RELEASE_*`).
- `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `.env*` files.
- Firebase/Google services JSON.
- `agconnect-services.json` (the Huawei module is included only when it exists locally).
- Session files, auth tokens, phone numbers, or user content.
- Absolute local filesystem paths in published documentation.

If a secret is exposed: stop, do not push, rotate the credential, and report the exposure.
Do not "fix" it by amending public history without explicit approval.

## Authentication and accounts

- Do not change `BackendPolicy`, `SessionCredentials`, or `AuthDiagnostics` behavior
  without understanding the login/2FA flow.
- Do not add logging of codes, passwords, tokens, or phone numbers.
  `fork-tests/test_auth_privacy.py` guards specific log and Firebase assertions; keep them
  passing.
- Do not automate a real Telegram account, and do not handle a user's codes or passwords.
- Multi-account isolation matters: account-scoped data must not leak across accounts.

## Updater trust

The updater downloads and installs APKs, so it is a code-execution path.

Required properties (implemented through `UpdatePolicy` and `GithubUpdateClient`):

- HTTPS only; reject redirects outside the trusted host.
- Repository allowlist via `UpdatePolicy.validRepository(...)`.
- Asset name must match the tag: `UpdatePolicy.validAsset(tag, name)`.
- Digest must be `sha256:<64 hex>` and match the downloaded bytes.
- `validIdentity(installedPackage, installedVersion, installedSigners, candidatePackage,
  candidateVersion, candidateSigners)` must reject package/signer mismatches.
- Reject downgrades unless explicitly intended.
- Check free space with `enoughSpace(...)` and fail cleanly.
- Reject drafts/unpublished assets in metadata parsing.
- Never install from a saved URL that fails `validSavedRelease(...)` revalidation.

Do not weaken any of these to make a test pass.

## Input handling

- Custom text packs: validate through `CustomTextValidator` and
  `CustomTextPackValidator`; do not bypass size/format limits.
- Treat imported JSON/HTML/text as untrusted. No file path traversal, no unbounded memory.
- Do not render user-provided links as clickable without the upstream URL policy.
- Do not add `WebView` JavaScript bridges for a fork feature.

## Data protection

- `berrygram_backup_rules.xml` and `berrygram_data_extraction_rules.xml` exclude
  `fork_custom_text.xml` from backup and device transfer. Keep it excluded; custom phrases
  can be personal.
- Do not move user text into logs, analytics, or crash reports.
- Do not add telemetry.

## Attack surface discipline

- Prefer pure Java policy classes; avoid native parsing for new features.
- No new exported components without a permission guard and a concrete need.
- No dynamic code loading, no reflection-based plugin systems.
- Validate intents and extras received by fork activities.

## Documentation security

Published skill or README files must not contain:

- tokens, passwords, API credentials, private keys, keystores,
- personal data or account identifiers,
- local Windows/macOS user paths,
- private configuration or service endpoints,
- copied third-party text without a compatible license.

Use repository-relative paths. Cite third-party inspiration in `SOURCES.md` with license.

## Pre-publication scan

Before committing docs or code:

1. Search added files for `token`, `password`, `secret`, `api_hash`, `keystore`, `BEGIN`,
   and long hex/base64 strings.
2. Search for drive-letter paths and user home directories.
3. `git diff --stat` and `git status` to confirm only intended files.
4. Confirm no generated APK, `.gradle`, `build/`, or IDE files are staged.

## Incident response

If a commit contains a secret: do not push. If already pushed, notify the owner, rotate
the credential, and follow the owner's instruction for history handling. Never rewrite
public history unilaterally.
