# Release gates

Beta 3 published from `ec5517afa8147fd9688be2904b0bc19112ec8e0c`, code `72609`, same permanent certificate, Android 6+/ARM64. SHA-256: `5700cbd0c209645097089129af4bb7db516f7eccf40788a397b051b8b7d69323`. Full smoke, Release/R8, APK identity/signature/ABI/minSdk/flags/source/native-lib/alignment checks passed. Final credential-bearing APK was not installed before publication at the owner's request; prior same-code build was device-tested for lock screen, Performance Center, Experimental Glass selection and menu/Edit paths. Owner acceptance remains pending.

Beta 2 (immutable): source `b1ce28e55e9755b95631cfe009e21f01156aea3b`, code `72599`, SHA-256 `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`. Its APK was built with empty fork API credentials; do not reuse that build.

Beta 1 (immutable): `0ce1f16`, code `72589`, SHA-256 `0a31242c7df6466cfe1408a0f3a3106a26721b174eba9849b2c3077090146c07`.

Build caution: AGP may fail to read packed branch refs. Re-extract version-control metadata from the exact detached source commit using `extractAfatReleaseVersionControlInfo --rerun`, then assemble/re-sign and verify the embedded revision. Restore the development branch afterward; do not change published tags/assets.

Builds must inject the approved local `FORK_TELEGRAM_API_ID`/`FORK_TELEGRAM_API_HASH` (or CI secrets) without printing or committing them. The Beta 3 build reused the approved values embedded in the published Stable 1.2.0 APK; the app hash is extractable from any APK, but must not be committed to Git or logged.

Android development stays on `dev/1.3.0`; preserve v1.2.0, v1.3.0-beta.1, Beta 2 and Beta 3 tags/artifacts. Publish signed ARM64 Beta prereleases after relevant tests/R8/source/secret/signature/updater checks. Stable v1.3.0 requires explicit owner acceptance. Desktop starts only after that actual Stable publication.
