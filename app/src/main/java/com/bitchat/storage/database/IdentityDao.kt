package com.bitchat.storage.database

import androidx.room.*
import com.bitchat.core.models.Identity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [Identity] entities in the local Room database.
 *
 * An Identity represents the user's cryptographic key-pair and associated
 * display name. At least one identity must be marked as the default
 * (`isDefault = 1`) for the application to function; all outgoing messages
 * are signed with the default identity's private key.
 *
 * ## Threading model
 * - **Suspend functions** perform single-shot reads or writes on Room's
 *   internal write dispatcher.
 * - **Reactive queries** (returning [Flow]) allow the UI to observe identity
 *   changes in real time without manual polling.
 *
 * ## Security considerations
 * Identity records contain sensitive key material. The private key should
 * be encrypted at the Android Keystore level **before** being persisted by
 * Room. This DAO does not handle encryption — that responsibility lies in
 * the repository / crypto layer above it.
 */
@Dao
interface IdentityDao {

    /**
     * Returns the single identity marked as default, or `null` if none has
     * been created yet (e.g., first launch before onboarding completes).
     *
     * This is a one-shot read — prefer [observeDefaultIdentity] when you
     * need live updates.
     *
     * @return The default [Identity] or `null`.
     */
    @Query("SELECT * FROM identities WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultIdentity(): Identity?

    /**
     * Returns a reactive stream of the default identity.
     * Emits `null` when no default identity exists yet.
     *
     * Useful for observing profile-display changes (e.g., user renames
     * themselves) without re-querying manually.
     *
     * @return [Flow] emitting the current default [Identity] or `null`.
     */
    @Query("SELECT * FROM identities WHERE isDefault = 1 LIMIT 1")
    fun observeDefaultIdentity(): Flow<Identity?>

    /**
     * Returns all stored identities.
     *
     * Primarily used in an identity-switcher UI where the user can manage
     * or rotate between multiple key-pairs.
     *
     * @return [Flow] emitting the full list of identities on every change.
     */
    @Query("SELECT * FROM identities")
    fun getAllIdentities(): Flow<List<Identity>>

    /**
     * Inserts an identity or replaces the existing record when one with the
     * same primary key already exists.
     *
     * @param identity The identity entity to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: Identity)

    /**
     * Applies a partial update to an existing identity.
     *
     * @param identity The identity with updated field values.
     */
    @Update
    suspend fun updateIdentity(identity: Identity)

    /**
     * Deletes a specific identity from the database.
     *
     * **Warning:** Deleting the default identity without first promoting
     * another identity will leave the application in a non-functional state.
     * The repository layer must enforce this invariant.
     *
     * @param identity The identity entity to remove.
     */
    @Delete
    suspend fun deleteIdentity(identity: Identity)

    /**
     * Irreversibly removes all identities from the database.
     *
     * Typically called during account reset. Because this deletes all
     * key-pair material, the user must go through onboarding again to
     * generate new keys.
     */
    @Query("DELETE FROM identities")
    suspend fun deleteAll()
}
