package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveObject
import kotlinx.coroutines.flow.Flow
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Reads and updates people's data in Firestore
 * (the "users" and "providers" collections).
 */
object UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")
    private val providers = db.collection("providers")

    /** Loads users/{uid}. Returns null if the document does not exist. */
    suspend fun getUser(uid: String): User? {
        val snapshot = users.document(uid).get().await()
        return snapshot.toObject(User::class.java)
    }

    /** users/{uid}, live. Used to log a person out the moment the admin blocks them. */
    fun liveUser(uid: String): Flow<User?> = users.document(uid).liveObject(User::class.java)

    /** Loads providers/{uid}. Returns null if the document does not exist. */
    suspend fun getProvider(uid: String): Provider? {
        val snapshot = providers.document(uid).get().await()
        return snapshot.toObject(Provider::class.java)
    }

    /**
     * Saves the editable profile fields.
     * Providers also have name/phone/area copied in providers/{uid},
     * so we update both documents together in one "batch" (all or nothing).
     */
    suspend fun updateProfile(user: User, name: String, phone: String, area: String, address: String) {
        val batch = db.batch()
        batch.update(
            users.document(user.id),
            mapOf("name" to name, "phone" to phone, "area" to area, "address" to address)
        )
        if (user.role == Roles.PROVIDER) {
            batch.update(
                providers.document(user.id),
                mapOf("name" to name, "phone" to phone, "area" to area)
            )
        }
        batch.commit().await()
    }
}
