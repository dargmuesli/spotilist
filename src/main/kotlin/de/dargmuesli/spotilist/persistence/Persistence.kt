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
import java.util.*
import kotlin.system.exitProcess

@OptIn(ExperimentalSerializationApi::class)
val module = SerializersModule {
    polymorphicDefaultSerializer(AbstractSerializable::class) { instance ->
        @Suppress("UNCHECKED_CAST")
        when (instance) {
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
    var isInitialized = SimpleBooleanProperty(false)

    val cacheDirectory: Path = Paths.get(System.getProperty("user.home"), ".cache", MainApp.APPLICATION_TITLE)
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
    private val fileMap = hashMapOf(
        PersistenceTypes.CONFIG to configDirectory
    )

    private val versionProperties = Properties()

    init {
        versionProperties.load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun getVersion(): String = versionProperties.getProperty("version")

    fun load(vararg types: PersistenceTypes) {
        if (types.isEmpty()) {
            SpotilistCache.open()
            load(*fileMap.keys.toTypedArray())
            isInitialized.set(true)
        } else {
            for (type in types) {
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
}
