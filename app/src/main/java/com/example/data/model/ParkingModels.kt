package com.example.data.model

data class ParkingLot(
    val id: String,
    val name: String,
    val tagLine: String,
    val address: String,
    val zone: String,
    val distanceMeters: Int,
    val hourlyRate: Double,
    val rating: Double,
    val reviewCount: Int,
    val availableSlots: Int,
    val totalSlots: Int,
    val isCovered: Boolean,
    val hasEVCharging: Boolean,
    val hasSecurity24: Boolean,
    val hasValet: Boolean,
    val hasAutomatedBarrier: Boolean,
    val maxVehicleHeight: String,
    val allowedVehicleTypes: List<String>,
    val openingHours: String,
    val securityFeatures: List<String>,
    val mapNormalizedX: Float,
    val mapNormalizedY: Float,
    val latitude: Double = -34.6037,
    val longitude: Double = -58.3816,
    val imageType: String, // "SUBTERRANEO", "VALET", "ROOFTOP", "CENTRAL", "ECO"
    val sampleReviews: List<ParkingReview> = emptyList()
)

data class ParkingReview(
    val author: String,
    val rating: Double,
    val date: String,
    val comment: String
)

enum class BookingStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

data class Booking(
    val id: String = "",
    val parkingLotId: String = "",
    val parkingLotName: String = "",
    val address: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val startHour: Int = 14,
    val endHour: Int = 18,
    val durationHours: Int = 1,
    val vehiclePlate: String = "",
    val vehicleModel: String = "",
    val slotAssigned: String = "",
    val accessCode: String = "",
    val qrData: String = "",
    val subtotal: Double = 0.0,
    val serviceFee: Double = 0.0,
    val discount: Double = 0.0,
    val totalPaid: Double = 0.0,
    val paymentMethod: String = "",
    val status: BookingStatus = BookingStatus.ACTIVE,
    val countdownText: String = ""
)

data class Vehicle(
    val id: String = "",
    val plate: String = "",
    val brandAndModel: String = "",
    val model: String = "",
    val color: String = "",
    val type: String = "Auto",
    val isDefault: Boolean = false
)

data class PaymentMethod(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val type: String = "CARD", // "CARD", "MERCADOPAGO", "GOOGLE_PAY", "CASH"
    val isDefault: Boolean = false
)

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val memberTier: String = "ParkSpot Plata",
    val totalBookings: Int = 0,
    val totalHours: Int = 0,
    val savedAmount: Double = 0.0,
    val id: String = ""
)
