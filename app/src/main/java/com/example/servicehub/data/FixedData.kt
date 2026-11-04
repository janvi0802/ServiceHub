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

    val paymentModes = listOf("Cash", "UPI")

    /** Fixed visit charge added to every booking, in rupees. */
    const val VISIT_FEE = 49

    /** Finds a category by its id, e.g. "plumber". Returns null if not found. */
    fun categoryById(id: String): Category? = categories.firstOrNull { it.id == id }
}
