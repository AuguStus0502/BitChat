package com.bitchat.storage.repositories

import com.bitchat.core.models.Session
import com.bitchat.storage.database.SessionDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing encrypted communication sessions.
 * Sessions represent established secure channels with authenticated peers.
 * Supports both persistent and ephemeral (auto-deleted) sessions.
 */
class SessionRepository(private val sessionDao: SessionDao) {

    /** Observe all currently active sessions. */
    fun observeActiveSessions(): Flow<List<Session>> =
        sessionDao.getActiveSessions()

    /** Get a specific session by ID. */
    suspend fun getSessionById(sessionId: String): Session? =
        sessionDao.getSessionById(sessionId)

    /** Find an active session with a specific remote peer. */
    suspend fun getActiveSessionWithPeer(peerId: String): Session? =
        sessionDao.getActiveSessionWithPeer(peerId)

    /** Persist a new session. */
    suspend fun saveSession(session: Session) =
        sessionDao.insertSession(session)

    /** Deactivate all sessions past their expiry time. */
    suspend fun deactivateExpired() =
        sessionDao.deactivateExpiredSessions(System.currentTimeMillis())

    /** Delete all ephemeral sessions (for privacy mode cleanup). */
    suspend fun deleteEphemeralSessions() =
        sessionDao.deleteEphemeralSessions()

    /** Remove all sessions (used during panic wipe). */
    suspend fun clearAll() = sessionDao.deleteAll()
}
