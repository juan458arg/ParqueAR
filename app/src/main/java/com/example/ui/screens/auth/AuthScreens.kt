package com.example.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    // En la pantalla inicial de login no permitimos volver a HOME sin autenticación
    val email by viewModel.authEmail.collectAsState()
    val password by viewModel.authPassword.collectAsState()
    val emailError by viewModel.authEmailError.collectAsState()
    val passwordError by viewModel.authPasswordError.collectAsState()
    val generalError by viewModel.authErrorMessage.collectAsState()
    val successMessage by viewModel.authSuccessMessage.collectAsState()
    val isLoading by viewModel.authLoading.collectAsState()

    var showForgotDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var forgotEmail by androidx.compose.runtime.remember(email) { androidx.compose.runtime.mutableStateOf(email) }
    var forgotFeedback by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LoginContent(
            email = email,
            onEmailChange = { viewModel.onAuthEmailChanged(it) },
            password = password,
            onPasswordChange = { viewModel.onAuthPasswordChanged(it) },
            emailError = emailError,
            passwordError = passwordError,
            generalError = generalError,
            onDismissError = { viewModel.dismissAuthError() },
            isLoading = isLoading,
            onLoginClick = { viewModel.submitLogin() },
            onGoogleClick = { viewModel.submitLogin() },
            onForgotPasswordClick = {
                forgotEmail = email
                forgotFeedback = null
                showForgotDialog = true
            },
            onNavigateToRegister = { viewModel.navigateTo(AppScreen.REGISTER) },
            modifier = Modifier.padding(innerPadding)
        )

        // DIÁLOGO PARA "OLVIDÉ MI CONTRASEÑA"
        if (showForgotDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    if (!isLoading) showForgotDialog = false
                },
                title = {
                    Text(
                        text = "Restablecer contraseña",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Ingresá tu correo electrónico registrado. Te enviaremos un enlace de Firebase para restablecer tu contraseña.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Correo electrónico") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input")
                        )
                        if (forgotFeedback != null) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = forgotFeedback ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            viewModel.sendPasswordReset(forgotEmail) { success, msg ->
                                forgotFeedback = msg
                                if (success) {
                                    // Cerrar diálogo tras breve confirmación
                                    showForgotDialog = false
                                }
                            }
                        },
                        enabled = forgotEmail.isNotBlank() && !isLoading,
                        modifier = Modifier.testTag("send_reset_password_button")
                    ) {
                        Text("Enviar enlace")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showForgotDialog = false },
                        enabled = !isLoading
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.LOGIN)
    }

    val name by viewModel.authName.collectAsState()
    val email by viewModel.authEmail.collectAsState()
    val password by viewModel.authPassword.collectAsState()
    val confirmPassword by viewModel.authConfirmPassword.collectAsState()
    val termsAccepted by viewModel.authTermsAccepted.collectAsState()
    val nameError by viewModel.authNameError.collectAsState()
    val emailError by viewModel.authEmailError.collectAsState()
    val passwordError by viewModel.authPasswordError.collectAsState()
    val confirmPasswordError by viewModel.authConfirmPasswordError.collectAsState()
    val termsError by viewModel.authTermsError.collectAsState()
    val generalError by viewModel.authErrorMessage.collectAsState()
    val isLoading by viewModel.authLoading.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                        modifier = Modifier.testTag("register_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        RegisterContent(
            name = name,
            onNameChange = { viewModel.onAuthNameChanged(it) },
            email = email,
            onEmailChange = { viewModel.onAuthEmailChanged(it) },
            password = password,
            onPasswordChange = { viewModel.onAuthPasswordChanged(it) },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { viewModel.onAuthConfirmPasswordChanged(it) },
            nameError = nameError,
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError,
            termsError = termsError,
            generalError = generalError,
            onDismissError = { viewModel.dismissAuthError() },
            isLoading = isLoading,
            termsAccepted = termsAccepted,
            onTermsToggle = { viewModel.onAuthTermsToggled(it) },
            onRegisterClick = { viewModel.submitRegister() },
            onNavigateToLogin = { viewModel.navigateTo(AppScreen.LOGIN) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    viewModel: ParkingViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.logout()
    }

    // Chequeo automático: cada 3 segundos revisa si el usuario ya verificó el mail
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            viewModel.pollEmailVerification()
        }
    }

    val email by viewModel.authEmail.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val timerSeconds by viewModel.resendTimer.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val isLoading by viewModel.authLoading.collectAsState()
    val generalError by viewModel.authErrorMessage.collectAsState()
    val successMessage by viewModel.authSuccessMessage.collectAsState()

    val displayEmail = email.ifBlank { currentUser?.email ?: "" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("verify_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        VerifyCodeContent(
            email = displayEmail,
            timerSeconds = timerSeconds,
            isTimerRunning = isTimerRunning,
            onDecrementTimer = { viewModel.decrementTimer() },
            onResendClick = { viewModel.resendVerificationEmail() },
            onCheckVerificationClick = { viewModel.checkEmailVerification() },
            isLoading = isLoading,
            errorMessage = generalError,
            successMessage = successMessage,
            onDismissError = { viewModel.dismissAuthError() },
            onDismissSuccess = { viewModel.dismissAuthMessage() },
            onBackToLogin = { viewModel.logout() },
            modifier = Modifier.padding(innerPadding)
        )
    }
}