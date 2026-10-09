package com.example.data.repository

import android.location.Location
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ParkingLot
import com.example.data.model.ParkingReview
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ParkingRepository {

    private val isFirebaseInitialized: Boolean
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

    private var vehiclesListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null
    private var paymentMethodsListener: ListenerRegistration? = null
    private var currentUserId: String? = null

    // initialParkingLots - Estacionamientos en Ciudad de Córdoba, Provincia de Córdoba, Argentina (Precios en ARS)
    private val initialParkingLots = listOf(
        ParkingLot(
            id = "park_cba_1",
            name = "Parking Patio Olmos Premium",
            tagLine = "Subterráneo con acceso directo al Shopping y Peatonal",
            address = "Av. Vélez Sarsfield 361, Centro",
            zone = "Nueva Córdoba / Centro",
            distanceMeters = 300,
            hourlyRate = 2800.0,
            rating = 4.9,
            reviewCount = 412,
            availableSlots = 24,
            totalSlots = 120,
            isCovered = true,
            hasEVCharging = true,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.10 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Moto", "Camioneta"),
            openingHours = "Abierto 24 Horas",
            securityFeatures = listOf("Cámaras CCTV 4K", "Guardia de seguridad 24/7", "Barrera automática QR", "Póliza contra todo riesgo"),
            mapNormalizedX = 0.48f,
            mapNormalizedY = 0.45f,
            latitude = -31.4196,
            longitude = -64.1882,
            imageType = "SUBTERRANEO",
            sampleReviews = listOf(
                ParkingReview("Facundo Romero", 5.0, "Hace 2 días", "Ubicación inmejorable en pleno centro de Córdoba. La barrera abrió al instante con el QR."),
                ParkingReview("Camila Benítez", 4.9, "Hace 5 días", "Muy seguro para dejar el auto mientras comprás en el Patio Olmos o vas al teatro.")
            )
        ),
        ParkingLot(
            id = "park_cba_2",
            name = "Estacionamiento Plaza San Martín",
            tagLine = "A metros de la Catedral y Manzana Jesuítica",
            address = "San Jerónimo 165, Casco Histórico",
            zone = "Centro",
            distanceMeters = 450,
            hourlyRate = 2400.0,
            rating = 4.8,
            reviewCount = 360,
            availableSlots = 18,
            totalSlots = 90,
            isCovered = true,
            hasEVCharging = false,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.20 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Moto"),
            openingHours = "06:00 - 00:00",
            securityFeatures = listOf("Monitoreo por cámaras HD", "Acceso biométrico y PIN", "Seguro La Segunda incluido"),
            mapNormalizedX = 0.52f,
            mapNormalizedY = 0.41f,
            latitude = -31.4168,
            longitude = -64.1832,
            imageType = "CENTRAL",
            sampleReviews = listOf(
                ParkingReview("Ignacio Álvarez", 5.0, "Ayer", "Súper práctico para trámites en el centro y bancos."),
                ParkingReview("Lucía Toledo", 4.7, "Hace 3 días", "Plazas cómodas y personal muy atento.")
            )
        ),
        ParkingLot(
            id = "park_cba_3",
            name = "EcoPark Buen Pastor & San Lorenzo",
            tagLine = "Estacionamiento sustentable en el corazón joven de la ciudad",
            address = "San Lorenzo 130, Nueva Córdoba",
            zone = "Nueva Córdoba",
            distanceMeters = 650,
            hourlyRate = 2600.0,
            rating = 4.95,
            reviewCount = 520,
            availableSlots = 15,
            totalSlots = 80,
            isCovered = true,
            hasEVCharging = true,
            hasSecurity24 = true,
            hasValet = true,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.30 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Sedán", "Moto"),
            openingHours = "Abierto 24 Horas",
            securityFeatures = listOf("Cargador ultra rápido EV", "Valet parking opcional", "Control perimetral térmico"),
            mapNormalizedX = 0.55f,
            mapNormalizedY = 0.52f,
            latitude = -31.4255,
            longitude = -64.1878,
            imageType = "ECO",
            sampleReviews = listOf(
                ParkingReview("Gonzalo Ferreyra", 5.0, "Hace 1 día", "Excelente poder cargar el auto eléctrico a metros del Paseo del Buen Pastor."),
                ParkingReview("Mariana S.", 5.0, "Hace 1 semana", "La mejor opción para salir a cenar por Nueva Córdoba sin dar vueltas.")
            )
        ),
        ParkingLot(
            id = "park_cba_4",
            name = "Garage Güemes Bohemia & Arte",
            tagLine = "Seguridad 24hs a pasos de ferias artesanales y bares",
            address = "Belgrano 745, Barrio Güemes",
            zone = "Güemes",
            distanceMeters = 850,
            hourlyRate = 2200.0,
            rating = 4.7,
            reviewCount = 295,
            availableSlots = 20,
            totalSlots = 70,
            isCovered = true,
            hasEVCharging = false,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.10 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Moto"),
            openingHours = "Abierto 24 Horas",
            securityFeatures = listOf("Lectura automática de patente LPR", "Vigilancia privada nocturna", "Seguro contra granizo y choque"),
            mapNormalizedX = 0.44f,
            mapNormalizedY = 0.55f,
            latitude = -31.4238,
            longitude = -64.1920,
            imageType = "CENTRAL",
            sampleReviews = listOf(
                ParkingReview("Matías Peralta", 4.8, "Hace 4 días", "Perfecto para el fin de semana en Güemes. Entrás sin demoras."),
                ParkingReview("Valentina D.", 4.6, "Hace 2 semanas", "Buena iluminación y pasillos anchos.")
            )
        ),
        ParkingLot(
            id = "park_cba_5",
            name = "ParkSpot Ciudad Universitaria UNC",
            tagLine = "Tarifa accesible para comunidad académica y profesionales",
            address = "Av. Haya de la Torre 450, Pabellón Argentina",
            zone = "Ciudad Universitaria",
            distanceMeters = 1200,
            hourlyRate = 1600.0,
            rating = 4.6,
            reviewCount = 185,
            availableSlots = 45,
            totalSlots = 200,
            isCovered = false,
            hasEVCharging = true,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "Sin límite",
            allowedVehicleTypes = listOf("Auto", "SUV", "Camioneta", "Moto", "Bicicleta"),
            openingHours = "06:30 - 23:00",
            securityFeatures = listOf("Predio cerrado con guardias", "Monitoreo universitario", "Luces LED solar"),
            mapNormalizedX = 0.47f,
            mapNormalizedY = 0.68f,
            latitude = -31.4358,
            longitude = -64.1895,
            imageType = "ROOFTOP",
            sampleReviews = listOf(
                ParkingReview("Agustín Molina", 4.7, "Hace 3 días", "La mejor tarifa por hora para dejar el auto mientras cursás o rendís."),
                ParkingReview("Sofía Navarro", 4.5, "Hace 5 días", "Muchas plazas libres siempre.")
            )
        ),
        ParkingLot(
            id = "park_cba_6",
            name = "Parking La Cañada & Bv. San Juan",
            tagLine = "Acceso estratégico sobre La Cañada con rampas amplias",
            address = "Bv. San Juan 520, Centro",
            zone = "Centro / Cañada",
            distanceMeters = 500,
            hourlyRate = 2500.0,
            rating = 4.8,
            reviewCount = 330,
            availableSlots = 28,
            totalSlots = 110,
            isCovered = true,
            hasEVCharging = false,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.15 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Camioneta", "Moto"),
            openingHours = "Abierto 24 Horas",
            securityFeatures = listOf("Control de acceso con molinetes", "CCTV perimetral", "Cabina de asistencia permanente"),
            mapNormalizedX = 0.42f,
            mapNormalizedY = 0.44f,
            latitude = -31.4182,
            longitude = -64.1928,
            imageType = "SUBTERRANEO",
            sampleReviews = listOf(
                ParkingReview("Esteban Quiroga", 5.0, "Hace 2 días", "Ubicación clave cerca de tribunales y shopping."),
                ParkingReview("Julieta F.", 4.7, "Hace 1 semana", "Buen servicio y precio razonable.")
            )
        ),
        ParkingLot(
            id = "park_cba_7",
            name = "General Paz GastroPark & Valet",
            tagLine = "Exclusivo valet parking en el polo culinario de General Paz",
            address = "24 de Septiembre 1040, General Paz",
            zone = "General Paz",
            distanceMeters = 1400,
            hourlyRate = 3000.0,
            rating = 4.9,
            reviewCount = 275,
            availableSlots = 16,
            totalSlots = 65,
            isCovered = true,
            hasEVCharging = true,
            hasSecurity24 = true,
            hasValet = true,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.35 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Sedán de Lujo"),
            openingHours = "11:00 - 03:00",
            securityFeatures = listOf("Valet con choferes certificados", "Cámaras 4K con IA", "Lavado ecológico opcional"),
            mapNormalizedX = 0.68f,
            mapNormalizedY = 0.38f,
            latitude = -31.4145,
            longitude = -64.1680,
            imageType = "VALET",
            sampleReviews = listOf(
                ParkingReview("Patricio Domínguez", 5.0, "Hace 3 días", "Dejé la camioneta con el valet y cené tranquilo en General Paz."),
                ParkingReview("Verónica Paz", 4.9, "Hace 6 días", "Excelente nivel de atención y seguridad.")
            )
        ),
        ParkingLot(
            id = "park_cba_8",
            name = "Costanera Plaza de la Música",
            tagLine = "Gran capacidad frente al Río Suquía y Plaza de la Música",
            address = "Costanera Norte y Mendoza, Alberdi",
            zone = "Alberdi",
            distanceMeters = 1600,
            hourlyRate = 2000.0,
            rating = 4.7,
            reviewCount = 210,
            availableSlots = 60,
            totalSlots = 250,
            isCovered = false,
            hasEVCharging = false,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "Sin límite",
            allowedVehicleTypes = listOf("Auto", "SUV", "Camioneta", "Furgón"),
            openingHours = "07:00 - 02:00",
            securityFeatures = listOf("Vigilancia reforzada para eventos", "Iluminación de alta potencia", "Cámaras domo 360°"),
            mapNormalizedX = 0.38f,
            mapNormalizedY = 0.32f,
            latitude = -31.4082,
            longitude = -64.1952,
            imageType = "ROOFTOP",
            sampleReviews = listOf(
                ParkingReview("Rodrigo Bustos", 4.8, "Hace 1 semana", "Ideal cuando hay recitales o eventos en Plaza de la Música."),
                ParkingReview("Federico H.", 4.6, "Hace 2 semanas", "Fácil para maniobrar con camioneta con enganche.")
            )
        ),
        ParkingLot(
            id = "park_cba_9",
            name = "Alta Córdoba Express Rivadavia",
            tagLine = "Frente a la Plaza Rivadavia y zona comercial Fragueiro",
            address = "Mariano Fragueiro 2040, Alta Córdoba",
            zone = "Alta Córdoba",
            distanceMeters = 2100,
            hourlyRate = 1900.0,
            rating = 4.65,
            reviewCount = 155,
            availableSlots = 22,
            totalSlots = 85,
            isCovered = true,
            hasEVCharging = false,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.10 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Moto"),
            openingHours = "07:00 - 23:00",
            securityFeatures = listOf("Lector QR en portón", "Monitoreo 24/7", "Alarma perimetral conectada"),
            mapNormalizedX = 0.50f,
            mapNormalizedY = 0.22f,
            latitude = -31.3985,
            longitude = -64.1842,
            imageType = "CENTRAL",
            sampleReviews = listOf(
                ParkingReview("Marcelo Sosa", 4.7, "Hace 4 días", "Muy cómodo para trámites en Alta Córdoba."),
                ParkingReview("Daniela R.", 4.6, "Hace 10 días", "Rápido y limpio.")
            )
        ),
        ParkingLot(
            id = "park_cba_10",
            name = "Cerro Premium Park & Detailing",
            tagLine = "Estacionamiento exclusivo en la zona norte de Córdoba",
            address = "Av. Rafael Núñez 4250, Cerro de las Rosas",
            zone = "Cerro de las Rosas",
            distanceMeters = 5200,
            hourlyRate = 3500.0,
            rating = 4.95,
            reviewCount = 380,
            availableSlots = 14,
            totalSlots = 75,
            isCovered = true,
            hasEVCharging = true,
            hasSecurity24 = true,
            hasValet = true,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.40 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Camioneta", "Sedán de Lujo"),
            openingHours = "Abierto 24 Horas",
            securityFeatures = listOf("Carga rápida EV 50kW", "Valet y detailing", "Monitoreo perimetral térmico"),
            mapNormalizedX = 0.22f,
            mapNormalizedY = 0.15f,
            latitude = -31.3718,
            longitude = -64.2385,
            imageType = "VALET",
            sampleReviews = listOf(
                ParkingReview("Joaquín Menéndez", 5.0, "Hace 2 días", "El mejor estacionamiento del Cerro. Dejé el auto impecable con el lavado."),
                ParkingReview("Florencia V.", 5.0, "Hace 4 días", "Seguridad absoluta y atención de primer nivel.")
            )
        ),
        ParkingLot(
            id = "park_cba_11",
            name = "Terminal Córdoba Centro Express",
            tagLine = "A 50m de la Nueva Terminal de Ómnibus T1 y T2",
            address = "Bv. Juan Domingo Perón 390, Centro",
            zone = "Centro / Terminal",
            distanceMeters = 750,
            hourlyRate = 2200.0,
            rating = 4.7,
            reviewCount = 230,
            availableSlots = 32,
            totalSlots = 130,
            isCovered = true,
            hasEVCharging = false,
            hasSecurity24 = true,
            hasValet = false,
            hasAutomatedBarrier = true,
            maxVehicleHeight = "2.20 m",
            allowedVehicleTypes = listOf("Auto", "SUV", "Camioneta", "Moto"),
            openingHours = "Abierto 24 Horas",
            securityFeatures = listOf("Vigilancia 24hs", "Cámaras con infrarrojo", "Control automático de ticket y QR"),
            mapNormalizedX = 0.62f,
            mapNormalizedY = 0.48f,
            latitude = -31.4232,
            longitude = -64.1765,
            imageType = "SUBTERRANEO",
            sampleReviews = listOf(
                ParkingReview("Guillermo Prado", 4.8, "Ayer", "Práctico para dejar el auto antes de viajar o recibir a alguien."),
                ParkingReview("Romina C.", 4.6, "Hace 5 días", "Cómodo y seguro a cualquier hora de la noche.")
            )
        )
    )

    private val _parkingLots = MutableStateFlow(initialParkingLots)
    val parkingLots: StateFlow<List<ParkingLot>> = _parkingLots.asStateFlow()

    private val _selectedParkingLot = MutableStateFlow(initialParkingLots[0])
    val selectedParkingLot: StateFlow<ParkingLot> = _selectedParkingLot.asStateFlow()

    // Search and Time Slot State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Todos")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow("Hoy, 28 Sept")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _startHour = MutableStateFlow(14)
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _endHour = MutableStateFlow(18)
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    // Vehículos reales (Firestore: users/{uid}/vehicles) - Sin mocks iniciales
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _selectedVehicle = MutableStateFlow(Vehicle())
    val selectedVehicle: StateFlow<Vehicle> = _selectedVehicle.asStateFlow()

    // Métodos de pago reales (Firestore: users/{uid}/paymentMethods)
    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod())
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

    // Reservas reales (Firestore: users/{uid}/bookings) - Sin mocks iniciales
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    // Perfil
    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = "Usuario ParkSpot",
            email = "",
            phone = "",
            memberTier = "ParkSpot Plata",
            totalBookings = 0,
            totalHours = 0,
            savedAmount = 0.0
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun setUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    fun updateProfileEditable(name: String, email: String, phone: String) {
        _userProfile.update { current ->
            current.copy(
                name = name,
                email = email,
                phone = phone
            )
        }
    }

    /**
     * Vincula el repositorio con el usuario autenticado en Firebase.
     * Escucha en tiempo real las subcolecciones:
     * - users/{uid}/vehicles
     * - users/{uid}/bookings
     * - users/{uid}/paymentMethods
     */
    fun attachUser(userId: String?) {
        if (currentUserId == userId && userId != null) return

        vehiclesListener?.remove()
        bookingsListener?.remove()
        paymentMethodsListener?.remove()

        currentUserId = userId

        if (userId.isNullOrBlank()) {
            _vehicles.value = emptyList()
            _selectedVehicle.value = Vehicle()
            _bookings.value = emptyList()
            _paymentMethods.value = emptyList()
            _selectedPaymentMethod.value = PaymentMethod()
            return
        }

        val db = firestore ?: return

        // 1. Escuchar vehículos en tiempo real: users/{userId}/vehicles
        vehiclesListener = db.collection("users").document(userId)
            .collection("vehicles")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Vehicle::class.java)?.copy(id = doc.id)
                }
                _vehicles.value = list
                val defVeh = list.find { it.isDefault } ?: list.firstOrNull() ?: Vehicle()
                _selectedVehicle.value = defVeh
            }

        // 2. Escuchar reservas en tiempo real: users/{userId}/bookings
        bookingsListener = db.collection("users").document(userId)
            .collection("bookings")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                val sorted = list.sortedWith(
                    compareByDescending<Booking> { it.status == BookingStatus.ACTIVE }
                        .thenByDescending { it.id }
                )
                _bookings.value = sorted
            }

        // 3. Escuchar métodos de pago en tiempo real: users/{userId}/paymentMethods
        paymentMethodsListener = db.collection("users").document(userId)
            .collection("paymentMethods")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
                }
                if (list.isEmpty()) {
                    seedDefaultPaymentMethods(userId)
                } else {
                    _paymentMethods.value = list
                    val defPay = list.find { it.isDefault } ?: list.firstOrNull() ?: PaymentMethod()
                    _selectedPaymentMethod.value = defPay
                }
            }
    }

    private fun seedDefaultPaymentMethods(userId: String) {
        val db = firestore ?: return
        val defaults = listOf(
            PaymentMethod(id = "pay_mp", title = "MercadoPago", subtitle = "Cuenta vinculada • Pago en 1 toque", type = "MERCADOPAGO", isDefault = true),
            PaymentMethod(id = "pay_card", title = "Tarjeta de Crédito / Débito", subtitle = "Visa / Mastercard", type = "CARD", isDefault = false),
            PaymentMethod(id = "pay_cash", title = "Efectivo / Al llegar", subtitle = "Pagar en cabina de acceso", type = "CASH", isDefault = false)
        )
        val batch = db.batch()
        defaults.forEach { pm ->
            val ref = db.collection("users").document(userId).collection("paymentMethods").document(pm.id)
            batch.set(ref, pm, SetOptions.merge())
        }
        batch.commit()
    }

    /**
     * Recalcula la distancia en metros desde la ubicación GPS del usuario a cada estacionamiento.
     */
    fun updateDistancesFromLocation(userLat: Double, userLon: Double) {
        val results = FloatArray(1)
        val updatedLots = initialParkingLots.map { lot ->
            Location.distanceBetween(userLat, userLon, lot.latitude, lot.longitude, results)
            lot.copy(distanceMeters = results[0].toInt())
        }.sortedBy { it.distanceMeters }

        _parkingLots.value = updatedLots
    }

    fun selectParkingLot(lot: ParkingLot) {
        _selectedParkingLot.value = lot
    }

    fun selectParkingLotById(id: String) {
        _parkingLots.value.find { it.id == id }?.let {
            _selectedParkingLot.value = it
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterParkingLots()
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
        filterParkingLots()
    }

    private fun filterParkingLots() {
        val query = _searchQuery.value.trim().lowercase()
        val filter = _selectedFilter.value

        val filtered = initialParkingLots.filter { lot ->
            val matchesQuery = query.isEmpty() ||
                    lot.name.lowercase().contains(query) ||
                    lot.address.lowercase().contains(query) ||
                    lot.zone.lowercase().contains(query)

            val matchesFilter = when (filter) {
                "Techado" -> lot.isCovered
                "Carga EV" -> lot.hasEVCharging
                "Seguridad 24/7" -> lot.hasSecurity24
                "Valet" -> lot.hasValet
                "Mejor precio" -> lot.hourlyRate <= 2200.0
                else -> true
            }

            matchesQuery && matchesFilter
        }
        _parkingLots.value = filtered
    }

    fun setTimeRange(start: Int, end: Int) {
        _startHour.value = start
        _endHour.value = if (end > start) end else start + 1
    }

    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
    }

    fun selectPaymentMethod(payment: PaymentMethod) {
        _selectedPaymentMethod.value = payment
    }

    fun addVehicle(userId: String? = currentUserId, plate: String, model: String, color: String, type: String) {
        val newId = "veh_${UUID.randomUUID().toString().take(8)}"
        val isFirst = _vehicles.value.isEmpty()
        val newVehicle = Vehicle(
            id = newId,
            plate = plate.uppercase().trim(),
            brandAndModel = model.trim(),
            model = model.trim(),
            color = color.trim(),
            type = type,
            isDefault = isFirst
        )

        _vehicles.update { it + newVehicle }
        _selectedVehicle.value = newVehicle

        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            db.collection("users").document(uid)
                .collection("vehicles").document(newId)
                .set(newVehicle)
        }
    }

    fun setDefaultVehicle(userId: String? = currentUserId, vehicleId: String) {
        _vehicles.update { list ->
            list.map { it.copy(isDefault = it.id == vehicleId) }
        }
        _vehicles.value.find { it.id == vehicleId }?.let {
            _selectedVehicle.value = it
        }

        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            val coll = db.collection("users").document(uid).collection("vehicles")
            coll.get().addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    val isDef = (doc.id == vehicleId)
                    batch.update(doc.reference, "isDefault", isDef)
                }
                batch.commit()
            }
        }
    }

    fun deleteVehicle(userId: String? = currentUserId, vehicleId: String) {
        _vehicles.update { list -> list.filter { it.id != vehicleId } }
        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            db.collection("users").document(uid)
                .collection("vehicles").document(vehicleId)
                .delete()
        }
    }

    fun addPaymentMethod(userId: String? = currentUserId, method: PaymentMethod) {
        val newId = method.id.ifBlank { "pay_${UUID.randomUUID().toString().take(8)}" }
        val toSave = method.copy(id = newId)
        _paymentMethods.update { it + toSave }
        _selectedPaymentMethod.value = toSave

        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            db.collection("users").document(uid)
                .collection("paymentMethods").document(newId)
                .set(toSave)
        }
    }

    fun setDefaultPaymentMethod(userId: String? = currentUserId, methodId: String) {
        _paymentMethods.update { list ->
            list.map { it.copy(isDefault = it.id == methodId) }
        }
        _paymentMethods.value.find { it.id == methodId }?.let {
            _selectedPaymentMethod.value = it
        }

        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            val coll = db.collection("users").document(uid).collection("paymentMethods")
            coll.get().addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, "isDefault", doc.id == methodId)
                }
                batch.commit()
            }
        }
    }

    fun createBooking(
        userId: String? = currentUserId,
        parkingLot: ParkingLot,
        date: String,
        startHour: Int,
        endHour: Int,
        vehicle: Vehicle,
        paymentMethodName: String,
        couponDiscount: Double
    ): Booking {
        val hours = (endHour - startHour).coerceAtLeast(1)
        val subtotal = parkingLot.hourlyRate * hours
        val fee = 500.0
        val total = (subtotal + fee - couponDiscount).coerceAtLeast(0.0)
        val bookingId = "BKG-${(1000..9999).random()}"

        val newBooking = Booking(
            id = bookingId,
            parkingLotId = parkingLot.id,
            parkingLotName = parkingLot.name,
            address = parkingLot.address,
            date = date,
            startTime = String.format("%02d:00", startHour),
            endTime = String.format("%02d:00", endHour),
            startHour = startHour,
            endHour = endHour,
            durationHours = hours,
            vehiclePlate = vehicle.plate.ifBlank { "SIN PATENTE" },
            vehicleModel = vehicle.brandAndModel.ifBlank { "Vehículo" },
            slotAssigned = "Plaza ${('A'..'E').random()}-${(10..40).random()} (${if (parkingLot.isCovered) "Techado" else "Explanada"})",
            accessCode = "#${(1000..9999).random()}",
            qrData = "PARKSPOT-PASS-${UUID.randomUUID().toString().take(8).uppercase()}",
            subtotal = subtotal,
            serviceFee = fee,
            discount = couponDiscount,
            totalPaid = total,
            paymentMethod = paymentMethodName,
            status = BookingStatus.ACTIVE,
            countdownText = "Empieza hoy a las ${String.format("%02d:00", startHour)}"
        )

        _bookings.update { listOf(newBooking) + it }
        _userProfile.update { current ->
            current.copy(
                totalBookings = current.totalBookings + 1,
                totalHours = current.totalHours + hours
            )
        }

        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            val userDoc = db.collection("users").document(uid)
            userDoc.collection("bookings").document(bookingId).set(newBooking)
            // Nota: totalBookings y totalHours en users/{uid} se incrementan mediante Cloud Functions
            // con Admin SDK, ya que firestore.rules restringe updates en users/{uid} solo a 'name' y 'phone'.
        }

        return newBooking
    }

    fun cancelBooking(userId: String? = currentUserId, bookingId: String) {
        _bookings.update { current ->
            current.map {
                if (it.id == bookingId) it.copy(status = BookingStatus.CANCELLED, countdownText = "Cancelada (Reembolsado)")
                else it
            }
        }
        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            // Regla firestore.rules: affectedKeys().hasOnly(['status', 'endHour'])
            db.collection("users").document(uid)
                .collection("bookings").document(bookingId)
                .update("status", BookingStatus.CANCELLED.name)
        }
    }

    fun extendBooking(userId: String? = currentUserId, bookingId: String, extraHours: Int) {
        var calculatedEndHour: Int = 18
        _bookings.update { current ->
            current.map {
                if (it.id == bookingId) {
                    val newDuration = it.durationHours + extraHours
                    val currentEnd = if (it.endHour > 0) it.endHour else (it.endTime.split(":")[0].toIntOrNull() ?: 18)
                    val newEnd = (currentEnd + extraHours) % 24
                    calculatedEndHour = newEnd
                    it.copy(
                        durationHours = newDuration,
                        endHour = newEnd,
                        endTime = String.format("%02d:00", newEnd),
                        countdownText = "Tiempo extendido +${extraHours}h"
                    )
                } else it
            }
        }
        val uid = userId ?: currentUserId
        val db = firestore
        if (db != null && !uid.isNullOrBlank()) {
            // Regla firestore.rules: affectedKeys().hasOnly(['status', 'endHour'])
            db.collection("users").document(uid)
                .collection("bookings").document(bookingId)
                .update("endHour", calculatedEndHour)
        }
    }
}
