package de.dargmuesli.spotilist

import de.dargmuesli.spotilist.persistence.Persistence
import de.dargmuesli.spotilist.persistence.PersistenceTypes
import de.dargmuesli.spotilist.ui.SpotilistStage
import de.dargmuesli.spotilist.ui.controllers.DashboardController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.util.*

class MainApp : Application() {
    override fun start(stage: Stage) {
        SpotilistStage.makeSpotilistStage(stage)
        Companion.stage = stage

        stage.setOnCloseRequest {
            dashboardController.dispose()
            Persistence.save(PersistenceTypes.CACHE)
        }

        try {
            val dashboardLoader = FXMLLoader(MainApp::class.java.getResource("fxml/dashboard.fxml"), RESOURCES)
            val dashboard = dashboardLoader.load<Parent>()
            dashboardController = dashboardLoader.getController()

            val scene = Scene(dashboard)

            stage.scene = scene
            stage.show()
        } catch (e: IOException) {
            LOGGER.error("Loading the dashboard failed!", e)
        }

        Persistence.load()
    }

    companion object {
        lateinit var stage: Stage
        lateinit var dashboardController: DashboardController
        val LOGGER: Logger = LogManager.getLogger()
        val RESOURCES: ResourceBundle = ResourceBundle.getBundle("i18n", Locale.getDefault())

        internal const val APPLICATION_TITLE = "Spotilist"

        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java, *args)
        }

        fun isStageInitialized() = this::stage.isInitialized
    }
}
