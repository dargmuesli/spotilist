package de.dargmuesli.spotilist.ui.controllers

import com.google.gson.Gson
import com.google.gson.JsonParser
import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.models.PlaylistMapping
import de.dargmuesli.spotilist.models.music.Track
import de.dargmuesli.spotilist.persistence.SpotilistConfig
import de.dargmuesli.spotilist.providers.SpotilistProviderType
import de.dargmuesli.spotilist.providers.provider.SpotifyProvider
import de.dargmuesli.spotilist.providers.util.SpotifyUtil.spotifyApi
import de.dargmuesli.spotilist.ui.SpotilistStage
import de.dargmuesli.spotilist.util.Util
import javafx.collections.ListChangeListener
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Accordion
import javafx.scene.control.Button
import javafx.stage.Modality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.michaelthelin.spotify.model_objects.AbstractModelObject
import java.io.File
import java.net.URL
import java.util.*

class DashboardController : Initializable, CoroutineScope {
    companion object {
        val LOGGER: Logger = LogManager.getLogger()
    }

    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.JavaFx + job

    @FXML
    private lateinit var playlistMappingsAccordion: Accordion

    @FXML
    private lateinit var reportGenerateButton: Button

    override fun initialize(url: URL?, rb: ResourceBundle?) {
        SpotilistConfig.playlistMappings.addListener(ListChangeListener { playlistMappingChange ->
            while (playlistMappingChange.next()) {
                playlistMappingChange.addedSubList.forEach(::playlistMappingAdd)
                playlistMappingChange.removed.forEach(::playlistMappingRemove)
            }
        })
    }

    @FXML
    private fun addPlaylistMapping() {
        SpotilistConfig.playlistMappings.add(PlaylistMapping())
    }

    @FXML
    private fun menuFileSettingsAction() {
        SpotilistStage(
            "/de/dargmuesli/spotilist/fxml/settings.fxml",
            Modality.APPLICATION_MODAL,
            MainApp.RESOURCES.getString("settings")
        ).show()
    }

    private fun playlistMappingAdd(playlistMapping: PlaylistMapping) {
        val loader =
            FXMLLoader(MainApp::class.java.getResource("fxml/playlistMapping.fxml"), MainApp.RESOURCES)
        playlistMappingsAccordion.panes.add(loader.load())
        val controller: PlaylistMappingController = loader.getController()
        controller.playlistMapping = playlistMapping
    }

    private fun playlistMappingRemove(playlistMapping: PlaylistMapping) {
        playlistMappingsAccordion.panes.forEach {
            LOGGER.info(it.content)
        }
    }

    fun generateReport(actionEvent: ActionEvent) {
        reportGenerateButton.isDisable = true
        LOGGER.info("Searching Spotify playlist tracks in local filesystem:")

        val playlistMappings = SpotilistConfig.playlistMappings.toList()

        launch(Dispatchers.IO) {
            playlistMappings.forEach { playlistMapping ->
                val sourcePlaylist =
                    SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                        playlistMapping.sourceResource.id.value
                    ) ?: return@forEach

                if (!arrayOf("Liked Songs").contains(sourcePlaylist.name)) {
                    return@forEach
                }

                val targetPlaylist =
                    SpotilistProviderType.valueOf(playlistMapping.targetResource.provider.value).type.getPlaylist(
                        playlistMapping.targetResource.id.value
                    ) ?: return@forEach

                val sourceTracks = sourcePlaylist.tracks
                val targetNames = targetPlaylist.tracks?.map { track ->
                    track.artists?.let { artists ->
                        Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                    } + Util.getValidFilename(track.name ?: "")
                }?.toHashSet() ?: return@forEach

                val notFound = sourceTracks?.filter {
                    !targetNames.contains(
                        it.artists?.let { artists ->
                            Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                        } + Util.getValidFilename(it.name ?: "")
                    )
                }

                if (notFound == null || notFound.isEmpty()) {
                    LOGGER.info("All found in \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks?.size})!")
                } else {
                    LOGGER.info(
                        "In \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks.size}), but not in \"${targetPlaylist.name}\" (${targetPlaylist.tracks.size}):\n${
                            notFound.joinToString(
                                "\n"
                            ) { it.name + " (" + it.id + ")" }
                        }"
                    )

                    if (arrayOf("Liked Songs").contains(sourcePlaylist.name)) {
                        val currentUserProfile = spotifyApi.currentUsersProfile.build().execute()
                        val playlist =
                            spotifyApi.createPlaylist(currentUserProfile.id, "TODO (Date)").public_(false).build().execute()
                        notFound.map { "spotify:track:" + it.id }.chunked(100).forEach { chunk ->
                            spotifyApi.addItemsToPlaylist(playlist.id, JsonParser.parseString(Gson().toJson(chunk)).asJsonArray).build().execute()
                        }
                    }
                }
            }

