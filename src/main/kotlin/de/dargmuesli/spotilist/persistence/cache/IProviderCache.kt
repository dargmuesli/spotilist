package de.dargmuesli.spotilist.persistence.cache

interface IProviderCache<PT, TT> : IClearable {
    var playlistData: MutableMap<String, PT>
    var playlistItemData: MutableMap<String, TT>
    var playlistItemMap: MutableMap<String, MutableList<String>>

    override fun clear() {
        playlistData.clear()
        playlistItemData.clear()
        playlistItemMap.clear()
    }
}
