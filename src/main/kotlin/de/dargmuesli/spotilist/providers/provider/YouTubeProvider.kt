package de.dargmuesli.spotilist.providers.provider

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.models.music.Artist
import de.dargmuesli.spotilist.models.music.Playlist
import de.dargmuesli.spotilist.models.music.Track
import de.dargmuesli.spotilist.persistence.cache.YouTubeCache
import de.dargmuesli.spotilist.persistence.config.YouTubeConfig
import de.dargmuesli.spotilist.providers.ISpotilistProviderAuthorizable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.JavaFxDispatcher
import org.apache.logging.log4j.LogManager


object YouTubeProvider :
    ISpotilistProviderAuthorizable<com.google.api.services.youtube.model.Playlist, com.google.api.services.youtube.model.PlaylistItem>,
    CoroutineScope {
    override val coroutineContext: JavaFxDispatcher
        get() = Dispatchers.JavaFx

    private val JSON_FACTORY = GsonFactory()
    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    private val YOUTUBE = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
        .setApplicationName(MainApp.APPLICATION_TITLE)
        .build()
    private val LOGGER = LogManager.getLogger()

    override fun getProviderPlaylist(playlistId: String): com.google.api.services.youtube.model.Playlist? {
        return if (YouTubeCache.playlistData.containsKey(playlistId)) {
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
        if (YouTubeCache.playlistItemMap.containsKey(playlistId)) {
            return YouTubeCache.playlistItemMap[playlistId]?.mapNotNull { itemId ->
                YouTubeCache.playlistItemData[itemId]
            }?.ifEmpty { null }
        }

        val playlistItemsListRequest = YOUTUBE.playlistItems()
            .list(mutableListOf("snippet,contentDetails"))
            .setFields("etag,items(etag,snippet(title),contentDetails(videoId,videoPublishedAt)),nextPageToken")
            .setKey(YouTubeConfig.apiKey.value)
            .setMaxResults(50L)
            .setPlaylistId(playlistId)
        val playlistItems = mutableListOf<com.google.api.services.youtube.model.PlaylistItem>()
        val playlistItemIds = mutableListOf<String>()
        var nextPageToken: String? = null

        do {
            LOGGER.info("Retrieving playlist page \"$nextPageToken\".")
            playlistItemsListRequest.pageToken = nextPageToken

            val playlistItemListResponse = playlistItemsListRequest.execute()

            for (playlistItem in playlistItemListResponse.items) {
                playlistItems.add(playlistItem)
                playlistItemIds.add(playlistItem.contentDetails.videoId)

                if (!YouTubeCache.playlistItemData.containsKey(playlistItem.contentDetails.videoId)) {
                    YouTubeCache.playlistItemData[playlistItem.contentDetails.videoId] = playlistItem
                }
            }

            nextPageToken = playlistItemListResponse.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())

        if (playlistItems.isNotEmpty()) {
            YouTubeCache.playlistItemMap[playlistId] = playlistItemIds
        }

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
