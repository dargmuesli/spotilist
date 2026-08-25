package de.dargmuesli.spotilist.persistence.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mapdb.DBMaker
import java.nio.file.Path

class MapDbBackedMapTest {
    @Test
    fun `entries survive a commit and a fresh open of the same file`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("cache.db").toFile()

        DBMaker.fileDB(file).transactionEnable().make().use { db ->
            val map = MapDbBackedMap(db, "playlists", encode = { it }, decode = { it })
            map["playlist-1"] = "Summer Hits"
        }

        DBMaker.fileDB(file).transactionEnable().make().use { db ->
            val map = MapDbBackedMap(db, "playlists", encode = { it }, decode = { it })
            assertEquals("Summer Hits", map["playlist-1"])
        }
    }

    @Test
    fun `remove and clear only touch the affected entries`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("cache.db").toFile()
        val db = DBMaker.fileDB(file).transactionEnable().make()
        val map = MapDbBackedMap(db, "playlists", encode = { it }, decode = { it })

        map["a"] = "1"
        map["b"] = "2"
        map.remove("a")

        assertNull(map["a"])
        assertEquals("2", map["b"])

        map.clear()
        assertNull(map["b"])

        db.close()
    }
}
