# BerryGram

![BerryGram icon](TMessagesProj/src/main/res/mipmap-xxhdpi/berrygram_launcher.png)

BerryGram is an unofficial Android client based on the official Telegram Android source. It keeps Telegram's messaging interface and adds focused settings for video messages, voice recording, text overrides and optional experimental panels.

**Version 2.0.0** starts the new application and signing generation: `org.berrygram.client`. Official APKs and SHA256SUMS are available from [GitHub Releases](https://github.com/mozyk96/Berrygram/releases). See [CHANGELOG.md](CHANGELOG.md).

## Requirements and installation

Android 6.0 (API 23) or newer; ARM64 APK. The new package installs separately from old-generation `org.example.forkclient`: it is not an in-place update of that app. Signing in again may be required. Old local-only data, secret chats and archives do not migrate automatically. New-generation private 1.5.1 installations can be updated in place with the same signing certificate.

Background FCM delivery requires fork-owned Firebase configuration and is not claimed as verified. Use only signed assets from this repository's Releases page and verify SHA256SUMS.

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
