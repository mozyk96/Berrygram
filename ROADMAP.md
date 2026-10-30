# BerryGram roadmap

The latest instruction allows independent 1.0.1 implementation while 1.0.0 manual testing continues; it supersedes older versioning and publication plans. BerryGram 1.0.0 includes exactly four custom areas: video-message camera and quality; voice-recording quality; an experimental panel toggle; and custom names, statuses and texts. Stock Telegram functionality remains available.

## 1.0.0

- Finish and verify each of the four areas on Android, including light/dark UI and persistence.
- Verify normal authentication, messages, calls, media and notifications with owner-operated accounts, without automated access to a real account.
- Run local checks, build Debug, exercise the available device, fix defects, and then build a signed Release with the existing identity. Check package/signature compatibility before any install; never uninstall or clear data. Publish only after the release and public source/history pass the requested checks.

Deferred work is preserved on branch `berrygram-pre-scope-20261030`; it is not active in 1.0.0. Gesture/menu editors, reactions, player changes, archive, updater, STT, translation, OCR, plugins, theme engine, extra search and experimental caching are outside this release.

## 1.0.1 — implementation in progress; acceptance pending

- Prepare a public BerryGram repository and stable release only after source, dependencies, licensing, and the entire published history have passed a secrets review. The local history contains a tracked signing keystore; never push it or its credentials. Preserve the current local signing identity and working history.
- Reuse the saved updater foundation from `berrygram-pre-scope-20261030`, validate trusted repository, versionCode, hash, ABI, package and installed signer, and use the Android installer. Add the requested expandable panel to BerryGram Settings.
- Redesign the settings and expand opt-in glass styling across real supported Telegram menus with a low-resource fallback. Verify light and dark UI on a device.
- Increment versionCode; keep applicationId and signing certificate. Test real update compatibility, publish only verified stable releases, and do not restore unrelated deferred features.

## Future versions — planned, not implemented

| Version | Planned focus |
|---|---|
| 1.0.2 and later patches | Bug fixes and small improvements, as needed |
| 1.1.0 | CPU, GPU, RAM, cache, threads, startup and battery optimization |
| 1.2.0 | Camera engine: zoom, lenses, exposure and flashlight |
| 1.3.0 | Voice engine: quality modes, codecs and noise suppression |
| 1.4.0 | Multilingual settings search with tags and synonyms |
| 1.5.0 | Gesture, menu and reaction editors; graphics improvements |
| 1.6.0 | Local STT, translation, model selection and transcript search |
| 1.7.0+ | Additional functions in separate releases |

Backlog for 1.7.0 and later: photo OCR, local archive of ordinary messages, plugins, full theme engine, keyboard layout fix, external SRT/ASS subtitles, video-frame copying and saving, per-chat cache control, gesture-based multi-selection, crash protection, theme and phrase-pack catalogs, further optimizations and a future desktop client. Compatibility, privacy and platform rules require review before implementation.

A large feature receives its own minor version; any number of patch releases can follow. Do not start the next minor version without the owner's explicit decision. The music player, standard photo sending and a separate iOS shell are out of scope.
