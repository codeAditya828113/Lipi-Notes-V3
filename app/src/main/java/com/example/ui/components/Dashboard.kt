package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// LIPI COLOR SYSTEM (Android 16 M3 Expressive)
// ==========================================
private val LipiBgLight = Color(0xFFF7F8FC)
private val LipiBgDark = Color(0xFF0F172A)
private val LipiCardWhite = Color(0xFFFFFFFF)
private val LipiCardDark = Color(0xFF1E293B)

private val LipiPrimary = Color(0xFF5B6DFF)      // Royal Indigo Accent
private val LipiSecondary = Color(0xFF8A7CFF)    // Lavender Accent
private val LipiAccent = Color(0xFF4DA3FF)       // Sky Blue
private val LipiSuccess = Color(0xFF2ECC71)      // Emerald Green
private val LipiWarning = Color(0xFFFF9F43)      // Warm Amber
private val LipiError = Color(0xFFFF5C5C)        // Coral Red

@Composable
fun NovaDashboard(
    notes: List<NoteEntity>,
    viewModel: NoteViewModel,
    onNavigateToNotes: () -> Unit,
    onMenuClick: () -> Unit,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    onNavigateToNotesWithFilter: ((String) -> Unit)? = null
) {
    var showCustomizeGoalsModal by remember { mutableStateOf(false) }

    if (showCustomizeGoalsModal) {
        CustomizeGoalsModal(
            viewModel = viewModel,
            onDismiss = { showCustomizeGoalsModal = false }
        )
    }

    val scrollState = rememberScrollState()
    val isDarkTheme = viewModel.themeMode == "dark" || viewModel.themeMode == "oled"

    val bgColor = if (isDarkTheme) LipiBgDark else LipiBgLight
    val cardBg = if (isDarkTheme) LipiCardDark else LipiCardWhite

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Soft ambient background aura for 2026 Material 3 Expressive look
        if (!isDarkTheme) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFEEF2FF), Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.08f),
                        radius = w * 0.65f
                    ),
                    center = Offset(w * 0.15f, h * 0.08f),
                    radius = w * 0.65f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE0E7FF).copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.25f),
                        radius = w * 0.55f
                    ),
                    center = Offset(w * 0.85f, h * 0.25f),
                    radius = w * 0.55f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = if (isTablet) 32.dp else 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. HOME HERO HEADER
            HomeHeroHeader(
                viewModel = viewModel,
                isTablet = isTablet,
                onMenuClick = onMenuClick,
                onNavigateToNotes = onNavigateToNotes,
                onCustomizeGoals = { showCustomizeGoalsModal = true },
                isDark = isDarkTheme
            )

            // 2. TOP METRICS ROW (Hierarchy with Hero Progress Card)
            TopMetricsRow(
                viewModel = viewModel,
                notesCount = notes.size,
                isTablet = isTablet,
                isDark = isDarkTheme,
                cardBg = cardBg
            )

            // 3. MAIN FEATURE: LARGE GLOWING AI SEARCH BAR
            HeroAISearchBar(
                onSearchSubmitted = { query ->
                    onNavigateToNotesWithFilter?.invoke(query) ?: onNavigateToNotes()
                },
                isDark = isDarkTheme,
                cardBg = cardBg
            )

            // 4. QUICK ACTIONS BAR
            QuickActionsRow(
                onActionClick = { action ->
                    when (action) {
                        "New Notebook", "Handwritten Note" -> onNavigateToNotesWithFilter?.invoke("Handwritten") ?: onNavigateToNotes()
                        "Voice Note" -> onNavigateToNotesWithFilter?.invoke("Audio") ?: onNavigateToNotes()
                        "Scan Document", "Import PDF" -> onNavigateToNotesWithFilter?.invoke("PDFs") ?: onNavigateToNotes()
                        "Flashcards" -> onNavigateToNotesWithFilter?.invoke("Flashcards") ?: onNavigateToNotes()
                        else -> onNavigateToNotes()
                    }
                },
                isDark = isDarkTheme,
                cardBg = cardBg
            )

            // 5. TODAY'S FOCUS & POMODORO TIMER
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(modifier = Modifier.weight(1.1f)) {
                        TodaysFocusCard(isDark = isDarkTheme, cardBg = cardBg)
                    }
                    Box(modifier = Modifier.weight(0.9f)) {
                        PomodoroTimerCard(isDark = isDarkTheme, cardBg = cardBg)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    TodaysFocusCard(isDark = isDarkTheme, cardBg = cardBg)
                    PomodoroTimerCard(isDark = isDarkTheme, cardBg = cardBg)
                }
            }

            // 6. CONTINUE WORKING (REALISTIC NOTEBOOK COVERS)
            ContinueWorkingSection(
                notes = notes,
                onNoteClick = { onNavigateToNotes() },
                onViewAllClick = { onNavigateToNotes() },
                isDark = isDarkTheme,
                cardBg = cardBg
            )

            // 7. AI SUGGESTIONS
            AISuggestionsSection(
                onSuggestionClick = { prompt ->
                    onNavigateToNotesWithFilter?.invoke(prompt) ?: onNavigateToNotes()
                },
                isDark = isDarkTheme,
                cardBg = cardBg
            )

            // 8. ANALYTICS & STUDY HEATMAP
            AnalyticsAndHeatmapSection(isDark = isDarkTheme, cardBg = cardBg)

            // 9. CALENDAR & UPCOMING DEADLINES
            UpcomingDeadlinesSection(isDark = isDarkTheme, cardBg = cardBg)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// 1. HOME HERO HEADER
