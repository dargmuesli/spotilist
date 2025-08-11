package de.dargmuesli.spotilist.persistence.cache

import de.dargmuesli.spotilist.persistence.Persistence
import de.dargmuesli.spotilist.persistence.PersistenceTypes
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistSerializer
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistTrackSerializer
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.ObservableMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack


@Serializable(with = SpotifyCache.Serializer::class)
object SpotifyCache : IProviderCache<Playlist, PlaylistTrack> {
    override var playlistData: ObservableMap<String, Playlist> = observableHashMap()
    override var playlistItemData: ObservableMap<String, PlaylistTrack> = observableHashMap()
    override var playlistItemMap: ObservableMap<String, MutableList<String>> = observableHashMap()

    val accessToken = SimpleStringProperty().also {
        it.addListener { _, _, _ -> Persistence.save(PersistenceTypes.CACHE) }
    }
    val refreshToken = SimpleStringProperty().also {
        it.addListener { _, _, _ -> Persistence.save(PersistenceTypes.CACHE) }
    }
    val accessTokenExpiresAt = SimpleLongProperty().also {
        it.addListener { _, _, _ -> Persistence.save(PersistenceTypes.CACHE) }
    }

    object Serializer : KSerializer<SpotifyCache> {
        override val descriptor: SerialDescriptor = SpotifyCacheSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SpotifyCache) {
            encoder.encodeSerializableValue(
                SpotifyCacheSurrogate.serializer(),
                SpotifyCacheSurrogate(
                    playlistData.toMap(), playlistItemData.toMap(), playlistItemMap.toMap(),
                    accessToken.value,
                    refreshToken.value,
                    accessTokenExpiresAt.value
                )
            )
        }

        override fun deserialize(decoder: Decoder): SpotifyCache {
            val spotifyCache = decoder.decodeSerializableValue(SpotifyCacheSurrogate.serializer())
            playlistData.putAll(spotifyCache.playlistData)
            playlistItemData.putAll(spotifyCache.playlistItemData)
            playlistItemMap.putAll(spotifyCache.playlistItemMap)
            accessToken.set(spotifyCache.accessToken)
            refreshToken.set(spotifyCache.refreshToken)
            spotifyCache.accessTokenExpiresAt?.also { accessTokenExpiresAt.set(it) }
            return SpotifyCache
        }
    }

    @Serializable
    @SerialName("SpotifyCache")
    private data class SpotifyCacheSurrogate(
        val playlistData: Map<String, @Serializable(with = SpotifyPlaylistSerializer.Serializer::class) Playlist>,
        val playlistItemData: Map<String, @Serializable(with = SpotifyPlaylistTrackSerializer.Serializer::class) PlaylistTrack>,
        val playlistItemMap: Map<String, MutableList<String>>,
        val accessToken: String?,
        val refreshToken: String?,
        val accessTokenExpiresAt: Long?
    )
}