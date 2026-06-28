package de.dargmuesli.spotilist.persistence

import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.ui.SpotilistNotification
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistSerializer
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistTrackSerializer
import de.dargmuesli.spotilist.util.serializer.YouTubePlaylistItemSerializer
import de.dargmuesli.spotilist.util.serializer.YouTubePlaylistSerializer
import javafx.beans.property.SimpleBooleanProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.system.exitProcess

@OptIn(ExperimentalSerializationApi::class)
val module = SerializersModule {
    polymorphicDefaultSerializer(AbstractSerializable::class) { instance ->
        @Suppress("UNCHECKED_CAST")
        when (instance) {
            is SpotilistCache -> SpotilistCache.Serializer as SerializationStrategy<AbstractSerializable>
            is SpotilistConfig -> SpotilistConfig.Serializer as SerializationStrategy<AbstractSerializable>
        }
    }
}

val format = Json {
    prettyPrint = true
    encodeDefaults = true
    serializersModule = module
}

object Persistence {
    private const val EXIT_CODE_FAILURE = 1
    private const val CACHE_DATABASE_FILENAME = "cache.sqlite"

    private const val SPOTIFY_PLAYLIST_TABLE = "spotify_playlists"
    private const val SPOTIFY_PLAYLIST_ITEM_TABLE = "spotify_playlist_items"
    private const val SPOTIFY_PLAYLIST_MAP_TABLE = "spotify_playlist_item_map"
    private const val SPOTIFY_AUTH_TABLE = "spotify_auth"

    private const val YOUTUBE_PLAYLIST_TABLE = "youtube_playlists"
    private const val YOUTUBE_PLAYLIST_ITEM_TABLE = "youtube_playlist_items"
    private const val YOUTUBE_PLAYLIST_MAP_TABLE = "youtube_playlist_item_map"

    private const val CREATE_SPOTIFY_PLAYLIST_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $SPOTIFY_PLAYLIST_TABLE (playlist_id TEXT PRIMARY KEY, payload TEXT NOT NULL)"
    private const val CREATE_SPOTIFY_PLAYLIST_ITEM_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $SPOTIFY_PLAYLIST_ITEM_TABLE (item_id TEXT PRIMARY KEY, payload TEXT NOT NULL)"
    private const val CREATE_SPOTIFY_PLAYLIST_MAP_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $SPOTIFY_PLAYLIST_MAP_TABLE (playlist_id TEXT NOT NULL, position INTEGER NOT NULL, item_id TEXT NOT NULL, PRIMARY KEY(playlist_id, position))"
    private const val CREATE_SPOTIFY_AUTH_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $SPOTIFY_AUTH_TABLE (id INTEGER PRIMARY KEY CHECK (id = 1), access_token TEXT, refresh_token TEXT, access_token_expires_at INTEGER)"

    private const val CREATE_YOUTUBE_PLAYLIST_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $YOUTUBE_PLAYLIST_TABLE (playlist_id TEXT PRIMARY KEY, payload TEXT NOT NULL)"
    private const val CREATE_YOUTUBE_PLAYLIST_ITEM_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $YOUTUBE_PLAYLIST_ITEM_TABLE (item_id TEXT PRIMARY KEY, payload TEXT NOT NULL)"
    private const val CREATE_YOUTUBE_PLAYLIST_MAP_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $YOUTUBE_PLAYLIST_MAP_TABLE (playlist_id TEXT NOT NULL, position INTEGER NOT NULL, item_id TEXT NOT NULL, PRIMARY KEY(playlist_id, position))"

