# Spotilist

Spotilist is a desktop app that keeps playlists in sync across Spotify, YouTube and your local filesystem. Define a mapping from a source playlist to a target playlist, and Spotilist adds the tracks it finds on one side to the other.

## Features

- **Cross-provider playlist mappings**: pair up any two of Spotify, YouTube or a local folder as source and target, and sync tracks between them.
- **Multiple mappings**: configure and run as many playlist pairs as you need, each independently enabled or disabled.
- **Blacklists**: exclude specific tracks from being synced on either side of a mapping.
- **Filesystem export**: treat a local directory of audio files as a playlist, matching tracks by an `Artist - Title` filename convention.
- **Playlist reports**: generate a report across your configured mappings.
- **Local persistence**: configuration is stored as JSON and provider data is cached in a local SQLite database, so nothing leaves your machine except calls to the providers you configure.

## Requirements

- Java 17 (JDK)
- A Spotify [app](https://developer.spotify.com/dashboard) (Client ID, Client Secret and a redirect URI), if you want to sync with Spotify
- A YouTube Data API v3 key from the [Google Cloud Console](https://console.cloud.google.com/), if you want to sync with YouTube

## Getting started

Clone the repository and run the app with the Gradle wrapper:

```shell
./gradlew run
```

On Windows, use `gradlew.bat run` instead.

On first launch, open **File → Settings** to enter your Spotify and/or YouTube credentials, then use **Open Authorization** to authorize Spotify access. Back on the dashboard, add a playlist mapping, pick a source and target provider and playlist ID, and enable it.

Configuration is stored at:

- Linux/macOS: `~/.config/Spotilist/config.json`
- Windows: `%AppData%\Spotilist\config.json`

Provider caches live in `~/.cache/Spotilist`.

## Building a distributable

To build a standalone runtime image with `jlink`:

```shell
./gradlew jlink
```

The resulting image, including a `spotilist` launcher, is written to `build/image`. An installable package can be built instead with `./gradlew jpackage`.

## Development

Run the test suite with:

```shell
./gradlew test
```

The project is a Kotlin/JavaFX application built with Gradle, using [Koin](https://insert-koin.io/) for dependency injection and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for its config format. Providers implement `ISpotilistProvider` (see `src/main/kotlin/de/dargmuesli/spotilist/providers/`); adding a new provider means implementing that interface and registering it in `ProviderModule.kt`.

## License

Spotilist is licensed under the [GNU General Public License v3.0](LICENSE).
