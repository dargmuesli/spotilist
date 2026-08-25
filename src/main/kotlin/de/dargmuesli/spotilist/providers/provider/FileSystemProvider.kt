package de.dargmuesli.spotilist.providers.provider

import de.dargmuesli.spotilist.models.music.Artist
import de.dargmuesli.spotilist.models.music.Playlist
import de.dargmuesli.spotilist.models.music.Track
import de.dargmuesli.spotilist.providers.ISpotilistProvider
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Paths

class FileSystemProvider : ISpotilistProvider<File, File> {
    private val LOGGER = LogManager.getLogger()

    override fun getProviderPlaylist(playlistId: String): File? {
        val file = File(playlistId)
        return if (file.isDirectory) file else null
    }

    override fun getProviderPlaylistItems(playlistId: String): List<File>? {
        return getProviderPlaylist(playlistId)?.listFiles { file -> file.isFile }?.toList()
    }

    override fun getPlaylist(playlistId: String): Playlist? {
        val playlist = getProviderPlaylist(playlistId)

        return if (playlist == null) {
            null
        } else {
            Playlist(name = playlist.name, tracks = getPlaylistItems(playlistId))
        }
    }

    override fun getPlaylistItems(playlistId: String): List<Track>? {
        return getProviderPlaylistItems(playlistId)?.map { file ->
            val nameParts = file.nameWithoutExtension.split(" - ")

            if (nameParts.size < 2) {
                LOGGER.error("Name parts are not at least two for \"${file.nameWithoutExtension}\"!")
            }

            val artistNames = nameParts[0].split(", ")
            Track(
                name = nameParts.subList(1, nameParts.size).joinToString(" - "),
                artists = artistNames.map { Artist(name = it) })
        }

//        path.toFile().listFiles { file -> file.isFile }!!.forEach { file ->
//            val mp3File = Mp3File(file)
//            val id3v2Tag = if (mp3File.hasId3v2Tag()) {
//                mp3File.id3v2Tag
//            } else {
//                ID3v24Tag()
//            }
//
//            val trackAlbum = if (id3v2Tag.album != null) {
//                Album(name = id3v2Tag.album)
//            } else {
//                Album()
//            }
//
//            val trackArtists = listOf(Artist(name = id3v2Tag.artist))
//            val trackDurationMs = mp3File.lengthInMilliseconds
//            val trackName = id3v2Tag.title
//
//            playlistTracks.add(Track(trackAlbum, trackArtists, trackDurationMs, trackName))
//        }
    }

    override fun isPlaylistIdValid(playlistId: String): Boolean {
        return Paths.get(playlistId).toFile().isDirectory // Returns false for non-existent directories too.
    }
}
