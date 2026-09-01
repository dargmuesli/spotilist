package de.dargmuesli.spotilist.persistence

import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.persistence.cache.YouTubeCache
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager

/**
 * Owns the single SQLite connection backing [SpotifyCache] and [YouTubeCache], replacing the former JSON-blob cache that had to be fully read into memory and fully rewritten on every save.
 */
object SpotilistCache {
    /** Bumped whenever a cached row's shape changes; [migrate] drops what the new code cannot read. */
    private const val SCHEMA_VERSION = 1

    private val connection: Connection by lazy {
        Files.createDirectories(Persistence.cacheDirectory)
        DriverManager.getConnection("jdbc:sqlite:${Persistence.cacheDirectory.resolve("cache.db")}")
    }

    fun open() {
        migrate()
        SpotifyCache.open(connection)
        YouTubeCache.open(connection)
    }

    /**
     * Drops cached rows whose shape no longer matches what the providers deserialize into.
     * Every row here is a re-fetchable copy of an API response, so dropping one costs a request, never data.
     * SQLite's own `user_version` records how far the file has been carried; it reads 0 for a database written before this existed.
     */
    private fun migrate() {
        connection.createStatement().use { statement ->
            val version = statement.executeQuery("PRAGMA user_version").use { resultSet ->
                if (resultSet.next()) resultSet.getInt(1) else 0
            }

            if (version < 1) {
                // spotify-web-api-java 10 renamed the playlist's `tracks` field to `items` and the playlist track's `track` field to `item`, so rows written against version 9 read back with a null item and crash on use.
                // The three tables go together: keeping `playlistItemMap` would leave track ids pointing at `playlistItemData` rows that are no longer there.
                for (table in listOf("spotify.playlistData", "spotify.playlistItemData", "spotify.playlistItemMap")) {
                    statement.executeUpdate("DROP TABLE IF EXISTS \"$table\"")
                }
            }

            statement.executeUpdate("PRAGMA user_version = $SCHEMA_VERSION")
        }
    }

    fun close() {
        connection.close()
    }
}
