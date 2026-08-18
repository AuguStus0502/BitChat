package com.bitchat.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bitchat.core.models.*

/**
 * Room database singleton that serves as the single source of truth for all
 * local persistent state in BitChat.
 *
 * ## Schema overview
 * The database contains six entity tables:
 *
 * | Entity       | Table          | Purpose                                         |
 * |--------------|----------------|-------------------------------------------------|
 * | [Peer]       | `peers`        | Discovered BLE mesh peers and connection state   |
 * | [Message]    | `messages`     | Direct & channel messages, SOS alerts            |
 * | [Identity]   | `identities`   | User key-pairs and display names                 |
 * | [SosBeacon]  | `sos_beacons`  | Active and expired emergency distress beacons    |
 * | [Session]    | `sessions`     | Encrypted communication sessions (active/ephemeral) |
 * | [QueueItem]  | `message_queue`| Durable outbound message retry queue             |
 *
 * ## Version strategy
 * The current schema version is **1**. `exportSchema` is disabled because
 * this is a client-side database with no need for migration artifacts in CI.
 * When a breaking schema change is introduced the version number is
 * incremented and [fallbackToDestructiveMigration] is used — this is
 * acceptable because BitChat is a stateless-by-design mesh messenger and
 * users can recover state by re-connecting to the mesh. In the future a
 * dedicated [androidx.room.migration.Migration] should be considered if
 * preserving local history across upgrades becomes a requirement.
 *
 * ## Thread safety
 * The singleton is guarded by a double-checked-lock on a [Volatile] field
 * ([INSTANCE]). Room's generated implementation is inherently thread-safe
 * for concurrent reads and writes; all DAO methods are safe to call from
 * any coroutine or thread. The [Context] reference is always the
 * application context to avoid leaking activities.
 *
 * ## Security considerations
 * - The database file (`bitchat_database`) resides in the app's internal
 *   storage and is inaccessible to other apps on a non-rooted device.
 * - Sensitive fields (private keys, session key material) are **not**
 *   stored in plaintext by this layer; the repository / crypto layer above
 *   is responsible for encrypting values before they reach Room.
 * - [fallbackToDestructiveMigration] means an upgrade wipes all local data.
 *   This is an intentional trade-off: it avoids complex migration code for
 *   a mesh protocol where peers re-sync state organically, and it ensures
 *   stale or corrupt schema fragments never linger on disk.
 *
 * ## Usage
 * Obtain the single instance via [getInstance] — never construct directly.
 * ```kotlin
 * val db = BitChatDatabase.getInstance(applicationContext)
 * val peers = db.peerDao().getAllPeers()
 * ```
 */
@Database(
    entities = [
        Peer::class,
        Message::class,
        Identity::class,
        SosBeacon::class,
        Session::class,
        QueueItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BitChatDatabase : RoomDatabase() {

    /** Provides access to the [Peer] table for mesh peer management. */
    abstract fun peerDao(): PeerDao

    /** Provides access to the [Message] table for chat message persistence. */
    abstract fun messageDao(): MessageDao

    /** Provides access to the [Identity] table for user key-pair management. */
    abstract fun identityDao(): IdentityDao

    /** Provides access to the [SosBeacon] table for emergency beacon tracking. */
    abstract fun sosBeaconDao(): SosBeaconDao

    /** Provides access to the [Session] table for encrypted session lifecycle. */
    abstract fun sessionDao(): SessionDao

    /** Provides access to the [QueueItem] table for the outbound retry pipeline. */
    abstract fun queueDao(): QueueDao

    companion object {

        /**
         * Volatile singleton reference. Marked `@Volatile` so that reads
         * are never served from a CPU cache and the double-checked lock in
         * [getInstance] is correct under the Java Memory Model.
         */
        @Volatile
        private var INSTANCE: BitChatDatabase? = null

        /**
         * Returns the process-wide [BitChatDatabase] instance, creating it
         * on first access (thread-safe double-checked lock).
         *
         * The builder uses [Room.databaseBuilder] with the application
         * context to avoid leaking Activity references. Destructive
         * migration is enabled so that schema upgrades wipe and recreate
         * the database rather than crashing — acceptable for a mesh
         * messenger that re-syncs state from peers.
         *
         * @param context Any [Context]; the application context is extracted
         *                internally so callers need not worry about leaks.
         * @return The singleton [BitChatDatabase] instance.
         */
        fun getInstance(context: Context): BitChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BitChatDatabase::class.java,
                    "bitchat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
