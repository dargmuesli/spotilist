package de.dargmuesli.spotilist.persistence.cache

import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer

/**
 * A [MutableMap] backed by a named MapDB hash map, so individual put/remove/clear calls touch only the changed entries on disk instead of rewriting the whole map, unlike the JSON-blob persistence this replaces.
 */
class MapDbBackedMap<T>(
    private val db: DB,
    name: String,
    private val encode: (T) -> String,
    private val decode: (String) -> T
) : AbstractMutableMap<String, T>() {
    private val delegate: HTreeMap<String, String> =
        db.hashMap(name, Serializer.STRING, Serializer.STRING).createOrOpen()

    override val entries: MutableSet<MutableMap.MutableEntry<String, T>>
        get() = delegate.entries
            .asSequence()
            .map { entry ->
                object : MutableMap.MutableEntry<String, T> {
                    override val key: String get() = entry.key!!
                    override val value: T get() = decode(entry.value!!)
                    override fun setValue(newValue: T): T {
                        val previous = get(key)
                        put(key, newValue)
                        return previous ?: newValue
                    }
                }
            }
            .toMutableSet()

    override fun get(key: String): T? = delegate[key]?.let(decode)

    override fun containsKey(key: String): Boolean = delegate.containsKey(key)

    override fun put(key: String, value: T): T? {
        val previous = get(key)
        delegate[key] = encode(value)
        db.commit()
        return previous
    }

    override fun remove(key: String): T? {
        val previous = get(key)
        if (previous != null) {
            delegate.remove(key)
            db.commit()
        }
        return previous
    }

    override fun clear() {
        delegate.clear()
        db.commit()
    }
}
