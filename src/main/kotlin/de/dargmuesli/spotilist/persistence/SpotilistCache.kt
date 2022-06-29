package de.dargmuesli.spotilist.persistence

import de.dargmuesli.spotilist.models.PlaylistMapping
import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = SpotilistCache.Serializer::class)
object SpotilistCache : AbstractSerializable() {
    var spotify = SpotifyCache

    var playlistMappings: ObservableList<PlaylistMapping> = observableArrayList()

    object Serializer : KSerializer<SpotilistCache> {
        override val descriptor: SerialDescriptor = SpotilistCacheSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SpotilistCache) {
            encoder.encodeSerializableValue(
                SpotilistCacheSurrogate.serializer(),
                SpotilistCacheSurrogate(spotify, playlistMappings)
            )
        }

        override fun deserialize(decoder: Decoder): SpotilistCache {
            val spotilistCache = decoder.decodeSerializableValue(SpotilistCacheSurrogate.serializer())
            spotify = spotilistCache.spotify
            playlistMappings.addAll(spotilistCache.playlistMappings)
            return SpotilistCache
        }
    }

    @Serializable
    @SerialName("SpotilistCache")
    private data class SpotilistCacheSurrogate(val spotify: SpotifyCache, val playlistMappings: List<PlaylistMapping>)
}