package com.bitchat.storage.database

import androidx.room.*
import com.bitchat.core.models.Session
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [Session] entities in the local Room database.
 *
 * Sessions represent active encrypted communication channels between the
 * local user and a remote peer. Each session is bound to a
 * [Session.remotePeerId] and carries an optional expiry timestamp as well
 * as an ephemeral flag that controls cleanup behavior.
 *
 * ## Threading model
 * - **Reactive queries** (returning [Flow]) provide live views of the
 *   active-session list for the connection-management UI.
 * - **Suspend functions** handle session lifecycle operations on Room's
 *   internal write dispatcher.
 *
 * ## Security considerations
 * Sessions may contain negotiated cryptographic state. Ephemeral sessions
 * (marked `isEphemeral = 1`) are designed to leave no trace once torn down;
 * [deleteEphemeralSessions] should be called promptly when the user ends
 * a disposable chat to minimize data at rest. Non-ephemeral sessions are
 * retained across app restarts for continuity and are only cleaned up when
 * they expire or the user explicitly disconnects.
 */
@Dao
interface SessionDao {

    /**
     * Returns all sessions currently marked as active.
     *
     * Used to populate the connections/sessions list in the UI. The [Flow]
     * re-emits whenever sessions are created, expired, or deleted.
     *
     * @return [Flow] emitting the list of active [Session] records.
     */
    @Query("SELECT * FROM sessions WHERE isActive = 1")
    fun getActiveSessions(): Flow<List<Session>>

    /**
     * Fetches a single session by its unique identifier.
     *
     * @param sessionId The session's primary key.
     * @return The matching [Session] or `null`.
     */
    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): Session?

    /**
     * Returns the currently active session with a specific remote peer, or
     * `null` if no active session exists with that peer.
     *
     * This is the fast-path check before initiating a new session — if a
     * session already exists and is active the caller can reuse it instead
     * of performing a new key exchange.
     *
     * @param peerId The remote peer's identifier.
     * @return The active [Session] with that peer, or `null`.
     */
    @Query("SELECT * FROM sessions WHERE remotePeerId = :peerId AND isActive = 1")
    suspend fun getActiveSessionWithPeer(peerId: String): Session?

    /**
     * Inserts a session or replaces the existing record when one with the
     * same primary key already exists.
     *
     * @param session The session entity to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    /**
     * Applies a partial update to an existing session.
     *
     * @param session The session with updated field values.
     */
    @Update
    suspend fun updateSession(session: Session)

    /**
     * Deletes a specific session from the database.
     *
     * @param session The session entity to remove.
     */
    @Delete
    suspend fun deleteSession(session: Session)

    /**
     * Deactivates all sessions whose [Session.expiresAt] is in the past.
     *
     * Expired sessions are marked inactive rather than immediately deleted
     * so that reconnect logic can inspect the previous session state. Call
     * this on a periodic timer or each time the app resumes.
     *
     * @param now The current epoch millis used as the comparison threshold.
     */
    @Query("UPDATE sessions SET isActive = 0 WHERE expiresAt < :now")
    suspend fun deactivateExpiredSessions(now: Long)

    /**
     * Permanently removes all ephemeral sessions from the database.
     *
     * **Security note:** Ephemeral sessions are intended to leave no
     * persistent trace. This method should be invoked immediately when a
     * disposable chat session is closed to ensure no key material or
     * metadata lingers at rest.
     */
    @Query("DELETE FROM sessions WHERE isEphemeral = 1")
    suspend fun deleteEphemeralSessions()

    /**
     * Irreversibly removes every session from the database.
     *
     * Called during account reset or sign-out to ensure a clean slate.
     */
    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