    private const val UPSERT_SPOTIFY_PLAYLIST_QUERY =
        "INSERT INTO $SPOTIFY_PLAYLIST_TABLE (playlist_id, payload) VALUES (?, ?) ON CONFLICT(playlist_id) DO UPDATE SET payload = excluded.payload"
    private const val DELETE_SPOTIFY_PLAYLIST_QUERY =
        "DELETE FROM $SPOTIFY_PLAYLIST_TABLE WHERE playlist_id = ?"
    private const val UPSERT_SPOTIFY_PLAYLIST_ITEM_QUERY =
        "INSERT INTO $SPOTIFY_PLAYLIST_ITEM_TABLE (item_id, payload) VALUES (?, ?) ON CONFLICT(item_id) DO UPDATE SET payload = excluded.payload"
    private const val DELETE_SPOTIFY_PLAYLIST_ITEM_QUERY =
        "DELETE FROM $SPOTIFY_PLAYLIST_ITEM_TABLE WHERE item_id = ?"
    private const val DELETE_SPOTIFY_PLAYLIST_MAP_QUERY =
        "DELETE FROM $SPOTIFY_PLAYLIST_MAP_TABLE WHERE playlist_id = ?"
    private const val INSERT_SPOTIFY_PLAYLIST_MAP_QUERY =
        "INSERT INTO $SPOTIFY_PLAYLIST_MAP_TABLE (playlist_id, position, item_id) VALUES (?, ?, ?)"
    private const val UPSERT_SPOTIFY_AUTH_QUERY =
        "INSERT INTO $SPOTIFY_AUTH_TABLE (id, access_token, refresh_token, access_token_expires_at) VALUES (1, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET access_token = excluded.access_token, refresh_token = excluded.refresh_token, access_token_expires_at = excluded.access_token_expires_at"

    private const val UPSERT_YOUTUBE_PLAYLIST_QUERY =
        "INSERT INTO $YOUTUBE_PLAYLIST_TABLE (playlist_id, payload) VALUES (?, ?) ON CONFLICT(playlist_id) DO UPDATE SET payload = excluded.payload"
    private const val DELETE_YOUTUBE_PLAYLIST_QUERY =
        "DELETE FROM $YOUTUBE_PLAYLIST_TABLE WHERE playlist_id = ?"
    private const val UPSERT_YOUTUBE_PLAYLIST_ITEM_QUERY =
        "INSERT INTO $YOUTUBE_PLAYLIST_ITEM_TABLE (item_id, payload) VALUES (?, ?) ON CONFLICT(item_id) DO UPDATE SET payload = excluded.payload"
    private const val DELETE_YOUTUBE_PLAYLIST_ITEM_QUERY =
        "DELETE FROM $YOUTUBE_PLAYLIST_ITEM_TABLE WHERE item_id = ?"
    private const val DELETE_YOUTUBE_PLAYLIST_MAP_QUERY =
        "DELETE FROM $YOUTUBE_PLAYLIST_MAP_TABLE WHERE playlist_id = ?"
    private const val INSERT_YOUTUBE_PLAYLIST_MAP_QUERY =
        "INSERT INTO $YOUTUBE_PLAYLIST_MAP_TABLE (playlist_id, position, item_id) VALUES (?, ?, ?)"

    var isInitialized = SimpleBooleanProperty(false)

    private val cacheDirectory: Path = Paths.get(System.getProperty("user.home"), ".cache", MainApp.APPLICATION_TITLE)
    private val configDirectory: Path
        get() {
            val os = System.getProperty("os.name").lowercase()

            return if (os.contains("win")) {
                Paths.get(System.getenv("AppData"), MainApp.APPLICATION_TITLE)
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                Paths.get(System.getProperty("user.home"), ".config", MainApp.APPLICATION_TITLE)
            } else {
                Paths.get("")
            }
        }
    private val localDirectory: Path =
        Paths.get(System.getProperty("user.home"), ".local", "share", MainApp.APPLICATION_TITLE)
    val tmpDirectory: Path = Paths.get(System.getProperty("java.io.tmpdir"), MainApp.APPLICATION_TITLE)
    private val cacheDatabasePath: Path = cacheDirectory.resolve(CACHE_DATABASE_FILENAME)
    private val fileMap = hashMapOf(
        PersistenceTypes.CACHE to cacheDirectory,
        PersistenceTypes.CONFIG to configDirectory
    )
    private val versionProperties = Properties()
    private var cachePersistenceSuppressed = false

