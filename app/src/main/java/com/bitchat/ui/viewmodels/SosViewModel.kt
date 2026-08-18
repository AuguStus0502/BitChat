package com.bitchat.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.app.BitChatApplication
import com.bitchat.core.models.SosBeacon
import com.bitchat.core.models.SosPriority
import com.bitchat.network.sos.SosManager
import com.bitchat.security.identity.IdentityManager
import com.bitchat.storage.repositories.SosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel powering the emergency SOS flow screens:
 * - [SosComposerScreen][com.bitchat.ui.screens.emergency.SosComposerScreen]
 * - [SosConfirmationScreen][com.bitchat.ui.screens.emergency.SosConfirmationScreen]
 * - [ActiveBeaconScreen][com.bitchat.ui.screens.emergency.ActiveBeaconScreen]
 * - [NearbySosFeedScreen][com.bitchat.ui.screens.emergency.NearbySosFeedScreen]
 * - [SosDetailScreen][com.bitchat.ui.screens.emergency.SosDetailScreen]
 *
 * ### Responsibilities
 * 1. **Form state** — holds the composer fields (priority, condition, message, location, relay)
 *    that the user fills in before broadcasting.
 * 2. **Broadcast & cancel** — delegates to [SosManager] for composing and transmitting SOS
 *    beacons, and for cancelling an active beacon.
 * 3. **Observations** — exposes [activeBeacons], [localBeacon], and [isBroadcasting] as
 *    immutable [StateFlow] properties for real-time UI updates.
 *
 * ### Dependency Wiring
 * All dependencies are constructed from [BitChatApplication.database] without a DI framework.
 * The [SosManager] singleton is obtained via [SosManager.getInstance], which ensures a
 * single instance is shared across the application.
 *
 * ### Thread Safety
 * Form state is held in [MutableStateFlow] which is thread-safe. SOS broadcast and cancel
 * operations run on [viewModelScope] (main thread by default) and delegate to
 * [SosManager] which internally dispatches to [kotlinx.coroutines.Dispatchers.IO].
 *
 * @param application The running [Application] instance, used to access system services
 *                    and the shared [BitChatApplication.database].
 */
class SosViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependency references ────────────────────────────────────────────

    /** Shared application instance for database and context access. */
    private val app = application as BitChatApplication

    /** Manages the local cryptographic identity (needed by SosManager). */
    private val identityManager = IdentityManager(application)

    /** Repository for observing and persisting SOS beacons. */
    private val sosRepository = SosRepository(app.database.sosBeaconDao())

    /**
     * SosManager singleton — handles beacon creation, broadcast, relay, and cancellation.
     * Constructed with a null [MessageRelay] for now; the mesh transmission layer will
     * be wired in a later phase.
     */
    private val sosManager: SosManager = SosManager.getInstance(
        context = application,
        identityManager = identityManager,
        sosRepository = sosRepository,
        messageRelay = null // Mesh relay integration deferred to transport layer phase
    )

    // ── Form state (MutableStateFlows) ───────────────────────────────────

    /**
     * Selected SOS priority level.
     * Defaults to [SosPriority.HELP_NEEDED] as the most common emergency classification.
     */
    private val _priority = MutableStateFlow(SosPriority.HELP_NEEDED)

    /**
     * Structured condition tag chosen from a preset list
     * (e.g. "Injured", "Trapped", "Need water").
     */
    private val _condition = MutableStateFlow("")

    /** Free-form additional details provided by the user. */
    private val _message = MutableStateFlow("")

    /** Whether to attach GPS coordinates to the beacon. */
    private val _includeLocation = MutableStateFlow(false)

    /** Whether to allow other mesh nodes to relay this beacon. */
    private val _allowRelay = MutableStateFlow(true)

    // ── Operation state ──────────────────────────────────────────────────

    /**
     * True while a [broadcastSos] coroutine is in flight.
     * Used by the UI to show a loading indicator on the confirm button.
     */
    private val _isBroadcasting = MutableStateFlow(false)

    /**
     * Set to a non-null value after a successful broadcast, containing the
     * newly created [SosBeacon]. The UI navigates to [ActiveBeaconScreen]
     * once this is populated.
     */
    private val _broadcastResult = MutableStateFlow<SosBeacon?>(null)

    /**
     * Holds an error message if broadcast fails, or null if no error.
     * Consumed (reset to null) by the UI after display.
     */
    private val _error = MutableStateFlow<String?>(null)

    // ── Public read-only StateFlows ──────────────────────────────────────

    /** Observable SOS priority form field. */
    val priority: StateFlow<SosPriority> = _priority.asStateFlow()

    /** Observable condition form field. */
    val condition: StateFlow<String> = _condition.asStateFlow()

    /** Observable message form field. */
    val message: StateFlow<String> = _message.asStateFlow()

    /** Observable include-location toggle. */
    val includeLocation: StateFlow<Boolean> = _includeLocation.asStateFlow()

    /** Observable allow-relay toggle. */
    val allowRelay: StateFlow<Boolean> = _allowRelay.asStateFlow()

    /** Observable broadcasting-in-progress flag. */
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    /** Observable result of the last broadcast attempt. */
    val broadcastResult: StateFlow<SosBeacon?> = _broadcastResult.asStateFlow()

    /** Observable error message from the last broadcast attempt. */
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Observable list of all active (non-expired) SOS beacons from the mesh. */
    val activeBeacons: StateFlow<List<SosBeacon>> = sosManager.activeBeacons

    /** Observable reference to the locally-originated SOS beacon, if any. */
    val localBeacon: StateFlow<SosBeacon?> = sosManager.localBeacon

    /** Observable flag indicating whether the local SOS is currently broadcasting. */
    val isSosActive: StateFlow<Boolean> = sosManager.isBroadcasting

    // ── Form mutations ───────────────────────────────────────────────────

    /** Update the selected SOS priority. */
    fun setPriority(value: SosPriority) {
        _priority.value = value
    }

    /** Update the condition tag. */
    fun setCondition(value: String) {
        _condition.value = value
    }

    /** Update the free-form message. */
    fun setMessage(value: String) {
        _message.value = value
    }

    /** Toggle GPS location inclusion. */
    fun setIncludeLocation(value: Boolean) {
        _includeLocation.value = value
    }

    /** Toggle relay permission. */
    fun setAllowRelay(value: Boolean) {
        _allowRelay.value = value
    }

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Compose and broadcast an SOS beacon using the current form state.
     *
     * On success, [_broadcastResult] is set to the created [SosBeacon] and
     * the UI should navigate to [ActiveBeaconScreen][com.bitchat.ui.screens.emergency.ActiveBeaconScreen].
     *
     * On failure, [_error] is populated with a human-readable message.
     */
    fun broadcastSos() {
        // Validate required fields
        if (_condition.value.isBlank()) {
            _error.value = "Condition is required"
            return
        }

        viewModelScope.launch {
            _isBroadcasting.value = true
            try {
                val beacon = sosManager.broadcastSos(
                    priority = _priority.value,
                    condition = _condition.value,
                    message = _message.value,
                    includeLocation = _includeLocation.value,
                    allowRelay = _allowRelay.value
                )
                _broadcastResult.value = beacon
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to broadcast SOS"
            } finally {
                _isBroadcasting.value = false
            }
        }
    }

    /**
     * Cancel the locally-originated SOS beacon.
     *
     * Delegates to [SosManager.cancelBeacon] which sends a cancellation packet
     * to the mesh and updates local state. The [localBeacon] and [isSosActive]
     * flows will update automatically via the underlying [SosManager].
     *
     * @param beaconId The ID of the beacon to cancel. If null, cancels the
     *                 current [localBeacon] if one exists.
     */
    fun cancelBeacon(beaconId: String? = null) {
        val targetId = beaconId ?: localBeacon.value?.beaconId ?: return
        viewModelScope.launch {
            try {
                sosManager.cancelBeacon(targetId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to cancel beacon"
            }
        }
    }

    /**
     * Reset the broadcast result and error state.
     *
     * Should be called by the UI after it has handled navigation
     * or displayed an error toast/dialog.
     */
    fun clearResult() {
        _broadcastResult.value = null
        _error.value = null
    }

    /**
     * Reset form fields to their default values.
     *
     * Useful when returning to the composer screen after a broadcast
     * or when the user explicitly discards the form.
     */
    fun resetForm() {
        _priority.value = SosPriority.HELP_NEEDED
        _condition.value = ""
        _message.value = ""
        _includeLocation.value = false
        _allowRelay.value = true
        _broadcastResult.value = null
        _error.value = null
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // Note: SosManager is a singleton and must not be destroyed here.
        // Its lifecycle is managed by the application process.
    }
}
