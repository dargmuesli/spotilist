package de.dargmuesli.spotilist.util.serializer

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import se.michaelthelin.spotify.model_objects.ModelObjects
import se.michaelthelin.spotify.model_objects.specification.Playlist
import java.util.*

class SpotifyPlaylistSerializer {
    object Serializer : KSerializer<Playlist> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Playlist", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Playlist) {
            encoder.encodeString(
                GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(Date::class.java, GsonUTCDateAdapter())
                    .create().toJson(value)
            )
        }

        override fun deserialize(decoder: Decoder): Playlist {
            return ModelObjects.fromJson(decoder.decodeString(), Playlist::class.java)
        }
    }
}