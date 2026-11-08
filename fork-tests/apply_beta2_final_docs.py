import io

progress = 'PROGRESS.md'
continue_file = 'CONTINUE.md'
digest = 'd8e1787a3ab2ead530fcff1453b1b22db8d595fa51fca2260c67fc9933369322'
size = '41910875'

with io.open(progress, 'r', encoding='utf-8') as handle:
    text = handle.read()
marker = '- Release ARM64 build with R8 and locally injected credentials — see below.'
addition = (
    '- Release ARM64 build with R8 and locally injected credentials: SUCCESS.\n'
    '  APK `C:\\Users\\Keoo2\\Documents\\BerryGram-1.4.0-beta.2-arm64-v8a.apk`, ' + size + ' bytes,\n'
    '  SHA-256 `' + digest + '`; package `org.example.forkclient`, versionName `1.4.0-beta.2`,\n'
    '  versionCode `72739`, minSdk 23, targetSdk 36, native-code arm64-v8a only,\n'
    '  `CN=BerryGram Release` certificate SHA-256 `b3ea9ca2…c722a5` (identical to the published Stable),\n'
    '  v1+v2 signatures verified, `zipalign -c -P 16` passed, POST_NOTIFICATIONS declared,\n'
    '  credential present in the release dex, no keystore packaged.\n'
    '- Published as the GitHub pre-release `v1.4.0-beta.2` (branch `feature/berrygram-1.4.0-beta2`,\n'
    '  tag `v1.4.0-beta.2`); asset `BerryGram-1.4.0-beta.2-arm64-v8a.apk` plus `SHA256SUMS`.\n'
)
if marker in text and 'versionCode `72739`' not in text:
    text = text.replace(marker, addition, 1)
    with io.open(progress, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)

with io.open(continue_file, 'r', encoding='utf-8') as handle:
    text = handle.read()
old = '- Release ARM64 APK built, signed with the existing release key (certificate SHA-256 `b3ea9ca2…c722a5`,\n  identical to the published Stable) and published as the GitHub pre-release `v1.4.0-beta.2`.'
new = ('- Release ARM64 APK built, signed with the existing release key (certificate SHA-256 `b3ea9ca2…c722a5`,\n'
       '  identical to the published Stable) and published as the GitHub pre-release `v1.4.0-beta.2`:\n'
       '  `https://github.com/mozyk96/Berrygram/releases/tag/v1.4.0-beta.2`, APK `BerryGram-1.4.0-beta.2-arm64-v8a.apk`,\n'
       '  SHA-256 `' + digest + '`, versionCode `72739`.')
if old in text:
    text = text.replace(old, new, 1)
    with io.open(continue_file, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)

print('final docs updated')
