package de.dargmuesli.spotilist.providers

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension

private class FakeProvider : ISpotilistProvider<Unit, Unit> {
    override fun isPlaylistIdValid(playlistId: String) = true
}

class SpotilistProviderTypeTest : KoinTest {
    private val fakeProvider = FakeProvider()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                single<ISpotilistProvider<*, *>>(named(SpotilistProviderType.FILESYSTEM.name)) { fakeProvider }
            }
        )
    }

    @Test
    fun `a provider type resolves whatever is currently bound in the DI container, so tests can substitute a fake`() {
        assertSame(fakeProvider, SpotilistProviderType.FILESYSTEM.type)
    }
}