// ==========================================
@Composable
private fun HomeHeroHeader(
    viewModel: NoteViewModel,
    isTablet: Boolean,
    onMenuClick: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onCustomizeGoals: () -> Unit,
    isDark: Boolean
) {
    val todayDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val context = LocalContext.current
    val isSignedIn = GoogleDriveBackupHelper.isSignedIn(context)
    val accountName = if (isSignedIn) GoogleDriveBackupHelper.getSavedAccountName(context) else ""
    val firstName = if (accountName.isNotBlank()) accountName.split(" ").firstOrNull() ?: "Aditya" else "Aditya"

    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isTablet) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = textPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = "$greeting, $firstName 👋",
                            fontSize = if (isTablet) 30.sp else 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ready to continue learning with Lipi AI?",
                            fontSize = 15.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                // Date Widget Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = LipiPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = todayDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress & Goal Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Goal Chip
                    Surface(
                        shape = CircleShape,
                        color = LipiPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { onCustomizeGoals() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Adjust, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Goal: ${viewModel.dailyGoalTargetMinutes}m/day", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LipiPrimary)
                        }
                    }

                    // Progress Chip
                    Surface(
                        shape = CircleShape,
                        color = LipiSuccess.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LipiSuccess.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = LipiSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Weekly Progress: 78%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LipiSuccess)
                        }
                    }

                    // Streak Pill
                    Surface(
                        shape = CircleShape,
                        color = LipiWarning.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LipiWarning.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥 12 Day Streak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LipiWarning)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onNavigateToNotes,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Resume Learning", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// 2. TOP METRICS ROW (Hierarchy with Hero Circular Card)
