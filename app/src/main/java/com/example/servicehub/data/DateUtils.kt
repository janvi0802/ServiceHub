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

    /** True if [millis] is a moment during today. */
    fun isToday(millis: Long): Boolean =
        millis != 0L && storageFormat().format(Date(millis)) == today()
}
