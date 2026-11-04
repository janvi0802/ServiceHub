package com.example.servicehub.data.model

import androidx.compose.ui.graphics.Color
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.theme.StatusCancelled
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusInProgress
import com.example.servicehub.ui.theme.StatusRejected
import com.example.servicehub.ui.theme.StatusRequested

/**
 * Every state a booking can be in.
 * In Firestore we save the name as text, e.g. "ACCEPTED".
 *
 * Normal flow: REQUESTED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 * Side exits:  REQUESTED -> REJECTED (provider), REQUESTED/ACCEPTED -> CANCELLED (customer)
 */
enum class BookingStatus(val label: String, val color: Color) {
    REQUESTED("Requested", StatusRequested),
    ACCEPTED("Accepted", StatusAccepted),
    IN_PROGRESS("In progress", StatusInProgress),
    COMPLETED("Completed", StatusCompleted),
    REJECTED("Rejected", StatusRejected),
    CANCELLED("Cancelled", StatusCancelled);

    /** True while the job is still "open" (it blocks the provider's slot). */
    val isActive: Boolean
        get() = this == REQUESTED || this == ACCEPTED || this == IN_PROGRESS

    companion object {
        /** Turns the text stored in Firestore back into a status. Unknown text -> REQUESTED. */
        fun fromName(name: String): BookingStatus =
            entries.firstOrNull { it.name == name } ?: REQUESTED
    }
}
