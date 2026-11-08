import io
import os

progress = 'PROGRESS.md'
continue_file = 'CONTINUE.md'

with io.open('release-notes-progress-beta2.md', 'r', encoding='utf-8') as handle:
    block = handle.read()

with io.open(progress, 'r', encoding='utf-8') as handle:
    existing = handle.read()
if 'Android 1.4.0 Beta 2' not in existing:
    with io.open(progress, 'w', encoding='utf-8', newline='') as handle:
        handle.write(block + '\n' + existing)

with io.open(continue_file, 'r', encoding='utf-8') as handle:
    existing = handle.read()
marker = '# BerryGram continuation — Android 1.4.0 Beta 1'
if marker in existing and 'Android 1.4.0 Beta 2' not in existing.split(marker)[0]:
    header = """# BerryGram continuation — Android 1.4.0 Beta 2

## State
- Branch: feature/berrygram-1.4.0-beta2, base `7fd3022` (tag `v1.4.0-beta.1`).
- Recovered from the wrong base: the authoritative 1.4.0 history was restored from the upstream
  repository and the unreleased local 1.4.0 work (notification diagnostics, menu surfaces, spring
  tab indicator) was re-applied. The previous reconstruction stays in `feature/berrygram-1.4.0`
  (checkpoint `be8a751`).
- Version: 1.4.0-beta.2, versionCode 72739 (arm64). Published Beta 1 is 72719, the earlier
  unpublished Beta 2 debug build was 72729.

## Beta 2 scope completed
- Ghost reading no longer suppresses explicit "Mark as read" (force path + regression tests).
- Per-account privacy settings with legacy migration.
- Keystore AES-GCM encryption for archive, deleted markers and edit history, with plaintext migration.
- Archive retention 7/30/90 days or forever, cleanup on app start and on demand.
- Notification diagnostics, three appearance modes, message menu surfaces and the spring tab
  indicator recovered from the 1.4.0 development history.

## Verification
- Debug and release Kotlin/Java compilation: success.
- Full `fork-tests/smoke.ps1`: success.
- Release ARM64 APK built, signed with the existing release key (certificate SHA-256 `b3ea9ca2…c722a5`,
  identical to the published Stable) and published as the GitHub pre-release `v1.4.0-beta.2`.

## Open items
- Owner device test: login, incoming messages, notifications, Ghost Mode vs explicit Mark as read,
  deleted-message bubbles, edit history, archive search/retention, navigation indicator, the three
  appearance modes and the in-app update.
- Remote push still needs a BerryGram-owned Firebase project; steps are in the release notes.
- Do not publish further releases without device evidence or explicit owner direction.

"""
    existing = existing.replace(marker, header + marker, 1)
    with io.open(continue_file, 'w', encoding='utf-8', newline='') as handle:
        handle.write(existing)

print('docs updated')
