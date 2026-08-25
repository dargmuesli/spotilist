package de.dargmuesli.spotilist.persistence

import de.dargmuesli.spotilist.models.PlaylistMapping
import de.dargmuesli.spotilist.persistence.config.ExportConfig
import de.dargmuesli.spotilist.persistence.config.SpotifyConfig
import de.dargmuesli.spotilist.persistence.config.YouTubeConfig
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SpotilistConfig.Serializer::class)
object SpotilistConfig : AbstractSerializable() {
    var spotify: SpotifyConfig = SpotifyConfig
    var youTube: YouTubeConfig = YouTubeConfig
    var export: ExportConfig = ExportConfig

    var playlistMappings: ObservableList<PlaylistMapping> = FXCollections.observableArrayList()

    object Serializer : KSerializer<SpotilistConfig> {
        override val descriptor: SerialDescriptor = SpotilistConfigSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SpotilistConfig) {
            encoder.encodeSerializableValue(
                SpotilistConfigSurrogate.serializer(),
                SpotilistConfigSurrogate(
                    spotify,
                    youTube,
                    export,
                    playlistMappings
                )
            )
        }

        override fun deserialize(decoder: Decoder): SpotilistConfig {
            val spotilistConfig = decoder.decodeSerializableValue(SpotilistConfigSurrogate.serializer())
            spotify = spotilistConfig.spotify
            youTube = spotilistConfig.youTube
            export = spotilistConfig.export
            playlistMappings.addAll(spotilistConfig.playlistMappings)
            return SpotilistConfig
        }
    }

    @Serializable
    @SerialName("SpotilistConfig")
    private data class SpotilistConfigSurrogate(
        val spotify: SpotifyConfig,
        val youTube: YouTubeConfig,
        val export: ExportConfig = ExportConfig,
        val playlistMappings: List<PlaylistMapping>
    )
}
