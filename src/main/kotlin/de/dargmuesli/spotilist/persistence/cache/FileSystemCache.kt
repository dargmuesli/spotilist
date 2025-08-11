package de.dargmuesli.spotilist.persistence.cache

import javafx.collections.FXCollections.observableHashMap
import javafx.collections.ObservableMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path

@Serializable(with = FileSystemCache.Serializer::class)
object FileSystemCache : IProviderCache<Path, Path> {
    override var playlistData: ObservableMap<String, Path> = observableHashMap()
    override var playlistItemData: ObservableMap<String, Path> = observableHashMap()
    override var playlistItemMap: ObservableMap<String, MutableList<String>> = observableHashMap()

    object Serializer : KSerializer<FileSystemCache> {
        override val descriptor: SerialDescriptor = FileSystemCacheSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: FileSystemCache) {
            encoder.encodeSerializableValue(
                FileSystemCacheSurrogate.serializer(),
                FileSystemCacheSurrogate(playlistData.toMap(), playlistItemData.toMap(), playlistItemMap.toMap())
            )
        }

        override fun deserialize(decoder: Decoder): FileSystemCache {
            val fileSystemCache = decoder.decodeSerializableValue(FileSystemCacheSurrogate.serializer())
            playlistData.putAll(fileSystemCache.playlistData)
            playlistItemData.putAll(fileSystemCache.playlistItemData)
            playlistItemMap.putAll(fileSystemCache.playlistItemMap)
            return FileSystemCache
        }
    }

    @Serializable
    @SerialName("FileSystemCache")
    private data class FileSystemCacheSurrogate(
        val playlistData: Map<String, Path>,
        val playlistItemData: Map<String, Path>,
        val playlistItemMap: Map<String, MutableList<String>>
    )
}