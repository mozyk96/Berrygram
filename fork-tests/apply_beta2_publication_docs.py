import io

progress = 'PROGRESS.md'
continue_file = 'CONTINUE.md'

block_progress = """- Published as the GitHub pre-release `v1.4.0-beta.2`: https://github.com/mozyk96/Berrygram/releases/tag/v1.4.0-beta.2
  (prerelease=true, draft=false). Assets: `BerryGram-1.4.0-beta.2-arm64-v8a.apk` (41910875 bytes,
  GitHub digest `sha256:d8e1787a3ab2ead530fcff1453b1b22db8d595fa51fca2260c67fc9933369322`, identical to the local
  build) and `SHA256SUMS`. Direct download:
  https://github.com/mozyk96/Berrygram/releases/download/v1.4.0-beta.2/BerryGram-1.4.0-beta.2-arm64-v8a.apk
- Branches and tag: `feature/berrygram-1.4.0-beta2` and `feature/berrygram-1.4.0` at `c35dfd8`, tag `v1.4.0-beta.2`
  at the same commit. The previous reconstruction stays local in `feature/berrygram-1.4.0` (checkpoint `be8a751`).
- Updater acceptance verified against the live release with the real `UpdatePolicy` rules
  (`fork-tests/UpdaterAcceptanceCheck.java`): ARM64 asset name accepted, GitHub digest accepted, version comparison
  against `1.4.0-beta.1` accepted. The APK download was verified with HTTP HEAD (200, 41910875 bytes) and a ranged
  GET (206, `application/vnd.android.package-archive`).
"""

with io.open(progress, 'r', encoding='utf-8') as handle:
    text = handle.read()
anchor = "- Published as the GitHub pre-release `v1.4.0-beta.2` (branch `feature/berrygram-1.4.0-beta2`,\n  tag `v1.4.0-beta.2`); asset `BerryGram-1.4.0-beta.2-arm64-v8a.apk` plus `SHA256SUMS`.\n"
if anchor in text and "Direct download" not in text:
    text = text.replace(anchor, anchor + block_progress, 1)
    with io.open(progress, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)

with io.open(continue_file, 'r', encoding='utf-8') as handle:
    text = handle.read()
old = """- Release ARM64 APK built, signed with the existing release key (certificate SHA-256 `b3ea9ca2…c722a5`,
  identical to the published Stable) and published as the GitHub pre-release `v1.4.0-beta.2`:
  `https://github.com/mozyk96/Berrygram/releases/tag/v1.4.0-beta.2`, APK `BerryGram-1.4.0-beta.2-arm64-v8a.apk`,
  SHA-256 `d8e1787a3ab2ead530fcff1453b1b22db8d595fa51fca2260c67fc9933369322`, versionCode `72739`."""
new = """- Release ARM64 APK built, signed with the existing release key (certificate SHA-256 `b3ea9ca2…c722a5`,
  identical to the published Stable) and published as the GitHub pre-release `v1.4.0-beta.2`:
  `https://github.com/mozyk96/Berrygram/releases/tag/v1.4.0-beta.2`, APK `BerryGram-1.4.0-beta.2-arm64-v8a.apk`,
  SHA-256 `d8e1787a3ab2ead530fcff1453b1b22db8d595fa51fca2260c67fc9933369322`, versionCode `72739`.
  Branches `feature/berrygram-1.4.0-beta2` and `feature/berrygram-1.4.0` plus tag `v1.4.0-beta.2` point at `c35dfd8`.
  The in-app updater acceptance rules were verified against the live release."""
if old in text:
    text = text.replace(old, new, 1)
    with io.open(continue_file, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)

print('publication docs updated')
