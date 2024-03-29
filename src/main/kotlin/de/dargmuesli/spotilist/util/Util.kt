package de.dargmuesli.spotilist.util

import de.dargmuesli.spotilist.models.PlaylistMapping
import java.util.regex.Matcher
import java.util.regex.Pattern

object Util {
    fun getUnusedPlaylistMappingName(playlistMappingList: List<PlaylistMapping>): String {
        var maxIndex = 0
        val pattern = Pattern.compile("^Playlist Mapping ([0-9]+)$")
        var matcher: Matcher

        for (playlistMapping in playlistMappingList) {
            matcher = pattern.matcher(playlistMapping.name.value)

            while (matcher.find()) {
                val currentMaxIndex = Integer.valueOf(matcher.group(1))

                if (currentMaxIndex > maxIndex) {
                    maxIndex = currentMaxIndex
                }
            }
        }

        return "Playlist Mapping " + (maxIndex + 1)
    }

    fun getValidFilename(string: String): String {
        return string.replace(Regex("[<>:\"/\\\\|?*]"), "_")
    }
}
