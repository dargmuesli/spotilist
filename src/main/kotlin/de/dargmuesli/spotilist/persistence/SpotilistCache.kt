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
    private val connection: Connection by lazy {
        Files.createDirectories(Persistence.cacheDirectory)
        DriverManager.getConnection("jdbc:sqlite:${Persistence.cacheDirectory.resolve("cache.db")}")
    }

    fun open() {
        SpotifyCache.open(connection)
        YouTubeCache.open(connection)
    }

    fun close() {
        connection.close()
    }
}
