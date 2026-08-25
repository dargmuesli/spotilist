package de.dargmuesli.spotilist.providers

import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named

/**
 * An enumeration of all possible module types.
 */
enum class SpotilistProviderType {
    NONE,
    FILESYSTEM,
    SPOTIFY,
    YOUTUBE;

    /**
     * The provider instance backing this type, resolved fresh from the DI container on every access.
     * Enum constants are JVM singletons, so caching this would permanently pin it to whichever provider was registered first, making it impossible for tests to swap in a fake for a later test.
     */
    val type: ISpotilistProvider<*, *>
        get() = GlobalContext.get().get(qualifier = named(name))

    companion object {
        fun isValid(provider: SpotilistProviderType): Boolean {
            return when (provider) {
                FILESYSTEM -> true
                NONE -> false
                SPOTIFY, YOUTUBE -> (provider.type as ISpotilistProviderAuthorizable<*, *>).isAuthorized()
            }
        }

        fun keyOf(type: ISpotilistProvider<*, *>): SpotilistProviderType? {
            return values().associateBy { it.type }[type]
        }
    }
}
