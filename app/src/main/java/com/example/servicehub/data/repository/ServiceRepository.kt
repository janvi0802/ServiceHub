package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

/**
 * The service catalog ("services" collection).
 * There are only ~20 services, so we load them all and filter in Kotlin
 * (by category, by search text, hidden or not). Simple, and Firestore needs no extra indexes.
 */
object ServiceRepository {

    private val services = FirebaseFirestore.getInstance().collection("services")

    /** Live list of ALL services, including hidden ones (screens filter what they need). */
    fun allServices(): Flow<List<Service>> = services.liveList(Service::class.java)

    suspend fun getService(id: String): Service? =
        services.document(id).get().await().toObject(Service::class.java)
}
