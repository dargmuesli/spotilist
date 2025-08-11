package de.dargmuesli.spotilist.persistence.cache

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import de.dargmuesli.spotilist.persistence.Persistence
import de.dargmuesli.spotilist.persistence.PersistenceTypes
import de.dargmuesli.spotilist.util.serializer.YouTubePlaylistItemSerializer
import de.dargmuesli.spotilist.util.serializer.YouTubePlaylistSerializer
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = YouTubeCache.Serializer::class)
object YouTubeCache : IProviderCache<Playlist, PlaylistItem> {
    override var playlistData: ObservableMap<String, Playlist> = observableHashMap()
    override var playlistItemData: ObservableMap<String, PlaylistItem> = observableHashMap()
    override var playlistItemMap: ObservableMap<String, MutableList<String>> = observableHashMap()

    object Serializer : KSerializer<YouTubeCache> {
        override val descriptor: SerialDescriptor = YouTubeCacheSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: YouTubeCache) {
            encoder.encodeSerializableValue(
                YouTubeCacheSurrogate.serializer(),
                YouTubeCacheSurrogate(
                    playlistData.toMap(), playlistItemData.toMap(), playlistItemMap.toMap()
                )
            )
        }

        override fun deserialize(decoder: Decoder): YouTubeCache {
            val youTubeCache = decoder.decodeSerializableValue(YouTubeCacheSurrogate.serializer())
            playlistData.putAll(youTubeCache.playlistData)
            playlistItemData.putAll(youTubeCache.playlistItemData)
            playlistItemMap.putAll(youTubeCache.playlistItemMap)
            return YouTubeCache
        }
    }

    @Serializable
    @SerialName("YouTubeCache")
    private data class YouTubeCacheSurrogate(
        val playlistData: Map<String, @Serializable(with = YouTubePlaylistSerializer.Serializer::class) Playlist>,
        val playlistItemData: Map<String, @Serializable(with = YouTubePlaylistItemSerializer.Serializer::class) PlaylistItem>,
        val playlistItemMap: Map<String, MutableList<String>>
    )
}