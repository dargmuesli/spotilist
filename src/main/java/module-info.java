open module de.dargmuesli.spotilist {
    requires com.google.api.client.json.gson;
    requires com.google.api.client;
    requires com.google.api.services.youtube;
    requires com.google.gson;
    requires google.api.client;
    requires java.desktop;
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core.jvm;
    requires kotlinx.coroutines.javafx;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;
    requires mp3agic;
    requires org.apache.logging.log4j;
    requires org.kordamp.ikonli.javafx;
    requires org.slf4j;
    requires se.michaelthelin.spotify;

    exports de.dargmuesli.spotilist.models;
    exports de.dargmuesli.spotilist.persistence.cache;
    exports de.dargmuesli.spotilist.persistence.config;
    exports de.dargmuesli.spotilist.persistence;
}
