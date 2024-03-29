package de.dargmuesli.spotilist.ui

import de.dargmuesli.spotilist.MainApp
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Control
import javafx.scene.image.Image
import javafx.stage.Modality
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.io.IOException

class SpotilistStage(
    fxmlPath: String,
    modality: Modality,
    title: String,
    isAlwaysOnTop: Boolean = false,
    minHeight: Double = Control.USE_COMPUTED_SIZE,
    minWidth: Double = Control.USE_COMPUTED_SIZE
) : Stage() {
    init {
        try {
            val dashboard = FXMLLoader.load<Parent>(MainApp::class.java.getResource(fxmlPath), MainApp.RESOURCES)
            val scene = Scene(dashboard)

            if (minHeight != Control.USE_COMPUTED_SIZE) {
                this.minHeight = minHeight
            }

            if (minWidth != Control.USE_COMPUTED_SIZE) {
                this.minWidth = minWidth
            }

            this.scene = scene
            this.title = MainApp.APPLICATION_TITLE + " - " + title
            this.icons.add(Image(javaClass.getResourceAsStream("/de/dargmuesli/spotilist/icons/icon.png")))
            this.isAlwaysOnTop = isAlwaysOnTop
            this.initModality(modality)

            if (MainApp.isStageInitialized()) {
                this.initOwner(MainApp.stage)
            } else {
                this.initOwner(Stage())
            }
        } catch (e: IOException) {
            LOGGER.error("Construction of stage failed!", e)
        }
    }

    companion object {
        val LOGGER: Logger = LogManager.getLogger()
        internal fun makeSpotilistStage(stage: Stage) {
            stage.title = MainApp.APPLICATION_TITLE
            stage.icons.add(Image(MainApp().javaClass.getResourceAsStream("/de/dargmuesli/spotilist/icons/icon.png")))
        }
    }
}
