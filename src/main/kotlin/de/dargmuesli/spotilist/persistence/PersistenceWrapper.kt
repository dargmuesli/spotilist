package de.dargmuesli.spotilist.persistence

import kotlinx.serialization.Serializable

@Serializable
object PersistenceWrapper {
    var config = SpotilistConfig

    operator fun get(persistenceType: PersistenceTypes): AbstractSerializable {
        return when (persistenceType) {
            PersistenceTypes.CONFIG -> config
        }
    }

    operator fun set(persistenceType: PersistenceTypes, value: AbstractSerializable) {
        when (persistenceType) {
            PersistenceTypes.CONFIG -> config = value as SpotilistConfig
        }
    }
}
