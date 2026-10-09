package com.example.ui.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ParkEmerald
import com.example.ui.theme.ParkEmeraldDark
import com.example.ui.theme.ParkEmeraldLight
import kotlinx.coroutines.delay

/**
 * PANTALLA 3: VERIFICACIÓN DE EMAIL POR LINK (Firebase Auth Nativo)
 *
 * Firebase Auth utiliza verificación nativa mediante enlace de correo electrónico.
 * Esta pantalla guía al usuario para:
 * 1. Abrir el correo recibido en su bandeja
 * 2. Hacer clic en el enlace de validación
 * 3. Tocar "Ya verifiqué mi email" para recargar el usuario y acceder a la app
 */
@Composable
fun VerifyCodeContent(
    email: String,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    onDecrementTimer: () -> Unit,
    onResendClick: () -> Unit,
    onCheckVerificationClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    onDismissError: () -> Unit = {},
    onDismissSuccess: () -> Unit = {},
    onBackToLogin: () -> Unit = {},
    // Parámetros de compatibilidad para el showcase de mockups
    codeDigits: List<String> = emptyList(),
    onDigitChanged: (Int, String) -> Unit = { _, _ -> },
    onResetTimer: () -> Unit = {},
    onVerifyClick: () -> Unit = onCheckVerificationClick,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Efecto de temporizador regresivo para el reenvío
    LaunchedEffect(isTimerRunning, timerSeconds) {
        if (isTimerRunning && timerSeconds > 0) {
            delay(1000L)
            onDecrementTimer()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Ícono de email en círculo minimalista con pulso visual
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AuthAccentBlue.copy(alpha = 0.12f))
                    .testTag("verify_email_icon"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = "Verificar Email",
                    tint = AuthAccentBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Título principal
            Text(
                text = "Verificá tu email",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Texto requerido: "Te enviamos un mail a <email>. Verificalo para continuar."
            Text(
                text = "Te enviamos un mail a ${email.ifBlank { "tu correo" }}. Verificalo para continuar.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Banner de error (si el usuario aún no verificó o falló Firebase)
            if (errorMessage != null) {
                MinimalistErrorBanner(
                    message = errorMessage,
                    onDismiss = onDismissError,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Banner de éxito (al reenviar el email)
            if (successMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ParkEmeraldLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ParkEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ParkEmeraldDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage,
                            color = ParkEmeraldDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissSuccess,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = ParkEmeraldDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 4. Tarjeta explicativa con instrucciones paso a paso
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pasos para continuar:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Abrí tu correo electrónico (revisá también spam o correo no deseado).\n2. Hacé clic en el enlace de verificación de Firebase.\n3. Volvé a esta pantalla y tocá el botón 'Ya verifiqué'.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Botón principal: "Ya verifiqué" con Loading State
            MinimalistPrimaryButton(
                text = "Ya verifiqué",
                onClick = onCheckVerificationClick,
                isLoading = isLoading,
                testTag = "check_email_verified_button"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Botón de reenvío con temporizador de 60 segundos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿No recibiste el mail? ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (timerSeconds > 0) {
                    val formatted = String.format("Reenviar mail en 00:%02d", timerSeconds)
                    Text(
                        text = formatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.testTag("resend_timer_label")
                    )
                } else {
                    Text(
                        text = "Reenviar mail",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuthAccentBlue,
                        modifier = Modifier
                            .clickable(enabled = !isLoading) { onResendClick() }
                            .testTag("resend_verification_email_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. Enlace para cerrar sesión / usar otra cuenta
            Text(
                text = "Cerrar sesión / usar otra cuenta",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(enabled = !isLoading) { onBackToLogin() }
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .testTag("verify_back_to_login_button")
            )
        }
    }
}
