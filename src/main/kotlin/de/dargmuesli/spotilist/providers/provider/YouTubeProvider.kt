package de.dargmuesli.spotilist.providers.provider

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.models.music.Artist
import de.dargmuesli.spotilist.models.music.Playlist
import de.dargmuesli.spotilist.models.music.Track
import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.persistence.cache.YouTubeCache
import de.dargmuesli.spotilist.persistence.config.YouTubeConfig
import de.dargmuesli.spotilist.providers.ISpotilistProviderAuthorizable
import org.apache.logging.log4j.LogManager


object YouTubeProvider :
    ISpotilistProviderAuthorizable<com.google.api.services.youtube.model.Playlist, com.google.api.services.youtube.model.PlaylistItem> {

    private val JSON_FACTORY = GsonFactory()
    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    private val YOUTUBE = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
        .setApplicationName(MainApp.APPLICATION_TITLE)
        .build()
    private val LOGGER = LogManager.getLogger()

    override fun getProviderPlaylist(playlistId: String): com.google.api.services.youtube.model.Playlist? {
        return if (SpotifyCache.playlistData.containsKey(playlistId)) {
            YouTubeCache.playlistData[playlistId]
        } else {
            val playlistListResponse = YOUTUBE.playlists()
                .list(mutableListOf("snippet"))
                .setFields("etag,items(snippet(channelTitle,title))")
                .setKey(YouTubeConfig.apiKey.value)
                .setMaxResults(1L)
                .setId(listOf(playlistId))
                .execute()

            if (playlistListResponse.items.isEmpty()) {
                null
            } else {
                playlistListResponse.items[0].also {
                    YouTubeCache.playlistData[it.id] = it
                }
            }
        }
    }

    override fun getProviderPlaylistItems(playlistId: String): List<com.google.api.services.youtube.model.PlaylistItem>? {
        // TODO: Is there a meaningful way to use the cache here?
        val playlistItemsListRequest = YOUTUBE.playlistItems()
            .list(mutableListOf("snippet,contentDetails"))
            .setFields("etag,items(etag,snippet(title),contentDetails(videoId,videoPublishedAt)),nextPageToken")
            .setKey(YouTubeConfig.apiKey.value)
            .setMaxResults(50L)
            .setPlaylistId(playlistId)
        val playlistItems = mutableListOf<com.google.api.services.youtube.model.PlaylistItem>()
        var nextPageToken: String? = null

        do {
            LOGGER.info("Retrieving playlist page \"$nextPageToken\".")
            playlistItemsListRequest.pageToken = nextPageToken

            val playlistItemListResponse = playlistItemsListRequest.execute()

            for (playlistItem in playlistItemListResponse.items) {
                playlistItems.add(playlistItem)

                if (!YouTubeCache.playlistItemData.containsKey(playlistItem.contentDetails.videoId)) {
                    YouTubeCache.playlistItemData[playlistItem.contentDetails.videoId] = playlistItem
                }
            }

            nextPageToken = playlistItemListResponse.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())

        return playlistItems.ifEmpty { null }
    }

    override fun getPlaylist(playlistId: String): Playlist? {
        val providerPlaylist = getProviderPlaylist(playlistId) ?: return null
        return Playlist(
            id = providerPlaylist.id,
            name = providerPlaylist.snippet.title,
            tracks = getPlaylistItems(playlistId)
        )
    }

    override fun getPlaylistItems(playlistId: String): List<Track>? {
        return getProviderPlaylistItems(playlistId)?.map {
            Track(
                artists = listOf(Artist(name = it.snippet.channelTitle, id = it.snippet.channelId)),
//                    durationMs = it.snippet.duration,
                id = it.id,
                name = it.snippet.title
            )
        }
    }

    override fun isPlaylistIdValid(playlistId: String): Boolean {
        return YouTubeCache.playlistData.containsKey(playlistId) || getPlaylist(playlistId) != null
    }

    override fun isAuthorized(): Boolean {
        return !YouTubeConfig.apiKey.value.isNullOrEmpty()
    }
}
