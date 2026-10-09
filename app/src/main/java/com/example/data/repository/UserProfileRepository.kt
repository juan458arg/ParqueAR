package com.example.data.repository

import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Perfil de Usuario con Cloud Firestore.
 * Colección: "users"
 * Documento: {userId}
 *
 * Mantiene la separación estricta: los campos editables son 'name', 'email', 'phone'.
 * Los campos calculados ('memberTier', 'totalBookings', 'totalHours', 'savedAmount')
 * se preservan y no son sobrescritos durante la edición del perfil.
 */
class UserProfileRepository {

    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getInstance() != null
        } catch (_: Exception) {
            false
        }

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseInitialized) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

    /**
     * Observa en tiempo real el documento del usuario en Firestore.
     */
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val db = firestore
        if (db == null || userId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = db.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Si falla o no hay permisos aún, no romper el flujo
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Obtiene el perfil de usuario una sola vez.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        val db = firestore ?: return Result.failure(
            IllegalStateException("Firestore no está inicializado.")
        )
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(UserProfile::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea el documento inicial de perfil en Firestore para un usuario recién registrado.
     * Cumple con la regla de creación:
     * - name is string
     * - email == request.auth.token.email
     * - totalBookings == 0
     * - totalHours == 0
     * - savedAmount == 0
     */
    suspend fun createInitialProfile(
        userId: String,
        name: String,
        email: String,
        phone: String = ""
    ): Result<UserProfile> {
        val db = firestore ?: return Result.failure(
            IllegalStateException("Firestore no está inicializado.")
        )
        val initialMap = mapOf(
            "id" to userId,
            "name" to name.trim(),
            "email" to email.trim(),
            "phone" to phone.trim(),
            "memberTier" to "ParkSpot Plata",
            "totalBookings" to 0,
            "totalHours" to 0,
            "savedAmount" to 0
        )
        return try {
            db.collection("users").document(userId).set(initialMap).await()
            val initialProfile = UserProfile(
                id = userId,
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                memberTier = "ParkSpot Plata",
                totalBookings = 0,
                totalHours = 0,
                savedAmount = 0.0
            )
            Result.success(initialProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza únicamente los campos 'name' y 'phone' del perfil.
     * La regla de seguridad en Firestore exige:
     * request.resource.data.diff(resource.data).affectedKeys().hasOnly(['name', 'phone'])
     */
    suspend fun updateEditableFields(
        userId: String,
        name: String,
        phone: String
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(
            IllegalStateException("Firestore no está inicializado.")
        )
        val updates = mapOf<String, Any>(
            "name" to name.trim(),
            "phone" to phone.trim()
        )
        return try {
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
