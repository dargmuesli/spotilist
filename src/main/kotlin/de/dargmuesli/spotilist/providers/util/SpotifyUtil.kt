package de.dargmuesli.spotilist.providers.util

import de.dargmuesli.spotilist.persistence.Persistence
import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.persistence.config.SpotifyConfig
import de.dargmuesli.spotilist.ui.SpotilistNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.JavaFxDispatcher
import kotlinx.coroutines.launch
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.enums.AuthorizationScope
import se.michaelthelin.spotify.exceptions.detailed.BadRequestException
import se.michaelthelin.spotify.requests.data.AbstractDataPagingRequest
import java.awt.Desktop
import java.net.URI
import java.util.*

object SpotifyUtil : CoroutineScope {
    override val coroutineContext: JavaFxDispatcher
        get() = Dispatchers.JavaFx

    private val spotifyApiBuilder: SpotifyApi.Builder = SpotifyApi.builder()
    val spotifyApi: SpotifyApi
        get() = spotifyApiBuilder.build()

    init {
        SpotifyCache.accessToken.addListener { _ ->
            createSpotifyApiBuilder()
        }

        SpotifyConfig.clientId.addListener { _ ->
            createSpotifyApiBuilder()
        }

        SpotifyConfig.clientSecret.addListener { _ ->
            createSpotifyApiBuilder()
        }

        SpotifyConfig.redirectUri.addListener { _ ->
            createSpotifyApiBuilder()
        }

        if (Persistence.isInitialized.value) {
            createSpotifyApiBuilder()
        } else {
            Persistence.isInitialized.addListener { _ ->
                createSpotifyApiBuilder()
            }
        }
    }

    private fun createSpotifyApiBuilder() {
        spotifyApiBuilder
            .setClientId(SpotifyConfig.clientId.value)
            .setClientSecret(SpotifyConfig.clientSecret.value)
            .setAccessToken(SpotifyCache.accessToken.value)

        if (!SpotifyConfig.redirectUri.value.isNullOrEmpty()) {
            spotifyApiBuilder
                .setRedirectUri(URI(SpotifyConfig.redirectUri.value))
        }
    }

    fun authorize() {
        if (SpotifyConfig.authorizationCode.value.isNullOrEmpty()) {
            val authorizationCodeUri = spotifyApi.authorizationCodeUri()
                .scope(AuthorizationScope.PLAYLIST_MODIFY_PRIVATE)
                .build().execute()

            launch(Dispatchers.IO) {
                Desktop.getDesktop().browse(authorizationCodeUri)
            }
        } else {
            try {
                val authorizationCode =
                    spotifyApi.authorizationCode(SpotifyConfig.authorizationCode.value).build().execute()
                SpotifyCache.accessToken.set(authorizationCode.accessToken)
                SpotifyCache.refreshToken.set(authorizationCode.refreshToken)
                SpotifyCache.accessTokenExpiresAt.set(Date().time / 1000 + authorizationCode.expiresIn)
            } catch (e: BadRequestException) {
                e.message?.let { SpotilistNotification.error(it) }
            }
        }
    }

    fun <T> getAllPagingItems(requestBuilder: AbstractDataPagingRequest.Builder<T, *>): List<T> {
        val list = arrayListOf<T>()

        do {
            val paging = requestBuilder.build().execute()
            list.addAll(paging.items)
            requestBuilder
                .offset(paging.offset + paging.limit)
        } while (paging.next != null)

        return list
    }
}