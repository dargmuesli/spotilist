package de.dargmuesli.spotilist.providers.provider

import de.dargmuesli.spotilist.models.music.Album
import de.dargmuesli.spotilist.models.music.Artist
import de.dargmuesli.spotilist.models.music.Playlist
import de.dargmuesli.spotilist.models.music.Track
import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.providers.ISpotilistProviderAuthorizable
import de.dargmuesli.spotilist.providers.util.SpotifyUtil.getAllPagingItems
import de.dargmuesli.spotilist.providers.util.SpotifyUtil.spotifyApi
import org.apache.logging.log4j.LogManager
import se.michaelthelin.spotify.enums.AlbumType
import java.util.*


object SpotifyProvider :
    ISpotilistProviderAuthorizable<se.michaelthelin.spotify.model_objects.specification.Playlist, se.michaelthelin.spotify.model_objects.specification.PlaylistTrack> {
    private val LOGGER = LogManager.getLogger()
    private val PLAYLIST_ID_REGEX = Regex("^[a-zA-Z\\d]{22}$")

    override fun getProviderPlaylist(playlistId: String): se.michaelthelin.spotify.model_objects.specification.Playlist? {
        return if (SpotifyCache.playlistData.containsKey(playlistId)) {
            SpotifyCache.playlistData[playlistId]
        } else {
            spotifyApi.getPlaylist(playlistId).build().execute().also {
                SpotifyCache.playlistData[it.id] = it
            }
        }
    }

    override fun getProviderPlaylistItems(playlistId: String): List<se.michaelthelin.spotify.model_objects.specification.PlaylistTrack>? {
        return if (SpotifyCache.playlistItemMap.containsKey(playlistId)) {
            SpotifyCache.playlistItemMap[playlistId]?.map {
                SpotifyCache.playlistItemData[it]!!
            }
        } else {
            getAllPagingItems(spotifyApi.getPlaylistsItems(playlistId))
                .ifEmpty { null }
                ?.also {
                    if (SpotifyCache.playlistItemMap.containsKey(playlistId)) {
                        SpotifyCache.playlistItemMap.clear()
                    }
                }
                ?.onEach {
                    if (!SpotifyCache.playlistItemData.containsKey(it.track.id)) {
                        SpotifyCache.playlistItemData[it.track.id] = it
                    }

                    if (!SpotifyCache.playlistItemMap.containsKey(playlistId)) {
                        SpotifyCache.playlistItemMap[playlistId] = mutableListOf()
                    }

                    SpotifyCache.playlistItemMap[playlistId]!!.add(it.track.id)
                }
        }
    }

    override fun getPlaylist(playlistId: String): Playlist? {
        val playlist = getProviderPlaylist(playlistId) ?: return null
        return Playlist(name = playlist.name, tracks = getPlaylistItems(playlistId))
    }

    override fun getPlaylistItems(playlistId: String): List<Track>? {
        val spotifyPlaylistItems = getProviderPlaylistItems(playlistId) ?: return null

        return spotifyPlaylistItems.map { spotifyPlaylistTrack: se.michaelthelin.spotify.model_objects.specification.PlaylistTrack ->
            val track = spotifyPlaylistTrack.track as se.michaelthelin.spotify.model_objects.specification.Track

            if (track.linkedFrom != null) {
                LOGGER.warn(track.name + "might differ! " + track.linkedFrom.id)
            }

            Track(
                album = Album(
                    albumType = AlbumType.valueOf(track.album.albumType.name),
                    artists = track.album.artists.map { artistSimplified ->
                        Artist(name = artistSimplified.name)
                    },
                    name = track.album.name
                ),
                artists = track.artists.map { artistSimplified ->
                    Artist(name = artistSimplified.name)
                },
                durationMs = spotifyPlaylistTrack.track.durationMs.toLong(),
                id = spotifyPlaylistTrack.track.id,
                name = spotifyPlaylistTrack.track.name
            )
        }
    }

    override fun isPlaylistIdValid(playlistId: String): Boolean {
        return PLAYLIST_ID_REGEX.matches(playlistId)
    }

    override fun isAuthorized(): Boolean {
        if (SpotifyCache.accessTokenExpiresAt.value > 0 && SpotifyCache.accessTokenExpiresAt.value > Date().time / 1000) {
            return true
        }

        return false
    }
}
