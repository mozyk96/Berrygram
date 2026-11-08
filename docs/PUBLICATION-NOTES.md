# Publication notes

BerryGram is an unofficial Telegram client. Release signing credentials and service configurations are supplied locally and are not tracked. Optional `FORK_MAPS_API_KEY` and `FORK_SAFETYNET_API_KEY` values must belong to the fork owner. Missing service configuration may affect maps, some login-verification paths and push notifications; these paths require device testing.

The source retains upstream WebRTC test fixtures containing deliberately public, non-production test certificate keys. They are not BerryGram signing credentials. The upstream license, copyright notices and third-party attribution remain in place.

Version 1.4.1 was built from the tagged commit on the maintainer host and verified (signed ARM64 release, signature continuity, resources, unit and updater fixtures, live GitHub API). A full clean-clone Android build and on-device runtime acceptance remain outstanding; the device checklist is tracked separately in this directory.
