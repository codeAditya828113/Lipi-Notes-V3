package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TestStrokePoint(val x: Float, val y: Float, val pressure: Float)
data class TestStroke(val points: List<TestStrokePoint>, val color: Color, val baseWidth: Float)

/**
 * Pressure Sensitivity Calibration Utility.
 * Allows users to adjust stroke weight response curves, minimum pressure deadzones,
 * sensitivity multipliers, and test input in real-time on a live calibration pad.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun StylusPressureCalibrationDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    var liveRawPressure by remember { mutableFloatStateOf(0f) }
    var liveCalibratedPressure by remember { mutableFloatStateOf(0f) }
    var testStrokes by remember { mutableStateOf<List<TestStroke>>(emptyList()) }
    var currentTestPoints by remember { mutableStateOf<List<TestStrokePoint>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Calibration Utility",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pressure Sensitivity Calibration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Adjust stroke weight response & stylus curve",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_calibration_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content Layout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Preset Selector Cards
                    Text(
                        text = "RESPONSE CURVE PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "soft" to "Soft (Light Touch)",
                            "linear" to "Linear (1:1)",
                            "firm" to "Firm (Heavy)",
                            "custom" to "Custom"
                        )

                        presets.forEach { (presetKey, presetLabel) ->
                            val isSelected = viewModel.pressurePreset == presetKey
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (presetKey != "custom") {
                                        viewModel.applyPressurePreset(presetKey)
                                    } else {
                                        viewModel.pressurePreset = "custom"
                                    }
                                },
                                label = {
                                    Text(
                                        text = presetLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // 2. Response Curve Graph Visualizer & Real-time Gauge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Curve Graph Box
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Response Curve Plot",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // Draw grid lines
                                    drawLine(Color.Gray.copy(alpha = 0.2f), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)
                                    drawLine(Color.Gray.copy(alpha = 0.2f), Offset(w / 2, 0f), Offset(w / 2, h), strokeWidth = 1f)

                                    // Plot pressure response curve
                                    val path = Path()
                                    val steps = 50
                                    for (i in 0..steps) {
                                        val rawP = i.toFloat() / steps
                                        val calP = viewModel.calculateCalibratedPressure(rawP)
                                        val x = rawP * w
                                        val y = h - (calP / viewModel.pressureMaxWeightMultiplier) * h
                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF2563EB),
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )

                                    // Draw current live pressure dot
                                    if (liveRawPressure > 0f) {
                                        val liveX = liveRawPressure * w
                                        val liveY = h - (liveCalibratedPressure / viewModel.pressureMaxWeightMultiplier) * h
                                        drawCircle(
                                            color = Color(0xFFEF4444),
                                            radius = 6.dp.toPx(),
                                            center = Offset(liveX, liveY)
                                        )
                                    }
                                }
                            }
                        }

                        // Real-time Pressure Meter Gauge
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "LIVE STYLUS METER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Column {
                                    Text(
                                        text = "Raw Input: ${String.format("%.2f", liveRawPressure)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Calibrated: ${String.format("%.2f", liveCalibratedPressure)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Weight: ${String.format("%.1f", 4f * liveCalibratedPressure)} dp",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { liveCalibratedPressure.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    // 3. Calibration Sliders
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Overall Sensitivity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pressure Gain / Sensitivity",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${viewModel.pressureSensitivity.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        Slider(
                            value = viewModel.pressureSensitivity,
                            onValueChange = {
                                viewModel.pressureSensitivity = it
                                viewModel.pressurePreset = "custom"
                            },
                            valueRange = 10f..200f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Response Curve Exponent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Response Curve Exponent",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            val expDesc = when {
                                viewModel.pressureCurveExponent < 0.8f -> "Soft (Light Touch)"
                                viewModel.pressureCurveExponent > 1.2f -> "Firm (Heavy)"
                                else -> "Linear"
                            }
                            Text(
                                text = "${String.format("%.2f", viewModel.pressureCurveExponent)} ($expDesc)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = viewModel.pressureCurveExponent,
                            onValueChange = {
                                viewModel.pressureCurveExponent = it
                                viewModel.pressurePreset = "custom"
                            },
                            valueRange = 0.3f..3.0f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Minimum Pressure Deadzone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Minimum Pressure Deadzone",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = String.format("%.2f", viewModel.pressureMinThreshold),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        Slider(
                            value = viewModel.pressureMinThreshold,
                            onValueChange = {
                                viewModel.pressureMinThreshold = it
                                viewModel.pressurePreset = "custom"
                            },
                            valueRange = 0.0f..0.25f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Max Weight Multiplier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Max Stroke Weight Multiplier",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${String.format("%.1f", viewModel.pressureMaxWeightMultiplier)}x",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        Slider(
                            value = viewModel.pressureMaxWeightMultiplier,
                            onValueChange = {
                                viewModel.pressureMaxWeightMultiplier = it
                                viewModel.pressurePreset = "custom"
                            },
                            valueRange = 1.0f..4.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. Interactive Live Pressure Test Pad
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE CALIBRATION TEST PAD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(
                                onClick = {
                                    testStrokes = emptyList()
                                    currentTestPoints = emptyList()
                                    liveRawPressure = 0f
                                    liveCalibratedPressure = 0f
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Pad", fontSize = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInteropFilter { motionEvent ->
                                        val rawP = if (motionEvent.pressure > 0) motionEvent.pressure else 0.5f
                                        val calP = viewModel.calculateCalibratedPressure(rawP)
                                        liveRawPressure = rawP
                                        liveCalibratedPressure = calP

                                        when (motionEvent.action) {
                                            MotionEvent.ACTION_DOWN -> {
                                                currentTestPoints = listOf(TestStrokePoint(motionEvent.x, motionEvent.y, calP))
                                                true
                                            }
                                            MotionEvent.ACTION_MOVE -> {
                                                currentTestPoints = currentTestPoints + TestStrokePoint(motionEvent.x, motionEvent.y, calP)
                                                true
                                            }
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                if (currentTestPoints.isNotEmpty()) {
                                                    testStrokes = testStrokes + TestStroke(
                                                        points = currentTestPoints,
                                                        color = Color(0xFF1E293B),
                                                        baseWidth = 4f
                                                    )
                                                }
                                                currentTestPoints = emptyList()
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Draw background guideline
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.4f),
                                        start = Offset(0f, size.height / 2),
                                        end = Offset(size.width, size.height / 2),
                                        strokeWidth = 1f
                                    )

                                    if (testStrokes.isEmpty() && currentTestPoints.isEmpty()) {
                                        // Empty prompt
                                    }

                                    // Draw completed test strokes
                                    testStrokes.forEach { stroke ->
                                        if (stroke.points.size > 1) {
                                            for (i in 0 until stroke.points.size - 1) {
                                                val p1 = stroke.points[i]
                                                val p2 = stroke.points[i + 1]
                                                val w = stroke.baseWidth * p1.pressure
                                                drawLine(
                                                    color = stroke.color,
                                                    start = Offset(p1.x, p1.y),
                                                    end = Offset(p2.x, p2.y),
                                                    strokeWidth = w.coerceAtLeast(1f),
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }

                                    // Draw current active stroke
                                    if (currentTestPoints.size > 1) {
                                        for (i in 0 until currentTestPoints.size - 1) {
                                            val p1 = currentTestPoints[i]
                                            val p2 = currentTestPoints[i + 1]
                                            val w = 4f * p1.pressure
                                            drawLine(
                                                color = Color(0xFF2563EB),
                                                start = Offset(p1.x, p1.y),
                                                end = Offset(p2.x, p2.y),
                                                strokeWidth = w.coerceAtLeast(1f),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }

                                if (testStrokes.isEmpty() && currentTestPoints.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✍️ Draw or press stylus here to test stroke weight calibration",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetPressureCalibration()
                            testStrokes = emptyList()
                            currentTestPoints = emptyList()
                            liveRawPressure = 0f
                            liveCalibratedPressure = 0f
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default")
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_pressure_calibration_button")
                    ) {
                        Text("Apply & Save Calibration")
                    }
                }
            }
        }
    }
}
