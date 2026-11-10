---
name: berrygram-development
description: Use when developing, testing, updating upstream Telegram or publishing the Berrygram Android fork.
---

Read AGENTS.md and the current section of CONTINUE.md. Inspect Git status including submodules before changing branches. Preserve local native sources and existing signing identity. Never push private signing history, force-push or replace a published tag.

TMessagesProj contains the Java/Kotlin Telegram UI, messenger services and JNI. Follow LaunchActivity and ActionBarLayout for navigation. Prefer ForkSettingsFragment and ForkSettingsSection for fork-owned settings. Place feature rows in their owning Telegram destination. Use theme keys, actual window insets, scalable rows and accessible touch targets. Inspect long labels in Ukrainian and Russian at narrow widths. Check recycled rows after scrolling. Resolve LOC_ERR through packaged resource fallback. Bind tab indicator motion to the pager.

Use the Windows Gradle wrapper from the repository root. Build debug with ./gradlew.bat :TMessagesProj_App:assembleAfatDebug -PforkAbi=arm64-v8a. Run ./fork-tests/smoke.ps1 and targeted updater fixtures. Run connected tests using :TMessagesProj_AppTests:connectedAfatDebugAndroidTest and the ABI supported by the emulator. Exercise normal navigation separately from isolated fragment rendering. For archive failures distinguish empty results from failed database reads. Keep I/O off the UI thread. Missing Firebase configuration affects push delivery.

Use ./fork-tests/build-release.ps1 -EnvFile <private-env-file> -LogFile <build-log> with fork-owned credentials. Keep credentials outside Git and logs. assembleAfatRelease processes resources, native libraries and R8. Wait for completion, inspect the exit status and build log. gradle.properties supplies the base code. The APK code is the base multiplied by ten plus the flavor digit. Compare actual codes using aapt dump badging.

Verify org.example.forkclient, arm64-v8a, version, apksigner verify --verbose --print-certs, signer continuity, zipalign -c -P 16 4 and Get-FileHash -Algorithm SHA256. Exercise install -r over the published predecessor on an isolated emulator. Check updater ABI filtering, downgrade rejection, verification and user-initiated installation. Publish a distinct version tag from verified source, attach the APK and SHA256SUMS, then verify public discovery and download.

Before an upstream update, identify the shared DrKLO/Telegram revision and compare changes including gitlinks. Preserve local native trees and validate fork hooks through their original entry points. Trace camera ownership through CameraController and CameraView, editing and playback through PhotoViewer and VideoPlayer. Verify the active path before changing engines. Check advertised capabilities, permission denial, rotation, cancellation, surface release and lifecycle transitions. Preserve restricted-media handling and original files. Distinguish emulator playback from measured physical-camera quality.
