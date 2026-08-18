package com.bitchat.storage.repositories

import com.bitchat.core.models.Identity
import com.bitchat.storage.database.IdentityDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing local user identity.
 * The identity consists of a display name and cryptographic key pair.
 * The default identity is used for all outgoing communications.
 * All identity material is stored locally and never transmitted in full.
 */
class IdentityRepository(private val identityDao: IdentityDao) {

    /** Observe the current default identity. Emits null if no identity exists. */
    fun observeDefaultIdentity(): Flow<Identity?> =
        identityDao.observeDefaultIdentity()

    /** Get the current default identity (non-reactive). */
    suspend fun getDefaultIdentity(): Identity? =
        identityDao.getDefaultIdentity()

    /** Create or update the default identity. */
    suspend fun saveIdentity(identity: Identity) =
        identityDao.insertIdentity(identity)

    /** Update the display name of the default identity. */
    suspend fun updateDisplayName(name: String) {
        val identity = identityDao.getDefaultIdentity() ?: return
        identityDao.updateIdentity(identity.copy(displayName = name))
    }

    /** Remove all identity data (used during panic wipe). */
    suspend fun clearAll() = identityDao.deleteAll()
}