    init {
        versionProperties.load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun getVersion(): String = versionProperties.getProperty("version")

    fun isCachePersistenceSuppressed(): Boolean = cachePersistenceSuppressed

    fun load(vararg types: PersistenceTypes) {
        if (types.isEmpty()) {
            load(*fileMap.keys.toTypedArray())
            isInitialized.set(true)
        } else {
            for (type in types) {
                if (type == PersistenceTypes.CACHE) {
                    loadCacheFromSqlite()
                    continue
                }

                fileMap[type]?.let { directory ->
                    val filePath = directory.resolve("${type.toString().lowercase()}.json")

                    if (Files.exists(filePath)) {
                        try {
                            PersistenceWrapper[type] =
                                format.decodeFromString(
                                    AbstractSerializable.serializer(),
                                    String(Files.readAllBytes(filePath))
                                )
                        } catch (e: Exception) {
                            SpotilistNotification.error("Loading application data failed!", e)
                            exitProcess(EXIT_CODE_FAILURE)
                        }
                    }
                }
            }
        }
    }

    fun save(vararg types: PersistenceTypes) {
        if (!isInitialized.value) return

        if (types.isEmpty()) {
            save(*fileMap.keys.toTypedArray())
        } else {
            for (type in types) {
                if (type == PersistenceTypes.CACHE) {
                    saveCacheToSqlite()
                    continue
                }

                fileMap[type]?.let { directory ->
                    val filePath = directory.resolve("${type.toString().lowercase()}.json")

                    if (!Files.exists(filePath.parent)) {
                        Files.createDirectories(filePath.parent)
                    }

                    Files.writeString(filePath, format.encodeToString(PersistenceWrapper[type]))
                }
            }
        }
    }

    fun upsertSpotifyPlaylist(
        playlistId: String,
        playlist: se.michaelthelin.spotify.model_objects.specification.Playlist
    ) {
        executeCacheWrite {
            it.prepareStatement(UPSERT_SPOTIFY_PLAYLIST_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.setString(2, format.encodeToString(SpotifyPlaylistSerializer.Serializer, playlist))
                statement.executeUpdate()
            }
        }
    }

    fun deleteSpotifyPlaylist(playlistId: String) {
        executeCacheWrite {
            it.prepareStatement(DELETE_SPOTIFY_PLAYLIST_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }
        }
    }

    fun upsertSpotifyPlaylistItem(
        itemId: String,
        item: se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
    ) {
        executeCacheWrite {
            it.prepareStatement(UPSERT_SPOTIFY_PLAYLIST_ITEM_QUERY).use { statement ->
                statement.setString(1, itemId)
                statement.setString(2, format.encodeToString(SpotifyPlaylistTrackSerializer.Serializer, item))
                statement.executeUpdate()
            }
        }
    }

    fun deleteSpotifyPlaylistItem(itemId: String) {
        executeCacheWrite {
            it.prepareStatement(DELETE_SPOTIFY_PLAYLIST_ITEM_QUERY).use { statement ->
                statement.setString(1, itemId)
                statement.executeUpdate()
            }
        }
    }

    fun replaceSpotifyPlaylistItemMap(playlistId: String, itemIds: List<String>) {
        executeCacheWrite { connection ->
            connection.prepareStatement(DELETE_SPOTIFY_PLAYLIST_MAP_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }

            connection.prepareStatement(INSERT_SPOTIFY_PLAYLIST_MAP_QUERY).use { statement ->
                itemIds.forEachIndexed { index, itemId ->
                    statement.setString(1, playlistId)
                    statement.setInt(2, index)
                    statement.setString(3, itemId)
                    statement.addBatch()
                }

                statement.executeBatch()
            }
        }
    }

    fun deleteSpotifyPlaylistItemMap(playlistId: String) {
        executeCacheWrite {
            it.prepareStatement(DELETE_SPOTIFY_PLAYLIST_MAP_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }
        }
    }

    fun upsertSpotifyAuth(accessToken: String?, refreshToken: String?, accessTokenExpiresAt: Long?) {
        executeCacheWrite {
            it.prepareStatement(UPSERT_SPOTIFY_AUTH_QUERY).use { statement ->
                statement.setString(1, accessToken)
                statement.setString(2, refreshToken)
                statement.setObject(3, accessTokenExpiresAt)
                statement.executeUpdate()
            }
        }
    }

    fun upsertYouTubePlaylist(playlistId: String, playlist: com.google.api.services.youtube.model.Playlist) {
        executeCacheWrite {
            it.prepareStatement(UPSERT_YOUTUBE_PLAYLIST_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.setString(2, format.encodeToString(YouTubePlaylistSerializer.Serializer, playlist))
                statement.executeUpdate()
            }
        }
    }

    fun deleteYouTubePlaylist(playlistId: String) {
        executeCacheWrite {
            it.prepareStatement(DELETE_YOUTUBE_PLAYLIST_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }
        }
    }

    fun upsertYouTubePlaylistItem(itemId: String, item: com.google.api.services.youtube.model.PlaylistItem) {
        executeCacheWrite {
            it.prepareStatement(UPSERT_YOUTUBE_PLAYLIST_ITEM_QUERY).use { statement ->
                statement.setString(1, itemId)
                statement.setString(2, format.encodeToString(YouTubePlaylistItemSerializer.Serializer, item))
                statement.executeUpdate()
            }
        }
    }

    fun deleteYouTubePlaylistItem(itemId: String) {
        executeCacheWrite {
            it.prepareStatement(DELETE_YOUTUBE_PLAYLIST_ITEM_QUERY).use { statement ->
                statement.setString(1, itemId)
                statement.executeUpdate()
            }
        }
    }

    fun replaceYouTubePlaylistItemMap(playlistId: String, itemIds: List<String>) {
        executeCacheWrite { connection ->
            connection.prepareStatement(DELETE_YOUTUBE_PLAYLIST_MAP_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }

            connection.prepareStatement(INSERT_YOUTUBE_PLAYLIST_MAP_QUERY).use { statement ->
                itemIds.forEachIndexed { index, itemId ->
                    statement.setString(1, playlistId)
                    statement.setInt(2, index)
                    statement.setString(3, itemId)
                    statement.addBatch()
                }

                statement.executeBatch()
            }
        }
    }

    fun deleteYouTubePlaylistItemMap(playlistId: String) {
        executeCacheWrite {
            it.prepareStatement(DELETE_YOUTUBE_PLAYLIST_MAP_QUERY).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }
        }
    }

