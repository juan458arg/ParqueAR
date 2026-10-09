package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParkingLot
import com.example.ui.theme.BookingBlue
import com.example.ui.theme.BookingNavy
import com.example.ui.theme.ParkAmberRating
import com.example.ui.theme.ParkEmerald

/**
 * Realistic QR Code generator drawn on Canvas with standard corner detection patterns
 * and central ParkSpot badge.
 */
@Composable
fun ParkingQrCode(
    dataString: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 180.dp,
    showScanLine: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.width
            val gridSize = 21
            val cellSize = canvasSize / gridSize

            // Deterministic pseudo matrix based on data string hash
            val hash = dataString.hashCode()

            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val inTopLeftTarget = row < 7 && col < 7
                    val inTopRightTarget = row < 7 && col >= gridSize - 7
                    val inBottomLeftTarget = row >= gridSize - 7 && col < 7
                    val inCenterLogo = row in 8..12 && col in 8..12

                    if (inTopLeftTarget || inTopRightTarget || inBottomLeftTarget || inCenterLogo) {
                        continue
                    }

                    // Pseudo-random pseudo module
                    val isBlack = (((row * 31 + col * 17) xor hash) % 3) == 0 ||
                            ((row + col) % 4 == 0) ||
                            ((row * col + hash) % 5 == 0)

                    if (isBlack) {
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize * 0.92f, cellSize * 0.92f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }

            // Draw Corner QR Target Finders (7x7)
            drawQrFinderTarget(0f, 0f, cellSize * 7)
            drawQrFinderTarget((gridSize - 7) * cellSize, 0f, cellSize * 7)
            drawQrFinderTarget(0f, (gridSize - 7) * cellSize, cellSize * 7)

            // Draw Center 'P' Shield Box
            val centerTopLeft = Offset(8 * cellSize, 8 * cellSize)
            val centerSize = Size(5 * cellSize, 5 * cellSize)
            drawRoundRect(
                color = BookingNavy,
                topLeft = centerTopLeft,
                size = centerSize,
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Center P letter representation
            drawRoundRect(
                color = ParkEmerald,
                topLeft = Offset(centerTopLeft.x + centerSize.width * 0.25f, centerTopLeft.y + centerSize.height * 0.2f),
                size = Size(centerSize.width * 0.2f, centerSize.height * 0.6f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = ParkEmerald,
                topLeft = Offset(centerTopLeft.x + centerSize.width * 0.25f, centerTopLeft.y + centerSize.height * 0.2f),
                size = Size(centerSize.width * 0.5f, centerSize.height * 0.32f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Animated Laser Scan Line
            if (showScanLine) {
                val lineY = canvasSize * scanProgress
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ParkEmerald.copy(alpha = 0.9f),
                            Color(0xFF34D399),
                            ParkEmerald.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, lineY),
                    end = Offset(canvasSize, lineY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawQrFinderTarget(x: Float, y: Float, targetSize: Float) {
    // Outer border
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x, y),
        size = Size(targetSize, targetSize),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = targetSize * 0.16f)
    )
    // Inner center filled square
    val innerMargin = targetSize * 0.32f
    val innerSize = targetSize - (innerMargin * 2)
    drawRoundRect(
        color = BookingBlue,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

/**
 * Rich parking hero card graphic illustrating underground garage, valet, rooftop, or eco station.
 */
@Composable
fun ParkingIllustrationHeader(
    imageType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (imageType) {
                "VALET" -> {
                    // Modern luxury hotel facade at dusk with warm canopy and valet staging
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF0A1128), Color(0xFF1C2541), Color(0xFF1E3A8A))
                        )
                    )
                    // Building glass panels
                    for (i in 0..6) {
                        drawRoundRect(
                            color = Color(0x3360A5FA),
                            topLeft = Offset(w * 0.08f + i * (w * 0.13f), h * 0.15f),
                            size = Size(w * 0.09f, h * 0.45f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                    // Canopy
                    val canopyPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.65f)
                        lineTo(w * 0.85f, h * 0.65f)
                        lineTo(w * 0.92f, h * 0.82f)
                        lineTo(w * 0.08f, h * 0.82f)
                        close()
                    }
                    drawPath(canopyPath, color = Color(0xFF0F172A))
                    drawLine(
                        color = Color(0xFFF59E0B),
                        start = Offset(w * 0.15f, h * 0.66f),
                        end = Offset(w * 0.85f, h * 0.66f),
                        strokeWidth = 6f
                    )
                    // Ground pavement
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(0f, h * 0.82f),
                        size = Size(w, h * 0.18f)
                    )
                    // Sleek car silhouette
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(w * 0.38f, h * 0.74f),
                        size = Size(w * 0.28f, h * 0.14f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
                "ECO" -> {
                    // Eco-friendly clean charging bays with emerald solar roof
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF059669))
                        )
                    )
                    // Solar canopy slats
                    for (i in 0..7) {
                        drawLine(
                            color = Color(0x66FFFFFF),
                            start = Offset(w * 0.1f + i * (w * 0.11f), h * 0.18f),
                            end = Offset(w * 0.15f + i * (w * 0.11f), h * 0.48f),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                    // Charging pylons with glowing LED cables
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(w * 0.22f, h * 0.45f),
                        size = Size(w * 0.08f, h * 0.45f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawCircle(
                        color = Color(0xFF34D399),
                        radius = 10f,
                        center = Offset(w * 0.26f, h * 0.52f)
                    )
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(w * 0.68f, h * 0.45f),
                        size = Size(w * 0.08f, h * 0.45f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawCircle(
                        color = Color(0xFF34D399),
                        radius = 10f,
                        center = Offset(w * 0.72f, h * 0.52f)
                    )
                    // Pavement
                    drawRect(
                        color = Color(0xFF1F2937),
                        topLeft = Offset(0f, h * 0.85f),
                        size = Size(w, h * 0.15f)
                    )
                }
                "ROOFTOP" -> {
                    // Skyline dusk sunset over open rooftop parking
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFFBE185D), Color(0xFFF59E0B))
                        )
                    )
                    // Distant skyline buildings
                    drawRect(
                        color = Color(0x990F172A),
                        topLeft = Offset(w * 0.08f, h * 0.38f),
                        size = Size(w * 0.14f, h * 0.38f)
                    )
                    drawRect(
                        color = Color(0xBB0F172A),
                        topLeft = Offset(w * 0.26f, h * 0.28f),
                        size = Size(w * 0.18f, h * 0.48f)
                    )
                    drawRect(
                        color = Color(0x990F172A),
                        topLeft = Offset(w * 0.48f, h * 0.34f),
                        size = Size(w * 0.22f, h * 0.42f)
                    )
                    drawRect(
                        color = Color(0xCC0F172A),
                        topLeft = Offset(w * 0.74f, h * 0.25f),
                        size = Size(w * 0.18f, h * 0.51f)
                    )
                    // Rooftop barrier & floor
                    drawRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(0f, h * 0.75f),
                        size = Size(w, h * 0.25f)
                    )
                    // White parking slot lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(w * 0.25f, h * 0.78f),
                        end = Offset(w * 0.25f, h * 0.98f),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(w * 0.5f, h * 0.78f),
                        end = Offset(w * 0.5f, h * 0.98f),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(w * 0.75f, h * 0.78f),
                        end = Offset(w * 0.75f, h * 0.98f),
                        strokeWidth = 6f
                    )
                }
                else -> {
                    // Underground garage with ceiling LED rails and green availability indicators
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                        )
                    )
                    // Overhead neon lines
                    for (i in 0..4) {
                        drawLine(
                            color = Color(0xFF93C5FD).copy(alpha = 0.7f),
                            start = Offset(w * 0.15f + i * (w * 0.18f), h * 0.12f),
                            end = Offset(w * 0.15f + i * (w * 0.18f), h * 0.32f),
                            strokeWidth = 4f
                        )
                        // Green availability lights above each parking slot
                        drawCircle(
                            color = ParkEmerald,
                            radius = 7f,
                            center = Offset(w * 0.15f + i * (w * 0.18f), h * 0.35f)
                        )
                    }
                    // Concrete floor
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(0f, h * 0.65f),
                        size = Size(w, h * 0.35f)
                    )
                    // Yellow safety chevron/stripes on columns
                    for (i in listOf(0.12f, 0.5f, 0.88f)) {
                        drawRoundRect(
                            color = Color(0xFF475569),
                            topLeft = Offset(w * i - 20f, h * 0.28f),
                            size = Size(40f, h * 0.45f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawLine(
                            color = Color(0xFFFBBF24),
                            start = Offset(w * i - 15f, h * 0.52f),
                            end = Offset(w * i + 15f, h * 0.52f),
                            strokeWidth = 5f
                        )
                    }
                    // Parking bay lines
                    for (i in 0..3) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f),
                            start = Offset(w * 0.18f + i * (w * 0.22f), h * 0.72f),
                            end = Offset(w * 0.22f + i * (w * 0.22f), h * 0.98f),
                            strokeWidth = 5f
                        )
                    }
                }
            }
        }

        // Overlay gradient for text readability and elegance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )
    }
}

