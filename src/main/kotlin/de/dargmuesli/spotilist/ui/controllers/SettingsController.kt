package de.dargmuesli.spotilist.ui.controllers

import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.persistence.config.ExportConfig
import de.dargmuesli.spotilist.persistence.config.SpotifyConfig
import de.dargmuesli.spotilist.persistence.config.YouTubeConfig
import de.dargmuesli.spotilist.providers.util.SpotifyUtil
import de.dargmuesli.spotilist.ui.SpotilistNotification
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.JavaFxDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.*


class SettingsController : Initializable, CoroutineScope {
    override val coroutineContext: JavaFxDispatcher
        get() = Dispatchers.JavaFx

    @FXML
    private lateinit var spotifyClientIdTextField: TextField

    @FXML
    private lateinit var spotifyClientSecretTextField: TextField

    @FXML
    private lateinit var spotifyRedirectUriTextField: TextField

    @FXML
    private lateinit var spotifyAuthorizationCodeTextField: TextField

    @FXML
    private lateinit var spotifyAuthorizationLabel: Label

    @FXML
    private lateinit var openAuthorizationButton: Button

    @FXML
    private lateinit var youTubeApiKeyTextField: TextField

    @FXML
    private lateinit var exportDirectoryTextField: TextField

    @FXML
    private lateinit var exportTrackPathPrefixTextField: TextField

    override fun initialize(url: URL?, rb: ResourceBundle?) {
        spotifyClientIdTextField.text = SpotifyConfig.clientId.value
        spotifyClientSecretTextField.text = SpotifyConfig.clientSecret.value
        spotifyRedirectUriTextField.text = SpotifyConfig.redirectUri.value
        spotifyAuthorizationCodeTextField.text = SpotifyConfig.authorizationCode.value
        youTubeApiKeyTextField.text = YouTubeConfig.apiKey.value
        exportDirectoryTextField.text = ExportConfig.directory.value
        exportTrackPathPrefixTextField.text = ExportConfig.trackPathPrefix.value

        SpotifyConfig.clientId.addListener { _ ->
            updateAuthorizationButton()
        }
        SpotifyConfig.clientSecret.addListener { _ ->
            updateAuthorizationButton()
        }
        SpotifyConfig.redirectUri.addListener { _ ->
            updateAuthorizationButton()
        }
        SpotifyConfig.authorizationCode.addListener { _ ->
            spotifyAuthorizationCodeTextField.text = SpotifyConfig.authorizationCode.value
            updateAuthorizationButton()
        }
        SpotifyCache.accessTokenExpiresAt.addListener { _ ->
            updateAuthorizationButton()
        }

        updateAuthorizationButton()
    }

    private fun updateAuthorizationButton() {
        spotifyAuthorizationLabel.text = if (SpotifyCache.accessTokenExpiresAt.value > Date().time / 1000) {
            "Authorized"
        } else {
            "Unauthorized"
        }
        openAuthorizationButton.isDisable = !isAuthorizable()
        openAuthorizationButton.text = if (SpotifyConfig.authorizationCode.value.isNullOrEmpty()) {
            "Open Authorization"
        } else {
            "Authorize"
        }
    }

    @FXML
    private fun onSpotifyClientIdInput() {
        SpotifyConfig.clientId.set(spotifyClientIdTextField.text)
        openAuthorizationButton.isDisable = !isAuthorizable()
    }

    @FXML
    private fun onSpotifyClientSecretInput() {
        SpotifyConfig.clientSecret.set(spotifyClientSecretTextField.text)
        openAuthorizationButton.isDisable = !isAuthorizable()
    }

    @FXML
    private fun onSpotifyRedirectUriInput() {
        SpotifyConfig.redirectUri.set(spotifyRedirectUriTextField.text)
        openAuthorizationButton.isDisable = !isAuthorizable()
    }

    @FXML
    private fun onSpotifyAuthorizationCodeInput() {
        SpotifyConfig.authorizationCode.set(spotifyAuthorizationCodeTextField.text)
        openAuthorizationButton.isDisable = !isAuthorizable()
    }

    @FXML
    private fun onYouTubeApiKeyInput() {
        YouTubeConfig.apiKey.set(youTubeApiKeyTextField.text)
    }

    @FXML
    private fun onExportDirectoryInput() {
        ExportConfig.directory.set(exportDirectoryTextField.text)
    }

    @FXML
    private fun onExportTrackPathPrefixInput() {
        ExportConfig.trackPathPrefix.set(exportTrackPathPrefixTextField.text)
    }

    @FXML
    private fun openAuthorization() {
        openAuthorizationButton.isDisable = true

        launch {
            try {
                withContext(Dispatchers.IO) {
                    SpotifyUtil.authorize()
                }

                if (SpotifyCache.accessTokenExpiresAt.value > Date().time / 1000) {
                    SpotifyConfig.authorizationCode.set("")
                }
            } catch (e: Exception) {
                SpotilistNotification.error("Spotify authorization failed!", e)
            } finally {
                updateAuthorizationButton()
            }
        }
    }

    private fun isAuthorizable(): Boolean {
        if ((spotifyClientIdTextField.text == ""
                    || spotifyClientSecretTextField.text == ""
                    || spotifyRedirectUriTextField.text == "") && spotifyAuthorizationCodeTextField.text == ""
        ) {
            return false
        }

        return try {
            URI(spotifyRedirectUriTextField.text)
            true
        } catch (exception: URISyntaxException) {
            false
        } catch (exception: MalformedURLException) {
            false
        }
    }
}
