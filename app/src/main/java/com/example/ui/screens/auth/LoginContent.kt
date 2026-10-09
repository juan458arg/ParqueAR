package com.example.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * PANTALLA 1: LOGIN (Minimalist Auth Flow)
 * - Logo pequeño arriba centrado
 * - Título "Bienvenido de nuevo"
 * - Input de email
 * - Input de contraseña (con ícono de mostrar/ocultar)
 * - Link "¿Olvidaste tu contraseña?" alineado a la derecha
 * - Botón principal "Iniciar sesión" (color de acento, full-width)
 * - Separador "o"
 * - Botón secundario "Continuar con Google" (outline)
 * - Texto abajo: "¿No tenés cuenta? Registrate" con link destacado
 */
@Composable
fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    emailError: String? = null,
    passwordError: String? = null,
    generalError: String? = null,
    onDismissError: () -> Unit = {},
    isLoading: Boolean = false,
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Logo pequeño arriba centrado
            MinimalistAuthLogo(modifier = Modifier.testTag("login_logo"))

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Título "Bienvenido de nuevo"
            Text(
                text = "Bienvenido de nuevo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ingresá a tu cuenta para continuar",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                textAlign = TextAlign.Center
            )

            // Banner de error general de Firebase
            if (generalError != null) {
                MinimalistErrorBanner(
                    message = generalError,
                    onDismiss = onDismissError,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 3. Input de email
            MinimalistTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                placeholder = "nombre@ejemplo.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                errorMessage = emailError,
                testTag = "login_email_input"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Input de contraseña (con ícono de mostrar/ocultar)
            MinimalistTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                errorMessage = passwordError,
                testTag = "login_password_input"
            )

            // 5. Link "¿Olvidaste tu contraseña?" alineado a la derecha
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AuthAccentBlue,
                    modifier = Modifier
                        .clickable {
                            onForgotPasswordClick()
                            scope.launch {
                                snackbarHostState.showSnackbar("Enlace de restablecimiento enviado a tu email.")
                            }
                        }
                        .testTag("forgot_password_link")
                )
            }

            // 6. Botón principal "Iniciar sesión" (color de acento, full-width)
            MinimalistPrimaryButton(
                text = "Iniciar sesión",
                onClick = onLoginClick,
                enabled = email.isNotBlank() && password.isNotBlank(),
                isLoading = isLoading,
                testTag = "login_submit_button"
            )

            // 7. Separador "o"
            AuthDivider(text = "o")

            // 8. Botón secundario "Continuar con Google" (outline)
            MinimalistGoogleButton(
                onClick = onGoogleClick,
                testTag = "login_google_button"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 9. Texto abajo: "¿No tenés cuenta? Registrate" con link destacado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "¿No tenés cuenta? ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Registrate",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthAccentBlue,
                    modifier = Modifier
                        .clickable { onNavigateToRegister() }
                        .testTag("go_to_register_link")
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
