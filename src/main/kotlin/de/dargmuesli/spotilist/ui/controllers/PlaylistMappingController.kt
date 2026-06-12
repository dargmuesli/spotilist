package de.dargmuesli.spotilist.ui.controllers

import de.dargmuesli.spotilist.models.PlaylistMapping
import de.dargmuesli.spotilist.persistence.Persistence
import de.dargmuesli.spotilist.persistence.PersistenceTypes
import de.dargmuesli.spotilist.persistence.SpotilistConfig
import de.dargmuesli.spotilist.persistence.cache.SpotifyCache
import de.dargmuesli.spotilist.providers.SpotilistProviderType
import de.dargmuesli.spotilist.util.Util
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.*
import kotlin.properties.Delegates

class PlaylistMappingController : Initializable, CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.JavaFx + job

    @FXML
    private lateinit var playlistMappingTitledPane: TitledPane

    @FXML
    private lateinit var playlistMappingGridPane: GridPane

    @FXML
    private lateinit var nameTextField: TextField

    @FXML
    private lateinit var sourceProviderCombobox: ComboBox<String>

    @FXML
    private lateinit var sourceIdTextField: TextField

    @FXML
    private lateinit var targetProviderCombobox: ComboBox<String>

    @FXML
    private lateinit var targetIdTextField: TextField

    @FXML
    private lateinit var useEditButton: Button

    @FXML
    private lateinit var dataLabel: TextArea

    var playlistMapping: PlaylistMapping by Delegates.observable(PlaylistMapping()) { _, _, newPlaylistMapping ->
        playlistMappingTitledPane.text = newPlaylistMapping.name.value
        nameTextField.text = newPlaylistMapping.name.value
        sourceProviderCombobox.value = newPlaylistMapping.sourceResource.provider.value
        sourceIdTextField.text = newPlaylistMapping.sourceResource.id.value
        targetProviderCombobox.value = newPlaylistMapping.targetResource.provider.value
        targetIdTextField.text = newPlaylistMapping.targetResource.id.value

        newPlaylistMapping.sourceResource.isValid.addListener { _ ->
            updateEditButton()
        }

        newPlaylistMapping.targetResource.isValid.addListener { _ ->
            updateEditButton()
        }

        newPlaylistMapping.isEnabled.addListener { _, _, newEnabled ->
            updateEditButton()

            if (newEnabled) {
                updateData()
            } else {
                dataLabel.text = ""
            }
        }

        updateEditButton()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        SpotifyCache.accessToken.addListener { _ ->
            updateEditButton()
        }

        val providerList: ObservableList<String> = observableArrayList()

        SpotilistProviderType.values().forEach {
            if (SpotilistProviderType.isValid(it)) {
                providerList.add(it.name)
            }
        }

        val cmbInputToSetterMap = mapOf<ComboBox<String>, (String) -> Unit>(
            sourceProviderCombobox to { playlistMapping.sourceResource.provider.set(it) },
            targetProviderCombobox to { playlistMapping.targetResource.provider.set(it) }
        )

        for ((combobox, setter) in cmbInputToSetterMap) {
            combobox.items = providerList
            combobox.valueProperty().addListener { _, _, newValue ->
                setter.invoke(newValue)
            }
        }

        val txtInputToSetterMap = mapOf<TextField, (String) -> Unit>(
            nameTextField to { playlistMapping.name.set(it) },
            sourceIdTextField to { playlistMapping.sourceResource.id.set(it) },
            targetIdTextField to { playlistMapping.targetResource.id.set(it) }
        )

        for ((textField, setter) in txtInputToSetterMap) {
            textField.textProperty().addListener { _, _, newText ->
                setter.invoke(newText ?: "")
            }
        }

        nameTextField.textProperty().addListener { _, _, newValue ->
            playlistMappingTitledPane.text = newValue
        }
    }

    private fun updateEditButton() {
        useEditButton.isDisable =
            !playlistMapping.sourceResource.isValid.value || !playlistMapping.targetResource.isValid.value

        if (playlistMapping.isEnabled.value) {
            useEditButton.text = "Edit"
            playlistMappingGridPane.isDisable = true
        } else {
            useEditButton.text = "Use"
            playlistMappingGridPane.isDisable = false
        }
    }

    @FXML
    private fun delete() {
        SpotilistConfig.playlistMappings.remove(playlistMapping)
    }

    @FXML
    private fun toggleUseEdit() {
        playlistMapping.isEnabled.set(!playlistMapping.isEnabled.value)
        Persistence.save(PersistenceTypes.CONFIG)
    }

    private fun updateData() {
        dataLabel.text = "Updating data..."

        val sourceProvider = playlistMapping.sourceResource.provider.value
        val sourceId = playlistMapping.sourceResource.id.value
        val targetProvider = playlistMapping.targetResource.provider.value
        val targetId = playlistMapping.targetResource.id.value

        launch(Dispatchers.IO) {
            val sourcePlaylist = SpotilistProviderType.valueOf(sourceProvider).type.getPlaylist(sourceId)
            val targetPlaylist = SpotilistProviderType.valueOf(targetProvider).type.getPlaylist(targetId)

            if (sourcePlaylist == null || targetPlaylist == null) return@launch

            val sourceNames = sourcePlaylist.tracks?.map { track ->
                track.artists?.let { artists ->
                    Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                } + Util.getValidFilename(track.name ?: "")
            }?.toHashSet()
            val targetNames = targetPlaylist.tracks?.map { track ->
                track.artists?.let { artists ->
                    Util.getValidFilename(artists.map { it.name }.joinToString()) + " - "
                } + Util.getValidFilename(track.name ?: "")
            }?.toHashSet()

            val notFound = if (sourceNames != null && targetNames != null) {
                sourceNames.filter { !targetNames.contains(it) }
            } else {
                hashSetOf()
            }.joinToString("\n")

            val text = "Source playlist name: " + sourcePlaylist.name +
                    "\nTarget playlist name: " + targetPlaylist.name +
                    "\nSource playlist track count: " + sourcePlaylist.tracks?.size +
                    "\nTarget playlist track count: " + targetPlaylist.tracks?.size +
                    (if (notFound.isEmpty()) "\nAll found in target!" else "\nNot found in target:\n${notFound}")

            withContext(Dispatchers.JavaFx) {
                dataLabel.text = text
            }
        }
    }
}
