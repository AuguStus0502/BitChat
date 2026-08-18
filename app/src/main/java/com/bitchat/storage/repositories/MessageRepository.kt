package com.bitchat.storage.repositories

import com.bitchat.core.models.Message
import com.bitchat.core.models.MessageStatus
import com.bitchat.core.models.MessageType
import com.bitchat.storage.database.MessageDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing messages across all types (DM, channel, SOS, relay).
 * Handles message persistence, status updates, and cleanup of old ephemeral data.
 */
class MessageRepository(private val messageDao: MessageDao) {

    /** Observe all messages exchanged with a specific peer. */
    fun observeMessagesWithPeer(peerId: String): Flow<List<Message>> =
        messageDao.getMessagesWithPeer(peerId)

    /** Observe messages in a named channel. */
    fun observeChannelMessages(channelName: String): Flow<List<Message>> =
        messageDao.getChannelMessages(channelName)

    /** Observe recent messages for diagnostics display. */
    fun observeRecentMessages(limit: Int = 50): Flow<List<Message>> =
        messageDao.getRecentMessages(limit)

    /** Get a message by its unique identifier. */
    suspend fun getMessageById(messageId: String): Message? =
        messageDao.getMessageById(messageId)

    /** Persist a new message or update an existing one. */
    suspend fun saveMessage(message: Message) = messageDao.insertMessage(message)

    /** Update only the delivery status of a message. */
    suspend fun updateStatus(messageId: String, status: MessageStatus) =
        messageDao.updateMessageStatus(messageId, status)

    /** Delete a specific message (e.g., after successful delivery confirmation). */
    suspend fun deleteMessage(message: Message) = messageDao.deleteMessage(message)

    /** Delete old encrypted messages beyond retention threshold. */
    suspend fun cleanupOldEncryptedMessages(retentionMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        messageDao.deleteOldEncryptedMessages(System.currentTimeMillis() - retentionMs)
    }

    /** Remove all messages (used during panic wipe). */
    suspend fun clearAll() = messageDao.deleteAll()
}
