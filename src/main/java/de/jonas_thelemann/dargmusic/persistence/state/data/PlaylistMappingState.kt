package de.jonas_thelemann.dargmusic.persistence.state.data

import de.jonas_thelemann.dargmusic.models.PlaylistMapping

object PlaylistMappingState {
    var playlistMappings: Map<String, PlaylistMapping<*, *>> = HashMap()
}