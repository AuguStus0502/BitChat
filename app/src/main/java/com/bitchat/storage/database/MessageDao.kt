package com.bitchat.storage.database

import androidx.room.*
import com.bitchat.core.models.Message
import com.bitchat.core.models.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [Message] entities in the local Room database.
 *
 * Messages are the core communication primitive in BitChat. They may be
 * directed (peer-to-peer) or broadcast to a named channel. Each message
 * carries metadata including its [Message.status], encryption flag, and
 * timestamp.
 *
 * ## Threading model
 * - **Reactive queries** (returning [Flow]) are long-lived and executed on
 *   Room's query executor. Collectors should dispatch to an appropriate
 *   thread / dispatcher for UI updates.
 * - **Suspend functions** perform single-shot reads or writes on Room's
 *   write dispatcher and are safe to call from any coroutine.
 *
 * ## Security considerations
 * Encrypted messages are flagged with `isEncrypted = 1`. The cleanup query
 * [deleteOldEncryptedMessages] targets only these rows so that plaintext
 * metadata (channel names, sender IDs) can be retained longer if desired.
 * Callers should ensure that plaintext message bodies are cleared in
 * accordance with the application's data-retention policy.
 */
@Dao
interface MessageDao {

    /**
     * Returns all messages exchanged with a specific peer, ordered
     * chronologically (oldest first).
     *
     * The query matches on either the sender or destination column so it
     * captures both inbound and outbound messages in a single result set.
     *
     * @param peerId The peer identifier shared with the other party.
     * @return [Flow] emitting the ordered message list; re-emits on every
     *          insert or status change.
     */
    @Query("SELECT * FROM messages WHERE (senderId = :peerId OR destinationId = :peerId) ORDER BY timestamp ASC")
    fun getMessagesWithPeer(peerId: String): Flow<List<Message>>

    /**
     * Returns all messages broadcast to a specific channel, ordered
     * chronologically (oldest first).
     *
     * @param channelName The case-sensitive channel name to query.
     * @return [Flow] emitting the ordered channel message list.
     */
    @Query("SELECT * FROM messages WHERE channelName = :channelName ORDER BY timestamp ASC")
    fun getChannelMessages(channelName: String): Flow<List<Message>>

    /**
     * Returns all SOS emergency messages, ordered most-recent-first.
     *
     * SOS messages use the dedicated type string `'SOS_MESSAGE'` and are
     * displayed prominently in the emergency beacon UI. Newer messages appear
     * at the top so the user sees the latest alerts first.
     *
     * @return [Flow] emitting the descending list of SOS messages.
     */
    @Query("SELECT * FROM messages WHERE type = 'SOS_MESSAGE' ORDER BY timestamp DESC")
    fun getActiveSosMessages(): Flow<List<Message>>

    /**
     * Fetches a single message by its unique identifier.
     *
     * @param messageId The message's primary key.
     * @return The matching [Message] or `null`.
     */
    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): Message?

    /**
     * Inserts a message or replaces the existing record when a message with
     * the same primary key already exists.
     *
     * The upsert strategy ensures idempotent inserts, which is essential
     * because messages may be re-broadcast over the BLE mesh and arrive
     * more than once.
     *
     * @param message The message entity to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    /**
     * Applies a partial update to an existing message.
     *
     * @param message The message with updated field values.
     */
    @Update
    suspend fun updateMessage(message: Message)

    /**
     * Updates only the delivery status of a single message.
     *
     * Called as the message progresses through the delivery pipeline
     * (e.g., PENDING -> SENT -> DELIVERED -> READ).
     *
     * @param messageId The message whose status should change.
     * @param status    The new [MessageStatus] value.
     */
    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    /**
     * Deletes a specific message from the database.
     *
     * @param message The message entity to remove.
     */
    @Delete
    suspend fun deleteMessage(message: Message)

    /**
     * Purges encrypted messages older than [threshold] epoch millis.
     *
     * This is the primary data-retention enforcement mechanism. Only rows
     * with `isEncrypted = 1` are deleted, allowing unencrypted system
     * messages or plaintext channel metadata to persist independently.
     *
     * **Security note:** This method should be called on a regular schedule
     * to minimize the window in which ciphertext (and associated metadata)
     * remains at rest on the device.
     *
     * @param threshold Epoch millis; encrypted messages older than this are removed.
     */
    @Query("DELETE FROM messages WHERE timestamp < :threshold AND isEncrypted = 1")
    suspend fun deleteOldEncryptedMessages(threshold: Long)

    /**
     * Irreversibly removes every message from the database.
     *
     * Used during account reset. Callers should confirm with the user
     * because this action cannot be undone.
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    /**
     * Returns the most recent [limit] messages across all conversations,
     * ordered newest-first.
     *
     * Useful for populating a "recent activity" or notification preview
     * screen without loading the full message history.
     *
     * @param limit Maximum number of messages to return.
     * @return [Flow] emitting the bounded, descending message list.
     */
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<Message>>
}