            LOGGER.info("Searching Spotify playlist tracks in other playlists:")

            playlistMappings.forEachIndexed { index, playlistMapping ->
                val sourcePlaylist =
                    SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                        playlistMapping.sourceResource.id.value
                    ) ?: return@forEachIndexed

                if (arrayOf("Liked Songs").contains(sourcePlaylist.name)) {
                    return@forEachIndexed
                }

                LOGGER.info("Comparing \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks?.size}):")

                playlistMappings.forEachIndexed { indexInner, playlistMappingInner ->
                    if (index <= indexInner || playlistMapping.sourceResource.id.value == playlistMappingInner.sourceResource.id.value) {
                        return@forEachIndexed
                    }

                    val targetPlaylist =
                        SpotilistProviderType.valueOf(playlistMappingInner.sourceResource.provider.value).type.getPlaylist(
                            playlistMappingInner.sourceResource.id.value
                        ) ?: return@forEachIndexed

                    if (arrayOf("Liked Songs").contains(targetPlaylist.name)) {
                        return@forEachIndexed
                    }

                    val sourceNames = sourcePlaylist.tracks?.map { track ->
                        track.artists?.let { artists ->
                            Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                        } + Util.getValidFilename(track.name ?: "")
                    }?.toHashSet() ?: return@forEachIndexed
                    val targetNames = targetPlaylist.tracks?.map { track ->
                        track.artists?.let { artists ->
                            Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                        } + Util.getValidFilename(track.name ?: "")
                    }?.toHashSet() ?: return@forEachIndexed

                    val found = sourceNames.filter { targetNames.contains(it) }.joinToString("\n")

                    if (found.isEmpty()) {
                        LOGGER.debug("None found in \"${targetPlaylist.name}\" (${targetPlaylist.tracks.size})!")
                    } else {
                        LOGGER.info("In \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks.size}), but also in \"${targetPlaylist.name}\" (${targetPlaylist.tracks.size}):\n${found}")
                    }
                }
            }

            LOGGER.info("Searching liked tracks that are not in any genre playlist:")

            val likedSongsId = "4P9gkeY7Pd8EXn4IijePlE"

            val likedSongsPlaylist = SpotifyProvider.getPlaylist(
                likedSongsId
            ) ?: return@launch

            val foundNames = mutableListOf<String>()

            for (playlistMapping in playlistMappings) {
                // TODO: allow to mark playlists as excluded from comparison
                if (likedSongsId == playlistMapping.sourceResource.id.value) {
                    continue
                }

                val targetPlaylist =
                    SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                        playlistMapping.sourceResource.id.value
                    ) ?: continue

                if (arrayOf("Liked Songs").contains(targetPlaylist.name)) {
                    continue
                }

                val targetNames = targetPlaylist.tracks?.map { track ->
                    track.artists?.let { artists ->
                        Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                    } + Util.getValidFilename(track.name ?: "")
                }?.toHashSet() ?: continue
                foundNames.addAll(targetNames)
            }

            val notFoundNames = mutableListOf<Track>()

            likedSongsPlaylist.tracks?.forEach { track ->
                val likedSongsName = track.artists?.let { artists ->
                    Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                } + Util.getValidFilename(track.name ?: "")

                if (!foundNames.contains(likedSongsName)) {
                    notFoundNames.add(track)
                }
            }

            if (notFoundNames.isEmpty()) {
                LOGGER.info("All \"Liked Songs\" are in a playlist.")
            } else {
                LOGGER.info("In \"Liked Songs\", but not in any playlist:\n${notFoundNames.joinToString("\n") { it.name + " (" + it.id + ")" }}")
            }

            LOGGER.info("Generating m3u files:")

            for (playlistMapping in playlistMappings) {
                if (likedSongsId == playlistMapping.sourceResource.id.value) {
                    continue
                }

                val targetPlaylist =
                    SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                        playlistMapping.sourceResource.id.value
                    ) ?: continue

                if (arrayOf("Liked Songs").contains(targetPlaylist.name)) {
                    continue
                }

                val targetNames = targetPlaylist.tracks?.map { track ->
                    "M:\\Quellen\\Spotify\\" + track.artists?.let { artists ->
                        Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                    } + Util.getValidFilename(track.name ?: "") + ".mp3"
                }?.reduce { acc, s -> acc + "\n" + s } ?: continue
                targetPlaylist.name?.let {
                    File("/run/media/jonas/music/Playlists/Spotilist/" + Util.getValidFilename(it) + ".m3u").writeText(
                        targetNames
                    )
                }
            }

            LOGGER.info("Done!")

            withContext(Dispatchers.JavaFx) {
                reportGenerateButton.isDisable = false
            }
        }
    }
}
