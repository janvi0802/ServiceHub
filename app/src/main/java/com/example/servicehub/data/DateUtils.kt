package com.example.servicehub.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Booking dates are saved as text "yyyy-MM-dd" (e.g. "2026-11-05").
 * This format sorts correctly as plain text and is easy to compare.
 * We use Calendar/SimpleDateFormat because they work on all Android versions (minSdk 24).
 */
object DateUtils {

    private const val STORAGE_PATTERN = "yyyy-MM-dd"

    private fun storageFormat() = SimpleDateFormat(STORAGE_PATTERN, Locale.US)

    /** Today as "yyyy-MM-dd". */
    fun today(): String = storageFormat().format(Date())

    /** Today plus the next [extraDays] days, e.g. 8 dates for "today + next 7 days". */
    fun nextDays(extraDays: Int = 7): List<String> {
        val calendar = Calendar.getInstance()
        val format = storageFormat()
        return (0..extraDays).map {
            val text = format.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            text
        }
    }

    /** "2026-11-05" -> "Thu, 5 Nov". */
    fun pretty(date: String): String {
        val parsed = runCatching { storageFormat().parse(date) }.getOrNull() ?: return date
        return SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(parsed)
    }

    /** Short label for date chips: "Today", "Tomorrow" or "Sat 8". */
    fun chipLabel(date: String): String {
        val days = nextDays(1)
        return when (date) {
            days[0] -> "Today"
            days[1] -> "Tomorrow"
            else -> {
                val parsed = runCatching { storageFormat().parse(date) }.getOrNull() ?: return date
                SimpleDateFormat("EEE d", Locale.ENGLISH).format(parsed)
            }
        }
    }

    /** Milliseconds -> "5 Nov 2026, 3:40 PM". 0 means "not happened yet" -> "". */
    fun dateTime(millis: Long): String {
        if (millis == 0L) return ""
        return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(millis))
    }

    /** The last [count] dates ending today, oldest first, e.g. 7 dates for "this week" charts. */
    fun lastDays(count: Int = 7, now: Long = System.currentTimeMillis()): List<String> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val format = storageFormat()
        return (0 until count).map {
            val text = format.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            text
        }.reversed()
    }

    /** Milliseconds -> "yyyy-MM-dd" (the day that moment happened). */
    fun dateOf(millis: Long): String = storageFormat().format(Date(millis))

    /** Day names in Calendar order (index 0 = Sunday = Calendar day 1). */
    val weekdayShortNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    /** "2026-11-22" -> Calendar day number (1 = Sunday ... 7 = Saturday). 0 if the text is not a date. */
    fun weekdayOf(date: String): Int {
        val parsed = runCatching { storageFormat().parse(date) }.getOrNull() ?: return 0
        return Calendar.getInstance().apply { time = parsed }.get(Calendar.DAY_OF_WEEK)
    }

    /** [7, 1] -> "Sun, Sat" (sorted in Calendar order). */
    fun weekdayNames(days: List<Int>): String =
        days.sorted().mapNotNull { weekdayShortNames.getOrNull(it - 1) }.joinToString(", ")

    /** True if [millis] is a moment during today. */
    fun isToday(millis: Long): Boolean =
        millis != 0L && storageFormat().format(Date(millis)) == today()
}
