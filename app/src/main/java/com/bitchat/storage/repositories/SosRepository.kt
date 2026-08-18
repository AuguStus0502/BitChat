package com.bitchat.storage.repositories

import com.bitchat.core.models.SosBeacon
import com.bitchat.storage.database.SosBeaconDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for emergency SOS beacons.
 * Manages the lifecycle of emergency broadcasts from creation
 * through active relay to expiry and archival.
 */
class SosRepository(private val sosBeaconDao: SosBeaconDao) {

    /** Observe all currently active (non-expired) beacons. */
    fun observeActiveBeacons(): Flow<List<SosBeacon>> =
        sosBeaconDao.getActiveBeacons()

    /** Observe all beacons including expired ones (for diagnostics). */
    fun observeAllBeacons(): Flow<List<SosBeacon>> =
        sosBeaconDao.getAllBeacons()

    /** Retrieve a specific beacon by ID. */
    suspend fun getBeaconById(beaconId: String): SosBeacon? =
        sosBeaconDao.getBeaconById(beaconId)

    /** Persist a new or updated beacon. */
    suspend fun saveBeacon(beacon: SosBeacon) =
        sosBeaconDao.insertBeacon(beacon)

    /** Update an existing beacon (e.g., relay count increment). */
    suspend fun updateBeacon(beacon: SosBeacon) =
        sosBeaconDao.updateBeacon(beacon)

    /** Deactivate all beacons that have exceeded their TTL. */
    suspend fun deactivateExpired() =
        sosBeaconDao.deactivateExpiredBeacons(System.currentTimeMillis())

    /** Remove all beacons (used during panic wipe). */
    suspend fun clearAll() = sosBeaconDao.deleteAll()
}
