package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ParkingLot
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * OpenStreetMap (OSM) interactive map for parking spots.
 * 100% Free, open-source, and does NOT require Google Maps API keys.
 */
@Composable
fun OpenStreetParkingMap(
    parkingLots: List<ParkingLot>,
    selectedLotId: String,
    onLotSelected: (ParkingLot) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    userLatitude: Double? = null,
    userLongitude: Double? = null
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration with application context and proper user-agent
    remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid_parkspot", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    val initialLat = userLatitude ?: -31.4167
    val initialLon = userLongitude ?: -64.1833

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(initialLat, initialLon))
        }
    }

    // Clean up mapView lifecycle on disposal
    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    // Centrar mapa en la ubicación del usuario cuando se obtiene por primera vez
    LaunchedEffect(userLatitude, userLongitude) {
        if (userLatitude != null && userLongitude != null) {
            val userGeoPoint = GeoPoint(userLatitude, userLongitude)
            mapView.controller.animateTo(userGeoPoint, 15.0, 750L)
        }
    }

    // Animate to selected parking lot when it changes
    val selectedLot = parkingLots.find { it.id == selectedLotId }
    LaunchedEffect(selectedLotId) {
        selectedLot?.let { lot ->
            val targetGeoPoint = GeoPoint(lot.latitude, lot.longitude)
            mapView.controller.animateTo(targetGeoPoint, 16.0, 700L)
        }
    }

    // Refresh markers on parking lots or selection change
    LaunchedEffect(parkingLots, selectedLotId, isDark, userLatitude, userLongitude) {
        mapView.overlays.clear()

        // Marcador de ubicación real del usuario
        if (userLatitude != null && userLongitude != null) {
            val userMarker = Marker(mapView).apply {
                position = GeoPoint(userLatitude, userLongitude)
                title = "Tu ubicación actual"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createUserLocationDrawable(context)
            }
            mapView.overlays.add(userMarker)
        }

        parkingLots.forEach { lot ->
            val isSelected = lot.id == selectedLotId
            val priceStr = "$${String.format(java.util.Locale("es", "AR"), "%,.0f", lot.hourlyRate)}/h"
            val marker = Marker(mapView).apply {
                position = GeoPoint(lot.latitude, lot.longitude)
                title = lot.name
                subDescription = "$priceStr • ★ ${lot.rating} • ${lot.availableSlots} libres"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // High-fidelity custom pricing pill icon
                icon = createParkingPillDrawable(
                    context = context,
                    priceText = priceStr,
                    rating = lot.rating,
                    isSelected = isSelected,
                    isDark = isDark
                )

                setOnMarkerClickListener { clickedMarker, _ ->
                    onLotSelected(lot)
                    clickedMarker.showInfoWindow()
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Creates a crisp vector-drawn BitmapDrawable badge showing price and rating for OpenStreetMap markers
 */
private fun createParkingPillDrawable(
    context: Context,
    priceText: String,
    rating: Double,
    isSelected: Boolean,
    isDark: Boolean
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val width = (122 * density).toInt()
    val height = (44 * density).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(50, 0, 0, 0)
        style = Paint.Style.FILL
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) {
            AndroidColor.rgb(0, 53, 128) // Booking Navy
        } else if (isDark) {
            AndroidColor.rgb(30, 41, 59) // Slate 800
        } else {
            AndroidColor.WHITE
        }
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) {
            AndroidColor.rgb(16, 185, 129) // Emerald accent border
        } else {
            AndroidColor.rgb(203, 213, 225) // Light gray border
        }
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }

    val textPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) AndroidColor.WHITE else if (isDark) AndroidColor.WHITE else AndroidColor.rgb(15, 23, 42)
        textSize = 12f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    val textRatingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(245, 158, 11) // Amber Star
        textSize = 10f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    val cornerRadius = 14f * density
    val rect = RectF(4f * density, 3f * density, (width - 4 * density), (height - 9 * density))

    // Draw shadow
    val shadowRect = RectF(rect.left, rect.top + 2f * density, rect.right, rect.bottom + 2f * density)
    canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

    // Draw main badge
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

    // Draw pin arrow pointer at the bottom center
    val path = android.graphics.Path().apply {
        val centerX = width / 2f
        val bottomY = height - 3f * density
        moveTo(centerX - 6f * density, height - 9f * density)
        lineTo(centerX + 6f * density, height - 9f * density)
        lineTo(centerX, bottomY)
        close()
    }
    canvas.drawPath(path, bgPaint)

    // Draw texts
    canvas.drawText(priceText, 10f * density, 22f * density, textPricePaint)
    canvas.drawText("★ $rating", 74f * density, 22f * density, textRatingPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Genera el marcador circular azul con pulso para la ubicación GPS del usuario.
 */
fun createUserLocationDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#3B82F6")
        alpha = 70
    }
    canvas.drawCircle(center, center, center, haloPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
    }
    canvas.drawCircle(center, center, 8f * density, borderPaint)

    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1D4ED8")
    }
    canvas.drawCircle(center, center, 6f * density, corePaint)

    return BitmapDrawable(context.resources, bitmap)
}

