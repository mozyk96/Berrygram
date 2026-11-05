# Release gates

Beta 2 published from `b1ce28e55e9755b95631cfe009e21f01156aea3b`, code `72599`, same permanent certificate, Android 6+/ARM64. SHA-256: `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`. Full smoke, Debug Java compile and Release/R8 passed; APK v1/v2 signature, 16 KB ZIP alignment, non-debug/non-testOnly, source revision and public download verified. Owner device acceptance remains pending.

Beta 1 (immutable): published from `0ce1f16`, code `72589`, SHA-256 `0a31242c7df6466cfe1408a0f3a3106a26721b174eba9849b2c3077090146c07`; do not modify its tag/assets.

Build caution: AGP may fail to read packed branch refs. Re-extract version-control metadata from the exact detached source commit using `extractAfatReleaseVersionControlInfo --rerun`, then assemble/re-sign and verify the embedded revision. Restore the development branch afterward; do not change published tags/assets.

Android development stays on `dev/1.3.0`; preserve v1.2.0 and v1.3.0-beta.1 tags/artifacts. Set beta version/code only when preparing an integrated Beta; do not create a version per commit. Publish signed ARM64 Beta prerelease after relevant tests/R8/source/secret/signature/updater checks; give verified direct URL. Owner then tests. Stable v1.3.0 requires explicit owner acceptance. Do not use the 1.3.0 Stable tag early. Desktop starts only after that actual Stable publication, with separate alpha/beta release names/assets and independent Windows signing.
