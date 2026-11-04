package com.example.servicehub.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Lists that never change while the app runs, so they live in code (not in Firestore).
 * Services DO live in Firestore, because the admin can edit their prices.
 */

/** One service category, e.g. Plumber. `id` is what we save in Firestore. */
data class Category(
    val id: String,
    val name: String,
    val icon: ImageVector,
)

object FixedData {

    val categories = listOf(
        Category("plumber", "Plumber", Icons.Filled.Plumbing),
        Category("electrician", "Electrician", Icons.Filled.ElectricalServices),
        Category("cleaning", "Cleaning", Icons.Filled.CleaningServices),
        Category("ac_repair", "AC Repair", Icons.Filled.AcUnit),
        Category("carpenter", "Carpenter", Icons.Filled.Carpenter),
        Category("painter", "Painter", Icons.Filled.FormatPaint),
    )

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

    val paymentModes = listOf("Cash", "UPI")

    /** Fixed visit charge added to every booking, in rupees. */
    const val VISIT_FEE = 49

    /** Finds a category by its id, e.g. "plumber". Returns null if not found. */
    fun categoryById(id: String): Category? = categories.firstOrNull { it.id == id }
}
