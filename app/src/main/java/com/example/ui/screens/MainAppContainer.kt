package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ParkingQrCode
import com.example.ui.theme.BookingBlue
import com.example.ui.theme.BookingNavy
import com.example.ui.theme.ParkEmerald
import com.example.ui.theme.ParkEmeraldDark
import com.example.ui.theme.ParkEmeraldLight
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ParkingViewModel

import com.example.ui.viewmodel.AuthState

@Composable
fun MainAppContainer(
    viewModel: ParkingViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            SplashLoadingScreen()
        }

        is AuthState.Unauthenticated -> {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.REGISTER -> com.example.ui.screens.auth.RegisterScreen(viewModel = viewModel)
                    else -> com.example.ui.screens.auth.LoginScreen(viewModel = viewModel)
                }
            }
        }

        is AuthState.Unverified -> {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.ui.screens.auth.VerifyCodeScreen(viewModel = viewModel)
            }
        }

        is AuthState.Authenticated -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    val hideBottomBar = currentScreen == AppScreen.CONFIRMATION

                    if (!hideBottomBar) {
                        // Standard M3 Navigation Bar with proper insets
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            // TAB 1: BUSCAR / HOME
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.HOME || currentScreen == AppScreen.DETAIL || currentScreen == AppScreen.CHECKOUT,
                                onClick = { viewModel.navigateTo(AppScreen.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar"
                                    )
                                },
                                label = { Text("Buscar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_search")
                            )

                            // TAB 2: MIS RESERVAS
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.BOOKINGS,
                                onClick = { viewModel.navigateTo(AppScreen.BOOKINGS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Mis Reservas"
                                    )
                                },
                                label = { Text("Reservas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_bookings")
                            )

                            // TAB 3: PERFIL
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PROFILE,
                                onClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Perfil"
                                    )
                                },
                                label = { Text("Perfil", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_profile")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                        AppScreen.DETAIL -> DetailScreen(viewModel = viewModel)
                        AppScreen.CHECKOUT -> CheckoutScreen(viewModel = viewModel)
                        AppScreen.CONFIRMATION -> ConfirmationScreen(viewModel = viewModel)
                        AppScreen.BOOKINGS -> BookingsScreen(viewModel = viewModel)
                        AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                        AppScreen.LOGIN, AppScreen.REGISTER, AppScreen.VERIFY_CODE -> HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

/**
 * Pantalla de carga y resolución de sesión al iniciar la aplicación.
 * Evita cualquier parpadeo de pantallas protegidas antes de resolver el estado de Auth.
 */
@Composable
private fun SplashLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("splash_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            com.example.ui.screens.auth.MinimalistAuthLogo(
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ParkSpot",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Encontrá tu lugar al mejor precio",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
