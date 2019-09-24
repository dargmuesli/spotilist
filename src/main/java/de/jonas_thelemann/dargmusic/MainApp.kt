package de.jonas_thelemann.dargmusic

import de.jonas_thelemann.dargmusic.persistence.Persistence
import de.jonas_thelemann.dargmusic.ui.DargmusicNotification
import de.jonas_thelemann.dargmusic.ui.DargmusicStage
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager

import java.io.IOException
import java.lang.Exception
import kotlin.system.exitProcess

class MainApp : Application() {

    override fun start(stage: Stage) {
        Persistence.loadSettings()

        DargmusicStage.makeDargmusicStage(stage)

        Companion.stage = stage

        stage.setOnCloseRequest {
            Persistence.saveSettings()
            exitProcess(0)
        }

        try {
            val dashboard = FXMLLoader.load<Parent>(javaClass.getResource("fxml/Dashboard.fxml"))
            val scene = Scene(dashboard)

            stage.scene = scene
            stage.show()
        } catch (e: IOException) {
            LogManager.getLogger().error("Loading the dashboard failed!", e)
        }
    }

    companion object {
        lateinit var stage: Stage

        internal const val APPLICATION_TITLE = "Dargmusic"

        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java, *args)
        }

        fun isStageInitialized() = this::stage.isInitialized
    }
}
