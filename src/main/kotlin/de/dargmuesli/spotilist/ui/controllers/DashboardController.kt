package de.dargmuesli.spotilist.ui.controllers

import de.dargmuesli.spotilist.MainApp
import de.dargmuesli.spotilist.models.PlaylistMapping
import de.dargmuesli.spotilist.persistence.SpotilistConfig
import de.dargmuesli.spotilist.ui.SpotilistStage
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Accordion
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
}
