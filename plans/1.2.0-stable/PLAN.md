# Stable1.2.0 promotion

- Freeze runtime at owner-tested a41d059 (Beta9). Only gradle version/name, docs and test expectations change. Base7257 -> APK72579.
- Run smoke and updater package/download/channel fixtures: Stable/Beta selection and Beta9->Stable version/code/signer compatibility. No physical installation claim or phone access.
- Commit, Release/R8 build, verify embedded source, manifest/resources/ABI/API23/signature/alignment/ZIP/SHA, package/signer continuity, changed-source/history secrets.
- Push sanitized source/exact immutable v1.2.0 tag. Upload APK/checksum to draft normal release, verify digest, publish Stable, verify public download/actual updater channels. Save final progress and stop. No1.3.0.

Owner explicitly tested Beta9 on a physical phone and approved Stable. Stable upgrade installation remains distinct from static package compatibility evidence.
