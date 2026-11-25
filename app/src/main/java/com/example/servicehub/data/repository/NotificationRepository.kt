package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * In-app notifications (plus a push to the phone, see push/PushService.kt).
 * Each important event writes one document to "notifications" for the person who should see it.
 * The other phone is listening live, so the bell count goes up right away.
 */
object NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val notifications = db.collection("notifications")

    /**
     * Creates a notification. Never throws: a failed notification must not undo
     * the real action (e.g. the booking was still accepted).
     */
    suspend fun send(userId: String, title: String, message: String, bookingId: String = "") {
        if (userId.isBlank()) return
        runCatching {
            val ref = notifications.document()
            ref.set(
                AppNotification(
                    id = ref.id, userId = userId, title = title, message = message,
                    bookingId = bookingId, isRead = false, createdAt = System.currentTimeMillis(),
                )
            ).await()
            // Also show it on their phone's notification bar (push), if the server is set up
            com.example.servicehub.push.Push.send(ref.id)
        }
    }

    /** A person's notifications, newest first (sorted in Kotlin, so no index is needed). */
    fun forUser(userId: String): Flow<List<AppNotification>> =
        notifications.whereEqualTo("userId", userId)
            .liveList(AppNotification::class.java)
            .map { list -> list.sortedByDescending { it.createdAt } }

    /** Live number of unread notifications, for the badge on the Alerts tab. */
    fun unreadCount(userId: String): Flow<Int> =
        forUser(userId).map { list -> list.count { !it.isRead } }

    suspend fun markRead(notificationId: String) {
        notifications.document(notificationId).update("isRead", true).await()
    }

    /** Marks many as read in one batch. */
    suspend fun markAllRead(list: List<AppNotification>) {
        val unread = list.filter { !it.isRead }
        if (unread.isEmpty()) return
        val batch = db.batch()
        unread.forEach { batch.update(notifications.document(it.id), "isRead", true) }
        batch.commit().await()
    }
}
