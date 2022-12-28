package de.dargmuesli.spotilist.ui.controllers

import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.models.PlaylistMapping
import de.dargmuesli.spotilist.persistence.SpotilistConfig
import de.dargmuesli.spotilist.providers.SpotilistProviderType
import de.dargmuesli.spotilist.providers.provider.SpotifyProvider
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
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.URL
import java.util.*

class DashboardController : Initializable {
    companion object {
        val LOGGER: Logger = LogManager.getLogger()
    }

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
        val likedSongsId = "4P9gkeY7Pd8EXn4IijePlE"
        val likedSongsTarget = "/mnt/data/Music/Quellen/Spotify/"

        LOGGER.info("Searching Spotify playlist tracks in local filesystem:")

        SpotilistConfig.playlistMappings.forEach { playlistMapping ->
            val sourcePlaylist =
                SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                    playlistMapping.sourceResource.id.value
                ) ?: return@forEach
            val targetPlaylist =
                SpotilistProviderType.valueOf(playlistMapping.targetResource.provider.value).type.getPlaylist(
                    playlistMapping.targetResource.id.value
                ) ?: return@forEach

            val sourceNames = sourcePlaylist.tracks?.map { track ->
                track.artists?.let { artists ->
                    Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                } + Util.getValidFilename(track.name ?: "")
            }?.toHashSet() ?: return@forEach
            val targetNames = targetPlaylist.tracks?.map { track ->
                track.artists?.let { artists ->
                    Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                } + Util.getValidFilename(track.name ?: "")
            }?.toHashSet() ?: return@forEach

            val notFound = sourceNames.filter { !targetNames.contains(it) }.joinToString("\n")

            LOGGER.info(if (notFound.isEmpty()) "All found in \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks.size})!" else "In \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks.size}), but not in \"${targetPlaylist.name}\" (${targetPlaylist.tracks.size}):\n${notFound}")
        }

        LOGGER.info("Searching Spotify playlist tracks in other playlists:")

        SpotilistConfig.playlistMappings.forEachIndexed { index, playlistMapping ->
            val sourcePlaylist =
                SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                    playlistMapping.sourceResource.id.value
                ) ?: return@forEachIndexed

            if (arrayOf("Liked Songs", "Songwünsche", "cReal's Probepackung").contains(sourcePlaylist.name)) {
                return@forEachIndexed
            }

            LOGGER.info("Comparing \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks?.size}):")

            SpotilistConfig.playlistMappings.forEachIndexed { indexInner, playlistMappingInner ->
                if (index <= indexInner || playlistMapping.sourceResource.id.value == playlistMappingInner.sourceResource.id.value) {
                    return@forEachIndexed
                }

                val targetPlaylist =
                    SpotilistProviderType.valueOf(playlistMappingInner.sourceResource.provider.value).type.getPlaylist(
                        playlistMappingInner.sourceResource.id.value
                    ) ?: return@forEachIndexed

                if (arrayOf(
                        "Liked Songs",
                        "Songwünsche",
                        "cReal's Probepackung"
                    ).contains(targetPlaylist.name)
                ) {
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

        val sourcePlaylist = SpotifyProvider.getPlaylist(
                likedSongsId
            ) ?: return

        val sourceNames = sourcePlaylist.tracks?.map { track ->
            track.artists?.let { artists ->
                Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
            } + Util.getValidFilename(track.name ?: "")
        }?.toHashSet() ?: return
        val foundNames = mutableListOf<String>()
        val notFoundNames = mutableListOf<String>()

        for (playlistMapping in SpotilistConfig.playlistMappings) {
            if (likedSongsId == playlistMapping.sourceResource.id.value) {
                continue
            }

            val targetPlaylist =
                SpotilistProviderType.valueOf(playlistMapping.sourceResource.provider.value).type.getPlaylist(
                    playlistMapping.sourceResource.id.value
                ) ?: continue

            if (arrayOf(
                    "Liked Songs",
                    "Songwünsche",
                    "cReal's Probepackung"
                ).contains(targetPlaylist.name)
            ) {
                continue
            }

            val targetNames = targetPlaylist.tracks?.map { track ->
                track.artists?.let { artists ->
                    Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                } + Util.getValidFilename(track.name ?: "")
            }?.toHashSet() ?: continue
            foundNames.addAll(targetNames)
        }

        for (sourceName in sourceNames) {
            if (!foundNames.contains(sourceName)) {
                notFoundNames.add(sourceName)
            }
        }

        if (notFoundNames.isEmpty()) {
            LOGGER.info("All \"Liked Songs\" are in a playlist.")
        } else {
            LOGGER.info("In \"Liked Songs\", but not in any playlist:\n${notFoundNames.joinToString("\n")}")
        }

        LOGGER.info("Done!")
    }
}
