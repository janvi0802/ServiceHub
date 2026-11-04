package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.liveObject
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Review
import com.example.servicehub.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Reading providers for customers, plus the provider's own availability switch. */
object ProviderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val providers = db.collection("providers")

    /**
     * Live list of providers a customer may book in one category:
     * approved + available + not blocked. (The area filter is applied on the screen.)
     *
     * "combine" joins two live flows: the providers of this category and the list of
     * blocked user ids. Whenever either changes, the result is recalculated.
     */
    fun bookableProviders(categoryId: String): Flow<List<Provider>> {
        val approvedInCategory = providers
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED)
            .liveList(Provider::class.java)

        return combine(approvedInCategory, blockedUserIds()) { list, blockedIds ->
            list.filter { it.isAvailable && it.id !in blockedIds }
                .sortedByDescending { it.ratingAvg }
        }
    }

    /** Live set of ids of blocked users (admin can block users in Phase 11). */
    private fun blockedUserIds(): Flow<Set<String>> =
        db.collection("users")
            .whereEqualTo("isBlocked", true)
            .liveList(User::class.java)
            .map { users -> users.map { it.id }.toSet() }

    /** One provider, live (rating and job count update by themselves). */
    fun liveProvider(providerId: String): Flow<Provider?> =
        providers.document(providerId).liveObject(Provider::class.java)

    /** Live reviews of a provider, newest first. Sorted in Kotlin so no Firestore index is needed. */
    fun reviewsFor(providerId: String): Flow<List<Review>> =
        db.collection("reviews")
            .whereEqualTo("providerId", providerId)
            .liveList(Review::class.java)
            .map { list -> list.sortedByDescending { it.createdAt } }

    /** Provider switches Available / Not available. Unavailable providers are hidden from customers. */
    suspend fun setAvailability(providerId: String, available: Boolean) {
        providers.document(providerId).update("isAvailable", available).await()
    }
}
