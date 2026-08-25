package de.dargmuesli.spotilist.persistence

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SpotilistConfigTest {
    @Test
    fun `decoding a config saved before the export section existed still works`() {
        // Encoding via the AbstractSerializable serializer matches what Persistence.save() actually writes, class discriminator included.
        val currentJson = format.encodeToString(AbstractSerializable.serializer(), SpotilistConfig)
        val withoutExport = JsonObject(format.parseToJsonElement(currentJson).jsonObject.filterKeys { it != "export" })

        val decoded = format.decodeFromString(AbstractSerializable.serializer(), withoutExport.toString())

        assertNull((decoded as SpotilistConfig).export.directory.value)
    }
}
