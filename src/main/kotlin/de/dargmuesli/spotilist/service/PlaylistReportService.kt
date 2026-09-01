package de.dargmuesli.spotilist.service

import com.google.gson.Gson
import com.google.gson.JsonParser
import de.dargmuesli.spotilist.models.PlaylistMappingResource
import de.dargmuesli.spotilist.models.music.Playlist
import de.dargmuesli.spotilist.models.music.Track
import de.dargmuesli.spotilist.persistence.SpotilistConfig
import de.dargmuesli.spotilist.providers.SpotilistProviderType
import de.dargmuesli.spotilist.providers.util.SpotifyUtil.spotifyApi
import de.dargmuesli.spotilist.util.Util
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File

/** Compares playlist mappings across providers and reports/exports the differences found. */
object PlaylistReportService {
    private val LOGGER: Logger = LogManager.getLogger()

    private const val LIKED_SONGS_NAME = "Liked Songs"
    private const val LIKED_SONGS_PLAYLIST_ID = "4P9gkeY7Pd8EXn4IijePlE"

    fun generateReport() {
        syncLikedSongsToTargets()
        reportCrossPlaylistOverlaps()
        reportLikedSongsNotInAnyPlaylist()
        exportM3uFiles()
        LOGGER.info("Done!")
    }

    private fun resolvePlaylist(resource: PlaylistMappingResource): Playlist? =
        SpotilistProviderType.valueOf(resource.provider.value).type.getPlaylist(resource.id.value)

    private fun syncLikedSongsToTargets() {
        LOGGER.info("Searching Spotify playlist tracks in local filesystem:")

        SpotilistConfig.playlistMappings.forEach { playlistMapping ->
            val sourcePlaylist = resolvePlaylist(playlistMapping.sourceResource) ?: return@forEach

            if (sourcePlaylist.name != LIKED_SONGS_NAME) return@forEach

            val targetPlaylist = resolvePlaylist(playlistMapping.targetResource) ?: return@forEach

            val sourceTracks = sourcePlaylist.tracks
            val targetKeys = targetPlaylist.tracks?.map { it.matchKey() }?.toHashSet() ?: return@forEach

            val notFound = sourceTracks?.filter { !targetKeys.contains(it.matchKey()) }

            if (notFound == null || notFound.isEmpty()) {
                LOGGER.info("All found in \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks?.size})!")
            } else {
                LOGGER.info(
                    "In \"${sourcePlaylist.name}\" (${sourceTracks.size}), but not in \"${targetPlaylist.name}\" (${targetPlaylist.tracks?.size}):\n${
                        notFound.joinToString("\n") { "${it.name} (${it.id})" }
                    }"
                )

                createMissingTracksPlaylist(notFound)
            }
        }
    }

    private fun createMissingTracksPlaylist(notFound: List<Track>) {
        // createPlaylist always targets the current user now, so the profile lookup that supplied the owner id is one request we no longer have to make.
        val playlist = spotifyApi.createPlaylist("TODO (Date)").public_(false).build().execute()

        notFound.map { "spotify:track:" + it.id }.chunked(100).forEach { chunk ->
            spotifyApi.addItemsToPlaylist(playlist.id, JsonParser.parseString(Gson().toJson(chunk)).asJsonArray)
                .build().execute()
        }
    }

    private fun reportCrossPlaylistOverlaps() {
        LOGGER.info("Searching Spotify playlist tracks in other playlists:")

        SpotilistConfig.playlistMappings.forEachIndexed { index, playlistMapping ->
            val sourcePlaylist = resolvePlaylist(playlistMapping.sourceResource) ?: return@forEachIndexed

            if (sourcePlaylist.name == LIKED_SONGS_NAME) return@forEachIndexed

            LOGGER.info("Comparing \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks?.size}):")

            SpotilistConfig.playlistMappings.forEachIndexed { indexInner, playlistMappingInner ->
                if (index <= indexInner ||
                    playlistMapping.sourceResource.id.value == playlistMappingInner.sourceResource.id.value
                ) {
                    return@forEachIndexed
                }

                val targetPlaylist = resolvePlaylist(playlistMappingInner.sourceResource) ?: return@forEachIndexed

                if (targetPlaylist.name == LIKED_SONGS_NAME) return@forEachIndexed

                val sourceKeys = sourcePlaylist.tracks?.map { it.matchKey() }?.toHashSet() ?: return@forEachIndexed
                val targetKeys = targetPlaylist.tracks?.map { it.matchKey() }?.toHashSet() ?: return@forEachIndexed

                val found = sourceKeys.filter { targetKeys.contains(it) }.joinToString("\n")

                if (found.isEmpty()) {
                    LOGGER.debug("None found in \"${targetPlaylist.name}\" (${targetPlaylist.tracks?.size})!")
                } else {
                    LOGGER.info(
                        "In \"${sourcePlaylist.name}\" (${sourcePlaylist.tracks?.size}), but also in \"${targetPlaylist.name}\" (${targetPlaylist.tracks?.size}):\n$found"
                    )
                }
            }
        }
    }

    private fun reportLikedSongsNotInAnyPlaylist() {
        LOGGER.info("Searching liked tracks that are not in any genre playlist:")

        val likedSongsPlaylist = SpotilistProviderType.SPOTIFY.type.getPlaylist(LIKED_SONGS_PLAYLIST_ID) ?: return

        val foundKeys = mutableListOf<String>()

        for (playlistMapping in SpotilistConfig.playlistMappings) {
            // TODO: allow to mark playlists as excluded from comparison
            if (LIKED_SONGS_PLAYLIST_ID == playlistMapping.sourceResource.id.value) continue

            val targetPlaylist = resolvePlaylist(playlistMapping.sourceResource) ?: continue

            if (targetPlaylist.name == LIKED_SONGS_NAME) continue

            val targetKeys = targetPlaylist.tracks?.map { it.matchKey() }?.toHashSet() ?: continue
            foundKeys.addAll(targetKeys)
        }

        val notFoundTracks = mutableListOf<Track>()

        likedSongsPlaylist.tracks?.forEach { track ->
            if (!foundKeys.contains(track.matchKey())) {
                notFoundTracks.add(track)
            }
        }

        if (notFoundTracks.isEmpty()) {
            LOGGER.info("All \"Liked Songs\" are in a playlist.")
        } else {
            LOGGER.info(
                "In \"Liked Songs\", but not in any playlist:\n${notFoundTracks.joinToString("\n") { "${it.name} (${it.id})" }}"
            )
        }
    }

    private fun exportM3uFiles() {
        val exportDirectory = SpotilistConfig.export.directory.value

        if (exportDirectory.isNullOrBlank()) {
            LOGGER.warn("Skipping m3u export: no export directory configured in settings.")
            return
        }

        val trackPathPrefix = SpotilistConfig.export.trackPathPrefix.value.orEmpty()

        LOGGER.info("Generating m3u files:")

        for (playlistMapping in SpotilistConfig.playlistMappings) {
            if (LIKED_SONGS_PLAYLIST_ID == playlistMapping.sourceResource.id.value) continue

            val targetPlaylist = resolvePlaylist(playlistMapping.sourceResource) ?: continue

            if (targetPlaylist.name == LIKED_SONGS_NAME) continue

            val targetLines = targetPlaylist.tracks
                ?.map { track -> trackPathPrefix + track.matchKey() + ".mp3" }
                ?.reduce { acc, s -> acc + "\n" + s } ?: continue

            targetPlaylist.name?.let {
                File(exportDirectory, Util.getValidFilename(it) + ".m3u").writeText(targetLines)
            }
        }
    }
}
