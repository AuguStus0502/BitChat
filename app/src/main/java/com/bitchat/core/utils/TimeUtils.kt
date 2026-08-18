package com.bitchat.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility object for formatting timestamps and durations for display in the BitChat UI.
 *
 * Provides locale-aware formatting using the device's default locale. All public methods
 * accept millisecond-precision Unix timestamps and return human-readable strings.
 *
 * Thread-safety note: [SimpleDateFormat] is not thread-safe. Each format instance is
 * reused without synchronization, which is acceptable here because Android UI calls
 * typically execute on the main thread. If this utility is ever called from background
 * threads, the format methods should be synchronized or replaced with `java.time` equivalents.
 */
object TimeUtils {

    /** Compact time-only format (HH:mm) used in chat message bubbles. */
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /** Full date-time format (yyyy-MM-dd HH:mm:ss) used in detail views and logs. */
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Formats a timestamp as a short time string (e.g., "14:30").
     *
     * @param timestamp Milliseconds since epoch.
     * @return Time string in the device's locale format.
     */
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    /**
     * Formats a timestamp as a full date-time string (e.g., "2026-08-18 14:30:00").
     *
     * @param timestamp Milliseconds since epoch.
     * @return Date-time string in the device's locale format.
     */
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    /**
     * Formats a duration in milliseconds as a human-readable string.
     *
     * Omits leading zero components to keep the output concise. For example:
     * - 500ms  -> "0s"
     * - 65000  -> "1m 05s"
     * - 3661000 -> "1h 01m 01s"
     *
     * @param elapsedMs Duration in milliseconds.
     * @return Formatted duration string.
     */
    fun formatDuration(elapsedMs: Long): String {
        val seconds = (elapsedMs / 1000) % 60
        val minutes = (elapsedMs / (1000 * 60)) % 60
        val hours = (elapsedMs / (1000 * 60 * 60))
        return when {
            hours > 0 -> "%dh %02dm %02ds".format(hours, minutes, seconds)
            minutes > 0 -> "%dm %02ds".format(minutes, seconds)
            else -> "%ds".format(seconds)
        }
    }

    /**
     * Formats a timestamp as a relative "age" string (e.g., "5m ago", "2h ago").
     *
     * Used for displaying when a peer was last seen, when a message was sent, etc.
     * Granularity is coarse by design: seconds below a minute, minutes below an hour,
     * and hours thereafter.
     *
     * @param timestamp Milliseconds since epoch to compute the age relative to now.
     * @return Human-readable relative time string.
     */
    fun formatAge(timestamp: Long): String {
        val elapsed = System.currentTimeMillis() - timestamp
        return when {
            elapsed < 60_000 -> "${elapsed / 1000}s ago"
            elapsed < 3_600_000 -> "${elapsed / 60_000}m ago"
            else -> "${elapsed / 3_600_000}h ago"
        }
    }
}
