package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.User
import com.example.servicehub.navigation.Routes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

/** Extra details a provider fills in at registration. */
data class ProviderDetails(
    val categoryId: String,
    val experienceYears: Int,
    val bio: String,
)

/** Where to go after login, and which role the person has. */
data class HomeDestination(val route: String, val role: String)

/**
 * Everything about logging in and out, using Firebase Authentication.
 * Firebase remembers the logged-in user on the phone, so "stay logged in" works automatically.
 */
object AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /** The id (uid) of the logged-in person, or null if nobody is logged in. */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        auth.signOut()
    }

    /** Firebase emails a link to choose a new password (free, no server code needed). */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /**
     * Creates the login account, then saves the person's data in Firestore.
     * Providers also get a providers/{uid} document with approvalStatus = PENDING.
     */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        area: String,
        address: String,
        role: String,
        providerDetails: ProviderDetails?,
    ) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Could not create account")
        val now = System.currentTimeMillis()

        val user = User(
            id = uid, name = name, email = email, phone = phone,
            role = role, area = area, address = address, createdAt = now,
        )

        // A batch saves both documents together: either both are saved or neither.
        val batch = db.batch()
        batch.set(db.collection("users").document(uid), user)
        if (role == Roles.PROVIDER && providerDetails != null) {
            val provider = Provider(
                id = uid, name = name, phone = phone,
                categoryId = providerDetails.categoryId,
                area = area,
                experienceYears = providerDetails.experienceYears,
                bio = providerDetails.bio,
                approvalStatus = ApprovalStatus.PENDING,
                createdAt = now,
            )
            batch.set(db.collection("providers").document(uid), provider)
        }
        try {
            batch.commit().await()
        } catch (e: Exception) {
            // The login account exists but its data could not be saved (e.g. internet dropped).
            // Delete the half-made account, so the person can simply register again.
            runCatching { result.user?.delete()?.await() }
            auth.signOut()
            throw Exception("Could not finish registration. Check your internet and try again.")
        }
    }

    /**
     * For a provider account whose providers/{uid} document is missing (e.g. the role was
     * changed by hand): creates it now as PENDING, using the name/phone/area from users/{uid}.
     */
    suspend fun createMissingProviderDetails(details: ProviderDetails) {
        val uid = currentUserId ?: throw Exception("Please log in again.")
        val user = UserRepository.getUser(uid) ?: throw Exception("Account data not found.")
        val provider = Provider(
            id = uid, name = user.name, phone = user.phone,
            categoryId = details.categoryId, area = user.area,
            experienceYears = details.experienceYears, bio = details.bio,
            approvalStatus = ApprovalStatus.PENDING,
            createdAt = System.currentTimeMillis(),
        )
        db.collection("providers").document(uid).set(provider).await()
    }

    /**
     * Decides which home screen the logged-in person should see, based on their role.
     * Throws an Exception with a friendly message if they cannot continue (e.g. blocked).
     */
    suspend fun findHomeDestination(): HomeDestination {
        val uid = currentUserId ?: throw Exception("Please log in")
        val user = UserRepository.getUser(uid)

        if (user == null) {
            logout()
            throw Exception("Account data not found. Please register again.")
        }
        if (user.isBlocked) {
            logout()
            throw Exception("Your account is blocked. Please contact the admin.")
        }

        return when (user.role) {
            Roles.ADMIN -> HomeDestination(Routes.ADMIN_DASHBOARD, Roles.ADMIN)
            Roles.PROVIDER -> {
                val provider = UserRepository.getProvider(uid)
                val route = if (provider?.approvalStatus == ApprovalStatus.APPROVED) {
                    Routes.PROVIDER_HOME
                } else {
                    Routes.PENDING_APPROVAL   // PENDING or REJECTED
                }
                HomeDestination(route, Roles.PROVIDER)
            }
            else -> HomeDestination(Routes.CUSTOMER_HOME, Roles.CUSTOMER)
        }
    }

    /** Turns Firebase's technical errors into simple messages for the user. */
    fun friendlyError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthUserCollisionException -> "This email is already registered. Please log in."
        is FirebaseAuthInvalidUserException -> "No account found with this email."
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        else -> e.message ?: "Something went wrong. Please try again."
    }
}
