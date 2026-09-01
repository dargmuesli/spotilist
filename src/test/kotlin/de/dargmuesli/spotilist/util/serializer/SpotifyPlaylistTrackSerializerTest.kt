package de.dargmuesli.spotilist.util.serializer

import de.dargmuesli.spotilist.persistence.format
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.michaelthelin.spotify.enums.AlbumType
import se.michaelthelin.spotify.enums.ModelObjectType
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.Track
import java.util.Date

/**
 * The Spotify cache stores playlist tracks as JSON written by Gson and read back by the library's own deserializer, so the two halves have to agree on names the compiler never checks: the snake_case policy, the lower-case enum wire format, and the `type` discriminator that decides whether an item is read as a track or an episode.
 */
class SpotifyPlaylistTrackSerializerTest {
    @Test
    fun `survives a round trip through the cache format`() {
        val original = PlaylistTrack.Builder()
            .setAddedAt(Date(1_700_000_000_000))
            .setIsLocal(false)
            .setItem(
                Track.Builder()
                    .setId("4cOdK2wGLETKBW3PvgPWqT")
                    .setName("Never Gonna Give You Up")
                    .setDurationMs(213_573)
                    .setType(ModelObjectType.TRACK)
                    .setArtists(ArtistSimplified.Builder().setName("Rick Astley").build())
                    .setAlbum(
                        AlbumSimplified.Builder()
                            .setName("Whenever You Need Somebody")
                            .setAlbumType(AlbumType.ALBUM)
                            .setArtists(ArtistSimplified.Builder().setName("Rick Astley").build())
                            .build()
                    )
                    .build()
            )
            .build()

        val restored = format.decodeFromString(
            SpotifyPlaylistTrackSerializer.Serializer,
            format.encodeToString(SpotifyPlaylistTrackSerializer.Serializer, original)
        )

        val track = restored.item as Track
        assertEquals("4cOdK2wGLETKBW3PvgPWqT", track.id)
        assertEquals("Never Gonna Give You Up", track.name)
        assertEquals(213_573, track.durationMs)
        assertEquals("Rick Astley", track.artists.single().name)
        assertEquals("Whenever You Need Somebody", track.album.name)
        assertEquals(AlbumType.ALBUM, track.album.albumType)
        assertEquals(original.addedAt, restored.addedAt)
    }
}