    private fun loadCacheFromSqlite() {
        if (!Files.exists(cacheDatabasePath)) return

        try {
            openCacheConnection().use { connection ->
                initializeCacheTables(connection)

                cachePersistenceSuppressed = true
                try {
                    loadSpotifyCache(connection)
                    loadYouTubeCache(connection)
                } finally {
                    cachePersistenceSuppressed = false
                }
            }
        } catch (e: Exception) {
            SpotilistNotification.error("Loading application data failed!", e)
            exitProcess(EXIT_CODE_FAILURE)
        }
    }

    private fun saveCacheToSqlite() {
        if (!Files.exists(cacheDatabasePath.parent)) {
            Files.createDirectories(cacheDatabasePath.parent)
        }

        try {
            openCacheConnection().use { connection ->
                initializeCacheTables(connection)

                connection.autoCommit = false
                try {
                    clearCacheTables(connection)
                    persistSpotifyCache(connection)
                    persistYouTubeCache(connection)
                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }
        } catch (e: Exception) {
            SpotilistNotification.error("Saving application data failed!", e)
            exitProcess(EXIT_CODE_FAILURE)
        }
    }

    private fun executeCacheWrite(writeOperation: (Connection) -> Unit) {
        if (cachePersistenceSuppressed) return

        if (!Files.exists(cacheDatabasePath.parent)) {
            Files.createDirectories(cacheDatabasePath.parent)
        }

        try {
            openCacheConnection().use { connection ->
                initializeCacheTables(connection)
                writeOperation(connection)
            }
        } catch (e: Exception) {
            SpotilistNotification.error("Saving application data failed!", e)
            exitProcess(EXIT_CODE_FAILURE)
        }
    }

    private fun loadSpotifyCache(connection: Connection) {
        PersistenceWrapper.cache.spotify.playlistData.clear()
        PersistenceWrapper.cache.spotify.playlistItemData.clear()
        PersistenceWrapper.cache.spotify.playlistItemMap.clear()

        connection.prepareStatement("SELECT playlist_id, payload FROM $SPOTIFY_PLAYLIST_TABLE").use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val playlist = format.decodeFromString(
                        SpotifyPlaylistSerializer.Serializer,
                        resultSet.getString("payload")
                    )
                    PersistenceWrapper.cache.spotify.playlistData[resultSet.getString("playlist_id")] = playlist
                }
            }
        }

        connection.prepareStatement("SELECT item_id, payload FROM $SPOTIFY_PLAYLIST_ITEM_TABLE").use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val item = format.decodeFromString(
                        SpotifyPlaylistTrackSerializer.Serializer,
                        resultSet.getString("payload")
                    )
                    PersistenceWrapper.cache.spotify.playlistItemData[resultSet.getString("item_id")] = item
                }
            }
        }

        val spotifyPlaylistMap = mutableMapOf<String, MutableList<String>>()
        connection.prepareStatement(
            "SELECT playlist_id, item_id, position FROM $SPOTIFY_PLAYLIST_MAP_TABLE ORDER BY playlist_id, position"
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val playlistId = resultSet.getString("playlist_id")
                    val itemId = resultSet.getString("item_id")
                    spotifyPlaylistMap.getOrPut(playlistId) { mutableListOf() }.add(itemId)
                }
            }
        }
        PersistenceWrapper.cache.spotify.playlistItemMap.putAll(spotifyPlaylistMap)

        connection.prepareStatement(
            "SELECT access_token, refresh_token, access_token_expires_at FROM $SPOTIFY_AUTH_TABLE WHERE id = 1"
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    PersistenceWrapper.cache.spotify.accessToken.set(resultSet.getString("access_token"))
                    PersistenceWrapper.cache.spotify.refreshToken.set(resultSet.getString("refresh_token"))
                    val expiresAt = resultSet.getLong("access_token_expires_at")
                    if (!resultSet.wasNull()) {
                        PersistenceWrapper.cache.spotify.accessTokenExpiresAt.set(expiresAt)
                    }
                }
            }
        }
    }

    private fun loadYouTubeCache(connection: Connection) {
        PersistenceWrapper.cache.youTube.playlistData.clear()
        PersistenceWrapper.cache.youTube.playlistItemData.clear()
        PersistenceWrapper.cache.youTube.playlistItemMap.clear()

        connection.prepareStatement("SELECT playlist_id, payload FROM $YOUTUBE_PLAYLIST_TABLE").use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val playlist = format.decodeFromString(
                        YouTubePlaylistSerializer.Serializer,
                        resultSet.getString("payload")
                    )
                    PersistenceWrapper.cache.youTube.playlistData[resultSet.getString("playlist_id")] = playlist
                }
            }
        }

        connection.prepareStatement("SELECT item_id, payload FROM $YOUTUBE_PLAYLIST_ITEM_TABLE").use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val item = format.decodeFromString(
                        YouTubePlaylistItemSerializer.Serializer,
                        resultSet.getString("payload")
                    )
                    PersistenceWrapper.cache.youTube.playlistItemData[resultSet.getString("item_id")] = item
                }
            }
        }

        val youtubePlaylistMap = mutableMapOf<String, MutableList<String>>()
        connection.prepareStatement(
            "SELECT playlist_id, item_id, position FROM $YOUTUBE_PLAYLIST_MAP_TABLE ORDER BY playlist_id, position"
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val playlistId = resultSet.getString("playlist_id")
                    val itemId = resultSet.getString("item_id")
                    youtubePlaylistMap.getOrPut(playlistId) { mutableListOf() }.add(itemId)
                }
            }
        }
        PersistenceWrapper.cache.youTube.playlistItemMap.putAll(youtubePlaylistMap)
    }

    private fun persistSpotifyCache(connection: Connection) {
        connection.prepareStatement(UPSERT_SPOTIFY_PLAYLIST_QUERY).use { statement ->
            PersistenceWrapper.cache.spotify.playlistData.forEach { entry ->
                statement.setString(1, entry.key)
                statement.setString(2, format.encodeToString(SpotifyPlaylistSerializer.Serializer, entry.value))
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(UPSERT_SPOTIFY_PLAYLIST_ITEM_QUERY).use { statement ->
            PersistenceWrapper.cache.spotify.playlistItemData.forEach { entry ->
                statement.setString(1, entry.key)
                statement.setString(2, format.encodeToString(SpotifyPlaylistTrackSerializer.Serializer, entry.value))
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(INSERT_SPOTIFY_PLAYLIST_MAP_QUERY).use { statement ->
            PersistenceWrapper.cache.spotify.playlistItemMap.forEach { entry ->
                entry.value.forEachIndexed { index, itemId ->
                    statement.setString(1, entry.key)
                    statement.setInt(2, index)
                    statement.setString(3, itemId)
                    statement.addBatch()
                }
            }
            statement.executeBatch()
        }

        connection.prepareStatement(UPSERT_SPOTIFY_AUTH_QUERY).use { statement ->
            statement.setString(1, PersistenceWrapper.cache.spotify.accessToken.value)
            statement.setString(2, PersistenceWrapper.cache.spotify.refreshToken.value)
            statement.setObject(3, PersistenceWrapper.cache.spotify.accessTokenExpiresAt.value)
            statement.executeUpdate()
        }
    }

    private fun persistYouTubeCache(connection: Connection) {
        connection.prepareStatement(UPSERT_YOUTUBE_PLAYLIST_QUERY).use { statement ->
            PersistenceWrapper.cache.youTube.playlistData.forEach { entry ->
                statement.setString(1, entry.key)
                statement.setString(2, format.encodeToString(YouTubePlaylistSerializer.Serializer, entry.value))
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(UPSERT_YOUTUBE_PLAYLIST_ITEM_QUERY).use { statement ->
            PersistenceWrapper.cache.youTube.playlistItemData.forEach { entry ->
                statement.setString(1, entry.key)
                statement.setString(2, format.encodeToString(YouTubePlaylistItemSerializer.Serializer, entry.value))
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(INSERT_YOUTUBE_PLAYLIST_MAP_QUERY).use { statement ->
            PersistenceWrapper.cache.youTube.playlistItemMap.forEach { entry ->
                entry.value.forEachIndexed { index, itemId ->
                    statement.setString(1, entry.key)
                    statement.setInt(2, index)
                    statement.setString(3, itemId)
                    statement.addBatch()
                }
            }
            statement.executeBatch()
        }
    }

    private fun initializeCacheTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(CREATE_SPOTIFY_PLAYLIST_TABLE_QUERY)
            statement.execute(CREATE_SPOTIFY_PLAYLIST_ITEM_TABLE_QUERY)
            statement.execute(CREATE_SPOTIFY_PLAYLIST_MAP_TABLE_QUERY)
            statement.execute(CREATE_SPOTIFY_AUTH_TABLE_QUERY)
            statement.execute(CREATE_YOUTUBE_PLAYLIST_TABLE_QUERY)
            statement.execute(CREATE_YOUTUBE_PLAYLIST_ITEM_TABLE_QUERY)
            statement.execute(CREATE_YOUTUBE_PLAYLIST_MAP_TABLE_QUERY)
        }
    }

    private fun clearCacheTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("DELETE FROM $SPOTIFY_PLAYLIST_TABLE")
            statement.execute("DELETE FROM $SPOTIFY_PLAYLIST_ITEM_TABLE")
            statement.execute("DELETE FROM $SPOTIFY_PLAYLIST_MAP_TABLE")
            statement.execute("DELETE FROM $SPOTIFY_AUTH_TABLE")
            statement.execute("DELETE FROM $YOUTUBE_PLAYLIST_TABLE")
            statement.execute("DELETE FROM $YOUTUBE_PLAYLIST_ITEM_TABLE")
            statement.execute("DELETE FROM $YOUTUBE_PLAYLIST_MAP_TABLE")
        }
    }

    private fun openCacheConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:${cacheDatabasePath.toAbsolutePath()}")
    }
}
