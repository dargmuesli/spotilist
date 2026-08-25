package de.dargmuesli.spotilist.persistence.cache

import de.dargmuesli.spotilist.persistence.format
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistSerializer
import de.dargmuesli.spotilist.util.serializer.SpotifyPlaylistTrackSerializer
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import se.michaelthelin.spotify.model_objects.specification.Playlist
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import java.sql.Connection

object SpotifyCache : IProviderCache<Playlist, PlaylistTrack> {
    override var playlistData: MutableMap<String, Playlist> = mutableMapOf()
    override var playlistItemData: MutableMap<String, PlaylistTrack> = mutableMapOf()
    override var playlistItemMap: MutableMap<String, MutableList<String>> = mutableMapOf()

    val accessToken = SimpleStringProperty()
    val refreshToken = SimpleStringProperty()
    val accessTokenExpiresAt = SimpleLongProperty()

    fun open(connection: Connection) {
        playlistData = SqliteBackedMap(
            connection, "spotify.playlistData",
            encode = { format.encodeToString(SpotifyPlaylistSerializer.Serializer, it) },
            decode = { format.decodeFromString(SpotifyPlaylistSerializer.Serializer, it) }
        )
        playlistItemData = SqliteBackedMap(
            connection, "spotify.playlistItemData",
            encode = { format.encodeToString(SpotifyPlaylistTrackSerializer.Serializer, it) },
            decode = { format.decodeFromString(SpotifyPlaylistTrackSerializer.Serializer, it) }
        )
        playlistItemMap = SqliteBackedMap(
            connection, "spotify.playlistItemMap",
            encode = { format.encodeToString(ListSerializer(String.serializer()), it) },
            decode = { format.decodeFromString(ListSerializer(String.serializer()), it).toMutableList() }
        )

        val tokens = SqliteBackedMap(connection, "spotify.tokens", encode = { it }, decode = { it })
        tokens["accessToken"]?.let { accessToken.set(it) }
        tokens["refreshToken"]?.let { refreshToken.set(it) }
        tokens["accessTokenExpiresAt"]?.toLongOrNull()?.let { accessTokenExpiresAt.set(it) }

        accessToken.addListener { _, _, newValue ->
            if (newValue != null) tokens["accessToken"] = newValue else tokens.remove("accessToken")
        }
        refreshToken.addListener { _, _, newValue ->
            if (newValue != null) tokens["refreshToken"] = newValue else tokens.remove("refreshToken")
        }
        accessTokenExpiresAt.addListener { _, _, newValue ->
            tokens["accessTokenExpiresAt"] = newValue.toString()
        }
    }
}
