package de.dargmuesli.spotilist.models.music

import de.dargmuesli.spotilist.util.Util
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val album: Album? = null,
    val artists: List<Artist>? = null,
    val durationMs: Long? = null,
    val id: String? = null,
    val name: String? = null
) {
    override fun toString(): String {
        return name ?: super.toString()
    }

    /** A filesystem-safe key used to match the same track across providers, since providers rarely share a common ID. */
    fun matchKey(): String {
        val artistNames = artists.orEmpty().joinToString { it.name.orEmpty() }
        return Util.getValidFilename(artistNames) + " - " + Util.getValidFilename(name.orEmpty())
    }
}