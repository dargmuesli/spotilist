package de.dargmuesli.spotilist.persistence.cache

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import de.dargmuesli.spotilist.persistence.format
import de.dargmuesli.spotilist.util.serializer.YouTubePlaylistItemSerializer
import de.dargmuesli.spotilist.util.serializer.YouTubePlaylistSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.mapdb.DB

object YouTubeCache : IProviderCache<Playlist, PlaylistItem> {
    override var playlistData: MutableMap<String, Playlist> = mutableMapOf()
    override var playlistItemData: MutableMap<String, PlaylistItem> = mutableMapOf()
    override var playlistItemMap: MutableMap<String, MutableList<String>> = mutableMapOf()

    fun open(db: DB) {
        playlistData = MapDbBackedMap(
            db, "youtube.playlistData",
            encode = { format.encodeToString(YouTubePlaylistSerializer.Serializer, it) },
            decode = { format.decodeFromString(YouTubePlaylistSerializer.Serializer, it) }
        )
        playlistItemData = MapDbBackedMap(
            db, "youtube.playlistItemData",
            encode = { format.encodeToString(YouTubePlaylistItemSerializer.Serializer, it) },
            decode = { format.decodeFromString(YouTubePlaylistItemSerializer.Serializer, it) }
        )
        playlistItemMap = MapDbBackedMap(
            db, "youtube.playlistItemMap",
            encode = { format.encodeToString(ListSerializer(String.serializer()), it) },
            decode = { format.decodeFromString(ListSerializer(String.serializer()), it).toMutableList() }
        )
    }
}
