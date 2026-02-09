package dev.notyouraverage.smscourier.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Tests for DateTimeFormatters utility functions.
 *
 * Note: We avoid mocking System.currentTimeMillis() as it causes test hangs.
 * Instead, we use timestamps relative to actual current time for relative date tests,
 * and fixed timestamps for format tests (which don't depend on "now").
 */
class DateTimeFormattersTest {

    // Fixed reference time for format tests: Feb 9, 2026 12:00:00 UTC
    private val fixedTimestamp = 1770638400000L

    // =========================================================================
    // formatRelativeDate() tests - use real time with relative offsets
    // =========================================================================

    @Test
    fun `formatRelativeDate returns Today for recent timestamp`() {
        val now = System.currentTimeMillis()
        // 1 hour ago should be "Today"
        val oneHourAgo = now - (60 * 60 * 1000)
        assertEquals("Today", DateTimeFormatters.formatRelativeDate(oneHourAgo))
    }

    @Test
    fun `formatRelativeDate returns Today for current time`() {
        val now = System.currentTimeMillis()
        assertEquals("Today", DateTimeFormatters.formatRelativeDate(now))
    }

    @Test
    fun `formatRelativeDate returns Yesterday for 25 hours ago`() {
        val now = System.currentTimeMillis()
        // 25 hours ago should be "Yesterday"
        val yesterday = now - (25 * 60 * 60 * 1000)
        assertEquals("Yesterday", DateTimeFormatters.formatRelativeDate(yesterday))
    }

    @Test
    fun `formatRelativeDate returns 2 days ago for 50 hours ago`() {
        val now = System.currentTimeMillis()
        // 50 hours ago should be "2 days ago"
        val twoDaysAgo = now - (50 * 60 * 60 * 1000)
        assertEquals("2 days ago", DateTimeFormatters.formatRelativeDate(twoDaysAgo))
    }

    @Test
    fun `formatRelativeDate returns days ago for 3-6 days`() {
        val now = System.currentTimeMillis()

        // 3 days ago
        val threeDaysAgo = now - (3L * 24 * 60 * 60 * 1000)
        assertEquals("3 days ago", DateTimeFormatters.formatRelativeDate(threeDaysAgo))

        // 6 days ago
        val sixDaysAgo = now - (6L * 24 * 60 * 60 * 1000)
        assertEquals("6 days ago", DateTimeFormatters.formatRelativeDate(sixDaysAgo))
    }

    @Test
    fun `formatRelativeDate returns date format for 7 or more days ago`() {
        val now = System.currentTimeMillis()
        // 8 days ago should return a date format
        val eightDaysAgo = now - (8L * 24 * 60 * 60 * 1000)
        val result = DateTimeFormatters.formatRelativeDate(eightDaysAgo)
        // Should be date format like "Feb 2, 2026"
        assertTrue(
            "Expected date format (MMM d, yyyy), got: $result",
            result.matches(Regex("\\w{3} \\d{1,2}, \\d{4}")),
        )
    }

    @Test
    fun `formatRelativeDate returns date format for 30 days ago`() {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val result = DateTimeFormatters.formatRelativeDate(thirtyDaysAgo)
        assertTrue(
            "Expected date format, got: $result",
            result.matches(Regex("\\w{3} \\d{1,2}, \\d{4}")),
        )
    }

    // =========================================================================
    // formatDuration() tests - pure functions, no time dependency
    // =========================================================================

    @Test
    fun `formatDuration returns minutes only for less than 60 minutes`() {
        assertEquals("45 min", DateTimeFormatters.formatDuration(45))
        assertEquals("1 min", DateTimeFormatters.formatDuration(1))
        assertEquals("59 min", DateTimeFormatters.formatDuration(59))
    }

    @Test
    fun `formatDuration returns hours and minutes for mixed duration`() {
        assertEquals("1h 30m", DateTimeFormatters.formatDuration(90))
        assertEquals("2h 15m", DateTimeFormatters.formatDuration(135))
        assertEquals("1h 1m", DateTimeFormatters.formatDuration(61))
    }

    @Test
    fun `formatDuration returns exact hours when no minutes`() {
        assertEquals("1h", DateTimeFormatters.formatDuration(60))
        assertEquals("2h", DateTimeFormatters.formatDuration(120))
        assertEquals("24h", DateTimeFormatters.formatDuration(1440))
    }

    @Test
    fun `formatDuration returns 0 min for zero duration`() {
        assertEquals("0 min", DateTimeFormatters.formatDuration(0))
    }

    @Test
    fun `formatDuration handles large durations`() {
        assertEquals("48h", DateTimeFormatters.formatDuration(2880))
        assertEquals("100h 30m", DateTimeFormatters.formatDuration(6030))
        assertEquals("168h", DateTimeFormatters.formatDuration(10080))
    }

    // =========================================================================
    // formatTime() tests - use fixed timestamp with UTC timezone
    // =========================================================================

    @Test
    fun `formatTime returns HH-mm format`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            // fixedTimestamp is 12:00 UTC
            assertEquals("12:00", DateTimeFormatters.formatTime(fixedTimestamp))

            // 3.5 hours before = 08:30
            val morning = fixedTimestamp - (3 * 60 * 60 * 1000 + 30 * 60 * 1000)
            assertEquals("08:30", DateTimeFormatters.formatTime(morning))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatTime handles various times of day`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            // Morning: 06:00 (6 hours before noon)
            val morning = fixedTimestamp - (6 * 60 * 60 * 1000)
            assertEquals("06:00", DateTimeFormatters.formatTime(morning))

            // Afternoon: 14:30 (2.5 hours after noon)
            val afternoon = fixedTimestamp + (2 * 60 * 60 * 1000 + 30 * 60 * 1000)
            assertEquals("14:30", DateTimeFormatters.formatTime(afternoon))

            // Evening: 20:45 (8h 45m after noon)
            val evening = fixedTimestamp + (8 * 60 * 60 * 1000 + 45 * 60 * 1000)
            assertEquals("20:45", DateTimeFormatters.formatTime(evening))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatTime handles midnight and noon`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            assertEquals("12:00", DateTimeFormatters.formatTime(fixedTimestamp))

            val midnight = fixedTimestamp - (12 * 60 * 60 * 1000)
            assertEquals("00:00", DateTimeFormatters.formatTime(midnight))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    // =========================================================================
    // formatDateTime() tests
    // =========================================================================

    @Test
    fun `formatDateTime returns date and time format`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            val result = DateTimeFormatters.formatDateTime(fixedTimestamp)
            assertEquals("Feb 9, 12:00 PM", result)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatDateTime handles morning time`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            // 6:00 AM
            val morningTime = fixedTimestamp - (6 * 60 * 60 * 1000)
            assertEquals("Feb 9, 6:00 AM", DateTimeFormatters.formatDateTime(morningTime))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    // =========================================================================
    // formatIso8601() tests
    // =========================================================================

    @Test
    fun `formatIso8601 returns ISO 8601 format`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            val result = DateTimeFormatters.formatIso8601(fixedTimestamp)
            assertTrue(
                "Expected ISO 8601 format, got: $result",
                result.matches(Regex("2026-02-09T12:00:00[Z+].*")),
            )
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatIso8601 includes timezone offset`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            val result = DateTimeFormatters.formatIso8601(fixedTimestamp)
            assertTrue(
                "Expected timezone indicator, got: $result",
                result.contains("+00:00") || result.endsWith("Z"),
            )
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatIso8601 adjusts for timezone`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

        try {
            val result = DateTimeFormatters.formatIso8601(fixedTimestamp)
            // Should have timezone offset
            assertTrue(
                "Expected timezone offset, got: $result",
                result.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}")),
            )
            // EST is UTC-5 in winter, so 12:00 UTC = 07:00 EST
            assertTrue(
                "Expected 07:00 for EST, got: $result",
                result.contains("T07:00:00"),
            )
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    // =========================================================================
    // formatExportDateTime() tests
    // =========================================================================

    @Test
    fun `formatExportDateTime returns sortable format`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            val result = DateTimeFormatters.formatExportDateTime(fixedTimestamp)
            assertEquals("2026-02-09 12:00:00", result)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatExportDateTime is filename safe`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            val result = DateTimeFormatters.formatExportDateTime(fixedTimestamp)
            // Should only contain digits, dashes, spaces, colons
            assertTrue(
                "Should be safe for filenames: $result",
                result.matches(Regex("[\\d\\-: ]+")),
            )
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `formatExportDateTime handles year boundary`() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        try {
            // Jan 1, 2026 00:00:00 UTC (1735689600000 + 365 days = 1767225600000)
            val newYear = 1767225600000L
            assertEquals("2026-01-01 00:00:00", DateTimeFormatters.formatExportDateTime(newYear))

            // Dec 31, 2025 23:59:59 UTC
            val newYearEve = newYear - 1000
            assertEquals("2025-12-31 23:59:59", DateTimeFormatters.formatExportDateTime(newYearEve))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
