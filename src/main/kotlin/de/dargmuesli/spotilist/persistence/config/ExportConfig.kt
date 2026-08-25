package de.dargmuesli.spotilist.persistence.config

import de.dargmuesli.spotilist.persistence.Persistence
import de.dargmuesli.spotilist.persistence.PersistenceTypes
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ExportConfig.Serializer::class)
object ExportConfig {
    val directory = SimpleStringProperty().also {
        it.addListener { _ ->
            Persistence.save(PersistenceTypes.CONFIG)
        }
    }
    val trackPathPrefix = SimpleStringProperty().also {
        it.addListener { _ ->
            Persistence.save(PersistenceTypes.CONFIG)
        }
    }

    object Serializer : KSerializer<ExportConfig> {
        override val descriptor: SerialDescriptor = ExportConfigSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ExportConfig) {
            encoder.encodeSerializableValue(
                ExportConfigSurrogate.serializer(),
                ExportConfigSurrogate(directory.value, trackPathPrefix.value)
            )
        }

        override fun deserialize(decoder: Decoder): ExportConfig {
            val exportConfig = decoder.decodeSerializableValue(ExportConfigSurrogate.serializer())
            directory.set(exportConfig.directory)
            trackPathPrefix.set(exportConfig.trackPathPrefix)
            return ExportConfig
        }
    }

    @Serializable
    @SerialName("ExportConfig")
    private data class ExportConfigSurrogate(
        val directory: String?,
        val trackPathPrefix: String?
    )
}
