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
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import java.util.*

class SpotifyPlaylistTrackSerializer {
    object Serializer : KSerializer<PlaylistTrack> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PlaylistTrack", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: PlaylistTrack) {
            encoder.encodeString(
                GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(Date::class.java, GsonUTCDateAdapter())
                    .create().toJson(value)
            )
        }

        override fun deserialize(decoder: Decoder): PlaylistTrack {
            return ModelObjects.fromJson(decoder.decodeString(), PlaylistTrack::class.java)
        }
    }
}