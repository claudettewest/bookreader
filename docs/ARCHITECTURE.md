# Architecture

Offline Bookshelf is a single-activity native Android application using Kotlin, Jetpack Compose, Material 3, Room, coroutines, and `StateFlow`.

## Layers

- `ui`: responsive Compose screens and view models. UI observes immutable state and sends user actions back to view models.
- `data`: Room entities and DAO. Visual reading position and TTS position are deliberately separate.
- `importer`: format detection, persisted Storage Access Framework permissions, private copies, hashing, parsing, chapter extraction, and local resource storage.
- `tts`: Android `TextToSpeech` foreground service. It uses only voices installed on the device.

Each imported book owns `files/books/{bookId}/` with `original`, `cover`, `chapters`, `images`, `search`, and `cache` subdirectories as needed. Large reflowable documents are split into bounded sections rather than placed in one Compose text node. EPUB spine order is preserved.

Room starts at schema version 1 and exports its schema. Future releases must add explicit migrations; destructive migration fallback is intentionally absent.
