package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.NoteEntity
import com.example.ui.security.SetNotebookPasscodeDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Primary Theme Colors for Lipi Notebook Studio
val LipiStudioPrimary = Color(0xFF4F46E5)
val LipiStudioAccent = Color(0xFF10B981)
val LipiStudioCardBg = Color(0xFFFFFFFF)
val LipiStudioCanvasBg = Color(0xFFF7F8FC)

@Composable
fun PageTemplateCanvasPreview(
    templateType: String,
    pageColor: Long = 0xFFFFFFFF,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = pageColor == 0xFF1A1A1AL || pageColor == 0xFF1E293BL
    val bgColor = Color(pageColor)
    val gridLineColor = if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color(0xFF94A3B8).copy(alpha = 0.55f)
    val marginLineColor = if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.7f) else Color(0xFFFF6B6B)
    val primaryLineColor = if (isDarkTheme) Color(0xFF60A5FA).copy(alpha = 0.8f) else Color(0xFF3B82F6).copy(alpha = 0.75f)
    val accentLineColor = if (isDarkTheme) Color(0xFF34D399).copy(alpha = 0.8f) else Color(0xFF10B981).copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .background(bgColor)
            .clipToBounds()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val type = templateType.lowercase().trim()

            when {
                type == "blank" -> {
                    // Clean blank sheet
                }
                type == "ruled" -> {
                    val topMargin = h * 0.14f
                    val lineSpacing = (h - topMargin) / 8f
                    drawLine(marginLineColor.copy(alpha = 0.5f), Offset(0f, topMargin), Offset(w, topMargin), strokeWidth = 1.8f)
                    val marginX = w * 0.16f
                    drawLine(marginLineColor, Offset(marginX, 0f), Offset(marginX, h), strokeWidth = 1.5f)
                    for (i in 1..7) {
                        val y = topMargin + i * lineSpacing
                        drawLine(gridLineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }
                }
                type == "grid" || type == "square" -> {
                    val cols = 8
                    val rows = 11
                    val stepX = w / cols
                    val stepY = h / rows
                    for (i in 1 until cols) {
                        val isMajor = i % 4 == 0
                        drawLine(
                            color = if (isMajor) primaryLineColor.copy(alpha = 0.6f) else gridLineColor,
                            start = Offset(i * stepX, 0f),
                            end = Offset(i * stepX, h),
                            strokeWidth = if (isMajor) 1.5f else 0.8f
                        )
                    }
                    for (j in 1 until rows) {
                        val isMajor = j % 4 == 0
                        drawLine(
                            color = if (isMajor) primaryLineColor.copy(alpha = 0.6f) else gridLineColor,
                            start = Offset(0f, j * stepY),
                            end = Offset(w, j * stepY),
                            strokeWidth = if (isMajor) 1.5f else 0.8f
                        )
                    }
                }
                type.contains("dot") || type.contains("bullet") -> {
                    val cols = 8
                    val rows = 12
                    val stepX = w / cols
                    val stepY = h / rows
                    for (i in 1 until cols) {
                        for (j in 1 until rows) {
                            val isCenter = (i == cols / 2 && j == rows / 2)
                            drawCircle(
                                color = if (isCenter) primaryLineColor else gridLineColor,
                                radius = if (isCenter) 2.5f else 1.5f,
                                center = Offset(i * stepX, j * stepY)
                            )
                        }
                    }
                }
                type == "cornell" -> {
                    val headerH = h * 0.12f
                    val splitX = w * 0.30f
                    val summaryY = h * 0.78f
                    drawLine(primaryLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.5f)
                    drawLine(primaryLineColor, Offset(splitX, headerH), Offset(splitX, summaryY), strokeWidth = 2f)
                    drawLine(primaryLineColor, Offset(0f, summaryY), Offset(w, summaryY), strokeWidth = 2f)
                    val lineSpacing = (summaryY - headerH) / 6f
                    for (i in 1..5) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(splitX, y), Offset(w, y), strokeWidth = 1f)
                    }
                }
                type == "engineering" || type == "graph" -> {
                    val cols = 14
                    val rows = 18
                    val stepX = w / cols
                    val stepY = h / rows
                    val engLineColor = if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color(0xFF0284C7).copy(alpha = 0.25f)
                    val engMajorColor = if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.7f) else Color(0xFF0284C7).copy(alpha = 0.65f)
                    for (i in 1 until cols) {
                        val isMajor = i % 5 == 0
                        drawLine(if (isMajor) engMajorColor else engLineColor, Offset(i * stepX, 0f), Offset(i * stepX, h * 0.88f), strokeWidth = if (isMajor) 1.5f else 0.6f)
                    }
                    for (j in 1 until rows) {
                        val isMajor = j % 5 == 0
                        drawLine(if (isMajor) engMajorColor else engLineColor, Offset(0f, j * stepY), Offset(w, j * stepY), strokeWidth = if (isMajor) 1.5f else 0.6f)
                    }
                    // Bottom Title Block
                    drawLine(primaryLineColor, Offset(0f, h * 0.88f), Offset(w, h * 0.88f), strokeWidth = 2f)
                    drawLine(primaryLineColor, Offset(w * 0.55f, h * 0.88f), Offset(w * 0.55f, h), strokeWidth = 1.5f)
                }
                type == "lecture" -> {
                    val headerH = h * 0.16f
                    val splitX = w * 0.28f
                    val bottomY = h * 0.84f
                    drawRect(primaryLineColor.copy(alpha = 0.12f), Offset(0f, 0f), Size(w, headerH))
                    drawLine(primaryLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.8f)
                    drawLine(primaryLineColor, Offset(splitX, headerH), Offset(splitX, bottomY), strokeWidth = 1.5f)
                    drawLine(primaryLineColor, Offset(0f, bottomY), Offset(w, bottomY), strokeWidth = 1.8f)
                    val lineSpacing = (bottomY - headerH) / 6f
                    for (i in 1..5) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(splitX, y), Offset(w, y), strokeWidth = 1f)
                    }
                }
                type == "research" -> {
                    val headerH = h * 0.14f
                    val midX = w * 0.5f
                    val bottomY = h * 0.82f
                    drawLine(accentLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.8f)
                    drawLine(accentLineColor, Offset(midX, headerH), Offset(midX, bottomY), strokeWidth = 1.5f)
                    drawLine(accentLineColor, Offset(0f, bottomY), Offset(w, bottomY), strokeWidth = 1.8f)
                    val lineSpacing = (bottomY - headerH) / 5f
                    for (i in 1..4) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                    }
                }
                type == "planner" -> {
                    val headerH = h * 0.12f
                    val splitX = w * 0.44f
                    drawLine(primaryLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.8f)
                    drawLine(primaryLineColor, Offset(splitX, headerH), Offset(splitX, h), strokeWidth = 1.5f)
                    // Schedule slots on left
                    val schedLines = 7
                    val schedSpacing = (h - headerH) / schedLines
                    for (i in 1 until schedLines) {
                        val y = headerH + i * schedSpacing
                        drawLine(gridLineColor, Offset(0f, y), Offset(splitX, y), strokeWidth = 1f)
                    }
                    // Checklist on right
                    val todoY = headerH + (h - headerH) * 0.45f
                    drawLine(primaryLineColor, Offset(splitX, todoY), Offset(w, todoY), strokeWidth = 1.2f)
                    for (i in 1..3) {
                        val y = headerH + i * ((todoY - headerH) / 4f)
                        drawCircle(accentLineColor, radius = 3f, center = Offset(splitX + 12f, y))
                        drawLine(gridLineColor, Offset(splitX + 22f, y), Offset(w - 10f, y), strokeWidth = 1f)
                    }
                }
                type == "journal" -> {
                    val headerH = h * 0.14f
                    val morningH = h * 0.35f
                    val eveningH = h * 0.78f
                    drawLine(accentLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.8f)
                    drawLine(accentLineColor, Offset(0f, morningH), Offset(w, morningH), strokeWidth = 1.2f)
                    drawLine(accentLineColor, Offset(0f, eveningH), Offset(w, eveningH), strokeWidth = 1.2f)
                    val lineSpacing = (eveningH - morningH) / 5f
                    for (i in 1..4) {
                        val y = morningH + i * lineSpacing
                        drawLine(gridLineColor, Offset(w * 0.08f, y), Offset(w * 0.92f, y), strokeWidth = 1f)
                    }
                }
                type == "meeting" -> {
                    val headerH = h * 0.15f
                    val splitX = w * 0.52f
                    val actionsY = h * 0.55f
                    drawLine(primaryLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.8f)
                    drawLine(primaryLineColor, Offset(splitX, headerH), Offset(splitX, h), strokeWidth = 1.5f)
                    drawLine(primaryLineColor, Offset(splitX, actionsY), Offset(w, actionsY), strokeWidth = 1.5f)
                    val lineSpacing = (h - headerH) / 6f
                    for (i in 1..5) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(w * 0.05f, y), Offset(splitX - 6f, y), strokeWidth = 1f)
                    }
                }
                type.contains("music") || type.contains("staff") -> {
                    val staves = 4
                    val staffSpacing = h / (staves + 1)
                    for (s in 1..staves) {
                        val startY = s * staffSpacing - 12f
                        for (line in 0..4) {
                            val y = startY + line * 6f
                            drawLine(if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF334155), Offset(w * 0.08f, y), Offset(w * 0.92f, y), strokeWidth = 1.2f)
                        }
                        // Left & measure vertical barlines
                        drawLine(if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF334155), Offset(w * 0.08f, startY), Offset(w * 0.08f, startY + 24f), strokeWidth = 1.8f)
                        drawLine(if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8), Offset(w * 0.5f, startY), Offset(w * 0.5f, startY + 24f), strokeWidth = 1.2f)
                        drawLine(if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF334155), Offset(w * 0.92f, startY), Offset(w * 0.92f, startY + 24f), strokeWidth = 1.8f)
                    }
                }
                type == "storyboard" -> {
                    val fW = w * 0.42f
                    val fH = h * 0.22f
                    val marginX = w * 0.05f
                    val gapX = w * 0.06f
                    val top1 = h * 0.08f
                    val top2 = h * 0.38f
                    val top3 = h * 0.68f

                    listOf(top1, top2, top3).forEach { rowTop ->
                        // Left frame
                        drawRoundRect(primaryLineColor, Offset(marginX, rowTop), Size(fW, fH), cornerRadius = CornerRadius(4f), style = DrawStroke(1.5f))
                        drawLine(gridLineColor, Offset(marginX, rowTop + fH + 8f), Offset(marginX + fW, rowTop + fH + 8f), strokeWidth = 1f)
                        drawLine(gridLineColor, Offset(marginX, rowTop + fH + 16f), Offset(marginX + fW, rowTop + fH + 16f), strokeWidth = 1f)

                        // Right frame
                        drawRoundRect(primaryLineColor, Offset(marginX + fW + gapX, rowTop), Size(fW, fH), cornerRadius = CornerRadius(4f), style = DrawStroke(1.5f))
                        drawLine(gridLineColor, Offset(marginX + fW + gapX, rowTop + fH + 8f), Offset(marginX + fW * 2 + gapX, rowTop + fH + 8f), strokeWidth = 1f)
                        drawLine(gridLineColor, Offset(marginX + fW + gapX, rowTop + fH + 16f), Offset(marginX + fW * 2 + gapX, rowTop + fH + 16f), strokeWidth = 1f)
                    }
                }
                type.contains("2-col") || type.contains("twocolumn") -> {
                    val midX = w * 0.5f
                    val headerH = h * 0.12f
                    drawLine(primaryLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 1.5f)
                    drawLine(marginLineColor, Offset(midX, 0f), Offset(midX, h), strokeWidth = 1.8f)
                    val lineSpacing = (h - headerH) / 8f
                    for (i in 1..7) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(0f, y), Offset(midX - 4f, y), strokeWidth = 1f)
                        drawLine(gridLineColor, Offset(midX + 4f, y), Offset(w, y), strokeWidth = 1f)
                    }
                }
                type.contains("legal") -> {
                    val topMargin = h * 0.14f
                    val marginX = w * 0.22f
                    val marginX2 = w * 0.22f + 4f
                    drawLine(marginLineColor, Offset(marginX, 0f), Offset(marginX, h), strokeWidth = 1.5f)
                    drawLine(marginLineColor, Offset(marginX2, 0f), Offset(marginX2, h), strokeWidth = 1.2f)
                    val lineSpacing = (h - topMargin) / 9f
                    for (i in 1..8) {
                        val y = topMargin + i * lineSpacing
                        drawLine(gridLineColor, Offset(marginX2, y), Offset(w, y), strokeWidth = 1f)
                    }
                }
                else -> {
                    val lineSpacing = h / 9f
                    for (i in 1..8) {
                        val y = i * lineSpacing
                        drawLine(gridLineColor, Offset(w * 0.08f, y), Offset(w * 0.92f, y), strokeWidth = 1f)
                    }
                }
            }
        }
    }
}

