package com.example.servicehub.data.repository

import com.example.servicehub.data.SampleData
import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/** Admin-only actions: loading sample data and approving providers. */
object AdminRepository {

    private val db = FirebaseFirestore.getInstance()
    private val providers = db.collection("providers")

    /**
     * A LIVE list of providers waiting for approval.
     * callbackFlow turns Firestore's listener into a Flow: every time the data changes
     * in the cloud (on any phone), a new list is sent to the screen automatically.
     */
    fun pendingProviders(): Flow<List<Provider>> = callbackFlow {
        val listener = providers
            .whereEqualTo("approvalStatus", ApprovalStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)   // stop the flow with the error
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Provider::class.java).orEmpty()
                trySend(list.sortedBy { it.createdAt })
            }
        // When the screen stops listening, remove the Firestore listener (saves battery/data).
        awaitClose { listener.remove() }
    }

    suspend fun setApproval(providerId: String, status: String) {
        providers.document(providerId).update("approvalStatus", status).await()
        if (status == ApprovalStatus.APPROVED) {
            NotificationRepository.send(providerId, "Account approved", "Welcome to ServiceHub! Customers can now find and book you.")
        } else if (status == ApprovalStatus.REJECTED) {
            NotificationRepository.send(providerId, "Account not approved", "Sorry, the admin did not approve your provider account.")
        }
    }

    // ---------------------------------------------------------------- Phase 11: admin control

    /** Every user, live (customers, providers, admins). */
    fun allUsers(): Flow<List<User>> =
        db.collection("users").liveList(User::class.java)

    /** Every provider, live, any approval status. */
    fun allProviders(): Flow<List<Provider>> =
        providers.liveList(Provider::class.java)

    /** Every booking, live, newest first. */
    fun allBookings(): Flow<List<Booking>> =
        db.collection("bookings").liveList(Booking::class.java)
            .map { list -> list.sortedByDescending { it.createdAt } }

    /** Block or unblock a user. Blocked users cannot log in and blocked providers are hidden. */
    suspend fun setBlocked(userId: String, blocked: Boolean) {
        db.collection("users").document(userId).update("isBlocked", blocked).await()
    }

    /** Admin edits a service's price and description. */
    suspend fun updateService(serviceId: String, price: Int, description: String) {
        db.collection("services").document(serviceId)
            .update(mapOf("price" to price, "description" to description)).await()
    }

    /** Admin adds a brand-new service (e.g. "Geyser repair") to a category. */
    suspend fun addService(categoryId: String, name: String, description: String, price: Int, durationMins: Int) {
        val ref = db.collection("services").document()
        ref.set(
            Service(
                id = ref.id, categoryId = categoryId, name = name, description = description,
                price = price, durationMins = durationMins, isActive = true,
            )
        ).await()
    }

    /** Hide (false) or show (true) a service for customers. */
    suspend fun setServiceActive(serviceId: String, active: Boolean) {
        db.collection("services").document(serviceId).update("isActive", active).await()
    }

    /** True if services were already loaded (so we can hide/disable the button). */
    suspend fun hasServices(): Boolean {
        val snapshot = db.collection("services").limit(1).get().await()
        return !snapshot.isEmpty
    }

    /**
     * Writes all sample services and providers in one batch.
     * Each sample provider also gets a users/{id} document (role = provider), like a real one.
     * Sample providers have no login: they exist so customers have someone to book.
     */
    suspend fun loadSampleData() {
        val batch = db.batch()
        SampleData.services.forEach { service ->
            batch.set(db.collection("services").document(service.id), service)
        }
        SampleData.providers.forEach { provider ->
            batch.set(providers.document(provider.id), provider)
            val user = User(
                id = provider.id, name = provider.name, email = "${provider.id}@sample.servicehub",
                phone = provider.phone, role = Roles.PROVIDER, area = provider.area,
                createdAt = provider.createdAt,
            )
            batch.set(db.collection("users").document(provider.id), user)
        }
        batch.commit().await()
    }
}
