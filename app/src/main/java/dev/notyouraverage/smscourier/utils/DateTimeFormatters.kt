package dev.notyouraverage.smscourier.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for formatting dates and durations in the Session History UI.
 * Per CONTEXT.md: relative dates ("Today", "Yesterday", "3 days ago") and
 * relative durations ("2h 15m", "45 min").
 */
object DateTimeFormatters {

    /**
     * Formats a timestamp as a relative date string.
     * - "Today" for same calendar day
     * - "Yesterday" for previous day
     * - "X days ago" for 2-6 days ago
     * - "MMM d, yyyy" for older dates (e.g., "Jan 15, 2026")
     */
    fun formatRelativeDate(timestampMillis: Long): String {
        val now = System.currentTimeMillis()

        // Calculate days difference using calendar days (not 24-hour periods)
        val nowDayStart = now - (now % DateUtils.DAY_IN_MILLIS)
        val timestampDayStart = timestampMillis - (timestampMillis % DateUtils.DAY_IN_MILLIS)
        val daysDiff = ((nowDayStart - timestampDayStart) / DateUtils.DAY_IN_MILLIS).toInt()

        return when {
            daysDiff == 0 -> "Today"
            daysDiff == 1 -> "Yesterday"
            daysDiff in 2..6 -> "$daysDiff days ago"
            else -> {
                // Absolute date for older items
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                dateFormat.format(Date(timestampMillis))
            }
        }
    }

    /**
     * Formats a duration in minutes as a human-readable string.
     * - "2h 15m" for hours + minutes
     * - "2h" for exact hours
     * - "45 min" for minutes only
     * - "0 min" for zero duration
     */
    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "$minutes min"
        }
    }

    /**
     * Formats a timestamp as a time string (HH:mm).
     * Used for message timestamps within a session.
     */
    fun formatTime(timestampMillis: Long): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(Date(timestampMillis))
    }

    /**
     * Formats a timestamp as date and time for message detail.
     * Format: "Jan 15, 2:30 PM"
     */
    fun formatDateTime(timestampMillis: Long): String {
        val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return dateTimeFormat.format(Date(timestampMillis))
    }

    /**
     * ISO 8601 format for JSON export with timezone offset.
     * Example: "2026-02-06T14:30:00+00:00"
     */
    fun formatIso8601(timestampMillis: Long): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return isoFormat.format(Date(timestampMillis))
    }

    /**
     * Local datetime format for CSV/TXT export.
     * Example: "2026-02-06 14:30:00"
     */
    fun formatExportDateTime(timestampMillis: Long): String {
        val exportFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return exportFormat.format(Date(timestampMillis))
    }
}
