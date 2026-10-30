# BerryGram

![BerryGram icon](TMessagesProj/src/main/res/mipmap-xxhdpi/berrygram_launcher.png)

BerryGram is an unofficial Android client based on the official Telegram Android source. It keeps Telegram's messaging interface and adds focused settings for video messages, voice recording, text overrides and optional experimental panels.

**Development status:** 1.0.0 device acceptance is in progress. Version 1.0.1 is under development; the updater and redesigned menus are not yet validated and no stable releases are available from this repository. Background FCM notifications require a fork-owned Firebase configuration and have not been verified.

Releases will be available from [GitHub Releases](https://github.com/mozyk96/Berrygram/releases) after publication. See [CHANGELOG.md](CHANGELOG.md) for the current development status.

## Requirements and installation

Android 6.0 (API 23) or newer. Current development APKs target `arm64-v8a`. Install only a verified APK from the project's Releases page when a stable release is published; Android's installer must confirm updates. Debug and Release currently use distinct application IDs and are separate installations.

## Build from source

Use JDK 17, Android SDK 36, NDK 27.2.12479018 and the checked-in Gradle 8.13 wrapper. Initialize recursive submodules. Set `FORK_TELEGRAM_API_ID` and `FORK_TELEGRAM_API_HASH` in the local build environment using your own Telegram API credentials; never commit them. A locally configured signing key is required for Release. Firebase requires your own application-specific configuration if push notifications are needed.

```sh
git submodule update --init --recursive
./gradlew :TMessagesProj_App:assembleAfatDebug -PforkAbi=arm64-v8a
```

See [build and Actions configuration](docs/BUILDING.md) for signing, optional service keys and known limitations. Build requirements must be independently rechecked before publication. See [roadmap](ROADMAP.md) for current evidence and limitations.

## License and credits

The upstream [GPL version 2 license](LICENSE), copyright notices and third-party component notices are retained. See the [original upstream README](docs/UPSTREAM-README.md) and each dependency's license. Telegram and the Telegram logo are trademarks of their respective owners; BerryGram is not affiliated with Telegram.

The public source history is prepared separately from the original local development history, which contains signing material. Release builds require private credentials supplied locally; never commit them.