// ==========================================
@Composable
private fun TopMetricsRow(
    viewModel: NoteViewModel,
    notesCount: Int,
    isTablet: Boolean,
    isDark: Boolean,
    cardBg: Color
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Metric Card 1: Study Progress (Featured with 1.8x weight & Donut Chart)
        Card(
            modifier = Modifier
                .weight(if (isTablet) 1.6f else 1.2f)
                .height(130.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(LipiPrimary, LipiSecondary))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = LipiPrimary.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Default.PieChart,
                                contentDescription = null,
                                tint = LipiPrimary,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Study Progress",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LipiPrimary,
                            maxLines = 1
                        )
                    }

                    Column {
                        Text(
                            text = "84%",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "+12% vs last week",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LipiSuccess
                        )
                    }
                }

                // Mini Circular Donut Progress Ring
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 7.dp.toPx()
                        drawCircle(
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            style = Stroke(width = strokeW)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(LipiPrimary, LipiSecondary, LipiPrimary)),
                            startAngle = -90f,
                            sweepAngle = 360f * 0.84f,
                            useCenter = false,
                            style = Stroke(width = strokeW)
                        )
                    }
                    Text(
                        text = "84%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                }
            }
        }

        // Metric 2: Weekly Goal
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Weekly Goal",
            value = "14.5 / 18 h",
            subtext = "2h 15m remaining",
            icon = Icons.Default.BarChart,
            accentColor = LipiAccent,
            cardBg = cardBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            progressValue = 0.80f
        )

        // Metric 3: Study Streak
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Study Streak",
            value = "12 Days 🔥",
            subtext = "Personal Best: 14 Days",
            icon = Icons.Default.OfflineBolt,
            accentColor = LipiWarning,
            cardBg = cardBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark
        )

        // Metric 4: Notes Created
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Notes Created",
            value = "$notesCount Notes",
            subtext = "+4 added this week",
            icon = Icons.Default.Book,
            accentColor = LipiSuccess,
            cardBg = cardBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark
        )

        // Metric 5: AI Usage
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "AI Usage",
            value = "128 Queries",
            subtext = "15 used today",
            icon = Icons.Default.AutoAwesome,
            accentColor = LipiSecondary,
            cardBg = cardBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    progressValue: Float? = null
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }

            Column {
                Text(
                    text = value,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (progressValue != null) {
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = subtext,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ==========================================
// 3. HERO FEATURE: LARGE GLOWING AI SEARCH BAR
// ==========================================
@Composable
private fun HeroAISearchBar(
    onSearchSubmitted: (String) -> Unit,
    isDark: Boolean,
    cardBg: Color
) {
    var searchText by remember { mutableStateOf("") }
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Glowing aura animation
    val infiniteTransition = rememberInfiniteTransition(label = "SearchGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    LipiPrimary.copy(alpha = glowAlpha),
                    LipiSecondary.copy(alpha = glowAlpha),
                    LipiAccent.copy(alpha = glowAlpha)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(LipiPrimary, LipiSecondary)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Ask Lipi AI...",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Search handwriting, PDFs, voice notes, diagrams or ask any question",
                        fontSize = 13.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Glowing Search Box
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = {
                            Text("e.g. Find Newton's laws in my Physics notebook...", fontSize = 14.sp, color = textSecondary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { /* Voice Search */ }) {
                        Icon(Icons.Default.MicNone, contentDescription = "Voice", tint = LipiPrimary)
                    }
                    IconButton(onClick = { /* OCR Scan */ }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = LipiPrimary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { if (searchText.isNotBlank()) onSearchSubmitted(searchText) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Popular tag chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Popular:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                listOf(
                    "✨ Summarize last lecture",
                    "📐 Find Physics formulas",
                    "🧬 Explain diagram",
                    "📝 PDF to Quiz",
                    "⚡ Create Flashcards"
                ).forEach { prompt ->
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            searchText = prompt.removePrefix("✨ ").removePrefix("📐 ").removePrefix("🧬 ").removePrefix("📝 ").removePrefix("⚡ ")
                        }
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. QUICK ACTIONS BAR
// ==========================================
@Composable
private fun QuickActionsRow(
    onActionClick: (String) -> Unit,
    isDark: Boolean,
    cardBg: Color
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)

    val actions = listOf(
        QuickActionData("New Notebook", Icons.Default.Book, LipiPrimary),
        QuickActionData("Handwritten Note", Icons.Default.Edit, LipiSuccess),
        QuickActionData("Voice Note", Icons.Default.MicNone, LipiError),
        QuickActionData("Scan Document", Icons.Default.Scanner, LipiWarning),
        QuickActionData("Import PDF", Icons.Default.PictureAsPdf, LipiAccent),
        QuickActionData("AI Summary", Icons.Default.Bolt, LipiSecondary),
        QuickActionData("Flashcards", Icons.Default.Style, Color(0xFFF43F5E)),
        QuickActionData("Mind Map", Icons.Default.Psychology, Color(0xFF14B8A6))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEach { action ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onActionClick(action.label) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(action.tint.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(action.icon, contentDescription = null, tint = action.tint, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = action.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class QuickActionData(val label: String, val icon: ImageVector, val tint: Color)

// ==========================================
// 5. TODAY'S FOCUS CARD
// ==========================================
@Composable
private fun TodaysFocusCard(isDark: Boolean, cardBg: Color) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var task1Done by remember { mutableStateOf(true) }
    var task2Done by remember { mutableStateOf(false) }
    var task3Done by remember { mutableStateOf(false) }

    val completedCount = (if (task1Done) 1 else 0) + (if (task2Done) 1 else 0) + (if (task3Done) 1 else 0)
    val focusProgress = completedCount / 3f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LipiSuccess, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Today's Focus", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Surface(
                    shape = CircleShape,
                    color = LipiSuccess.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Est. 1h 35m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LipiSuccess,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { focusProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = LipiSuccess,
                trackColor = LipiSuccess.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Checklist
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusTaskRow("Quantum Physics Ch 4 Review (45m)", task1Done, isDark) { task1Done = !task1Done }
                FocusTaskRow("Calculus Integration Practice (30m)", task2Done, isDark) { task2Done = !task2Done }
                FocusTaskRow("Biology Flashcards Review (20m)", task3Done, isDark) { task3Done = !task3Done }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Motivational Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "\"Consistency is the key to mastery. Keep pushing forward!\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Start Focus Session */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LipiSuccess)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Focus Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FocusTaskRow(
    title: String,
    checked: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) LipiSuccess else textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (checked) textSecondary else textPrimary,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
    }
}

// ==========================================
// 6. POMODORO TIMER CARD
// ==========================================
@Composable
private fun PomodoroTimerCard(isDark: Boolean, cardBg: Color) {
    var isRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(25 * 60) }
    var selectedPresetMinutes by remember { mutableStateOf(25) }

    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Pulse animation when running
    val infiniteTransition = rememberInfiniteTransition(label = "PomodoroPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        } else if (secondsLeft == 0) {
            isRunning = false
        }
    }

    val minutes = secondsLeft / 60
    val secs = secondsLeft % 60
    val formattedTime = String.format("%02d:%02d", minutes, secs)
    val progress = secondsLeft.toFloat() / (selectedPresetMinutes * 60).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pomodoro Timer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Surface(
                    shape = CircleShape,
                    color = LipiPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Today: 1h 45m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LipiPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Timer with Breathing Aura
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    if (isRunning) {
                        drawCircle(
                            color = LipiPrimary.copy(alpha = pulseAlpha),
                            radius = size.minDimension / 2f + 8.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(LipiPrimary, LipiSecondary, LipiPrimary)),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = if (isRunning) "FOCUSING" else "PAUSED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) LipiSuccess else textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(10, 25, 50).forEach { mins ->
                    val isSelected = selectedPresetMinutes == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPresetMinutes = mins
                            secondsLeft = mins * 60
                            isRunning = false
                        },
                        label = { Text("$mins min", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LipiPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { isRunning = !isRunning },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) LipiWarning else LipiPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "Pause" else "Start Timer", fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        secondsLeft = selectedPresetMinutes * 60
                        isRunning = false
                    },
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = textPrimary)
                }
            }
        }
    }
}

// ==========================================
// 7. CONTINUE WORKING (REALISTIC NOTEBOOK COVERS)
// ==========================================
@Composable
private fun ContinueWorkingSection(
    notes: List<NoteEntity>,
    onNoteClick: () -> Unit,
    onViewAllClick: () -> Unit,
    isDark: Boolean,
    cardBg: Color
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val sampleCovers = listOf(
        NotebookCoverData("Quantum Physics Ch 4", "Edited 2 hrs ago", "16 pages", Color(0xFF3B82F6), true, true),
        NotebookCoverData("Organic Chemistry Lab", "Edited 5 hrs ago", "24 pages", Color(0xFF10B981), true, false),
        NotebookCoverData("Calculus Integration", "Edited Yesterday", "18 pages", Color(0xFF8B5CF6), false, true),
        NotebookCoverData("European History 101", "Edited Aug 3", "32 pages", Color(0xFFF59E0B), true, false)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Continue Working", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            TextButton(onClick = onViewAllClick) {
                Text("View All Notes", color = LipiPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(sampleCovers) { cover ->
                Card(
                    modifier = Modifier
                        .width(185.dp)
                        .height(245.dp)
                        .clickable { onNoteClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Notebook realistic cover header with spine
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(cover.coverColor)
                        ) {
                            // Left spine detail
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(14.dp)
                                    .background(Color.Black.copy(alpha = 0.22f))
                            )

                            // Badges top right
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (cover.isAi) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.35f)
                                    ) {
                                        Text("✨ AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                if (cover.isPinned) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.35f)
                                    ) {
                                        Icon(Icons.Default.PushPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp).padding(2.dp))
                                    }
                                }
                            }

                            // Title on Cover
                            Text(
                                text = cover.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 22.dp, end = 12.dp, bottom = 12.dp)
                            )
                        }

                        // Realistic Notebook Page Preview Lines
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { idx ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (idx == 2) 0.6f else 0.95f)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(cover.lastEdited, fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Medium)
                                Text(cover.pageCount, fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = LipiPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text("Open Note", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LipiPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NotebookCoverData(
    val title: String,
    val lastEdited: String,
    val pageCount: String,
    val coverColor: Color,
    val isAi: Boolean,
    val isPinned: Boolean
)

// ==========================================
// 8. AI SUGGESTIONS SECTION
// ==========================================
@Composable
private fun AISuggestionsSection(
    onSuggestionClick: (String) -> Unit,
    isDark: Boolean,
    cardBg: Color
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val suggestions = listOf(
        "✨ Continue yesterday's Quantum Physics notes",
        "📄 Summarize latest PDF: Organic_Chemistry_Ch3.pdf",
        "⚡ Generate 10 Flashcards for European History",
        "🔍 Explain highlighted diagram in Neurobiology",
        "🤖 Ask Lipi AI Tutor to review practice exam"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LipiSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Smart Recommendations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            suggestions.take(3).forEach { sug ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSuggestionClick(sug) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sug,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LipiSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. ANALYTICS & STUDY HEATMAP
// ==========================================
@Composable
private fun AnalyticsAndHeatmapSection(isDark: Boolean, cardBg: Color) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Study Analytics & Heatmap", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }

                Surface(
                    shape = CircleShape,
                    color = LipiPrimary.copy(alpha = 0.12f)
                ) {
                    Text("Focus Score: 92%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LipiPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GitHub-style 7x4 Study Activity Heatmap Grid
            Text("Activity Heatmap (Last 28 Days)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { rowIdx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) { colIdx ->
                            val intensity = ((rowIdx * 7 + colIdx) * 37) % 100
                            val alpha = when {
                                intensity > 75 -> 0.9f
                                intensity > 45 -> 0.6f
                                intensity > 20 -> 0.3f
                                else -> 0.1f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LipiSuccess.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("⏱️ Total Spent: 18.5 hrs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("📚 Top Subject: Physics", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("🏆 Badges Unlocked: 12", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        }
    }
}

// ==========================================
// 10. CALENDAR & UPCOMING DEADLINES
// ==========================================
@Composable
private fun UpcomingDeadlinesSection(isDark: Boolean, cardBg: Color) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LipiWarning, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upcoming Tasks & Exams", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DeadlineRow("🔴 Physics Midterm Exam", "Tomorrow, 10:00 AM", LipiError, isDark)
                DeadlineRow("🟡 Organic Chemistry Lab Report", "Aug 8, 11:59 PM", LipiWarning, isDark)
                DeadlineRow("🟢 History Essay Final Draft", "Aug 12, 5:00 PM", LipiSuccess, isDark)
            }
        }
    }
}

@Composable
private fun DeadlineRow(title: String, dueDate: String, badgeColor: Color, isDark: Boolean) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Surface(
            shape = CircleShape,
            color = badgeColor.copy(alpha = 0.15f)
        ) {
            Text(dueDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

// ==========================================
// CUSTOMIZE GOALS MODAL
// ==========================================
@Composable
fun CustomizeGoalsModal(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    var tempGoalMins by remember { mutableStateOf(viewModel.dailyGoalTargetMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Tune, contentDescription = null, tint = LipiPrimary)
        },
        title = {
            Text(
                "Customize Study Dashboard & Goals",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Study Target", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(15, 30, 45, 60, 90).forEach { mins ->
                                val isSel = tempGoalMins == mins.toString()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) LipiPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSel) LipiPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { tempGoalMins = mins.toString() }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text("${mins}m", fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    tempGoalMins.toIntOrNull()?.let { mins -> viewModel.updateDailyGoalMinutes(mins) }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
