package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Lipi App Logo Icon matching the brand graphic:
 * - Stacked note pages
 * - Smooth 3D stylized Ribbon "L" in vibrant Blue to Purple gradient
 * - White stylus pen drawing the ribbon tip
 * - Sparkle stars
 */
@Composable
fun LipiLogoIcon(
    modifier: Modifier = Modifier.size(48.dp),
    cardBackground: Color = Color(0xFFECEEFE)
) {
    Surface(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        color = cardBackground
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Stacked white paper notes background
            rotate(degrees = -14f, pivot = Offset(w * 0.35f, h * 0.5f)) {
                drawRoundRect(
                    color = Color(0xFFF1F4FF),
                    topLeft = Offset(w * 0.18f, h * 0.16f),
                    size = Size(w * 0.52f, h * 0.65f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
                )
            }
            rotate(degrees = -7f, pivot = Offset(w * 0.45f, h * 0.5f)) {
                drawRoundRect(
                    color = Color(0xFFFAFAFE),
                    topLeft = Offset(w * 0.22f, h * 0.15f),
                    size = Size(w * 0.54f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
                )
            }
            // Main front white sheet
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.25f, h * 0.14f),
                size = Size(w * 0.56f, h * 0.70f),
                cornerRadius = CornerRadius(w * 0.09f, h * 0.09f)
            )

            // 2. Stylized Ribbon "L" Path
            val lGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1D4ED8), // Royal Blue top
                    Color(0xFF4F46E5), // Indigo
                    Color(0xFF7C3AED), // Purple loop
                    Color(0xFF9333EA)  // Vibrant Violet
                ),
                start = Offset(w * 0.45f, h * 0.15f),
                end = Offset(w * 0.6f, h * 0.62f)
            )

            // Outer thick ribbon L stroke
            val ribbonPath = Path().apply {
                // Start top stem of L
                moveTo(w * 0.44f, h * 0.16f)
                cubicTo(
                    w * 0.53f, h * 0.24f,
                    w * 0.48f, h * 0.42f,
                    w * 0.38f, h * 0.52f
                )
                // Bottom loop of L
                cubicTo(
                    w * 0.25f, h * 0.64f,
                    w * 0.25f, h * 0.76f,
                    w * 0.35f, h * 0.78f
                )
                cubicTo(
                    w * 0.45f, h * 0.80f,
                    w * 0.58f, h * 0.73f,
                    w * 0.64f, h * 0.66f
                )
                // Curve to stylus tip
                cubicTo(
                    w * 0.62f, h * 0.72f,
                    w * 0.46f, h * 0.75f,
                    w * 0.36f, h * 0.73f
                )
                cubicTo(
                    w * 0.29f, h * 0.71f,
                    w * 0.29f, h * 0.62f,
                    w * 0.42f, h * 0.48f
                )
                close()
            }
            drawPath(path = ribbonPath, brush = lGradient)

            // Ribbon stem outline for depth
            val stemPath = Path().apply {
                moveTo(w * 0.44f, h * 0.16f)
                cubicTo(
                    w * 0.54f, h * 0.22f,
                    w * 0.48f, h * 0.44f,
                    w * 0.32f, h * 0.68f
                )
                cubicTo(
                    w * 0.28f, h * 0.75f,
                    w * 0.38f, h * 0.78f,
                    w * 0.63f, h * 0.66f
                )
            }
            drawPath(
                path = stemPath,
                brush = lGradient,
                style = Stroke(
                    width = w * 0.12f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Inner ribbon shade accent
            val innerShadePath = Path().apply {
                moveTo(w * 0.32f, h * 0.60f)
                cubicTo(
                    w * 0.27f, h * 0.68f,
                    w * 0.29f, h * 0.77f,
                    w * 0.38f, h * 0.76f
                )
                cubicTo(
                    w * 0.48f, h * 0.75f,
                    w * 0.58f, h * 0.68f,
                    w * 0.63f, h * 0.66f
                )
            }
            drawPath(
                path = innerShadePath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF5B21B6), Color(0xFF7C3AED)),
                    start = Offset(w * 0.3f, h * 0.6f),
                    end = Offset(w * 0.6f, h * 0.66f)
                ),
                style = Stroke(
                    width = w * 0.08f,
                    cap = StrokeCap.Round
                )
            )

            // 3. Stylus Pen drawing the stroke
            rotate(degrees = -32f, pivot = Offset(w * 0.68f, h * 0.52f)) {
                // Stylus body
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.65f, h * 0.28f),
                    size = Size(w * 0.09f, h * 0.38f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                // Stylus grip ring
                drawRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(w * 0.65f, h * 0.58f),
                    size = Size(w * 0.09f, h * 0.04f)
                )
                // Stylus dark tip
                val tipPath = Path().apply {
                    moveTo(w * 0.65f, h * 0.62f)
                    lineTo(w * 0.74f, h * 0.62f)
                    lineTo(w * 0.695f, h * 0.69f)
                    close()
                }
                drawPath(path = tipPath, color = Color(0xFF1E1B4B))
            }

            // 4. Sparkle stars (4-point star shapes)
            drawSparkleStar(
                center = Offset(w * 0.62f, h * 0.29f),
                radius = w * 0.055f,
                color = Color(0xFF6366F1)
            )
            drawSparkleStar(
                center = Offset(w * 0.57f, h * 0.37f),
                radius = w * 0.032f,
                color = Color(0xFF818CF8)
            )
        }
    }
}

/**
 * Draws a 4-point sparkle star
 */
private fun DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

/**
 * Lipi brand header component with icon and styled title + star
 */
@Composable
fun LipiBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    showTagline: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        LipiLogoIcon(
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lipi",
                    fontSize = if (iconSize >= 40.dp) 22.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.4).sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(2.dp))
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawSparkleStar(
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 2f,
                        color = Color(0xFF6366F1)
                    )
                }
            }
            if (showTagline) {
                Text(
                    text = "Your Ideas, Beautifully Noted",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

/**
 * Prominent Lipi Logo Card used in Welcome / Onboarding / About dialogs
 */
@Composable
fun LipiLogoCard(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        LipiLogoIcon(
            modifier = Modifier.size(110.dp),
            cardBackground = Color(0xFFEEF2FF)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lipi",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.8).sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.width(4.dp))
            Canvas(modifier = Modifier.size(16.dp)) {
                drawSparkleStar(
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width / 2f,
                    color = Color(0xFF6366F1)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your Ideas, Beautifully Noted",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}