/**
 * Modern Full-Screen Notebook Studio (Redesigned Notebook Creation Experience)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotebookStudioDialog(
    note: NoteEntity? = null,
    onDismiss: () -> Unit,
    onSave: ((templateType: String, coverType: String, pageColor: Long, coverTitle: String, coverSubtitle: String, coverAuthor: String, coverExtra: String) -> Unit)? = null,
    onCreateNew: ((title: String, templateType: String, coverType: String, pageColor: Long, coverTitle: String, coverSubtitle: String, coverAuthor: String, coverExtra: String, folder: String, isLocked: Boolean, pinCode: String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // Wizard Step State: 0 = Notebook Details, 1 = Paper Type, 2 = Cover Design, 3 = Review
    var currentStep by remember { mutableStateOf(0) }

    // Step 1: Notebook Details
    var notebookName by remember { mutableStateOf(note?.title?.ifBlank { "Physics Lecture Notes" } ?: "Physics Lecture Notes") }
    var folderName by remember { mutableStateOf(note?.tags?.replace("dir:", "")?.split(",")?.firstOrNull()?.trim() ?: "School") }
    var subjectTag by remember { mutableStateOf("Physics & Quantum") }
    var selectedThemeColor by remember { mutableStateOf(Color(0xFF4F46E5)) }
    var notebookSize by remember { mutableStateOf("A4") } // A4, A5, Letter, Legal
    var notebookOrientation by remember { mutableStateOf("Portrait") } // Portrait, Landscape

    // Step 2: Paper Type
    var currentTemplateType by remember { mutableStateOf(note?.templateType ?: "ruled") }
    var currentPageColor by remember { mutableStateOf(note?.pageColor ?: 0xFFFFFFFF) }
    var paperSearchQuery by remember { mutableStateOf("") }
    var selectedPaperCategory by remember { mutableStateOf("All") }

    // Step 3: Cover Gallery & AI Generator
    var currentCoverType by remember { mutableStateOf(note?.coverType ?: "3d_academic") }
    var coverTitle by remember { mutableStateOf(note?.coverTitle?.ifBlank { null } ?: note?.title?.ifBlank { null } ?: notebookName) }
    var coverSubtitle by remember { mutableStateOf(note?.coverSubtitle?.ifBlank { "Semester I · 2026 Edition" } ?: "Semester I · 2026 Edition") }
    var coverAuthor by remember { mutableStateOf(note?.coverAuthor?.ifBlank { "Alex Rivera" } ?: "Alex Rivera") }
    var coverExtra by remember { mutableStateOf(note?.coverExtra?.ifBlank { "Lipi Studio Premium" } ?: "Lipi Studio Premium") }
    var coverSearchQuery by remember { mutableStateOf("") }
    var selectedCoverFilter by remember { mutableStateOf("All") }
    var selectedCoverCategory by remember { mutableStateOf("3D Covers") }

    // AI Cover Generator state
    var aiPromptInput by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }

    // Live 3D Preview State
    var isPreviewFlipped by remember { mutableStateOf(false) } // false = cover, true = paper page
    var compactTabState by remember { mutableStateOf(0) } // 0 = Edit Settings, 1 = Live 3D Preview

    // Step 4 Toggles & Security
    var isFavorite by remember { mutableStateOf(false) }
    var autoBackup by remember { mutableStateOf(true) }
    var passcodeLock by remember { mutableStateOf(note?.isLocked ?: false) }
    var configuredPin by remember { mutableStateOf(note?.pinCode ?: "") }
    var showSetPinModal by remember { mutableStateOf(false) }

    // Update coverTitle when notebookName changes if coverTitle was synchronized
    LaunchedEffect(notebookName) {
        if (coverTitle == "Physics Lecture Notes" || coverTitle == note?.title || coverTitle.isBlank()) {
            coverTitle = notebookName
        }
    }

    val pageColors = listOf(
        0xFFFFFFFF to "Pure White",
        0xFFFFF8E7 to "Warm Cream",
        0xFF1A1A1A to "Midnight Dark",
        0xFFF5F5F5 to "Soft Gray",
        0xFFE3F2FD to "Sky Pastel"
    )

    if (showSetPinModal) {
        SetNotebookPasscodeDialog(
            notebookTitle = notebookName,
            onDismiss = {
                if (configuredPin.isBlank()) {
                    passcodeLock = false
                }
                showSetPinModal = false
            },
            onPasscodeSet = { pin, bio ->
                configuredPin = pin
                passcodeLock = true
                showSetPinModal = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FC))
        ) {
            val availableWidthDp = maxWidth
            val availableHeightDp = maxHeight

            val isCompactWidth = availableWidthDp < 720.dp
            val isUltraCompact = availableWidthDp < 480.dp
            val isShortHeight = availableHeightDp < 550.dp

            val outerPadding = when {
                isShortHeight -> 6.dp
                isUltraCompact -> 8.dp
                isCompactWidth -> 12.dp
                else -> 16.dp
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F8FC)),
                color = Color(0xFFF7F8FC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(outerPadding)
                ) {
                // ==========================================
                // 1. STUDIO HEADER & STEP INDICATOR
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isUltraCompact) 12.dp else 18.dp,
                                vertical = if (isShortHeight || isUltraCompact) 8.dp else 12.dp
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isUltraCompact) 8.dp else 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isUltraCompact) 36.dp else 42.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(LipiStudioPrimary, Color(0xFF8B5CF6))
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = "Lipi Studio",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isUltraCompact) 20.dp else 22.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Notebook Studio",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = if (isUltraCompact) 17.sp else 19.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LipiStudioPrimary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LipiStudioPrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (!isShortHeight) {
                                        Text(
                                            text = if (isUltraCompact) "Bespoke 3D notebooks & paper patterns" else "Craft bespoke notebooks with realistic 3D covers and paper patterns",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .background(Color(0xFFF1F5F9), CircleShape)
                                    .size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF334155), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isShortHeight || isUltraCompact) 8.dp else 12.dp))

                        // Step Wizard Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isUltraCompact) 4.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val steps = if (isUltraCompact) {
                                listOf("1. Details", "2. Paper", "3. Cover", "4. Review")
                            } else {
                                listOf("① Details", "② Paper Type", "③ Cover Design", "④ Review")
                            }
                            steps.forEachIndexed { index, title ->
                                val isSelected = currentStep == index
                                val isDone = currentStep > index

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { currentStep = index },
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isSelected -> LipiStudioPrimary
                                        isDone -> LipiStudioPrimary.copy(alpha = 0.15f)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = if (isShortHeight || isUltraCompact) 6.dp else 9.dp,
                                                horizontal = if (isUltraCompact) 4.dp else 8.dp
                                            ),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = LipiStudioPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        }
                                        Text(
                                            text = title,
                                            fontSize = if (isUltraCompact) 10.sp else 12.sp,
                                            fontWeight = if (isSelected || isDone) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                isDone -> LipiStudioPrimary
                                                else -> Color(0xFF64748B)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isShortHeight || isUltraCompact) 8.dp else 12.dp))

                // ==========================================
                // 2. MAIN TWO-PANEL CONTENT (LEFT: WIZARD, RIGHT: LIVE PREVIEW)
                // ==========================================
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT PANEL — STEP WIZARD SETTINGS
                    Card(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                                },
                                label = "StudioStepTransition"
                            ) { step ->
                                when (step) {
                                    0 -> Step1NotebookDetails(
                                        notebookName = notebookName,
                                        onNameChange = { notebookName = it },
                                        folderName = folderName,
                                        onFolderChange = { folderName = it },
                                        subjectTag = subjectTag,
                                        onSubjectTagChange = { subjectTag = it },
                                        selectedThemeColor = selectedThemeColor,
                                        onThemeColorSelected = { selectedThemeColor = it },
                                        notebookSize = notebookSize,
                                        onSizeSelected = { notebookSize = it },
                                        orientation = notebookOrientation,
                                        onOrientationSelected = { notebookOrientation = it }
                                    )
                                    1 -> Step2PaperTypes(
                                        selectedTemplate = currentTemplateType,
                                        onTemplateSelected = { currentTemplateType = it },
                                        selectedColor = currentPageColor,
                                        onColorSelected = { currentPageColor = it },
                                        searchQuery = paperSearchQuery,
                                        onSearchQueryChange = { paperSearchQuery = it },
                                        selectedCategory = selectedPaperCategory,
                                        onCategorySelected = { selectedPaperCategory = it },
                                        availableColors = pageColors
                                    )
                                    2 -> Step3CoverGallery(
                                        selectedCover = currentCoverType,
                                        onCoverSelected = { currentCoverType = it },
                                        coverTitle = coverTitle,
                                        coverSubtitle = coverSubtitle,
                                        coverAuthor = coverAuthor,
                                        coverExtra = coverExtra,
                                        onTitleChange = { coverTitle = it },
                                        onSubtitleChange = { coverSubtitle = it },
                                        onAuthorChange = { coverAuthor = it },
                                        onExtraChange = { coverExtra = it },
                                        searchQuery = coverSearchQuery,
                                        onSearchQueryChange = { coverSearchQuery = it },
                                        selectedFilter = selectedCoverFilter,
                                        onFilterSelected = { selectedCoverFilter = it },
                                        selectedCategory = selectedCoverCategory,
                                        onCategorySelected = { selectedCoverCategory = it },
                                        aiPromptInput = aiPromptInput,
                                        onAiPromptChange = { aiPromptInput = it },
                                        isAiGenerating = isAiGenerating,
                                        onGenerateAiCover = { prompt ->
                                            coroutineScope.launch {
                                                isAiGenerating = true
                                                delay(600)
                                                val aiCovers = listOf("3d_tech", "3d_creative", "3d_luxury", "3d_glass", "3d_nature", "subject_physics", "subject_computer")
                                                currentCoverType = aiCovers.random()
                                                if (prompt.isNotBlank()) {
                                                    coverTitle = prompt.split(" ").take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
                                                    coverSubtitle = "AI Generated Cover · 2026"
                                                }
                                                isAiGenerating = false
                                            }
                                        }
                                    )
                                    3 -> Step4ReviewAndFinalize(
                                        notebookName = notebookName,
                                        folderName = folderName,
                                        subjectTag = subjectTag,
                                        notebookSize = notebookSize,
                                        orientation = notebookOrientation,
                                        templateType = currentTemplateType,
                                        coverType = currentCoverType,
                                        pageColor = currentPageColor,
                                        coverTitle = coverTitle,
                                        coverSubtitle = coverSubtitle,
                                        coverAuthor = coverAuthor,
                                        isFavorite = isFavorite,
                                        onToggleFavorite = { isFavorite = !isFavorite },
                                        autoBackup = autoBackup,
                                        onToggleAutoBackup = { autoBackup = !autoBackup },
                                        passcodeLock = passcodeLock,
                                        configuredPin = configuredPin,
                                        onTogglePasscode = {
                                            if (!passcodeLock) {
                                                showSetPinModal = true
                                            } else {
                                                passcodeLock = false
                                                configuredPin = ""
                                            }
                                        },
                                        onConfigurePin = {
                                            showSetPinModal = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT PANEL — LARGE LIVE 3D NOTEBOOK PREVIEW
                    Card(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = LipiStudioPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Live Studio Preview",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Surface(
                                    modifier = Modifier.clickable { isPreviewFlipped = !isPreviewFlipped },
                                    shape = RoundedCornerShape(12.dp),
                                    color = LipiStudioPrimary.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, LipiStudioPrimary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlipToBack,
                                            contentDescription = "Flip",
                                            tint = LipiStudioPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isPreviewFlipped) "View Cover" else "Flip Inside",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LipiStudioPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3D Realist Notebook Stage Container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFC)),
                                            center = Offset(200f, 200f),
                                            radius = 800f
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val isPortrait = notebookOrientation == "Portrait"
                                val aspect = if (isPortrait) 0.72f else 1.35f

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(0.92f)
                                        .aspectRatio(aspect)
                                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(12.dp), clip = false)
                                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                                ) {
                                    // Simulated Paper Thickness Edges (Stacked 3D Pages)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(12.dp)
                                            .align(Alignment.CenterEnd)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFFF1F5F9))
                                                )
                                            )
                                    )

                                    // Bookmark Ribbon Dangling
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(50.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-24).dp, y = (-8).dp)
                                            .background(selectedThemeColor, shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                            .shadow(2.dp)
                                    )

                                    // 3D Front Cover OR Paper Page View
                                    if (!isPreviewFlipped) {
                                        if (isAiGenerating) {
                                            AiCoverLoadingSkeleton(modifier = Modifier.fillMaxSize())
                                        } else {
                                            RenderCover(
                                                coverType = currentCoverType,
                                                title = coverTitle,
                                                subtitle = coverSubtitle,
                                                author = coverAuthor,
                                                extra = coverExtra,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(currentPageColor))
                                        ) {
                                            PageTemplateCanvasPreview(
                                                templateType = currentTemplateType,
                                                pageColor = currentPageColor,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            // Simulated Header Note Title
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = notebookName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentPageColor == 0xFF1A1A1AL) Color.White else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "Page 1 · $subjectTag",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    // Left Spine Binding (Ring Binder / Leather Spine Detail)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(18.dp)
                                            .align(Alignment.CenterStart)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.4f),
                                                        Color.Black.copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxHeight(),
                                            verticalArrangement = Arrangement.SpaceEvenly,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            repeat(8) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFFCBD5E1), CircleShape)
                                                        .border(1.dp, Color(0xFF64748B), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Live Notebook Info Badge
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = notebookName.ifBlank { "Untitled Notebook" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$notebookSize · $notebookOrientation · ${currentTemplateType.replaceFirstChar { it.uppercase() }} Paper",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = selectedThemeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = folderName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = selectedThemeColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isShortHeight || isUltraCompact) 6.dp else 12.dp))

                // ==========================================
                // 3. BOTTOM ACTION BAR
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isUltraCompact) 10.dp else 16.dp,
                                vertical = if (isShortHeight || isUltraCompact) 6.dp else 10.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(if (isUltraCompact) 4.dp else 8.dp)) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = { currentStep -= 1 },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = if (isUltraCompact) 10.dp else 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    if (!isUltraCompact) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back")
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    notebookName = "Physics Lecture Notes"
                                    currentTemplateType = "ruled"
                                    currentCoverType = "3d_academic"
                                    currentPageColor = 0xFFFFFFFF
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            }

                            if (!isUltraCompact) {
                                TextButton(onClick = { /* Save custom template preset */ }) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save")
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStep < 3) {
                                    currentStep += 1
                                } else {
                                    if (onCreateNew != null) {
                                        onCreateNew(
                                            notebookName,
                                            currentTemplateType,
                                            currentCoverType,
                                            currentPageColor,
                                            coverTitle,
                                            coverSubtitle,
                                            coverAuthor,
                                            coverExtra,
                                            folderName,
                                            passcodeLock && configuredPin.isNotBlank(),
                                            configuredPin
                                        )
                                    } else if (onSave != null) {
                                        onSave(
                                            currentTemplateType,
                                            currentCoverType,
                                            currentPageColor,
                                            coverTitle,
                                            coverSubtitle,
                                            coverAuthor,
                                            coverExtra
                                        )
                                    }
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LipiStudioPrimary, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = if (isUltraCompact) 14.dp else 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (currentStep < 3) "Next Step" else "Create Notebook",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (currentStep < 3) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Step 1: Notebook Details Component
 */
@Composable
private fun Step1NotebookDetails(
    notebookName: String,
    onNameChange: (String) -> Unit,
    folderName: String,
    onFolderChange: (String) -> Unit,
    subjectTag: String,
    onSubjectTagChange: (String) -> Unit,
    selectedThemeColor: Color,
    onThemeColorSelected: (Color) -> Unit,
    notebookSize: String,
    onSizeSelected: (String) -> Unit,
    orientation: String,
    onOrientationSelected: (String) -> Unit
) {
    val themeColors = listOf(
        Color(0xFF4F46E5) to "Indigo",
        Color(0xFF10B981) to "Emerald",
        Color(0xFFEF4444) to "Crimson",
        Color(0xFFF59E0B) to "Amber",
        Color(0xFF8B5CF6) to "Violet",
        Color(0xFF06B6D4) to "Ocean"
    )

    val presetTitles = listOf(
        "Physics Lecture Notes",
        "2026 Daily Planner",
        "Design Systems Journal",
        "Computer Science",
        "Biology Lab",
        "Math & Calculus"
    )

    val folders = listOf("School", "Work", "Personal", "Research", "General")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Notebook Information", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
            Text("Set title, folder category, theme color, paper size & orientation", fontSize = 12.sp, color = Color(0xFF64748B))
        }

        item {
            OutlinedTextField(
                value = notebookName,
                onValueChange = onNameChange,
                label = { Text("Notebook Name") },
                placeholder = { Text("e.g., Quantum Physics, Product Roadmap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                trailingIcon = {
                    if (notebookName.isNotEmpty()) {
                        IconButton(onClick = { onNameChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Preset Title Suggestions:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetTitles) { preset ->
                    FilterChip(
                        selected = notebookName == preset,
                        onClick = { onNameChange(preset) },
                        label = { Text(preset, fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Folder Category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(folders) { folder ->
                            val isSel = folderName == folder
                            FilterChip(
                                selected = isSel,
                                onClick = { onFolderChange(folder) },
                                label = { Text(folder, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Subject Tag", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = subjectTag,
                        onValueChange = onSubjectTagChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Text("Color Theme Accent", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                themeColors.forEach { (colorVal, name) ->
                    val isSel = selectedThemeColor == colorVal
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorVal)
                            .border(
                                width = if (isSel) 3.dp else 0.dp,
                                color = if (isSel) Color(0xFF0F172A) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onThemeColorSelected(colorVal) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("Notebook Size", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val sizes = listOf(
                    "A4" to "210 × 297 mm",
                    "A5" to "148 × 210 mm",
                    "Letter" to "8.5 × 11 in",
                    "Legal" to "8.5 × 14 in"
                )
                sizes.forEach { (sz, desc) ->
                    val isSel = notebookSize == sz
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSizeSelected(sz) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) LipiStudioPrimary else Color(0xFFE2E8F0)),
                        colors = CardDefaults.cardColors(containerColor = if (isSel) LipiStudioPrimary.copy(alpha = 0.08f) else Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(sz, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSel) LipiStudioPrimary else Color(0xFF0F172A))
                            Text(desc, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item {
            Text("Orientation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val orientations = listOf(
                    "Portrait" to "Vertical Document View",
                    "Landscape" to "Wide Horizontal Canvas"
                )
                orientations.forEach { (orient, desc) ->
                    val isSel = orientation == orient
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOrientationSelected(orient) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) LipiStudioPrimary else Color(0xFFE2E8F0)),
                        colors = CardDefaults.cardColors(containerColor = if (isSel) LipiStudioPrimary.copy(alpha = 0.08f) else Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (orient == "Portrait") Icons.Default.CropPortrait else Icons.Default.CropLandscape,
                                contentDescription = null,
                                tint = if (isSel) LipiStudioPrimary else Color.Gray
                            )
                            Column {
                                Text(orient, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSel) LipiStudioPrimary else Color(0xFF0F172A))
                                Text(desc, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 2: Paper Types Component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2PaperTypes(
    selectedTemplate: String,
    onTemplateSelected: (String) -> Unit,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    availableColors: List<Pair<Long, String>>
) {
    val categories = listOf("All", "Standard", "Study & Science", "Planners & Journals", "Creative & Music")

    val allPaperTypes = listOf(
        PaperTypeData("blank", "Blank Sheet", "Simple clean canvas for drawing & sketching", "Standard", "Mind maps, freehand sketches & diagrams"),
        PaperTypeData("ruled", "Ruled Line", "Standard horizontal lines with margin guide", "Standard", "General note-taking & lectures"),
        PaperTypeData("grid", "Grid Pattern", "Precise square grid for math & technical diagrams", "Standard", "Graphs, UI wireframes, Math equations"),
        PaperTypeData("dotted", "Dot Grid", "Minimal dot pattern for flexible bullet journaling", "Standard", "Bullet journaling & custom layouts"),
        PaperTypeData("cornell", "Cornell Notes", "Split layout with cues column & summary section", "Study & Science", "Lectures, Exam prep & structured summaries"),
        PaperTypeData("engineering", "Engineering Graph", "Dense graph layout with axes guides", "Study & Science", "Circuits, Physics formulas & CAD sketches"),
        PaperTypeData("lecture", "Lecture Notes", "Class header, key points & review column", "Study & Science", "University classes & seminar notes"),
        PaperTypeData("research", "Research Notes", "Hypothesis, methodology & reference columns", "Study & Science", "Scientific papers & literature reviews"),
        PaperTypeData("planner", "Daily Planner", "Schedule timeline, priorities & action items", "Planners & Journals", "Daily time-blocking & task tracking"),
        PaperTypeData("journal", "Daily Journal", "Reflection prompts & gratitude log", "Planners & Journals", "Daily mindfulness & personal journal"),
        PaperTypeData("meeting", "Meeting Minutes", "Agenda, action items & attendee checklist", "Planners & Journals", "Team syncs & client meetings"),
        PaperTypeData("music", "Music Staff", "Five-line staves for musical notation", "Creative & Music", "Composition, Songwriting & harmony"),
        PaperTypeData("storyboard", "Storyboard", "Frame boxes with caption lines for video scenes", "Creative & Music", "Video planning, comics & animation"),
        PaperTypeData("2-col-ruled", "2-Column Ruled", "Dual column ruled sheet for side-by-side notes", "Standard", "Vocabulary, translation & comparative analysis"),
        PaperTypeData("legal-ruled", "Legal Ruled", "Wide left margin rule sheet", "Standard", "Structured legal briefs & outlines")
    )

    val filteredPapers = allPaperTypes.filter { paper ->
        val matchesCategory = selectedCategory == "All" || paper.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() || paper.name.contains(searchQuery, ignoreCase = true) || paper.desc.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Paper Layout Gallery", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
        Text("Choose paper grid pattern, rule background color, and structure", fontSize = 12.sp, color = Color(0xFF64748B))

        Spacer(modifier = Modifier.height(12.dp))

        // Search + Paper Color Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search Paper Types...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Color Swatches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableColors.forEach { (colorVal, name) ->
                    val isSel = selectedColor == colorVal
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal))
                            .border(1.dp, Color.LightGray, CircleShape)
                            .border(
                                width = if (isSel) 2.dp else 0.dp,
                                color = if (isSel) LipiStudioPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(colorVal) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) {
                            Box(modifier = Modifier.size(8.dp).background(LipiStudioPrimary, CircleShape))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                FilterChip(
                    selected = isSel,
                    onClick = { onCategorySelected(cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of Paper Type Cards
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredPapers) { paper ->
                val isSel = selectedTemplate == paper.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTemplateSelected(paper.id) },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(if (isSel) 2.5.dp else 1.dp, if (isSel) LipiStudioPrimary else Color(0xFFE2E8F0)),
                    colors = CardDefaults.cardColors(containerColor = if (isSel) LipiStudioPrimary.copy(alpha = 0.05f) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSel) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        ) {
                            PageTemplateCanvasPreview(
                                templateType = paper.id,
                                pageColor = selectedColor,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSel) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(LipiStudioPrimary, CircleShape)
                                        .padding(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = paper.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = paper.desc,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(28.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = paper.recommended,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = LipiStudioPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PaperTypeData(
    val id: String,
    val name: String,
    val desc: String,
    val category: String,
    val recommended: String
)

/**
 * Step 3: Cover Gallery & AI Cover Generator Component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3CoverGallery(
    selectedCover: String,
    onCoverSelected: (String) -> Unit,
    coverTitle: String,
    coverSubtitle: String,
    coverAuthor: String,
    coverExtra: String,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onExtraChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    aiPromptInput: String,
    onAiPromptChange: (String) -> Unit,
    isAiGenerating: Boolean,
    onGenerateAiCover: (String) -> Unit
) {
    val filters = listOf("All", "Popular", "New", "Minimal", "School", "Business", "Creative", "AI Generated")
    val categories = listOf("3D Covers", "Subject Covers", "Academic", "Journals", "Creative", "Illustrations", "Basic")

    val coverMap = mapOf(
        "3D Covers" to listOf("3d_academic", "3d_journal", "3d_tech", "3d_creative", "3d_luxury", "3d_glass", "3d_nature", "3d_minimal"),
        "Subject Covers" to listOf("subject_math", "subject_gk_gs", "subject_current_affairs", "subject_reasoning", "subject_hindi", "subject_english", "subject_science", "subject_sst", "subject_computer", "subject_physics", "subject_chemistry", "subject_biology", "subject_history"),
        "Academic" to listOf("science", "earth", "language", "english", "math"),
        "Journals" to listOf("journal", "daily"),
        "Creative" to listOf("treehouse", "geo1", "geo2", "geo3"),
        "Illustrations" to listOf("tiger", "reader", "sketch", "wash", "ink", "car"),
        "Basic" to listOf("dark", "light", "none")
    )

    val currentCovers = coverMap[selectedCategory] ?: emptyList()
    val filteredCovers = currentCovers.filter { cover ->
        searchQuery.isBlank() || cover.contains(searchQuery, ignoreCase = true)
    }

    val aiPrompts = listOf("Machine Learning", "Physics", "Business Notes", "Anime Theme", "Minimal Blue", "Cyber Theme", "Nature Theme")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Notebook Cover Studio", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
            Text("Browse 3D covers, subject graphics, or generate a custom cover with AI", fontSize = 12.sp, color = Color(0xFF64748B))
        }

        // AI Cover Generator Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Unspecified),
                border = BorderStroke(1.dp, LipiStudioPrimary.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF2563EB))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Text("Generate Cover with AI", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
                        }
                        Text("Describe a theme, subject, or style to render a custom AI cover", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aiPromptInput,
                                onValueChange = onAiPromptChange,
                                placeholder = { Text("e.g. Cyberpunk Neon Physics", fontSize = 12.sp, color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = { onGenerateAiCover(aiPromptInput) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                if (isAiGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LipiStudioPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text("Generate", color = LipiStudioPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(aiPrompts) { prompt ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        onAiPromptChange(prompt)
                                        onGenerateAiCover(prompt)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "✨ $prompt",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Filter Chips & Category Tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filters) { flt ->
                        val isSel = selectedFilter == flt
                        FilterChip(
                            selected = isSel,
                            onClick = { onFilterSelected(flt) },
                            label = { Text(flt, fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSel = selectedCategory == cat
                        Surface(
                            modifier = Modifier.clickable { onCategorySelected(cat) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) LipiStudioPrimary else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Cover Gallery Grid Box
        item {
            Box(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(95.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCovers) { cover ->
                        val isSel = selectedCover == cover
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onCoverSelected(cover) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f)
                                    .shadow(if (isSel) 6.dp else 2.dp, shape = RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSel) 2.5.dp else 1.dp,
                                        color = if (isSel) LipiStudioPrimary else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                RenderCover(
                                    coverType = cover,
                                    title = coverTitle,
                                    subtitle = coverSubtitle,
                                    author = coverAuthor,
                                    extra = coverExtra,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSel) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(LipiStudioPrimary, CircleShape)
                                            .padding(3.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cover.replace("subject_", "").replace("3d_", "").replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) LipiStudioPrimary else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Customizable Cover Text Fields
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Custom Cover Text Overlay", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = coverTitle,
                            onValueChange = onTitleChange,
                            label = { Text("Title / Subject") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = coverAuthor,
                            onValueChange = onAuthorChange,
                            label = { Text("Author / Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = coverSubtitle,
                            onValueChange = onSubtitleChange,
                            label = { Text("Subtitle") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = coverExtra,
                            onValueChange = onExtraChange,
                            label = { Text("Edition / Vol") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 4: Review and Finalize Component
 */
@Composable
private fun Step4ReviewAndFinalize(
    notebookName: String,
    folderName: String,
    subjectTag: String,
    notebookSize: String,
    orientation: String,
    templateType: String,
    coverType: String,
    pageColor: Long,
    coverTitle: String,
    coverSubtitle: String,
    coverAuthor: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    autoBackup: Boolean,
    onToggleAutoBackup: () -> Unit,
    passcodeLock: Boolean,
    configuredPin: String = "",
    onTogglePasscode: () -> Unit,
    onConfigurePin: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Review Notebook Specifications", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
            Text("Verify your notebook configuration before creating", fontSize = 12.sp, color = Color(0xFF64748B))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(notebookName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Folder: $folderName · Tag: $subjectTag", fontSize = 12.sp, color = Color(0xFF64748B))
                        }

                        Surface(shape = RoundedCornerShape(10.dp), color = LipiStudioPrimary.copy(alpha = 0.12f)) {
                            Text("READY TO BUILD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LipiStudioPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Paper Layout", fontSize = 11.sp, color = Color.Gray)
                            Text(templateType.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column {
                            Text("Size & Aspect", fontSize = 11.sp, color = Color.Gray)
                            Text("$notebookSize ($orientation)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column {
                            Text("Cover Style", fontSize = 11.sp, color = Color.Gray)
                            Text(coverType.replace("3d_", "").replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("Quick Options", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionToggleCard(
                    title = "Add to Favorite Notebooks",
                    subtitle = "Pin notebook to top of home dashboard",
                    icon = Icons.Default.Star,
                    isChecked = isFavorite,
                    onToggle = onToggleFavorite
                )

                OptionToggleCard(
                    title = "Auto Sync to Google Drive",
                    subtitle = "Automatic background backup & cloud sync",
                    icon = Icons.Default.CloudSync,
                    isChecked = autoBackup,
                    onToggle = onToggleAutoBackup
                )

                OptionToggleCard(
                    title = "Passcode & Biometric Protection",
                    subtitle = if (passcodeLock && configuredPin.isNotBlank()) "Protected with 4-digit PIN • Biometric enabled" else "Require PIN or biometric unlock for this notebook",
                    icon = Icons.Default.Lock,
                    isChecked = passcodeLock,
                    onToggle = onTogglePasscode,
                    extraContent = if (passcodeLock && configuredPin.isNotBlank()) {
                        {
                            TextButton(
                                onClick = onConfigurePin,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Change PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LipiStudioPrimary)
                            }
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun OptionToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isChecked) LipiStudioPrimary.copy(alpha = 0.12f) else Color(0xFFF1F5F9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (isChecked) LipiStudioPrimary else Color.Gray, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (extraContent != null) {
                    extraContent()
                }
                Switch(checked = isChecked, onCheckedChange = { onToggle() })
            }
        }
    }
}

/**
 * Backward-compatible AdvancedTemplateDialog entry point
 */
@Composable
fun AdvancedTemplateDialog(
    note: NoteEntity? = null,
    onDismiss: () -> Unit,
    onSave: ((templateType: String, coverType: String, pageColor: Long, coverTitle: String, coverSubtitle: String, coverAuthor: String, coverExtra: String) -> Unit)? = null,
    onCreateNew: ((title: String, templateType: String, coverType: String, pageColor: Long, coverTitle: String, coverSubtitle: String, coverAuthor: String, coverExtra: String, folder: String, isLocked: Boolean, pinCode: String) -> Unit)? = null
) {
    NotebookStudioDialog(
        note = note,
        onDismiss = onDismiss,
        onSave = onSave,
        onCreateNew = onCreateNew
    )
}

@Composable
fun AiCoverLoadingSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF94A3B8).copy(alpha = alpha),
                        Color(0xFFCBD5E1).copy(alpha = alpha)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Generating",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Rendering AI Design...",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
