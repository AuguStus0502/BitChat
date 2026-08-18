package com.bitchat.storage.database

import androidx.room.*
import com.bitchat.core.models.SosBeacon
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [SosBeacon] entities in the local Room database.
 *
 * SOS beacons represent distress signals broadcast by peers over the BLE
 * mesh. Each beacon has a finite lifetime defined by [SosBeacon.expiryTime]
 * and is automatically deactivated once that time has elapsed.
 *
 * ## Threading model
 * - **Reactive queries** (returning [Flow]) deliver live lists of active
 *   or all beacons to the emergency UI screen.
 * - **Suspend functions** handle one-shot writes and deactivation sweeps on
 *   Room's write dispatcher.
 *
 * ## Security & safety considerations
 * SOS beacons may contain location data and should be treated as sensitive.
 * Expired beacons are deactivated rather than immediately deleted so that
 * audit records remain available. A full purge can be triggered explicitly
 * via [deleteAll] or during account reset.
 */
@Dao
interface SosBeaconDao {

    /**
     * Returns all beacons that are currently active (`isActive = 1`), ordered
     * newest-first.
     *
     * This is the primary query behind the emergency-beacon list UI. The
     * [Flow] re-emits whenever a beacon is inserted, updated, or deactivated.
     *
     * @return [Flow] emitting the descending list of active SOS beacons.
     */
    @Query("SELECT * FROM sos_beacons WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveBeacons(): Flow<List<SosBeacon>>

    /**
     * Returns every stored beacon regardless of active status, ordered
     * newest-first.
     *
     * Useful for a history or audit screen that shows both active and
     * expired beacons.
     *
     * @return [Flow] emitting the descending list of all SOS beacons.
     */
    @Query("SELECT * FROM sos_beacons ORDER BY timestamp DESC")
    fun getAllBeacons(): Flow<List<SosBeacon>>

    /**
     * Fetches a single beacon by its unique identifier.
     *
     * @param beaconId The beacon's primary key.
     * @return The matching [SosBeacon] or `null`.
     */
    @Query("SELECT * FROM sos_beacons WHERE beaconId = :beaconId")
    suspend fun getBeaconById(beaconId: String): SosBeacon?

    /**
     * Inserts a beacon or replaces the existing record if one with the same
     * primary key already exists.
     *
     * @param beacon The beacon entity to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: SosBeacon)

    /**
     * Applies a partial update to an existing beacon.
     *
     * @param beacon The beacon with updated field values.
     */
    @Update
    suspend fun updateBeacon(beacon: SosBeacon)

    /**
     * Deletes a specific beacon from the database.
     *
     * @param beacon The beacon entity to remove.
     */
    @Delete
    suspend fun deleteBeacon(beacon: SosBeacon)

    /**
     * Deactivates all beacons whose [SosBeacon.expiryTime] is in the past.
     *
     * Rather than deleting expired beacons, this method flips `isActive` to
     * `0` so the records remain available for audit purposes while no longer
     * appearing in the active-beacon list.
     *
     * Should be called on a periodic timer or whenever the app returns to
     * the foreground.
     *
     * @param now The current epoch millis used as the comparison threshold.
     */
    @Query("UPDATE sos_beacons SET isActive = 0 WHERE expiryTime < :now")
    suspend fun deactivateExpiredBeacons(now: Long)

    /**
     * Irreversibly removes all beacons from the database.
     *
     * Typically invoked during account reset or explicit data wipe.
     */
    @Query("DELETE FROM sos_beacons")
    suspend fun deleteAll()
}
