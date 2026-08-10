package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Stroke
import com.example.handwriting.*

private val LipiPrimaryBlue = Color(0xFF3B82F6)
private val LipiAccentPurple = Color(0xFF8B5CF6)
private val LipiHwCardDark = Color(0xFF1E293B)
private val LipiHwCardLight = Color(0xFFFFFFFF)

/**
 * Compact floating menu panel for Lipi Smart Handwriting.
 */
@Composable
fun SmartHandwritingPanel(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var autoRefineEnabled by remember { mutableStateOf(viewModel.autoRefineEnabled) }
    var currentLevel by remember { mutableStateOf(viewModel.handwritingRefinementLevel) }
    var selectedLanguage by remember { mutableStateOf(viewModel.handwritingLanguage) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showSpacingMenu by remember { mutableStateOf(false) }

    val profile = remember { PersonalHandwritingProfileManager.getProfile(context) }

    Card(
        modifier = Modifier
            .width(360.dp)
            .padding(16.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .testTag("smart_handwriting_panel"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LipiPrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LipiPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Handwriting",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preserves style, enhances legibility",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("close_smart_handwriting_panel")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto Refine Toggle Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time Auto Refine",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Quietly smooths strokes as you write",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoRefineEnabled,
                        onCheckedChange = { enabled ->
                            autoRefineEnabled = enabled
                            viewModel.autoRefineEnabled = enabled
                        },
                        modifier = Modifier.testTag("auto_refine_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Refinement Strength Level Slider
            Text(
                text = "Refinement Level: ${currentLevel.displayName}",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = when (currentLevel) {
                    RefinementLevel.NATURAL -> 0f
                    RefinementLevel.LIGHT -> 1f
                    RefinementLevel.BALANCED -> 2f
                    RefinementLevel.STRONG -> 3f
                },
                onValueChange = { value ->
                    val newLevel = when (value.toInt()) {
                        0 -> RefinementLevel.NATURAL
                        1 -> RefinementLevel.LIGHT
                        2 -> RefinementLevel.BALANCED
                        else -> RefinementLevel.STRONG
                    }
                    currentLevel = newLevel
                    viewModel.handwritingRefinementLevel = newLevel
                },
                valueRange = 0f..3f,
                steps = 2,
                modifier = Modifier.testTag("refinement_level_slider")
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Text("Natural", fontSize = 9.sp, color = Color.Gray)
                Text("Light", fontSize = 9.sp, color = Color.Gray)
                Text("Balanced", fontSize = 9.sp, color = Color.Gray)
                Text("Strong", fontSize = 9.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ✨ Refine Selection
                Button(
                    onClick = {
                        viewModel.refineSelectedHandwriting()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("refine_selection_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LipiPrimaryBlue)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refine Selection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // Straighten
                    OutlinedButton(
                        onClick = {
                            viewModel.straightenSelectedHandwriting()
                        },
                        modifier = Modifier.weight(1f).testTag("straighten_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Straighten", fontSize = 11.sp)
                    }

                    // Spacing
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showSpacingMenu = true },
                            modifier = Modifier.fillMaxWidth().testTag("spacing_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SpaceBar, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Spacing ▾", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = showSpacingMenu,
                            onDismissRequest = { showSpacingMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tighten Spacing") },
                                onClick = {
                                    viewModel.adjustHandwritingSpacing(SpacingMode.TIGHTEN)
                                    showSpacingMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Increase Spacing") },
                                onClick = {
                                    viewModel.adjustHandwritingSpacing(SpacingMode.INCREASE)
                                    showSpacingMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Normalize Word Spacing") },
                                onClick = {
                                    viewModel.adjustHandwritingSpacing(SpacingMode.NORMALIZE_WORDS)
                                    showSpacingMenu = false
                                }
                            )
                        }
                    }
                }

                // Convert to Editable Text (Lipi Scribble)
                OutlinedButton(
                    onClick = {
                        viewModel.convertHandwritingToText()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("convert_to_text_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert to Editable Text", fontSize = 12.sp)
                }

                // My Handwriting Style
                OutlinedButton(
                    onClick = {
                        viewModel.openWriteInMyStyleDialog()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("my_handwriting_style_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("My Handwriting Style (Learned ${profile.sampleCount} samples)", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language & Privacy Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    TextButton(
                        onClick = { showLanguageMenu = true },
                        modifier = Modifier.testTag("language_selector")
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lang: $selectedLanguage ▾", fontSize = 11.sp)
                    }

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                selectedLanguage = "English"
                                viewModel.handwritingLanguage = "English"
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hindi") },
                            onClick = {
                                selectedLanguage = "Hindi"
                                viewModel.handwritingLanguage = "Hindi"
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Auto-Detect") },
                            onClick = {
                                selectedLanguage = "Auto-Detect"
                                viewModel.handwritingLanguage = "Auto-Detect"
                                showLanguageMenu = false
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("On-Device Privacy", fontSize = 10.sp, color = Color(0xFF10B981))
                }
            }
        }
    }
}

/**
 * Interactive Before/After Comparison view for Manual Refinement.
 */
@Composable
fun HandwritingCompareDialog(
    originalStrokes: List<Stroke>,
    refinedStrokes: List<Stroke>,
    onApply: () -> Unit,
    onRestoreOriginal: () -> Unit,
    onDismiss: () -> Unit
) {
    var compareSliderPos by remember { mutableStateOf(0.5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LipiPrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refinement Comparison", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Drag the slider to compare original vs. refined handwriting.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Comparison Canvas Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val splitX = canvasWidth * compareSliderPos

                        // Render Original on left side
                        originalStrokes.forEach { stroke ->
                            val pts = stroke.points
                            for (i in 0 until pts.size - 1) {
                                if (pts[i].x < splitX) {
                                    drawLine(
                                        color = Color(stroke.color),
                                        start = Offset(pts[i].x, pts[i].y),
                                        end = Offset(pts[i + 1].x, pts[i + 1].y),
                                        strokeWidth = stroke.width,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Render Refined on right side
                        refinedStrokes.forEach { stroke ->
                            val pts = stroke.points
                            for (i in 0 until pts.size - 1) {
                                if (pts[i].x >= splitX) {
                                    drawLine(
                                        color = Color(stroke.color),
                                        start = Offset(pts[i].x, pts[i].y),
                                        end = Offset(pts[i + 1].x, pts[i + 1].y),
                                        strokeWidth = stroke.width,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Split divider line
                        drawLine(
                            color = LipiPrimaryBlue,
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, canvasHeight),
                            strokeWidth = 3f
                        )
                    }

                    // Labels
                    Text(
                        text = "Original",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                    Text(
                        text = "Refined ✨",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LipiPrimaryBlue,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compare Slider
                Slider(
                    value = compareSliderPos,
                    onValueChange = { compareSliderPos = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth().testTag("compare_slider")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = LipiPrimaryBlue),
                modifier = Modifier.testTag("apply_refinement_button")
            ) {
                Text("Apply Refinement")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestoreOriginal, modifier = Modifier.testTag("restore_original_button")) {
                    Text("Restore Original")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * "Write in My Style" Generator Dialog.
 */
@Composable
fun WriteInMyStyleDialog(
    profile: PersonalStyleProfile,
    onGenerate: (String, Int, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF1E293B.toInt()) }
    var strokeWidth by remember { mutableStateOf(4.5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Brush, contentDescription = null, tint = LipiPrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write in My Style", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Type text to render as handwritten strokes matching your personal style profile.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Enter text to convert...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("write_in_style_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pen Color Options
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Color:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    listOf(
                        0xFF1E293B.toInt() to Color(0xFF1E293B),
                        0xFF2563EB.toInt() to Color(0xFF2563EB),
                        0xFFDC2626.toInt() to Color(0xFFDC2626),
                        0xFF16A34A.toInt() to Color(0xFF16A34A)
                    ).forEach { (cInt, cVal) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(cVal)
                                .border(
                                    if (selectedColor == cInt) 2.dp else 0.dp,
                                    LipiPrimaryBlue,
                                    CircleShape
                                )
                                .clickable { selectedColor = cInt }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Style profile learned on-device from ${profile.sampleCount} handwriting samples.",
                        fontSize = 10.sp,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onGenerate(textInput, selectedColor, strokeWidth)
                    }
                },
                enabled = textInput.isNotBlank(),
                modifier = Modifier.testTag("generate_handwriting_button")
            ) {
                Text("Insert Handwritten Strokes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Floating overlay bar during active Scribble mode.
 */
@Composable
fun ScribbleOverlayBar(
    recognizedText: String,
    onCopyText: () -> Unit,
    onInsertAsText: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .testTag("scribble_overlay_bar"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp).fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Gesture, contentDescription = null, tint = LipiPrimaryBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Lipi Scribble Active", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = if (recognizedText.isBlank()) "Write on canvas..." else recognizedText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onCopyText, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onInsertAsText, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Insert", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
