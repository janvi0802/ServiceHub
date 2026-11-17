package com.example.servicehub.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Lists that never change while the app runs live here in code (areas, slots, fees).
 * Categories: the 6 below are BUILT-IN defaults. The admin can add more (e.g. Yoga Teacher)
 * or hide any of them; those changes live in Firestore and CategoryStore merges them in.
 */

/**
 * One service category, e.g. Plumber. `id` is what we save in Firestore.
 * [iconKey] names an icon from CategoryIcons; [colorHex] tints its tile (0xAARRGGBB).
 */
data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val isActive: Boolean = true,
    val order: Int = 0,
) {
    val icon: ImageVector get() = CategoryIcons.iconFor(iconKey)
    val color: Color get() = Color(colorHex)
}

object FixedData {

    /** The built-in categories (always exist, even before anything is in Firestore). */
    val defaultCategories = listOf(
        Category("plumber", "Plumber", "plumbing", 0xFF1E88E5, order = 1),          // blue (water)
        Category("electrician", "Electrician", "electric", 0xFFF9A825, order = 2),  // yellow (power)
        Category("cleaning", "Cleaning", "cleaning", 0xFF00897B, order = 3),        // teal (fresh)
        Category("ac_repair", "AC Repair", "ac", 0xFF039BE5, order = 4),            // light blue (cool)
        Category("carpenter", "Carpenter", "carpenter", 0xFF8D6E63, order = 5),     // brown (wood)
        Category("painter", "Painter", "paint", 0xFFD81B60, order = 6),             // pink (paint)
    )

    /** Categories customers can see right now (built-in + admin-added, minus hidden), live. */
    val categories: List<Category>
        get() = CategoryStore.active()

    /** Areas a customer or provider can choose. */
    val areas = listOf(
        "Navrangpura",
        "Satellite",
        "Bopal",
        "Maninagar",
        "Vastrapur",
        "Chandkheda",
    )

    /** The three booking time slots. */
    val timeSlots = listOf(
        "Morning (9 AM – 12 PM)",
        "Afternoon (12 PM – 4 PM)",
        "Evening (4 PM – 8 PM)",
    )

    /** Hour (24h clock) when each slot above ends: 12 PM, 4 PM, 8 PM. Same order as timeSlots. */
    private val slotEndHours = listOf(12, 16, 20)

    /** True if this slot is already over for today (so it can't be booked today). */
    fun isSlotOverToday(slot: String, currentHour: Int): Boolean {
        val index = timeSlots.indexOf(slot)
        return index >= 0 && currentHour >= slotEndHours[index]
    }

    /**
     * True if this date + slot can still be booked: not in the past, and not a slot that is
     * already over today. ("yyyy-MM-dd" dates compare correctly as text.)
     */
    fun isBookableTime(date: String, slot: String, today: String, currentHour: Int): Boolean =
        date > today || (date == today && !isSlotOverToday(slot, currentHour))

    val paymentModes = listOf("Cash", "UPI")

    /** Fixed visit charge added to every booking, in rupees. */
    const val VISIT_FEE = 49

    /** Shown under prices so nobody is surprised at the door. */
    const val PRICE_NOTE = "Price covers labour. Spare parts, if needed, are paid separately to the provider after you agree."

    /** Finds a category by its id, e.g. "plumber" (hidden ones too, for old bookings). Null if not found. */
    fun categoryById(id: String): Category? = CategoryStore.byId(id)
}
