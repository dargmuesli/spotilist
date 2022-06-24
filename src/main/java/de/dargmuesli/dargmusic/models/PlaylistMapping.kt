package de.dargmuesli.dargmusic.models

import de.dargmuesli.dargmusic.models.enums.DargmusicProvider
import de.dargmuesli.dargmusic.persistence.state.DargmusicState
import de.dargmuesli.dargmusic.util.Util

data class PlaylistMapping(
    var name: String = Util.getUnusedPlaylistMappingName(DargmusicState.data.playlistMappings),
    var sourceResource: PlaylistMappingResource = PlaylistMappingResource(),
    var targetResource: PlaylistMappingResource = PlaylistMappingResource(),
    var blacklistSource: Array<String> = arrayOf(),
    var blacklistTarget: Array<String> = arrayOf()
) {

    fun validate(): Boolean {
        return name != ""
                && sourceResource.provider != DargmusicProvider.NONE
                && DargmusicProvider.isPlaylistMappingValid(sourceResource)
                && targetResource.provider != DargmusicProvider.NONE
                && DargmusicProvider.isPlaylistMappingValid(targetResource)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistMapping

        if (name != other.name) return false
        if (sourceResource != other.sourceResource) return false
        if (targetResource != other.targetResource) return false
        if (!blacklistSource.contentEquals(other.blacklistSource)) return false
        if (!blacklistTarget.contentEquals(other.blacklistTarget)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sourceResource.hashCode()
        result = 31 * result + targetResource.hashCode()
        result = 31 * result + blacklistSource.contentHashCode()
        result = 31 * result + blacklistTarget.contentHashCode()
        return result
    }
}
