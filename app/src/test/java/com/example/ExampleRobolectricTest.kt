package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ParkingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ParkSpot", appName)
    }

    @Test
    fun `test parking reservation workflow`() {
        val viewModel = ParkingViewModel()

        // El usuario debe estar autenticado para acceder al flujo principal
        viewModel.setAuthenticatedForTesting()
        assertEquals(AppScreen.HOME, viewModel.currentScreen.value)
        val lots = viewModel.parkingLots.value
        assertTrue(lots.isNotEmpty())

        // Select spot
        val targetLot = lots[0]
        viewModel.openDetail(targetLot)
        assertEquals(AppScreen.DETAIL, viewModel.currentScreen.value)
        assertEquals(targetLot.id, viewModel.selectedParkingLot.value.id)

        // Select hours and apply coupon
        viewModel.onTimeRangeChanged(14, 18)
        viewModel.onCouponInputChanged("PARK20")
        viewModel.applyCoupon()
        assertEquals(1500.0, viewModel.appliedDiscount.value, 0.01)

        // Confirm booking
        viewModel.confirmBooking()
        assertEquals(AppScreen.CONFIRMATION, viewModel.currentScreen.value)
        assertNotNull(viewModel.lastConfirmedBooking.value)
        assertEquals(targetLot.name, viewModel.lastConfirmedBooking.value?.parkingLotName)
    }

    @Test
    fun `test email verification timer and validation flow`() {
        val viewModel = ParkingViewModel()

        // Temporizador de reenvío con cooldown de 60 segundos
        assertEquals(60, viewModel.resendTimer.value)
        assertTrue(viewModel.isTimerRunning.value)

        viewModel.decrementTimer()
        assertEquals(59, viewModel.resendTimer.value)

        viewModel.resetTimer()
        assertEquals(60, viewModel.resendTimer.value)

        // Test client-side validation on register
        viewModel.onAuthNameChanged("")
        viewModel.onAuthEmailChanged("invalid-email")
        viewModel.onAuthPasswordChanged("123")
        viewModel.onAuthConfirmPasswordChanged("456")
        viewModel.submitRegister()

        assertNotNull(viewModel.authNameError.value)
        assertNotNull(viewModel.authEmailError.value)
        assertNotNull(viewModel.authPasswordError.value)
        assertNotNull(viewModel.authConfirmPasswordError.value)
    }

    @Test
    fun `test unauthenticated navigation is blocked and logout clears session`() {
        val viewModel = ParkingViewModel()

        // Sin sesión, intentar navegar a pantallas principales es bloqueado
        viewModel.navigateTo(AppScreen.HOME)
        assertTrue(viewModel.currentScreen.value != AppScreen.HOME)

        viewModel.navigateTo(AppScreen.BOOKINGS)
        assertTrue(viewModel.currentScreen.value != AppScreen.BOOKINGS)

        // Cerrar sesión
        viewModel.logout()
        assertEquals(AppScreen.LOGIN, viewModel.currentScreen.value)
    }
}
