package com.example.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Resultado de un registro: el usuario creado y si el mail de verificación salió bien.
 */
data class SignUpResult(
    val user: FirebaseUser,
    val verificationEmailSent: Boolean
)

/**
 * Repositorio de Autenticación con Firebase Auth.
 * Proporciona métodos suspendibles para registro, inicio de sesión,
 * cierre de sesión y traducción de excepciones al español.
 *
 * Incluye verificación defensiva de inicialización de Firebase
 * para evitar crashes si 'google-services.json' aún no fue agregado.
 */
class AuthRepository {

    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getInstance() != null
        } catch (_: Exception) {
            false
        }

    private val auth: FirebaseAuth?
        get() = if (isFirebaseInitialized) {
            try {
                FirebaseAuth.getInstance()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

    /**
     * Flujo que emite el usuario autenticado actual cada vez que cambia el estado de sesión.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val currentAuth = auth
        if (currentAuth == null) {
            trySend(null)
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                trySend(firebaseAuth.currentUser)
            }
            currentAuth.addAuthStateListener(listener)
            trySend(currentAuth.currentUser)
            awaitClose {
                currentAuth.removeAuthStateListener(listener)
            }
        }
    }

    /**
     * Obtiene el usuario actual de forma síncrona si existe.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth?.currentUser
    }

    /**
     * Inicia sesión con correo y contraseña.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        val currentAuth = auth ?: return Result.failure(
            IllegalStateException("Firebase no está inicializado. Descarga y copia 'google-services.json' en la carpeta app/.")
        )
        return try {
            val authResult = currentAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("No se pudo obtener el usuario autenticado."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra un nuevo usuario con correo, contraseña y nombre en Firebase Auth.
     */
    suspend fun signUp(name: String, email: String, password: String): Result<SignUpResult> {
        val currentAuth = auth ?: return Result.failure(
            IllegalStateException("Firebase no está inicializado. Descarga y copia 'google-services.json' en la carpeta app/.")
        )
        return try {
            val authResult = currentAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user
            if (user != null) {
                // Actualizar el displayName en Firebase Auth
                try {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name.trim()
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (_: Exception) {
                    // Si falla la actualización del displayName, no rompemos el registro
                }
                // Enviar enlace de verificación de correo nativo de Firebase
                val emailSent = try {
                    user.sendEmailVerification().await()
                    true
                } catch (_: Exception) {
                    // Si falla (red transitoria, etc.), el usuario puede reenviarlo desde la pantalla de verificación
                    false
                }
                Result.success(SignUpResult(user, emailSent))
            } else {
                Result.failure(Exception("No se pudo crear el usuario en Firebase."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reenvía el correo de verificación nativo de Firebase al usuario actual.
     */
    suspend fun sendVerificationEmail(): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(
            Exception("No hay una sesión de usuario activa.")
        )
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Consulta si el correo del usuario actual está verificado en Firebase Auth.
     */
    fun isEmailVerified(): Boolean {
        return auth?.currentUser?.isEmailVerified == true
    }

    /**
     * Recarga el usuario desde Firebase Auth para refrescar el estado de verificación
     * tras hacer clic en el enlace del correo electrónico.
     *
     * IMPORTANTE: reload() solo actualiza el objeto FirebaseUser local. El ID token que se
     * envía a Firestore sigue con email_verified=false hasta renovarse, y las reglas de
     * Firestore exigen email_verified == true. Por eso, si ya está verificado, forzamos
     * la renovación del token con getIdToken(true).
     */
    suspend fun reloadUser(): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(
            Exception("No hay una sesión de usuario activa.")
        )
        return try {
            user.reload().await()
            if (user.isEmailVerified) {
                user.getIdToken(true).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión activa en Firebase Auth.
     */
    fun signOut() {
        auth?.signOut()
    }

    /**
     * Envía un correo electrónico para restablecer la contraseña.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(
            IllegalStateException("Firebase no está inicializado.")
        )
        return try {
            currentAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el correo electrónico del usuario autenticado.
     */
    suspend fun updateUserEmail(newEmail: String): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(
            Exception("No hay una sesión de usuario activa.")
        )
        return try {
            user.verifyBeforeUpdateEmail(newEmail.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el nombre en Firebase Auth.
     */
    suspend fun updateDisplayName(name: String): Result<Unit> {
        val user = auth?.currentUser ?: return Result.failure(
            Exception("No hay una sesión de usuario activa.")
        )
        return try {
            val profileUpdates = userProfileChangeRequest {
                displayName = name.trim()
            }
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Traduce los errores comunes de Firebase a mensajes amigables en español.
     */
    fun mapFirebaseError(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthUserCollisionException -> {
                "Este correo electrónico ya está registrado. Por favor iniciá sesión."
            }
            is FirebaseAuthWeakPasswordException -> {
                "La contraseña es demasiado débil. Debe tener al menos 6 caracteres."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Credenciales incorrectas. Verificá tu correo electrónico y contraseña."
            }
            is FirebaseAuthInvalidUserException -> {
                "No existe ninguna cuenta asociada a este correo electrónico."
            }
            is FirebaseAuthRecentLoginRequiredException -> {
                "Por seguridad, debés volver a iniciar sesión para realizar este cambio."
            }
            is FirebaseNetworkException -> {
                "Error de conexión. Verificá tu acceso a internet e intentá nuevamente."
            }
            is FirebaseTooManyRequestsException -> {
                "Demasiados intentos fallidos. Por seguridad, aguardá unos minutos."
            }
            is IllegalStateException -> {
                throwable.message ?: "Firebase no está listo. Verifica el archivo google-services.json."
            }
            else -> {
                val msg = throwable.message ?: ""
                when {
                    msg.contains("The email address is already in use", ignoreCase = true) ->
                        "Este correo electrónico ya está registrado. Por favor iniciá sesión."
                    msg.contains("The email address is badly formatted", ignoreCase = true) ->
                        "El formato del correo electrónico no es válido."
                    msg.contains("Password should be at least 6 characters", ignoreCase = true) ->
                        "La contraseña debe tener al menos 6 caracteres."
                    msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                        "Correo electrónico o contraseña incorrectos."
                    msg.contains("user-not-found", ignoreCase = true) ->
                        "No se encontró un usuario con este correo electrónico."
                    msg.contains("network", ignoreCase = true) ->
                        "Error de red. Verificá tu conexión a internet."
                    else -> msg.ifBlank { "Ocurrió un error inesperado. Por favor intentá nuevamente." }
                }
            }
        }
    }
}