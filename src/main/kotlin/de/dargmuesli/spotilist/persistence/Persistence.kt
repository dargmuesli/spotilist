package de.dargmuesli.spotilist.persistence

import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.ui.SpotilistNotification
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
    private const val SQLITE_CACHE_TABLE = "persistence_cache"
    private const val SQLITE_CACHE_PAYLOAD_KEY = "cache"
    private const val SQLITE_CREATE_CACHE_TABLE_QUERY =
        "CREATE TABLE IF NOT EXISTS $SQLITE_CACHE_TABLE (key TEXT PRIMARY KEY, payload TEXT NOT NULL)"

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
    private val cacheDatabasePath: Path = cacheDirectory.resolve("${PersistenceTypes.CACHE.toString().lowercase()}.sqlite")
    private val fileMap = hashMapOf(
        PersistenceTypes.CACHE to cacheDirectory,
        PersistenceTypes.CONFIG to configDirectory
    )

    private val versionProperties = Properties()

    init {
        versionProperties.load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun getVersion(): String = versionProperties.getProperty("version")

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
                            exitProcess(0)
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

                    Files.writeString(
                        filePath,
                        format.encodeToString(PersistenceWrapper[type])
                    )
                }
            }
        }

    }

    private fun loadCacheFromSqlite() {
        if (!Files.exists(cacheDatabasePath)) return

        try {
            DriverManager.getConnection("jdbc:sqlite:${cacheDatabasePath.toAbsolutePath()}").use { connection ->
                initializeCacheTable(connection)

                connection.prepareStatement("SELECT payload FROM $SQLITE_CACHE_TABLE WHERE key = ?").use { statement ->
                    statement.setString(1, SQLITE_CACHE_PAYLOAD_KEY)

                    statement.executeQuery().use { resultSet ->
                        if (!resultSet.next()) return

                        PersistenceWrapper[PersistenceTypes.CACHE] =
                            format.decodeFromString(
                                AbstractSerializable.serializer(),
                                resultSet.getString("payload")
                            )
                    }
                }
            }
        } catch (e: Exception) {
            SpotilistNotification.error("Loading application data failed!", e)
            exitProcess(0)
        }
    }

    private fun saveCacheToSqlite() {
        if (!Files.exists(cacheDatabasePath.parent)) {
            Files.createDirectories(cacheDatabasePath.parent)
        }

        try {
            DriverManager.getConnection("jdbc:sqlite:${cacheDatabasePath.toAbsolutePath()}").use { connection ->
                initializeCacheTable(connection)

                connection.prepareStatement(
                    "INSERT INTO $SQLITE_CACHE_TABLE (key, payload) VALUES (?, ?) " +
                        "ON CONFLICT(key) DO UPDATE SET payload = excluded.payload"
                ).use { statement ->
                    statement.setString(1, SQLITE_CACHE_PAYLOAD_KEY)
                    statement.setString(2, format.encodeToString(PersistenceWrapper[PersistenceTypes.CACHE]))
                    statement.executeUpdate()
                }
            }
        } catch (e: Exception) {
            SpotilistNotification.error("Saving application data failed!", e)
        }
    }

    private fun initializeCacheTable(connection: Connection) {
        connection.createStatement().use {
            it.execute(SQLITE_CREATE_CACHE_TABLE_QUERY)
        }
    }
}
