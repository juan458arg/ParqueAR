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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PANTALLA 2: REGISTRO (Minimalist Auth Flow)
 * - Logo pequeño arriba centrado
 * - Título "Creá tu cuenta"
 * - Input de nombre completo
 * - Input de email
 * - Input de contraseña
 * - Checkbox "Acepto los términos y condiciones"
 * - Botón principal "Registrarme" (color de acento, full-width)
 * - Texto abajo: "¿Ya tenés cuenta? Iniciá sesión" con link destacado
 */
@Composable
fun RegisterContent(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String = "",
    onConfirmPasswordChange: (String) -> Unit = {},
    nameError: String? = null,
    emailError: String? = null,
    passwordError: String? = null,
    confirmPasswordError: String? = null,
    termsError: String? = null,
    generalError: String? = null,
    onDismissError: () -> Unit = {},
    isLoading: Boolean = false,
    termsAccepted: Boolean,
    onTermsToggle: (Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Logo pequeño arriba centrado
            MinimalistAuthLogo(modifier = Modifier.testTag("register_logo"))

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Título "Creá tu cuenta"
            Text(
                text = "Creá tu cuenta",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Registrate en segundos para reservar estacionamientos",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                textAlign = TextAlign.Center
            )

            // Banner de error general de Firebase
            if (generalError != null) {
                MinimalistErrorBanner(
                    message = generalError,
                    onDismiss = onDismissError,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // 3. Input de nombre completo
            MinimalistTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Nombre completo",
                placeholder = "Ej. Juan Pérez",
                leadingIcon = Icons.Default.Person,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                errorMessage = nameError,
                testTag = "register_name_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Input de email
            MinimalistTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                placeholder = "nombre@ejemplo.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                errorMessage = emailError,
                testTag = "register_email_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Input de contraseña (con ícono de mostrar/ocultar)
            MinimalistTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                placeholder = "Mínimo 6 caracteres",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                errorMessage = passwordError,
                testTag = "register_password_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Input de confirmación de contraseña
            MinimalistTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar contraseña",
                placeholder = "Repetí tu contraseña",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                errorMessage = confirmPasswordError,
                testTag = "register_confirm_password_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Checkbox "Acepto los términos y condiciones"
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTermsToggle(!termsAccepted) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = onTermsToggle,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AuthAccentBlue,
                            uncheckedColor = if (termsError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            checkmarkColor = androidx.compose.ui.graphics.Color.White
                        ),
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("terms_checkbox")
                    )
                    Text(
                        text = "Acepto los términos y condiciones",
                        fontSize = 12.sp,
                        color = if (termsError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                if (termsError != null) {
                    Text(
                        text = termsError,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Botón principal "Registrarme" (color de acento, full-width)
            MinimalistPrimaryButton(
                text = "Registrarme",
                onClick = onRegisterClick,
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                isLoading = isLoading,
                testTag = "register_submit_button"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 8. Texto abajo: "¿Ya tenés cuenta? Iniciá sesión" con link destacado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "¿Ya tenés cuenta? ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Iniciá sesión",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthAccentBlue,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .testTag("go_to_login_link")
                )
            }
        }
    }
}
