package de.dargmuesli.spotilist.models.music

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TrackTest {
    @Test
    fun `matchKey combines artist names and track name`() {
        val track = Track(artists = listOf(Artist(name = "Artist One"), Artist(name = "Artist Two")), name = "Song Title")

        assertEquals("Artist One, Artist Two - Song Title", track.matchKey())
    }

    @Test
    fun `matchKey sanitizes characters that are invalid in filenames`() {
        val track = Track(artists = listOf(Artist(name = "AC/DC")), name = "T.N.T: Live")

        assertEquals("AC_DC - T.N.T_ Live", track.matchKey())
    }

    @Test
    fun `matchKey handles missing artists and name`() {
        val track = Track()

        assertEquals(" - ", track.matchKey())
    }

    @Test
    fun `matchKey is stable regardless of album or duration`() {
        val a = Track(album = Album(name = "Album A"), durationMs = 1000, artists = listOf(Artist(name = "X")), name = "Y")
        val b = Track(album = Album(name = "Album B"), durationMs = 2000, artists = listOf(Artist(name = "X")), name = "Y")

        assertEquals(a.matchKey(), b.matchKey())
    }
}
