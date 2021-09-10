package de.dargmuesli.dargmusic.providers.spotify

import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.exceptions.detailed.NotFoundException
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest
import de.dargmuesli.dargmusic.models.enums.AlbumType
import de.dargmuesli.dargmusic.models.music.Album
import de.dargmuesli.dargmusic.models.music.Artist
import de.dargmuesli.dargmusic.models.music.Playlist
import de.dargmuesli.dargmusic.models.music.Track
import de.dargmuesli.dargmusic.persistence.state.DargmusicState
import de.dargmuesli.dargmusic.providers.IDargmusicProviderAuthorizable
import org.apache.logging.log4j.LogManager
import java.io.IOException


object SpotifyProvider : IDargmusicProviderAuthorizable {

    override fun getPlaylist(playlistId: String): Playlist {
        val spotifyPlaylistName = SpotifyUtil.spotifyApi.getPlaylist(playlistId).build().execute().name
        val spotifyPlaylistTracks = SpotifyUtil.getAllPagingItems(GetPlaylistsTracksRequest.Builder(SpotifyUtil.spotifyApi.accessToken).playlist_id(playlistId))
        val playlistTracks = arrayListOf<Track>()

        spotifyPlaylistTracks.forEach { spotifyPlaylistTrack: PlaylistTrack ->
            val trackAlbumType = AlbumType.valueOf(spotifyPlaylistTrack.track.album.albumType.name)
            val trackAlbumArtists: MutableList<Artist> = mutableListOf()

            spotifyPlaylistTrack.track.album.artists.forEach { artistSimplified ->
                trackAlbumArtists.add(Artist(name = artistSimplified.name))
            }

            val trackAlbumName: String = spotifyPlaylistTrack.track.album.name
            val trackAlbum = Album(albumType = trackAlbumType, artists = trackAlbumArtists, name = trackAlbumName)
            val trackDurationMs = spotifyPlaylistTrack.track.durationMs
            val trackName = spotifyPlaylistTrack.track.name

            playlistTracks.add(Track(trackAlbum, trackAlbumArtists, trackDurationMs, trackName))
        }

        return Playlist(spotifyPlaylistName, playlistTracks)
    }

    override fun isPlaylistIdValid(playlistId: String): Boolean {
        val errorMessage = "Playlist validation failed!"

        if (playlistId == "") {
            return false
        }

        return try {
            SpotifyUtil.spotifyApi.getPlaylist(playlistId).build().execute()
            true
        } catch (e: IOException) {
            LogManager.getLogger().error(errorMessage, e)
            false
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: SpotifyWebApiException) {
            if (e !is NotFoundException) {
                LogManager.getLogger().error("$errorMessage SpotifyWebApiException is not a NotFoundException.", e)
            }

            false
        }
    }

    override fun isAuthorized(): Boolean {
        if (DargmusicState.data.spotifyData.authorizationData.authorizationCodeCredentials.accessToken == null) {
            return false
        }

        var id = String()

        try {
            id = SpotifyUtil.spotifyApi.currentUsersProfile.build().execute().id
        } catch (e: UnauthorizedException) {
            LogManager.getLogger().debug("Access to the Spotify API was unauthorized. Check the access credentials!")
        }

        return id != ""
    }
}