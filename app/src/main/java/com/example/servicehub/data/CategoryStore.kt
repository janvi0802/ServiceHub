package com.example.servicehub.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The icons the admin can choose for a category. We store only the KEY (e.g. "yoga") in
 * Firestore, because an icon picture cannot be saved there.
 */
object CategoryIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "plumbing" to Icons.Filled.Plumbing,
        "electric" to Icons.Filled.ElectricalServices,
        "cleaning" to Icons.Filled.CleaningServices,
        "ac" to Icons.Filled.AcUnit,
        "carpenter" to Icons.Filled.Carpenter,
        "paint" to Icons.Filled.FormatPaint,
        "yoga" to Icons.Filled.SelfImprovement,
        "fitness" to Icons.Filled.FitnessCenter,
        "beauty" to Icons.Filled.Face,
        "spa" to Icons.Filled.Spa,
        "tutor" to Icons.Filled.School,
        "pest" to Icons.Filled.BugReport,
        "appliance" to Icons.Filled.Kitchen,
        "laundry" to Icons.Filled.LocalLaundryService,
        "garden" to Icons.Filled.Grass,
        "pets" to Icons.Filled.Pets,
        "car" to Icons.Filled.DirectionsCar,
        "computer" to Icons.Filled.Computer,
        "photo" to Icons.Filled.PhotoCamera,
        "music" to Icons.Filled.MusicNote,
        "other" to Icons.Filled.Handyman,
    )

    /** Unknown keys get a general "handyman" icon instead of crashing. */
    fun iconFor(key: String): ImageVector = all.firstOrNull { it.first == key }?.second ?: Icons.Filled.Handyman
}

/** Colours the admin can choose for a new category (0xAARRGGBB). */
val categoryColorChoices: List<Long> = listOf(
    0xFF1E88E5, 0xFF00897B, 0xFF43A047, 0xFFF9A825, 0xFFFB8C00,
    0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF6D4C41,
)

/** categories/{id} as stored in Firestore. */
data class CategoryDoc(
    val id: String = "",
    val name: String = "",
    val iconKey: String = "other",
    val colorHex: Long = 0xFF1E88E5,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val order: Int = 100,
)

fun CategoryDoc.toCategory() = Category(id, name, iconKey, colorHex, isActive, order)

/**
 * Keeps the up-to-date list of ALL categories in memory for the whole app, live.
 * It starts with the built-in defaults, then listens to the "categories" collection:
 *  - a document with a built-in id (e.g. "painter") changes that category (e.g. hides it)
 *  - any other document is a new category the admin added (e.g. "Yoga Teacher")
 */
object CategoryStore {

    private val _all = MutableStateFlow(FixedData.defaultCategories)
    /** Every category, hidden ones included, sorted for display. Screens collect this to update live. */
    val all: StateFlow<List<Category>> = _all

    private var listener: ListenerRegistration? = null

    /** Start listening once (called when the app starts). Safe to call again. */
    fun start() {
        if (listener != null) return
        listener = FirebaseFirestore.getInstance().collection("categories")
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.toObjects(CategoryDoc::class.java) ?: return@addSnapshotListener
                _all.value = merge(FixedData.defaultCategories, docs)
            }
    }

    fun active(): List<Category> = _all.value.filter { it.isActive }

    fun byId(id: String): Category? = _all.value.firstOrNull { it.id == id }

    /**
     * Built-in defaults + Firestore documents. A document with the same id replaces the default.
     * Pure function (no Firebase), so it is easy to test.
     */
    fun merge(defaults: List<Category>, docs: List<CategoryDoc>): List<Category> {
        val fromDocs = docs.filter { it.id.isNotBlank() && it.name.isNotBlank() }.associateBy { it.id }
        val updatedDefaults = defaults.map { d -> fromDocs[d.id]?.toCategory() ?: d }
        val added = fromDocs.values.filter { doc -> defaults.none { it.id == doc.id } }.map { it.toCategory() }
        return (updatedDefaults + added).sortedWith(compareBy<Category> { it.order }.thenBy { it.name })
    }
}
