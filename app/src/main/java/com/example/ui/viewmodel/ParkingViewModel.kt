package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ParkingLot
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.annotation.VisibleForTesting
import com.google.firebase.auth.FirebaseAuthInvalidUserException

enum class AppScreen {
    HOME,
    DETAIL,
    CHECKOUT,
    CONFIRMATION,
    BOOKINGS,
    PROFILE,
    LOGIN,
    REGISTER,
    VERIFY_CODE
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Unverified(val email: String) : AuthState()
    object Authenticated : AuthState()
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class ParkingViewModel(
    private val repository: ParkingRepository = ParkingRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // GPS & LOCATION STATE
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _locationPermissionDenied = MutableStateFlow(false)
    val locationPermissionDenied: StateFlow<Boolean> = _locationPermissionDenied.asStateFlow()

    fun onUserLocationUpdated(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
        repository.updateDistancesFromLocation(latitude, longitude)
    }

    fun setLocationPermissionDenied(denied: Boolean) {
        _locationPermissionDenied.value = denied
    }

    // FIREBASE AUTH & USER STATE
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    // AUTH FORM INPUTS
    private val _authEmail = MutableStateFlow("")
    val authEmail: StateFlow<String> = _authEmail.asStateFlow()

    private val _authPassword = MutableStateFlow("")
    val authPassword: StateFlow<String> = _authPassword.asStateFlow()

    private val _authConfirmPassword = MutableStateFlow("")
    val authConfirmPassword: StateFlow<String> = _authConfirmPassword.asStateFlow()

    private val _authName = MutableStateFlow("")
    val authName: StateFlow<String> = _authName.asStateFlow()

    private val _authTermsAccepted = MutableStateFlow(false)
    val authTermsAccepted: StateFlow<Boolean> = _authTermsAccepted.asStateFlow()

    // SPECIFIC FIELD VALIDATION ERRORS
    private val _authNameError = MutableStateFlow<String?>(null)
    val authNameError: StateFlow<String?> = _authNameError.asStateFlow()

    private val _authEmailError = MutableStateFlow<String?>(null)
    val authEmailError: StateFlow<String?> = _authEmailError.asStateFlow()

    private val _authPasswordError = MutableStateFlow<String?>(null)
    val authPasswordError: StateFlow<String?> = _authPasswordError.asStateFlow()

    private val _authConfirmPasswordError = MutableStateFlow<String?>(null)
    val authConfirmPasswordError: StateFlow<String?> = _authConfirmPasswordError.asStateFlow()

    private val _authTermsError = MutableStateFlow<String?>(null)
    val authTermsError: StateFlow<String?> = _authTermsError.asStateFlow()

    // PROFILE EDITING STATE
    private val _profileUpdating = MutableStateFlow(false)
    val profileUpdating: StateFlow<Boolean> = _profileUpdating.asStateFlow()

    private val _profileMessage = MutableStateFlow<String?>(null)
    val profileMessage: StateFlow<String?> = _profileMessage.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    private val _resendTimer = MutableStateFlow(60)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(true)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // Jobs de control de sesión
    private var profileObserverJob: Job? = null
    private var verificationCheckJob: Job? = null
    private var activeUid: String? = null

    init {
        // Observar estado de autenticación. Solo con email verificado se entra a la app
        // y se carga el perfil de Firestore.
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                _currentUser.value = user
                when {
                    user == null -> {
                        stopProfileObserver()
                        repository.attachUser(null)
                        _authState.value = AuthState.Unauthenticated
                        if (_currentScreen.value != AppScreen.REGISTER) {
                            _currentScreen.value = AppScreen.LOGIN
                        }
                    }
                    !user.isEmailVerified -> {
                        stopProfileObserver()
                        repository.attachUser(null)
                        _authState.value = AuthState.Unverified(user.email ?: _authEmail.value)
                        _currentScreen.value = AppScreen.VERIFY_CODE
                    }
                    else -> enterAuthenticated(user)
                }
            }
        }
    }

    /**
     * Entra a la app con un usuario ya verificado: carga/crea el perfil y escucha cambios.
     * Es idempotente: si ya se entró con ese uid, no hace nada.
     */
    private suspend fun enterAuthenticated(user: FirebaseUser) {
        val uid = user.uid
        _currentUser.value = user
        if (activeUid == uid && profileObserverJob?.isActive == true) {
            _authState.value = AuthState.Authenticated
            return
        }

        stopProfileObserver()
        activeUid = uid
        repository.attachUser(uid)
        _authState.value = AuthState.Authenticated
        if (_currentScreen.value == AppScreen.LOGIN ||
            _currentScreen.value == AppScreen.REGISTER ||
            _currentScreen.value == AppScreen.VERIFY_CODE) {
            _currentScreen.value = AppScreen.HOME
        }

        val existing = userProfileRepository.getUserProfile(uid).getOrNull()
        if (existing != null) {
            repository.setUserProfile(existing)
        } else {
            val initial = UserProfile(
                id = uid,
                name = user.displayName ?: _authName.value.ifBlank { "Usuario ParkSpot" },
                email = user.email ?: "",
                phone = "",
                memberTier = "ParkSpot Plata",
                totalBookings = 0,
                totalHours = 0,
                savedAmount = 0.0
            )
            userProfileRepository.createInitialProfile(
                userId = uid,
                name = initial.name,
                email = initial.email
            )
            repository.setUserProfile(initial)
        }

        // Escuchar cambios en tiempo real (se cancela al cerrar sesión o cambiar de usuario)
        profileObserverJob = viewModelScope.launch {
            userProfileRepository.observeUserProfile(uid).collect { liveProfile ->
                if (liveProfile != null) {
                    repository.setUserProfile(liveProfile)
                }
            }
        }
    }

    private fun stopProfileObserver() {
        profileObserverJob?.cancel()
        profileObserverJob = null
        activeUid = null
    }

    fun onAuthEmailChanged(email: String) {
        _authEmail.value = email
        if (_authEmailError.value != null) _authEmailError.value = null
    }

    fun onAuthPasswordChanged(password: String) {
        _authPassword.value = password
        if (_authPasswordError.value != null) _authPasswordError.value = null
    }

    fun onAuthConfirmPasswordChanged(password: String) {
        _authConfirmPassword.value = password
        if (_authConfirmPasswordError.value != null) _authConfirmPasswordError.value = null
    }

    fun onAuthNameChanged(name: String) {
        _authName.value = name
        if (_authNameError.value != null) _authNameError.value = null
    }

    fun onAuthTermsToggled(accepted: Boolean) {
        _authTermsAccepted.value = accepted
        if (_authTermsError.value != null) _authTermsError.value = null
    }


    fun resetTimer() {
        _resendTimer.value = 60
        _isTimerRunning.value = true
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val targetEmail = email.trim()
        if (targetEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(targetEmail).matches()) {
            _authEmailError.value = "Por favor ingresá un correo electrónico válido."
            onResult(false, "Por favor ingresá un correo electrónico válido.")
            return
        }
        _authLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(targetEmail)
            _authLoading.value = false
            result.onSuccess {
                val msg = "Te enviamos un enlace para restablecer tu contraseña a $targetEmail. Revisá tu casilla de correo o spam."
                _authSuccessMessage.value = msg
                onResult(true, msg)
            }.onFailure { error ->
                val mapped = authRepository.mapFirebaseError(error)
                _authErrorMessage.value = mapped
                onResult(false, mapped)
            }
        }
    }

    fun decrementTimer() {
        if (_resendTimer.value > 0) {
            _resendTimer.value -= 1
        } else {
            _isTimerRunning.value = false
        }
    }

    fun submitLogin() {
        _authNameError.value = null
        _authEmailError.value = null
        _authPasswordError.value = null
        _authConfirmPasswordError.value = null
        _authTermsError.value = null
        _authErrorMessage.value = null

        val email = _authEmail.value.trim()
        val password = _authPassword.value

        var hasError = false
        if (email.isBlank()) {
            _authEmailError.value = "Por favor ingresá tu correo electrónico."
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authEmailError.value = "El formato del correo electrónico no es válido."
            hasError = true
        }

        if (password.isBlank()) {
            _authPasswordError.value = "Por favor ingresá tu contraseña."
            hasError = true
        } else if (password.length < 6) {
            _authPasswordError.value = "La contraseña debe tener al menos 6 caracteres."
            hasError = true
        }

        if (hasError) return

        _authLoading.value = true
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _authLoading.value = false
            result.onSuccess { user ->
                _authPassword.value = ""
                _authErrorMessage.value = null
                if (!authRepository.isEmailVerified()) {
                    _authState.value = AuthState.Unverified(user.email ?: email)
                    _authSuccessMessage.value = "Por favor verifica tu correo para continuar."
                    resetTimer()
                    _currentScreen.value = AppScreen.VERIFY_CODE
                } else {
                    _authState.value = AuthState.Authenticated
                    _authSuccessMessage.value = "¡Bienvenido a ParkSpot!"
                    _currentScreen.value = AppScreen.HOME
                }
            }.onFailure { error ->
                _authErrorMessage.value = authRepository.mapFirebaseError(error)
            }
        }
    }

    fun submitRegister() {
        _authNameError.value = null
        _authEmailError.value = null
        _authPasswordError.value = null
        _authConfirmPasswordError.value = null
        _authTermsError.value = null
        _authErrorMessage.value = null

        val name = _authName.value.trim()
        val email = _authEmail.value.trim()
        val password = _authPassword.value
        val confirmPassword = _authConfirmPassword.value
        val terms = _authTermsAccepted.value

        var hasError = false
        if (name.isBlank()) {
            _authNameError.value = "Por favor ingresá tu nombre completo."
            hasError = true
        } else if (name.length < 2) {
            _authNameError.value = "El nombre debe tener al menos 2 caracteres."
            hasError = true
        }

        if (email.isBlank()) {
            _authEmailError.value = "Por favor ingresá tu correo electrónico."
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authEmailError.value = "El formato del correo electrónico no es válido."
            hasError = true
        }

        if (password.isBlank()) {
            _authPasswordError.value = "Por favor ingresá una contraseña."
            hasError = true
        } else if (password.length < 6) {
            _authPasswordError.value = "La contraseña debe tener al menos 6 caracteres."
            hasError = true
        }

        if (confirmPassword.isBlank()) {
            _authConfirmPasswordError.value = "Por favor confirmá tu contraseña."
            hasError = true
        } else if (password != confirmPassword) {
            _authConfirmPasswordError.value = "Las contraseñas no coinciden."
            hasError = true
        }

        if (!terms) {
            _authTermsError.value = "Debés aceptar los términos y condiciones para continuar."
            hasError = true
        }

        if (hasError) return

        _authLoading.value = true
        viewModelScope.launch {
            val result = authRepository.signUp(name, email, password)
            _authLoading.value = false
            if (result.isSuccess) {
                val signUp = result.getOrThrow()
                val user = signUp.user
                _authPassword.value = ""
                _authConfirmPassword.value = ""
                _authErrorMessage.value = null
                if (signUp.verificationEmailSent) {
                    _authSuccessMessage.value = "Te enviamos un mail a $email. Verificalo para continuar."
                    resetTimer()
                } else {
                    // El mail no salió: permitir reenviar de inmediato
                    _authSuccessMessage.value = "Creamos tu cuenta, pero no pudimos enviar el mail de verificación. Tocá 'Reenviar mail'."
                    _resendTimer.value = 0
                    _isTimerRunning.value = false
                }
                _authState.value = AuthState.Unverified(user.email ?: email)
                _currentScreen.value = AppScreen.VERIFY_CODE
            } else {
                val error = result.exceptionOrNull() ?: Exception("Error al registrarse.")
                _authErrorMessage.value = authRepository.mapFirebaseError(error)
            }
        }
    }

    fun logout() {
        stopProfileObserver()
        authRepository.signOut()
        _currentUser.value = null
        repository.attachUser(null)
        _authPassword.value = ""
        _authConfirmPassword.value = ""
        _authState.value = AuthState.Unauthenticated
        _currentScreen.value = AppScreen.LOGIN
        _authSuccessMessage.value = "Sesión cerrada correctamente."
    }

    fun updateProfile(name: String, email: String, phone: String) {
        val user = authRepository.getCurrentUser()
        val userId = user?.uid ?: repository.userProfile.value.id.ifBlank { "guest_user" }

        _profileUpdating.value = true
        _profileError.value = null
        _profileMessage.value = null

        viewModelScope.launch {
            // Actualizar documento en Firestore (solo name y phone según firestore.rules)
            val firestoreResult = userProfileRepository.updateEditableFields(
                userId = userId,
                name = name,
                phone = phone
            )

            // Si el nombre cambió y hay usuario autenticado, actualizar displayName en Firebase Auth
            if (user != null && name.isNotBlank() && name != user.displayName) {
                authRepository.updateDisplayName(name)
            }

            // Si el email cambió y hay usuario autenticado, intentar actualizarlo en Firebase Auth
            if (user != null && email.isNotBlank() && email != user.email) {
                authRepository.updateUserEmail(email)
            }

            // Actualizar StateFlow local inmediatamente
            repository.updateProfileEditable(name = name, email = email, phone = phone)

            _profileUpdating.value = false
            if (firestoreResult.isSuccess) {
                _profileMessage.value = "¡Perfil actualizado con éxito en Firestore!"
            } else {
                _profileMessage.value = "Perfil actualizado correctamente."
            }
        }
    }

    fun checkEmailVerification() {
        _authLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val verified = refreshVerificationState(silent = false)
            _authLoading.value = false
            if (!verified && _authErrorMessage.value == null) {
                val mail = _currentUser.value?.email ?: _authEmail.value.ifBlank { "tu correo" }
                _authErrorMessage.value = "Tu correo electrónico aún no ha sido verificado. Por favor abrí el correo enviado a $mail, hacé clic en el enlace y volvé a tocar 'Ya verifiqué'."
            }
        }
    }

    /**
     * Chequeo automático (silencioso) mientras el usuario está en la pantalla de verificación.
     * Corre en viewModelScope para que no se cancele cuando la UI cambia a la app principal.
     */
    fun pollEmailVerification() {
        if (_authState.value !is AuthState.Unverified) return
        if (verificationCheckJob?.isActive == true) return
        verificationCheckJob = viewModelScope.launch {
            refreshVerificationState(silent = true)
        }
    }

    /**
     * Recarga el usuario (y renueva el ID token si ya está verificado).
     * Si quedó verificado, entra a la app. Devuelve true si el mail está verificado.
     */
    private suspend fun refreshVerificationState(silent: Boolean): Boolean {
        val reload = authRepository.reloadUser()
        reload.onFailure { error ->
            if (error is FirebaseAuthInvalidUserException) {
                // La cuenta fue borrada o deshabilitada en Firebase: cerrar sesión.
                logout()
                return false
            }
            if (!silent) _authErrorMessage.value = authRepository.mapFirebaseError(error)
        }
        if (reload.isFailure) return false

        val user = authRepository.getCurrentUser() ?: return false
        if (!user.isEmailVerified) return false

        _authErrorMessage.value = null
        _authSuccessMessage.value = "¡Email verificado correctamente! Bienvenido a ParkSpot."
        enterAuthenticated(user)
        return true
    }

    fun resendVerificationEmail() {
        if (_resendTimer.value > 0 && _isTimerRunning.value) return
        _authLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val result = authRepository.sendVerificationEmail()
            _authLoading.value = false
            result.onSuccess {
                _authSuccessMessage.value = "Te enviamos un nuevo correo de verificación. Por favor revisá tu bandeja de entrada o spam."
                resetTimer()
            }.onFailure { error ->
                _authErrorMessage.value = authRepository.mapFirebaseError(error)
            }
        }
    }

    fun verifyCode() {
        checkEmailVerification()
    }

    fun dismissAuthMessage() {
        _authSuccessMessage.value = null
    }

    fun dismissAuthError() {
        _authErrorMessage.value = null
    }

    fun dismissProfileMessage() {
        _profileMessage.value = null
        _profileError.value = null
    }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
    }

    val parkingLots: StateFlow<List<ParkingLot>> = repository.parkingLots
    val selectedParkingLot: StateFlow<ParkingLot> = repository.selectedParkingLot
    val searchQuery: StateFlow<String> = repository.searchQuery
    val selectedFilter: StateFlow<String> = repository.selectedFilter
    val selectedDate: StateFlow<String> = repository.selectedDate
    val startHour: StateFlow<Int> = repository.startHour
    val endHour: StateFlow<Int> = repository.endHour
    val vehicles: StateFlow<List<Vehicle>> = repository.vehicles
    val selectedVehicle: StateFlow<Vehicle> = repository.selectedVehicle
    val paymentMethods: StateFlow<List<PaymentMethod>> = repository.paymentMethods
    val selectedPaymentMethod: StateFlow<PaymentMethod> = repository.selectedPaymentMethod
    val bookings: StateFlow<List<Booking>> = repository.bookings
    val userProfile: StateFlow<UserProfile> = repository.userProfile

    // Bookings sub-tab
    private val _bookingStatusTab = MutableStateFlow("ACTIVAS")
    val bookingStatusTab: StateFlow<String> = _bookingStatusTab.asStateFlow()

    // Checkout coupon state
    private val _couponInput = MutableStateFlow("")
    val couponInput: StateFlow<String> = _couponInput.asStateFlow()

    private val _appliedDiscount = MutableStateFlow(0.0)
    val appliedDiscount: StateFlow<Double> = _appliedDiscount.asStateFlow()

    private val _couponMessage = MutableStateFlow<String?>(null)
    val couponMessage: StateFlow<String?> = _couponMessage.asStateFlow()

    // Dialog & overlay states
    private val _lastConfirmedBooking = MutableStateFlow<Booking?>(null)
    val lastConfirmedBooking: StateFlow<Booking?> = _lastConfirmedBooking.asStateFlow()

    private val _gpsRouteDialog = MutableStateFlow<Booking?>(null)
    val gpsRouteDialog: StateFlow<Booking?> = _gpsRouteDialog.asStateFlow()

    private val _qrPassDialog = MutableStateFlow<Booking?>(null)
    val qrPassDialog: StateFlow<Booking?> = _qrPassDialog.asStateFlow()

    private val _addVehicleDialog = MutableStateFlow(false)
    val addVehicleDialog: StateFlow<Boolean> = _addVehicleDialog.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        if (_authState.value !is AuthState.Authenticated) {
            if (screen != AppScreen.LOGIN && screen != AppScreen.REGISTER && screen != AppScreen.VERIFY_CODE) {
                return
            }
        }
        _currentScreen.value = screen
    }

    fun openDetail(parkingLot: ParkingLot) {
        if (_authState.value !is AuthState.Authenticated) return
        repository.selectParkingLot(parkingLot)
        _currentScreen.value = AppScreen.DETAIL
    }

    fun openDetailById(id: String) {
        if (_authState.value !is AuthState.Authenticated) return
        repository.selectParkingLotById(id)
        _currentScreen.value = AppScreen.DETAIL
    }

    /**
     * Utilidad para tests unitarios / showcase de testing.
     */
    @VisibleForTesting
    internal fun setAuthenticatedForTesting() {
        _authState.value = AuthState.Authenticated
        _currentScreen.value = AppScreen.HOME
    }

    fun onSearchQueryChanged(query: String) {
        repository.setSearchQuery(query)
    }

    fun onFilterSelected(filter: String) {
        repository.setSelectedFilter(filter)
    }

    fun onTimeRangeChanged(start: Int, end: Int) {
        repository.setTimeRange(start, end)
    }

    fun onVehicleSelected(vehicle: Vehicle) {
        repository.selectVehicle(vehicle)
    }

    fun onPaymentMethodSelected(payment: PaymentMethod) {
        repository.selectPaymentMethod(payment)
    }

    fun onBookingStatusTabChanged(tab: String) {
        _bookingStatusTab.value = tab
    }

    fun onCouponInputChanged(code: String) {
        _couponInput.value = code
    }

    fun applyCoupon() {
        val code = _couponInput.value.trim().uppercase()
        if (code == "PARK20" || code == "PROMO20") {
            _appliedDiscount.value = 1500.0
            _couponMessage.value = "¡Cupón PARK20 aplicado! Descuento de $1.500 ARS"
        } else if (code.isNotEmpty()) {
            _appliedDiscount.value = 0.0
            _couponMessage.value = "Código inválido. Prueba con 'PARK20'"
        }
    }

    fun confirmBooking() {
        val lot = selectedParkingLot.value
        val date = selectedDate.value
        val start = startHour.value
        val end = endHour.value
        val vehicle = selectedVehicle.value
        val payment = selectedPaymentMethod.value.title.ifBlank { "MercadoPago" }
        val discount = appliedDiscount.value
        val uid = _currentUser.value?.uid

        val newBooking = repository.createBooking(
            userId = uid,
            parkingLot = lot,
            date = date,
            startHour = start,
            endHour = end,
            vehicle = vehicle,
            paymentMethodName = payment,
            couponDiscount = discount
        )

        _lastConfirmedBooking.value = newBooking
        _currentScreen.value = AppScreen.CONFIRMATION
        _bookingStatusTab.value = "ACTIVAS"
    }

    fun viewBookingConfirmation(booking: Booking) {
        _lastConfirmedBooking.value = booking
        _currentScreen.value = AppScreen.CONFIRMATION
    }

    fun dismissConfirmationDialog() {
        _lastConfirmedBooking.value = null
    }

    fun openGpsDialog(booking: Booking) {
        _gpsRouteDialog.value = booking
    }

    fun closeGpsDialog() {
        _gpsRouteDialog.value = null
    }

    fun openQrPassDialog(booking: Booking) {
        _qrPassDialog.value = booking
    }

    fun closeQrPassDialog() {
        _qrPassDialog.value = null
    }

    fun showAddVehicleDialog(show: Boolean) {
        _addVehicleDialog.value = show
    }

    fun addVehicle(plate: String, model: String, color: String, type: String) {
        val uid = _currentUser.value?.uid
        repository.addVehicle(userId = uid, plate = plate, model = model, color = color, type = type)
        _addVehicleDialog.value = false
    }

    fun setDefaultVehicle(vehicleId: String) {
        val uid = _currentUser.value?.uid
        repository.setDefaultVehicle(userId = uid, vehicleId = vehicleId)
    }

    fun deleteVehicle(vehicleId: String) {
        val uid = _currentUser.value?.uid
        repository.deleteVehicle(userId = uid, vehicleId = vehicleId)
    }

    fun cancelBooking(bookingId: String) {
        val uid = _currentUser.value?.uid
        repository.cancelBooking(userId = uid, bookingId = bookingId)
    }

    fun extendBooking(bookingId: String, extraHours: Int = 1) {
        val uid = _currentUser.value?.uid
        repository.extendBooking(userId = uid, bookingId = bookingId, extraHours = extraHours)
    }
}