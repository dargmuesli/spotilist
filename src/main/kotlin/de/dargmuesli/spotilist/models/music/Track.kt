package de.dargmuesli.spotilist.models.music

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
}