/**
 * Interactive Vector Map with street grid, city blocks, user location pulse,
 * and pins showing hourly price & availability.
 */
@Composable
fun InteractiveParkingMap(
    parkingLots: List<ParkingLot>,
    selectedLotId: String,
    onPinClicked: (ParkingLot) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    val mapBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val mapBorderColor = if (isDark) Color(0xFF2D3C5E) else Color(0xFFCBD5E1)
    val landColor = if (isDark) Color(0xFF0B132B) else Color(0xFFF1F5F9)
    val blockColor = if (isDark) Color(0xFF162038) else Color(0xFFE2E8F0)
    val parkColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFDCFCE7)
    val riverColor = if (isDark) Color(0xFF0369A1).copy(alpha = 0.5f) else Color(0xFFBAE6FD)
    val streetColor = if (isDark) Color(0xFF1E293B) else Color.White
    val streetLineColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val overlaySurfaceColor = if (isDark) Color(0xFF162038).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.92f)
    val overlayTextColor = if (isDark) Color(0xFFF8FAFC) else BookingNavy
    val gpsBtnBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val gpsBtnTint = if (isDark) Color(0xFF60A5FA) else BookingBlue
    val userPinBlue = if (isDark) Color(0xFF60A5FA) else BookingBlue

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(mapBgColor)
            .border(1.dp, mapBorderColor, RoundedCornerShape(20.dp))
    ) {
        val mapWidth = maxWidth
        val mapHeight = maxHeight

        // Canvas Map Drawing: Streets, Blocks, Parks, River
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Land background
            drawRect(color = landColor)

            // Urban Blocks
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(w * 0.05f, h * 0.08f),
                size = Size(w * 0.22f, h * 0.25f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(w * 0.32f, h * 0.08f),
                size = Size(w * 0.28f, h * 0.22f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(w * 0.65f, h * 0.08f),
                size = Size(w * 0.30f, h * 0.25f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Central Park area
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(w * 0.05f, h * 0.40f),
                size = Size(w * 0.22f, h * 0.35f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Blocks south
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(w * 0.32f, h * 0.36f),
                size = Size(w * 0.30f, h * 0.40f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(w * 0.68f, h * 0.40f),
                size = Size(w * 0.27f, h * 0.36f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // River / Canal on East
            val riverPath = Path().apply {
                moveTo(w * 0.88f, 0f)
                cubicTo(w * 0.84f, h * 0.35f, w * 0.94f, h * 0.65f, w * 0.86f, h)
                lineTo(w, h)
                lineTo(w, 0f)
                close()
            }
            drawPath(riverPath, color = riverColor)

            // Main Avenues & Streets
            drawRect(color = streetColor, topLeft = Offset(0f, h * 0.33f), size = Size(w, h * 0.06f))
            drawRect(color = streetColor, topLeft = Offset(0f, h * 0.77f), size = Size(w, h * 0.05f))

            // Vertical avenues
            drawRect(color = streetColor, topLeft = Offset(w * 0.28f, 0f), size = Size(w * 0.045f, h))
            drawRect(color = streetColor, topLeft = Offset(w * 0.63f, 0f), size = Size(w * 0.045f, h))

            // Street lines
            drawLine(
                color = streetLineColor,
                start = Offset(0f, h * 0.36f),
                end = Offset(w, h * 0.36f),
                strokeWidth = 2f
            )

            // User Location GPS Pin (Pulse)
            val userCenter = Offset(w * 0.45f, h * 0.52f)
            drawCircle(
                color = userPinBlue.copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = userCenter
            )
            drawCircle(
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                radius = 9f,
                center = userCenter
            )
            drawCircle(
                color = userPinBlue,
                radius = 6f,
                center = userCenter
            )
        }

        // Map Watermark / Overlay info
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp),
            color = overlaySurfaceColor,
            shadowElevation = 2.dp,
            border = if (isDark) BorderStroke(1.dp, Color(0xFF283655)) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ParkEmerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${parkingLots.size} estacionamientos cerca",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = overlayTextColor
                )
            }
        }

        // Compass / GPS Button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = CircleShape,
            color = gpsBtnBgColor,
            shadowElevation = 3.dp,
            border = if (isDark) BorderStroke(1.dp, Color(0xFF283655)) else null
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { /* Re-center */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Mi ubicación",
                    tint = gpsBtnTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Parking Pins placed according to normalized coordinates
        parkingLots.forEach { lot ->
            val isSelected = lot.id == selectedLotId
            val pinX = (mapWidth.value * lot.mapNormalizedX).dp - 36.dp
            val pinY = (mapHeight.value * lot.mapNormalizedY).dp - 24.dp

            Box(
                modifier = Modifier
                    .offset(x = pinX, y = pinY)
                    .testTag("pin_${lot.id}")
                    .clickable { onPinClicked(lot) }
            ) {
                ParkingMapPin(
                    price = "$${String.format(java.util.Locale("es", "AR"), "%,.0f", lot.hourlyRate)}/h",
                    availableSlots = lot.availableSlots,
                    isSelected = isSelected
                )
            }
        }
    }
}

/**
 * Modern Booking.com style map price badge pin adapting to dark & light mode
 */
@Composable
fun ParkingMapPin(
    price: String,
    availableSlots: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val elevation = if (isSelected) 8.dp else 4.dp
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = bgColor,
            shadowElevation = elevation,
            border = if (!isSelected) BorderStroke(1.dp, outlineColor) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Availability dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (availableSlots > 10) ParkEmerald else ParkAmberRating)
                )
                Text(
                    text = price,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
        // Triangle pointer
        Canvas(modifier = Modifier.size(10.dp, 6.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(path, color = bgColor)
        }
    }
}

private fun BorderStroke(width: Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
