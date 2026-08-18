package com.bitchat.app

import android.app.Application
import com.bitchat.storage.database.BitChatDatabase

/**
 * Application-scoped entry point and dependency container for BitChat.
 *
 * Because an [Application] instance lives for the entire process lifetime,
 * it is the natural place to hold singletons that must outlive any single
 * Activity or Fragment. Currently this class provides:
 *
 * - A lazily-initialized [BitChatDatabase] that is shared across the app,
 *   ensuring a single Room database instance and avoiding WAL conflicts.
 * - A [companion-object][instance] reference that gives non-DI code
 *   (e.g. services, broadcast receivers) a convenient way to access the
 *   application context and its singletons.
 *
 * ## Thread safety
 *
 * [BitChatDatabase.getInstance] is synchronised internally, so the lazy
 * delegate here is safe to call from any thread. The [instance] field is
 * written once in [onCreate] before any component can observe it.
 */
class BitChatApplication : Application() {

    /**
     * Shared Room database instance, created on first access.
     *
     * Using [lazy] defers the cost of database creation until it is actually
     * needed — the first DAO call triggers schema verification and WAL
     * initialisation rather than blocking [onCreate].
     */
    val database: BitChatDatabase by lazy { BitChatDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        /**
         * Global reference to the running [BitChatApplication] instance.
         *
         * Populated in [onCreate] and thereafter available to any component
         * that can hold a reference to the process (services, providers, etc.).
         *
         * **Access pattern:** `BitChatApplication.instance`
         */
        lateinit var instance: BitChatApplication
            private set
    }
}
