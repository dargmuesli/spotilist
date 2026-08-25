package de.dargmuesli.spotilist.di

import de.dargmuesli.spotilist.providers.ISpotilistProvider
import de.dargmuesli.spotilist.providers.SpotilistProviderType
import de.dargmuesli.spotilist.providers.provider.FileSystemProvider
import de.dargmuesli.spotilist.providers.provider.NoneProvider
import de.dargmuesli.spotilist.providers.provider.SpotifyProvider
import de.dargmuesli.spotilist.providers.provider.YouTubeProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Binds each ISpotilistProvider implementation under a qualifier matching its SpotilistProviderType name, so tests can override a single provider with a fake without touching the others. */
val providerModule = module {
    single<ISpotilistProvider<*, *>>(named(SpotilistProviderType.NONE.name)) { NoneProvider() }
    single<ISpotilistProvider<*, *>>(named(SpotilistProviderType.FILESYSTEM.name)) { FileSystemProvider() }
    single<ISpotilistProvider<*, *>>(named(SpotilistProviderType.SPOTIFY.name)) { SpotifyProvider() }
    single<ISpotilistProvider<*, *>>(named(SpotilistProviderType.YOUTUBE.name)) { YouTubeProvider() }
}
