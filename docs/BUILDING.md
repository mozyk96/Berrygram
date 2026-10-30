# Building BerryGram

Use JDK 17, Android SDK platform 36, Android build tools 36.0.0, NDK 27.2.12479018 and the Gradle wrapper. Initialize all recursive submodules before building. Configure your own Telegram application ID and hash in the local environment as FORK_TELEGRAM_API_ID and FORK_TELEGRAM_API_HASH. The repository contains no fork-owned values.

For a signed Release build, provide BERRYGRAM_RELEASE_KEYSTORE, BERRYGRAM_RELEASE_KEY_ALIAS, BERRYGRAM_RELEASE_STORE_PASSWORD and BERRYGRAM_RELEASE_KEY_PASSWORD locally. Back up the private key and its credentials securely: changing the signing identity prevents normal Android updates of previously signed installations.

In the sanitized public source, Google Maps and the legacy SafetyNet path use optional FORK_MAPS_API_KEY and FORK_SAFETYNET_API_KEY. Their absence can affect maps or some login-verification paths. Push notifications additionally need fork-owned Firebase configuration; automated Firebase configuration is not currently provided. Never commit local service JSON files or keys.

## GitHub Actions Secrets

In the repository, open Settings, then Secrets and variables, then Actions. The release workflow reads these repository Secrets:

- FORK_TELEGRAM_API_ID and FORK_TELEGRAM_API_HASH.
- BERRYGRAM_KEYSTORE_BASE64, BERRYGRAM_STORE_PASSWORD, BERRYGRAM_KEY_ALIAS and BERRYGRAM_KEY_PASSWORD.
- Optional FORK_MAPS_API_KEY and FORK_SAFETYNET_API_KEY.

The debug CI build uses no Telegram credentials. The optional release job validates the APK locally in the runner and does not upload it while credential publication is unresolved. GitHub does not display stored secret values. A GitHub billing lock currently prevents the workflows from starting, so CI has not been verified remotely.

**APK credential visibility:** the current Telegram client implementation embeds the configured API hash in APK bytecode. Actions Secrets protect the build-time inputs, but do not make an APK containing those inputs secret. Do not publish an APK while the owner prohibits publishing that hash. A shared client credential retrieved from a server can also be extracted by a client.

