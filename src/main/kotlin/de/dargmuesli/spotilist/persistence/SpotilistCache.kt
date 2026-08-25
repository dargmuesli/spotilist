package de.dargmuesli.spotilist.persistence

import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.persistence.cache.YouTubeCache
import org.mapdb.DB
import org.mapdb.DBMaker
import java.nio.file.Files

/**
 * Owns the single MapDB file backing [SpotifyCache] and [YouTubeCache], replacing the former JSON-blob cache that had to be fully read into memory and fully rewritten on every save.
 */
object SpotilistCache {
    private val db: DB by lazy {
        Files.createDirectories(Persistence.cacheDirectory)
        DBMaker
            .fileDB(Persistence.cacheDirectory.resolve("cache.db").toFile())
            .transactionEnable()
            .make()
    }

    fun open() {
        SpotifyCache.open(db)
        YouTubeCache.open(db)
    }

    fun close() {
        db.commit()
        db.close()
    }
}
