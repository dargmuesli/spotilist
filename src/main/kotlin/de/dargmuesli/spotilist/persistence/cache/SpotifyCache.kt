package de.dargmuesli.spotilist.persistence.cache

import de.dargmuesli.spotilist.persistence.format
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistSerializer
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistTrackSerializer
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.mapdb.DB
import org.mapdb.Serializer
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack

object SpotifyCache : IProviderCache<Playlist, PlaylistTrack> {
    override var playlistData: MutableMap<String, Playlist> = mutableMapOf()
    override var playlistItemData: MutableMap<String, PlaylistTrack> = mutableMapOf()
    override var playlistItemMap: MutableMap<String, MutableList<String>> = mutableMapOf()

    val accessToken = SimpleStringProperty()
    val refreshToken = SimpleStringProperty()
    val accessTokenExpiresAt = SimpleLongProperty()

    fun open(db: DB) {
        playlistData = MapDbBackedMap(
            db, "spotify.playlistData",
            encode = { format.encodeToString(SpotifyPlaylistSerializer.Serializer, it) },
            decode = { format.decodeFromString(SpotifyPlaylistSerializer.Serializer, it) }
        )
        playlistItemData = MapDbBackedMap(
            db, "spotify.playlistItemData",
            encode = { format.encodeToString(SpotifyPlaylistTrackSerializer.Serializer, it) },
            decode = { format.decodeFromString(SpotifyPlaylistTrackSerializer.Serializer, it) }
        )
        playlistItemMap = MapDbBackedMap(
            db, "spotify.playlistItemMap",
            encode = { format.encodeToString(ListSerializer(String.serializer()), it) },
            decode = { format.decodeFromString(ListSerializer(String.serializer()), it).toMutableList() }
        )

        val tokens = db.hashMap("spotify.tokens", Serializer.STRING, Serializer.STRING).createOrOpen()
        tokens["accessToken"]?.let { accessToken.set(it) }
        tokens["refreshToken"]?.let { refreshToken.set(it) }
        tokens["accessTokenExpiresAt"]?.toLongOrNull()?.let { accessTokenExpiresAt.set(it) }

        accessToken.addListener { _, _, newValue ->
            if (newValue != null) tokens["accessToken"] = newValue else tokens.remove("accessToken")
            db.commit()
        }
        refreshToken.addListener { _, _, newValue ->
            if (newValue != null) tokens["refreshToken"] = newValue else tokens.remove("refreshToken")
            db.commit()
        }
        accessTokenExpiresAt.addListener { _, _, newValue ->
            tokens["accessTokenExpiresAt"] = newValue.toString()
            db.commit()
        }
    }
}
