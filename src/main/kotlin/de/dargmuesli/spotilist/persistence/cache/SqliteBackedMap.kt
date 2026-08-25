package de.dargmuesli.spotilist.persistence.cache

import java.sql.Connection

/**
 * A [MutableMap] backed by a table in a SQLite database, so individual put/remove/clear calls touch only the changed rows on disk instead of rewriting the whole map, unlike the JSON-blob persistence this replaces.
 */
class SqliteBackedMap<T>(
    private val connection: Connection,
    tableName: String,
    private val encode: (T) -> String,
    private val decode: (String) -> T
) : AbstractMutableMap<String, T>() {
    private val table = "\"$tableName\""

    init {
        connection.createStatement().use { statement ->
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS $table (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, T>>
        get() = connection.createStatement().use { statement ->
            statement.executeQuery("SELECT key, value FROM $table").use { resultSet ->
                val result = mutableSetOf<MutableMap.MutableEntry<String, T>>()

                while (resultSet.next()) {
                    val entryKey = resultSet.getString("key")
                    val entryValue = decode(resultSet.getString("value"))

                    result.add(object : MutableMap.MutableEntry<String, T> {
                        override val key: String = entryKey
                        override val value: T = entryValue

                        override fun setValue(newValue: T): T {
                            val previous = get(key)
                            put(key, newValue)
                            return previous ?: newValue
                        }
                    })
                }

                result
            }
        }

    override fun get(key: String): T? =
        connection.prepareStatement("SELECT value FROM $table WHERE key = ?").use { statement ->
            statement.setString(1, key)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) decode(resultSet.getString("value")) else null
            }
        }

    override fun containsKey(key: String): Boolean =
        connection.prepareStatement("SELECT 1 FROM $table WHERE key = ?").use { statement ->
            statement.setString(1, key)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    override fun put(key: String, value: T): T? {
        val previous = get(key)

        connection.prepareStatement(
            "INSERT INTO $table (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value"
        ).use { statement ->
            statement.setString(1, key)
            statement.setString(2, encode(value))
            statement.executeUpdate()
        }

        return previous
    }

    override fun remove(key: String): T? {
        val previous = get(key)

        if (previous != null) {
            connection.prepareStatement("DELETE FROM $table WHERE key = ?").use { statement ->
                statement.setString(1, key)
                statement.executeUpdate()
            }
        }

        return previous
    }

    override fun clear() {
        connection.createStatement().use { statement ->
            statement.executeUpdate("DELETE FROM $table")
        }
    }
}
