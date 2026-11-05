# 1.3.0 tasks
## Android
[x] Shared native-option Action Registry and account+user-local settings; ID parity/availability/reset/isolation tests.
[~] Gesture editor and real native dispatch integrated; production body/dispatch tests passed. Device timing/touch/selection acceptance pending.
[~] Full enabled standard-reaction catalog,1..4 ordering/filter/doubletap binding/reset integrated; policy/settings tests passed. Actual sending/picker UI pending.
[~] Menu drag/visibility/pin/preview/reset and shared native-cell More integrated. Policy/persistence tests passed; device menu/drag acceptance pending.
[x] en/uk/ru editor resources and settings entrypoints implemented; smoke tests passed.
[x] Initial incremental Android compilation passed after fixing an invalid icon name to real menu_reply. Subsequent More/native Delete/icon polish is included in the upcoming full build.
[x] Release/R8 Beta1, signature/source/security/upgrade metadata and live updater channels verified.
[x] Publish Beta1 and independently download/checksum the public APK.
[~] Owner manual acceptance of gestures, reactions, menus, touch and account/restart behavior.
[x] Beta2: gesture native-path gate/validation/re-entry/feedback and reaction filter/fallback fixed; full smoke, Java compile and Release/R8 passed.
[~] Beta2: Performance Engine profile policy, Performance Center, diagnostics and effect integration implemented; pure tests passed. No FPS benchmark.
[x] Beta2 published: `v1.3.0-beta.2`, source `b1ce28e`, code72599, ARM64, SHA256 `7b25f4ad97592b1e88a998dea09c754cafd02d2c3ced6aec3c9ea8d2762670fd`. Owner device acceptance pending.
[x] Beta3: fixed device-confirmed More/menu `addViewToSwipeBack` crash, edit validation, empty API credentials, Material tint/mojibake, lock-screen black fallback, Performance Center responsiveness/functionality, Experimental Glass, preference/thermal/low-RAM work.
[x] Beta3 published: `v1.3.0-beta.3`, source `ec5517a`, code72609, ARM64, SHA256 `5700cbd0c209645097089129af4bb7db516f7eccf40788a397b051b8b7d69323`. Final credential build not installed before publication; owner acceptance pending.
[x] Beta4 updater/API hotfix published: `v1.3.0-beta.4`, source `7bf93d3`, code72619, ARM64, SHA256 `4e10c9f3a43604fe039f00205c77e817d85218867f50bfcf5e154bfb5659c604`. Credential-bearing build; updater release list selects it. Owner acceptance pending.
[x] Beta5 published: `v1.3.0-beta.5`, source `9379423`, code72629, ARM64, SHA256 `dacc6aac28e214e14c9c0e31eb575f92069703a22c6e4d85eec44f57b0f81d6f`. Glass 2.0, embedded round preview, adaptive camera profiles and voice profiles/effects. Smoke/Debug/Release/R8/APK checks passed; no physical-device test. Post-release `533d604` adds scroll-away preview stop (not in Beta 5 APK). Owner acceptance pending.
[ ] Android Stable1.3.0: requires owner acceptance and final technical checks.
## Desktop — gate closed
[ ] Research official code/build/licenses only after Android Stable1.3.0 publication.
[ ] Independent Windows build, dock/workspace/Settings Studio/menu/profile/chat graphics.
[ ] Functionality port, Alpha, owner testing, Beta/approved Stable.
