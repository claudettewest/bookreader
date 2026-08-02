# Offline Bookshelf

A native, private Android ebook reader and read-aloud library. Books are imported from the device, stored locally, and opened without an account or Internet connection.

## Technology

- Kotlin and Jetpack Compose with Material 3
- Room, coroutines, and StateFlow
- Android Storage Access Framework
- Android native TextToSpeech foreground service
- Minimum Android 8 (API 26), target/compile API 36

The visual system uses Platinum `#E9E4DF`, Silver `#BDA9A4`, Taupe Gray `#958893`, Charcoal `#494C60`, and Russian Violet `#1B1745`.

## Build

1. Open this repository in a current stable Android Studio.
2. Install Android SDK 36 and a compatible JDK 17.
3. Let Android Studio sync Gradle dependencies.
4. Run the `app` configuration on an API 26+ device or emulator.
5. For a release build, configure a private signing key and run `assembleRelease` from Android Studio's Gradle panel.

No `INTERNET` permission is present. Initial dependency resolution during development still requires the build machine to access Google's and Maven Central's artifact repositories.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Supported formats and known limitations](docs/SUPPORTED_FORMATS.md)
- [Privacy statement](docs/PRIVACY.md)

## Current limitations

This first native baseline prioritizes local library import, EPUB/text parsing, reflowable reading, position persistence, bookmarks, and installed-device TTS. PDF visual paging, full EPUB inline style rendering, media-session lock-screen actions, backup/restore UI, metadata editing, and storage cleanup are architected but require further production hardening before a store release.
