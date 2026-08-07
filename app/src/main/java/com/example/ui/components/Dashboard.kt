package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
private val LipiTextPrimary = Color(0xFF1E293B)
private val LipiTextSecondary = Color(0xFF64748B)

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
    var showStudyProgressModal by remember { mutableStateOf(false) }
    var showStreakModal by remember { mutableStateOf(false) }
    var showAIInteractionsModal by remember { mutableStateOf(false) }
    var showNotesCreatedModal by remember { mutableStateOf(false) }
    var activeQuickActionModal by remember { mutableStateOf<String?>(null) }
    var showAddFocusTaskModal by remember { mutableStateOf(false) }
    var showAddDeadlineModal by remember { mutableStateOf(false) }

    if (showCustomizeGoalsModal) {
        CustomizeGoalsModal(
            viewModel = viewModel,
            onDismiss = { showCustomizeGoalsModal = false }
        )
    }

    if (showStudyProgressModal) {
        StudyProgressDetailModal(
            viewModel = viewModel,
            notes = notes,
            onDismiss = { showStudyProgressModal = false }
        )
    }

    if (showStreakModal) {
        StudyStreakDetailModal(
            viewModel = viewModel,
            notes = notes,
            onDismiss = { showStreakModal = false }
        )
    }

    if (showAIInteractionsModal) {
        AIInteractionsDetailModal(
            viewModel = viewModel,
            onDismiss = { showAIInteractionsModal = false }
        )
    }

    if (showNotesCreatedModal) {
        NotesCreatedDetailModal(
            notes = notes,
            viewModel = viewModel,
            onDismiss = { showNotesCreatedModal = false },
            onNavigateToNotesWithFilter = { filter ->
                showNotesCreatedModal = false
                onNavigateToNotesWithFilter?.invoke(filter) ?: onNavigateToNotes()
            }
        )
    }

    activeQuickActionModal?.let { action ->
        QuickActionInteractiveModal(
            actionName = action,
            viewModel = viewModel,
            onDismiss = { activeQuickActionModal = null },
            onNavigateToNotesWithFilter = onNavigateToNotesWithFilter ?: { onNavigateToNotes() }
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
                notes = notes,
                isTablet = isTablet,
                isDark = isDarkTheme,
                cardBg = cardBg,
                onStudyProgressClick = { showStudyProgressModal = true },
                onStreakClick = { showStreakModal = true },
                onNotesCreatedClick = { showNotesCreatedModal = true },
                onAIInteractionsClick = { showAIInteractionsModal = true }
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
                    activeQuickActionModal = action
                },
                isDark = isDarkTheme,
                cardBg = cardBg
            )

            // 5. TODAY'S FOCUS & POMODORO TIMER
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 600.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(modifier = Modifier.weight(1.1f)) {
                            TodaysFocusCard(
                                isDark = isDarkTheme,
                                cardBg = cardBg,
                                onAddTask = { showAddFocusTaskModal = true }
                            )
                        }
                        Box(modifier = Modifier.weight(0.9f)) {
                            PomodoroTimerCard(isDark = isDarkTheme, cardBg = cardBg)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TodaysFocusCard(
                            isDark = isDarkTheme,
                            cardBg = cardBg,
                            onAddTask = { showAddFocusTaskModal = true }
                        )
                        PomodoroTimerCard(isDark = isDarkTheme, cardBg = cardBg)
                    }
                }
            }

            // 6. CONTINUE WORKING (REALISTIC NOTEBOOK COVERS)
            ContinueWorkingSection(
                notes = notes,
                viewModel = viewModel,
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
            AnalyticsAndHeatmapSection(notes = notes, viewModel = viewModel, isDark = isDarkTheme, cardBg = cardBg)

            // 9. CALENDAR & UPCOMING DEADLINES
            UpcomingDeadlinesSection(
                isDark = isDarkTheme,
                cardBg = cardBg,
                onAddDeadline = { showAddDeadlineModal = true }
            )

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
        BoxWithConstraints {
            val isCompact = maxWidth < 600.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 16.dp else 24.dp)
            ) {
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (!isTablet) {
                                    IconButton(
                                        onClick = onMenuClick,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = textPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column {
                                    Text(
                                        text = "$greeting, $firstName 👋",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "Ready to continue learning?",
                                        fontSize = 13.sp,
                                        color = textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = todayDate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                }
                            }
                        }
                    }
                } else {
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress & Goal Row
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Goal Chip
                            Surface(
                                shape = CircleShape,
                                color = LipiPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { onCustomizeGoals() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Adjust, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Goal: ${viewModel.dailyGoalTargetMinutes}m/day", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LipiPrimary)
                                }
                            }

                            // Progress Chip
                            Surface(
                                shape = CircleShape,
                                color = LipiSuccess.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, LipiSuccess.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = LipiSuccess, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Weekly Progress: 78%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LipiSuccess)
                                }
                            }

                            // Streak Pill
                            Surface(
                                shape = CircleShape,
                                color = LipiWarning.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, LipiWarning.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔥 12 Day Streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LipiWarning)
                                }
                            }
                        }

                        Button(
                            onClick = onNavigateToNotes,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("Resume Learning", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
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
    }
}

// ==========================================
// 2. TOP METRICS ROW (Hierarchy with Hero Circular Card matching provided visual spec)
// ==========================================
private enum class MetricChartType {
    ORANGE_WAVE,
    PURPLE_LINE,
    BLUE_LINE
}

@Composable
private fun TopMetricsRow(
    viewModel: NoteViewModel,
    notes: List<NoteEntity>,
    isTablet: Boolean,
    isDark: Boolean,
    cardBg: Color,
    onStudyProgressClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onNotesCreatedClick: () -> Unit = {},
    onAIInteractionsClick: () -> Unit = {}
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val studySecs = viewModel.dailyStudySeconds
    val totalStudySecs = viewModel.dailyStudySeconds
    val hours = totalStudySecs / 3600
    val mins = (totalStudySecs % 3600) / 60
    val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    val goalSecs = 30 * 3600 // Example 30 hour goal
    val progressPct = if (totalStudySecs > 0) {
        ((totalStudySecs.toFloat() / goalSecs) * 100).toInt().coerceAtMost(100)
    } else {
        0
    }
    val progressFraction = progressPct / 100f

    val streakVal = viewModel.studyStreakDays.toString()
    val streakSubtext = "Keep it up! 🔥"

    val totalNotesStr = notes.size.toString()
    val createdThisWkCount = notes.count { isThisWeek(it.createdTime) }
    val notesSubtext = "+$createdThisWkCount this week"

    val aiNotesCountVal = notes.count { !it.summary.isNullOrBlank() || !it.audioTranscription.isNullOrBlank() }
    val aiNotesCount = aiNotesCountVal.toString()
    val aiThisWkCount = notes.count { (!it.summary.isNullOrBlank() || !it.audioTranscription.isNullOrBlank()) && isThisWeek(it.createdTime) }
    val aiSubtext = "+$aiThisWkCount this week"

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Study Progress Card (Full width on compact)
                StudyProgressCard(
                    modifier = Modifier.fillMaxWidth(),
                    isDark = isDark,
                    progressPct = progressPct,
                    timeFormatted = timeFormatted,
                    onStudyProgressClick = onStudyProgressClick
                )
                // 3 Metrics cards in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Study Streak Card
                    WaveMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Streak",
                        value = streakVal,
                        unit = "Days",
                        subtext = streakSubtext,
                        subtextColor = textPrimary,
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = LipiWarning,
                        iconBgColor = if (isDark) Color(0xFF451A03) else Color(0xFFFFF7ED),
                        chartType = MetricChartType.ORANGE_WAVE,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark,
                        onClick = onStreakClick
                    )

                    // Notes Created Card
                    WaveMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Notes",
                        value = totalNotesStr,
                        unit = "",
                        subtext = notesSubtext,
                        subtextColor = LipiSuccess,
                        icon = Icons.Default.Book,
                        iconTint = LipiSecondary,
                        iconBgColor = if (isDark) Color(0xFF2E1065) else Color(0xFFF5F3FF),
                        chartType = MetricChartType.PURPLE_LINE,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark,
                        onClick = onNotesCreatedClick
                    )

                    // AI Interactions Card
                    WaveMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "AI Use",
                        value = aiNotesCount,
                        unit = "",
                        subtext = aiSubtext,
                        subtextColor = LipiSuccess,
                        icon = Icons.Default.SmartToy,
                        iconTint = LipiAccent,
                        iconBgColor = if (isDark) Color(0xFF0C4A6E) else Color(0xFFF0F9FF),
                        chartType = MetricChartType.BLUE_LINE,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark,
                        onClick = onAIInteractionsClick
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Study Progress Card
                StudyProgressCard(
                    modifier = Modifier.weight(if (isTablet) 2.2f else 1.6f),
                    isDark = isDark,
                    progressPct = progressPct,
                    timeFormatted = timeFormatted,
                    onStudyProgressClick = onStudyProgressClick
                )
                // Study Streak Card
                WaveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Study Streak",
                    value = streakVal,
                    unit = "Days",
                    subtext = streakSubtext,
                    subtextColor = textPrimary,
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = LipiWarning,
                    iconBgColor = if (isDark) Color(0xFF451A03) else Color(0xFFFFF7ED),
                    chartType = MetricChartType.ORANGE_WAVE,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = onStreakClick
                )

                // Notes Created Card
                WaveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Notes Created",
                    value = totalNotesStr,
                    unit = "",
                    subtext = notesSubtext,
                    subtextColor = LipiSuccess,
                    icon = Icons.Default.Book,
                    iconTint = LipiSecondary,
                    iconBgColor = if (isDark) Color(0xFF2E1065) else Color(0xFFF5F3FF),
                    chartType = MetricChartType.PURPLE_LINE,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = onNotesCreatedClick
                )

                // AI Interactions Card
                WaveMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "AI Interactions",
                    value = aiNotesCount,
                    unit = "",
                    subtext = aiSubtext,
                    subtextColor = LipiSuccess,
                    icon = Icons.Default.SmartToy,
                    iconTint = LipiAccent,
                    iconBgColor = if (isDark) Color(0xFF0C4A6E) else Color(0xFFF0F9FF),
                    chartType = MetricChartType.BLUE_LINE,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = onAIInteractionsClick
                )
            }
        }
    }
}

@Composable
fun Modifier.springCardPress(
    enabled: Boolean = true,
    scaleDownFactor: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDownFactor else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "springCardScale"
    )

    val translationY by animateFloatAsState(
        targetValue = if (isPressed) 3f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springCardTranslation"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.translationY = translationY
        }
        .pointerInput(enabled, onClick) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = {
                    onClick?.invoke()
                }
            )
        }
}

@Composable
private fun WaveMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String = "",
    subtext: String,
    subtextColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    chartType: MetricChartType,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .springCardPress(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Graphic Canvas at bottom of card
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val w = size.width
                val h = size.height

                when (chartType) {
                    MetricChartType.ORANGE_WAVE -> {
                        // Double-layered orange soft wave
                        val pathBg = Path().apply {
                            moveTo(0f, h * 0.75f)
                            cubicTo(w * 0.25f, h * 0.48f, w * 0.65f, h * 0.92f, w, h * 0.58f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path = pathBg,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF9800).copy(alpha = 0.22f), Color(0xFFFFE0B2).copy(alpha = 0.03f))
                            )
                        )

                        val pathFg = Path().apply {
                            moveTo(0f, h * 0.85f)
                            cubicTo(w * 0.35f, h * 0.62f, w * 0.72f, h * 0.78f, w, h * 0.52f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path = pathFg,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFF97316).copy(alpha = 0.28f), Color(0xFFFFF7ED).copy(alpha = 0.02f))
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(0f, h * 0.85f)
                            cubicTo(w * 0.35f, h * 0.62f, w * 0.72f, h * 0.78f, w, h * 0.52f)
                        }
                        drawPath(
                            path = linePath,
                            color = Color(0xFFF97316).copy(alpha = 0.55f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    MetricChartType.PURPLE_LINE -> {
                        // Purple line chart with nodes
                        val points = listOf(
                            Offset(0f, h * 0.55f),
                            Offset(w * 0.18f, h * 0.72f),
                            Offset(w * 0.35f, h * 0.88f),
                            Offset(w * 0.52f, h * 0.55f),
                            Offset(w * 0.7f, h * 0.38f),
                            Offset(w * 0.86f, h * 0.42f),
                            Offset(w, h * 0.58f)
                        )

                        val linePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val cx = (p1.x + p2.x) / 2f
                                cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFF8B5CF6).copy(alpha = 0.01f))
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = Color(0xFF8B5CF6),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Keypoint dots
                        drawCircle(color = Color(0xFF7C3AED), radius = 3.5.dp.toPx(), center = points.last())
                        drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = points.last())
                    }

                    MetricChartType.BLUE_LINE -> {
                        // Light blue sine wave line chart with nodes
                        val points = listOf(
                            Offset(0f, h * 0.45f),
                            Offset(w * 0.22f, h * 0.78f),
                            Offset(w * 0.45f, h * 0.32f),
                            Offset(w * 0.68f, h * 0.78f),
                            Offset(w * 0.88f, h * 0.38f),
                            Offset(w, h * 0.45f)
                        )

                        val linePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val cx = (p1.x + p2.x) / 2f
                                cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.25f), Color(0xFF38BDF8).copy(alpha = 0.01f))
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = Color(0xFF0EA5E9),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Keypoint dot
                        drawCircle(color = Color(0xFF0284C7), radius = 3.5.dp.toPx(), center = points.last())
                        drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = points.last())
                    }
                }
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = unit,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtext,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subtextColor
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
                        fontWeight = FontWeight.Bold,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(actions) { action ->
                Card(
                    modifier = Modifier
                        .width(108.dp)
                        .springCardPress { onActionClick(action.label) },
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

private data class FocusTaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val completedTimestamp: Long? = if (isCompleted) System.currentTimeMillis() else null
)

// ==========================================
// 5. TODAY'S FOCUS CARD
// ==========================================
@Composable
private fun TodaysFocusCard(
    isDark: Boolean,
    cardBg: Color,
    onAddTask: () -> Unit = {}
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val nowInit = remember { System.currentTimeMillis() }
    var taskList by remember {
        mutableStateOf(
            listOf(
                FocusTaskItem(title = "Quantum Physics Ch 4 Review (45m)", isCompleted = true, completedTimestamp = nowInit),
                FocusTaskItem(title = "Calculus Integration Practice (30m)", isCompleted = false),
                FocusTaskItem(title = "Biology Flashcards Review (20m)", isCompleted = false)
            )
        )
    }

    var showNewTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    val ONE_HOUR_MS = 60 * 60 * 1000L

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Periodically remove tasks that have been completed for > 1 hour (3600 seconds)
    LaunchedEffect(taskList) {
        while (true) {
            delay(5000L)
            currentTime = System.currentTimeMillis()
            val expiredTaskIds = taskList.filter { item ->
                item.isCompleted && item.completedTimestamp != null && (currentTime - item.completedTimestamp >= ONE_HOUR_MS)
            }.map { it.id }.toSet()

            if (expiredTaskIds.isNotEmpty()) {
                taskList = taskList.filterNot { it.id in expiredTaskIds }
            }
        }
    }

    if (showNewTaskDialog) {
        AlertDialog(
            onDismissRequest = { showNewTaskDialog = false },
            title = { Text("Add Focus Task", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Task Description") },
                    placeholder = { Text("e.g. Organic Chemistry Chapter 2 (30m)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            taskList = taskList + FocusTaskItem(title = newTaskTitle, isCompleted = false)
                            newTaskTitle = ""
                            showNewTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary)
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val completedCount = taskList.count { it.isCompleted }
    val focusProgress = if (taskList.isNotEmpty()) completedCount.toFloat() / taskList.size else 0f

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
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = LipiSuccess.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${completedCount}/${taskList.size} Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LipiSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onAddTask()
                            showNewTaskDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = LipiPrimary)
                    }
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
                if (taskList.isEmpty()) {
                    Text(
                        text = "No focus tasks yet. Tap + to add one!",
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    taskList.forEachIndexed { index, item ->
                        FocusTaskRow(
                            title = item.title,
                            checked = item.isCompleted,
                            completedTimestamp = item.completedTimestamp,
                            currentTime = currentTime,
                            isDark = isDark,
                            onToggle = {
                                val updated = taskList.toMutableList()
                                val nextCompleted = !item.isCompleted
                                updated[index] = item.copy(
                                    isCompleted = nextCompleted,
                                    completedTimestamp = if (nextCompleted) System.currentTimeMillis() else null
                                )
                                taskList = updated
                            }
                        )
                    }
                }
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
    completedTimestamp: Long?,
    currentTime: Long,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val remainingMinutes = if (checked && completedTimestamp != null) {
        val elapsed = currentTime - completedTimestamp
        val remainingMs = (60 * 60 * 1000L - elapsed).coerceAtLeast(0)
        (remainingMs / 60000L).coerceAtLeast(1L)
    } else null

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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) textSecondary else textPrimary,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            )
            if (checked && remainingMinutes != null) {
                Text(
                    text = "Completed • Removes in ${remainingMinutes}m",
                    fontSize = 10.sp,
                    color = LipiSuccess.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
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
                        fontWeight = FontWeight.Bold,
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
    viewModel: NoteViewModel,
    onNoteClick: () -> Unit,
    onViewAllClick: () -> Unit,
    isDark: Boolean,
    cardBg: Color
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFF8B5CF6),
        Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF06B6D4)
    )

    val covers = remember(notes) {
        if (notes.isNotEmpty()) {
            notes.sortedByDescending { it.lastModifiedTime }.take(8).map { note ->
                val color = if (note.cardColor != 0L) Color(note.cardColor.toULong()) else colors[Math.abs(note.id) % colors.size]
                val pageText = when {
                    note.pdfTitle != null -> "PDF Document"
                    note.audioPath != null -> "Audio Note"
                    note.templateType != "blank" -> "${note.templateType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} Note"
                    note.drawingData.length > 20 -> "Handwritten Note"
                    else -> "Text Note"
                }
                NotebookCoverData(
                    title = note.title.ifBlank { "Untitled Note" },
                    lastEdited = formatRelativeTime(note.lastModifiedTime),
                    pageCount = pageText,
                    coverColor = color,
                    isAi = !note.summary.isNullOrBlank() || !note.audioTranscription.isNullOrBlank(),
                    isPinned = note.isPinned,
                    noteEntity = note
                )
            }
        } else emptyList()
    }

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

        if (covers.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(covers) { cover ->
                    Card(
                        modifier = Modifier
                            .width(185.dp)
                            .height(245.dp)
                            .springCardPress {
                                cover.noteEntity?.let { viewModel.selectNote(it) }
                                onNoteClick()
                            },
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
                                    fontWeight = FontWeight.Bold,
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
        } else {
            // Empty State Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { onViewAllClick() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No notes created yet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                    Text("Tap here to view notebooks or create your first note", fontSize = 12.sp, color = textSecondary)
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Edited just now"
        minutes < 60 -> "Edited ${minutes}m ago"
        hours < 24 -> "Edited ${hours}h ago"
        days == 1L -> "Edited Yesterday"
        days < 7 -> "Edited ${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            "Edited ${sdf.format(Date(timestamp))}"
        }
    }
}

private fun isThisWeek(timestamp: Long): Boolean {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }
    return timestamp >= cal.timeInMillis
}

private data class NotebookCoverData(
    val title: String,
    val lastEdited: String,
    val pageCount: String,
    val coverColor: Color,
    val isAi: Boolean,
    val isPinned: Boolean,
    val noteEntity: NoteEntity? = null
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

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 600.dp) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suggestions) { sug ->
                        Card(
                            modifier = Modifier
                                .width(250.dp)
                                .springCardPress { onSuggestionClick(sug) },
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
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    suggestions.take(3).forEach { sug ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .springCardPress { onSuggestionClick(sug) },
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
    }
}

// ==========================================
// 9. ANALYTICS & STUDY HEATMAP
// ==========================================
@Composable
private fun AnalyticsAndHeatmapSection(
    notes: List<NoteEntity>,
    viewModel: NoteViewModel,
    isDark: Boolean,
    cardBg: Color
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var selectedDay by remember { mutableStateOf("Wed") }
    var selectedTimeframe by remember { mutableStateOf("This Week") }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val (studyTimesMap, barValues) = remember(notes, viewModel.dailyStudySeconds) {
        val calendar = Calendar.getInstance()
        val todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 .. Sun=6
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        val mondayStart = calendar.timeInMillis

        val timesMap = mutableMapOf<String, String>()
        val valuesMap = mutableMapOf<String, Float>()

        days.forEachIndexed { index, dayName ->
            val dayStart = mondayStart + index * 86400000L
            val dayEnd = dayStart + 86400000L
            val count = notes.count { it.lastModifiedTime in dayStart until dayEnd || it.createdTime in dayStart until dayEnd }
            
            val totalSecsForDay = if (index == todayIndex) viewModel.dailyStudySeconds else count * 900
            val h = totalSecsForDay / 3600
            val m = (totalSecsForDay % 3600) / 60
            
            timesMap[dayName] = if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m" else "${count} notes"
            val rawRatio = if (index == todayIndex && totalSecsForDay > 0) (totalSecsForDay.toFloat() / 7200f).coerceIn(0.15f, 1f) else (count.toFloat() / 5f).coerceIn(0.1f, 1f)
            valuesMap[dayName] = rawRatio
        }
        Pair(timesMap, valuesMap)
    }

    var selectedHeatmapCell by remember { mutableStateOf<Pair<Int, Int>?>(Pair(0, 0)) }
    var selectedSubjectFilter by remember { mutableStateOf("All Notes") }

    val (heatmapGrid, heatmapLabels) = remember(notes) {
        val grid = List(4) { MutableList(7) { 0 } }
        val labels = List(4) { MutableList(7) { "" } }
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())

        for (w in 3 downTo 0) {
            for (d in 6 downTo 0) {
                val dayEnd = cal.timeInMillis + 86400000L
                val dayStart = cal.timeInMillis
                val count = notes.count { it.lastModifiedTime in dayStart until dayEnd || it.createdTime in dayStart until dayEnd }
                val level = when {
                    count >= 4 -> 4
                    count == 3 -> 3
                    count == 2 -> 2
                    count == 1 -> 1
                    else -> 0
                }
                grid[w][d] = level
                labels[w][d] = "${sdf.format(Date(dayStart))}: $count notes created/edited"
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
        }
        Pair(grid, labels)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Study Analytics (Bar Chart)
                Card(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Study Analytics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Study Time (${selectedTimeframe})", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                            }

                            // Timeframe chips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("This Week", "Last Week").forEach { tf ->
                                    val isSel = selectedTimeframe == tf
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSel) LipiPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isSel) LipiPrimary else Color.Transparent),
                                        modifier = Modifier.clickable { selectedTimeframe = tf }
                                    ) {
                                        Text(
                                            text = tf,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) LipiPrimary else textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Y-Axis
                            Column(
                                modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                listOf("8h", "6h", "4h", "2h", "0h").forEach { label ->
                                    Text(label, fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            // Bar Chart
                            Box(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    days.forEach { day ->
                                        val isSelected = day == selectedDay
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDay = day }
                                        ) {
                                            if (isSelected) {
                                                Surface(
                                                    color = Color(0xFF334155),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                ) {
                                                    Text(
                                                        "${day}\n${studyTimesMap[day] ?: "0h"}",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .fillMaxHeight(barValues[day] ?: 0.3f)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(if (isSelected) LipiPrimary else LipiSecondary.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                                
                                // X-Axis Labels
                                Row(
                                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    days.forEach { day ->
                                        Text(
                                            day,
                                            fontSize = 10.sp,
                                            color = if (day == selectedDay) LipiPrimary else textSecondary,
                                            fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDay = day }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Study Heatmap
                Card(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Study Heatmap", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            
                            // Filter tag
                            Surface(
                                shape = CircleShape,
                                color = LipiPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = selectedSubjectFilter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LipiPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Days header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEach { day ->
                                Text(day, fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Grid
                        val weeks = listOf("W1", "W2", "W3", "W4")

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            weeks.forEachIndexed { rowIndex, week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(week, fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp))
                                    
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        heatmapGrid[rowIndex].forEachIndexed { colIndex, level ->
                                            val isSelectedCell = selectedHeatmapCell == Pair(rowIndex, colIndex)
                                            val alpha = when (level) {
                                                0 -> 0.1f
                                                1 -> 0.3f
                                                2 -> 0.6f
                                                3 -> 0.8f
                                                4 -> 1.0f
                                                else -> 0.1f
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isSelectedCell) LipiWarning else LipiPrimary.copy(alpha = alpha)
                                                    )
                                                    .border(
                                                        width = if (isSelectedCell) 1.5.dp else 0.dp,
                                                        color = if (isSelectedCell) textPrimary else Color.Transparent,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { selectedHeatmapCell = Pair(rowIndex, colIndex) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        selectedHeatmapCell?.let { (r, c) ->
                            val labelText = heatmapLabels.getOrNull(r)?.getOrNull(c) ?: "No activity"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = labelText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Study Analytics (Bar Chart)
                Card(
                    modifier = Modifier.weight(1f).height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Study Analytics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Study Time (${selectedTimeframe})", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                            }

                            // Timeframe chips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("This Week", "Last Week").forEach { tf ->
                                    val isSel = selectedTimeframe == tf
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSel) LipiPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isSel) LipiPrimary else Color.Transparent),
                                        modifier = Modifier.clickable { selectedTimeframe = tf }
                                    ) {
                                        Text(
                                            text = tf,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) LipiPrimary else textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Y-Axis
                            Column(
                                modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                listOf("8h", "6h", "4h", "2h", "0h").forEach { label ->
                                    Text(label, fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            // Bar Chart
                            Box(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    days.forEach { day ->
                                        val isSelected = day == selectedDay
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDay = day }
                                        ) {
                                            if (isSelected) {
                                                Surface(
                                                    color = Color(0xFF334155),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                ) {
                                                    Text(
                                                        "${day}\n${studyTimesMap[day] ?: "0h"}",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .fillMaxHeight(barValues[day] ?: 0.3f)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(if (isSelected) LipiPrimary else LipiSecondary.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                                
                                // X-Axis Labels
                                Row(
                                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    days.forEach { day ->
                                        Text(
                                            day,
                                            fontSize = 10.sp,
                                            color = if (day == selectedDay) LipiPrimary else textSecondary,
                                            fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDay = day }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Study Heatmap
                Card(
                    modifier = Modifier.weight(1f).height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Study Heatmap", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            
                            // Filter tag
                            Surface(
                                shape = CircleShape,
                                color = LipiPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = selectedSubjectFilter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LipiPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Days header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEach { day ->
                                Text(day, fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Grid
                        val weeks = listOf("W1", "W2", "W3", "W4")

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            weeks.forEachIndexed { rowIndex, week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(week, fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp))
                                    
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        heatmapGrid[rowIndex].forEachIndexed { colIndex, level ->
                                            val isSelectedCell = selectedHeatmapCell == Pair(rowIndex, colIndex)
                                            val alpha = when (level) {
                                                0 -> 0.1f
                                                1 -> 0.3f
                                                2 -> 0.6f
                                                3 -> 0.8f
                                                4 -> 1.0f
                                                else -> 0.1f
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isSelectedCell) LipiWarning else LipiPrimary.copy(alpha = alpha)
                                                    )
                                                    .border(
                                                        width = if (isSelectedCell) 1.5.dp else 0.dp,
                                                        color = if (isSelectedCell) textPrimary else Color.Transparent,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { selectedHeatmapCell = Pair(rowIndex, colIndex) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        selectedHeatmapCell?.let { (r, c) ->
                            val labelText = heatmapLabels.getOrNull(r)?.getOrNull(c) ?: "No activity"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = labelText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. CALENDAR & UPCOMING DEADLINES
// ==========================================
@Composable
private fun UpcomingDeadlinesSection(
    isDark: Boolean,
    cardBg: Color,
    onAddDeadline: () -> Unit = {}
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var deadlinesList by remember {
        mutableStateOf(
            listOf(
                Triple("🔴 Physics Midterm Exam", "Tomorrow, 10:00 AM", LipiError),
                Triple("🟡 Organic Chemistry Lab Report", "Aug 8, 11:59 PM", LipiWarning),
                Triple("🟢 History Essay Final Draft", "Aug 12, 5:00 PM", LipiSuccess)
            )
        )
    }

    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Upcoming Task/Exam", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Computer Science Quiz") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("Due Date / Time") },
                        placeholder = { Text("e.g. Aug 15, 2:00 PM") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newDate.isNotBlank()) {
                            deadlinesList = deadlinesList + Triple("🟣 $newTitle", newDate, LipiAccent)
                            newTitle = ""
                            newDate = ""
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary)
                ) {
                    Text("Add Deadline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LipiWarning, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upcoming Tasks & Exams", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Deadline", tint = LipiPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                deadlinesList.forEach { (title, dueDate, badgeColor) ->
                    DeadlineRow(title, dueDate, badgeColor, isDark)
                }
            }
        }
    }
}

// ==========================================
// INTERACTIVE STATE MODALS FOR GRAPHS & METRICS
// ==========================================

@Composable
fun StudyProgressDetailModal(viewModel: NoteViewModel, notes: List<NoteEntity> = emptyList(), onDismiss: () -> Unit) {
    var selectedTimeRange by remember { mutableStateOf("7 Days") }
    var selectedSubjectFilter by remember { mutableStateOf("All Subjects") }
    var selectedBarIndex by remember { mutableStateOf<Int?>(3) } // default Thu selected
    var targetHours by remember { mutableFloatStateOf(30f) }

    val daysData7D = remember(viewModel.dailyStudySeconds) {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val daysList = mutableListOf<Pair<String, Float>>()
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        for (i in 6 downTo 0) {
            val dayStart = todayStart.timeInMillis - (i * 24 * 60 * 60 * 1000L)
            val dayName = sdf.format(java.util.Date(dayStart))
            if (i == 0) {
                daysList.add(dayName to (viewModel.dailyStudySeconds / 3600f))
            } else {
                daysList.add(dayName to 0f)
            }
        }
        daysList
    }

    val weeksData30D = remember(viewModel.dailyStudySeconds) {
        listOf(
            "Wk 1" to 0f,
            "Wk 2" to 0f,
            "Wk 3" to 0f,
            "Wk 4" to (viewModel.dailyStudySeconds / 3600f)
        )
    }

    val monthsData1Y = remember(viewModel.dailyStudySeconds) {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val monthsList = mutableListOf<Pair<String, Float>>()
        val sdf = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
        for (i in 0..11) {
            calendar.set(java.util.Calendar.MONTH, i)
            val monthName = sdf.format(calendar.time)
            if (i == currentMonth) {
                monthsList.add(monthName to (viewModel.dailyStudySeconds / 3600f))
            } else {
                monthsList.add(monthName to 0f)
            }
        }
        monthsList
    }

    val chartData = when (selectedTimeRange) {
        "30 Days" -> weeksData30D
        "1 Year" -> monthsData1Y
        else -> daysData7D
    }

    val maxVal = chartData.maxOfOrNull { it.second } ?: 10f
    val totalStudiedHours = chartData.sumOf { it.second.toDouble() }.toFloat()
    val donePct = (totalStudiedHours / (if (selectedTimeRange == "7 Days") targetHours else targetHours * 4)).coerceIn(0f, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = LipiPrimary) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Interactive Study Progress", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Tap bars for exact study breakdown & set targets", fontSize = 12.sp, color = LipiTextSecondary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Time Range Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    listOf("7 Days", "30 Days", "1 Year").forEach { range ->
                        FilterChip(
                            selected = selectedTimeRange == range,
                            onClick = {
                                selectedTimeRange = range
                                selectedBarIndex = 0
                            },
                            label = { Text(range, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LipiPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Interactive Chart Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LipiPrimary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Selected Bar Floating Tooltip
                        selectedBarIndex?.let { index ->
                            if (index in chartData.indices) {
                                val item = chartData[index]
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LipiPrimary,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "${item.first}: ${item.second} hrs studied",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Canvas Interactive Bar Chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            chartData.forEachIndexed { idx, pair ->
                                val isSelected = selectedBarIndex == idx
                                val barHeightRatio = (pair.second / maxVal).coerceIn(0.1f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedBarIndex = idx }
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .height((90 * barHeightRatio).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (isSelected) LipiPrimary else LipiPrimary.copy(alpha = 0.35f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = pair.first,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) LipiPrimary else LipiTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Goal Target Adjuster Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target Goal:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "${targetHours.toInt()} Hours / Week (${(donePct * 100).toInt()}% Met)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = LipiPrimary
                        )
                    }
                    Slider(
                        value = targetHours,
                        onValueChange = { targetHours = it },
                        valueRange = 10f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = LipiPrimary, activeTrackColor = LipiPrimary)
                    )
                }

                HorizontalDivider(color = LipiPrimary.copy(alpha = 0.15f))

                // Subject Distribution Breakdown
                Text("Subject Distribution:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val subjects = listOf(
                    "Compiler Design" to 0.38f,
                    "Machine Learning" to 0.30f,
                    "Mathematics" to 0.19f,
                    "Physics" to 0.13f
                )
                subjects.forEach { (subject, pct) ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${(pct * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LipiSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = LipiSecondary,
                            trackColor = LipiSecondary.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StudyStreakDetailModal(viewModel: NoteViewModel, notes: List<NoteEntity> = emptyList(), onDismiss: () -> Unit) {
    var selectedTimeRange by remember { mutableStateOf("This Week") }
    var selectedDayIndex by remember { mutableStateOf<Int?>(5) }
    var showLoggedToast by remember { mutableStateOf(false) }

    val days7D = remember(viewModel.studyStreakDays) {
        val streak = viewModel.studyStreakDays
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val daysList = mutableListOf<Pair<String, Boolean>>()
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        for (i in 6 downTo 0) {
            val dayStart = todayStart.timeInMillis - (i * 24 * 60 * 60 * 1000L)
            val dayName = sdf.format(java.util.Date(dayStart))
            val isActive = i < streak
            daysList.add(dayName to isActive)
        }
        daysList
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = LipiWarning) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Interactive Streak & Milestones", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Tap days to view history or log study session", fontSize = 12.sp, color = LipiTextSecondary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Streak Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LipiWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, LipiWarning.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = LipiWarning, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔥 ${viewModel.studyStreakDays} DAYS STREAK", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LipiWarning)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (viewModel.studyStreakDays >= 14) "Phenomenal discipline! Champion status active!" else "You're on fire! Keep going to unlock the 14-Day Champion Badge",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Log Today's Study Interactive Action
                Button(
                    onClick = {
                        viewModel.incrementStudyStreak()
                        showLoggedToast = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LipiWarning)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Today's Study (+1 Day Streak)", fontWeight = FontWeight.Bold)
                }

                if (showLoggedToast) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LipiSuccess.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🔥 Study session logged! Streak increased to ${viewModel.studyStreakDays} days!",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LipiSuccess,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Interactive Week Days Heatmap Chart
                Text("Streak Activity Heatmap:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days7D.forEachIndexed { idx, (day, active) ->
                        val isSelected = selectedDayIndex == idx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected -> LipiWarning
                                active -> LipiWarning.copy(alpha = 0.25f)
                                else -> Color(0xFFE2E8F0)
                            },
                            border = if (isSelected) BorderStroke(2.dp, LipiWarning) else null,
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clickable { selectedDayIndex = idx }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    day,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else LipiTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else if (active) LipiWarning else Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                selectedDayIndex?.let { idx ->
                    val dayName = days7D.getOrNull(idx)?.first ?: ""
                    val isActive = days7D.getOrNull(idx)?.second ?: false
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LipiWarning.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isActive) "$dayName: 🔥 Streak Maintained • 4.5h Study Logged • 2 Notes Created" else "$dayName: ⏸ Rest Day",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LipiWarning,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = LipiWarning.copy(alpha = 0.15f))

                // Badges Unlocked Section
                Text("Badges & Milestones:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiSuccess.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌱 3-Day", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Warmup ✓", fontSize = 10.sp, color = LipiSuccess)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiSuccess.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡ 7-Day", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Scholar ✓", fontSize = 10.sp, color = LipiSuccess)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiWarning.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 ${viewModel.studyStreakDays}-Day", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Active Now", fontSize = 10.sp, color = LipiWarning)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = LipiWarning)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun NotesCreatedDetailModal(
    notes: List<NoteEntity>,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onNavigateToNotesWithFilter: (String) -> Unit
) {
    val notesCount = notes.size
    var selectedTimeRange by remember { mutableStateOf("7 Days") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedBarIndex by remember { mutableStateOf<Int?>(2) }

    val daysData = remember(notes) {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val daysList = mutableListOf<Pair<String, Int>>()
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        for (i in 6 downTo 0) {
            val dayStart = todayStart.timeInMillis - (i * 24 * 60 * 60 * 1000L)
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            val count = notes.count { it.createdTime in dayStart until dayEnd }
            daysList.add(sdf.format(java.util.Date(dayStart)) to count)
        }
        daysList
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Book, contentDescription = null, tint = LipiSecondary) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Notebooks Activity Graph", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Total: $notesCount Notebooks Created", fontSize = 12.sp, color = LipiTextSecondary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Category Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Handwritten", "PDF", "Projects", "School", "Templates").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LipiSecondary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Interactive Bar Chart
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LipiSecondary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, LipiSecondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        selectedBarIndex?.let { index ->
                            if (index in daysData.indices) {
                                val item = daysData[index]
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LipiSecondary,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "${item.first}: ${item.second} Notebooks Created",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            daysData.forEachIndexed { idx, pair ->
                                val isSelected = selectedBarIndex == idx
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedBarIndex = idx }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height((12 * pair.second).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(if (isSelected) LipiSecondary else LipiSecondary.copy(alpha = 0.35f))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        pair.first,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) LipiSecondary else LipiTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Navigation Shortcut Action
                OutlinedButton(
                    onClick = { onNavigateToNotesWithFilter(if (selectedCategory == "All") "" else selectedCategory) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explore Filtered Notebooks", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = LipiSecondary)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AIInteractionsDetailModal(viewModel: NoteViewModel, onDismiss: () -> Unit) {
    var selectedPromptCategory by remember { mutableStateOf("All") }
    var selectedBarIndex by remember { mutableStateOf<Int?>(1) }
    var clickedPromptMsg by remember { mutableStateOf<String?>(null) }

    val aiUsageData = remember {
        listOf("Mon" to 5, "Tue" to 8, "Wed" to 12, "Thu" to 6, "Fri" to 9, "Sat" to 4, "Sun" to 2)
    }

    val queries = listOf(
        "Explain Backpropagation with math",
        "Summarize Physics Ch 4 PDF",
        "Create 10 Flashcards for Organic Chem",
        "Generate Quiz on Matrix Multiplication"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = LipiAccent) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AI Usage Analytics & History", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("36 Queries Run This Week", fontSize = 12.sp, color = LipiTextSecondary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Interactive AI Bar Chart
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LipiAccent.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, LipiAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        selectedBarIndex?.let { index ->
                            if (index in aiUsageData.indices) {
                                val item = aiUsageData[index]
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LipiAccent,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "${item.first}: ${item.second} AI Prompts Executed",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            aiUsageData.forEachIndexed { idx, pair ->
                                val isSelected = selectedBarIndex == idx
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedBarIndex = idx }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height((7 * pair.second).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(if (isSelected) LipiAccent else LipiAccent.copy(alpha = 0.35f))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        pair.first,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) LipiAccent else LipiTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Interactive Prompt Library (Tap to inspect):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                queries.forEach { q ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LipiAccent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, LipiAccent.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clickedPromptMsg = "Selected: '$q'" }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LipiAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(q, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LipiTextPrimary)
                        }
                    }
                }

                clickedPromptMsg?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LipiAccent.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            msg,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LipiAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = LipiAccent)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun QuickActionInteractiveModal(
    actionName: String,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onNavigateToNotesWithFilter: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var cardFlipped by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(actionName, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (actionName) {
                    "Voice Note" -> {
                        Text("Record Voice Note", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Surface(shape = RoundedCornerShape(16.dp), color = LipiError.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    repeat(7) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .height(if (isRecording) (20..60).random().dp else 20.dp)
                                                .clip(CircleShape)
                                                .background(LipiError)
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { isRecording = !isRecording },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) LipiError else LipiPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRecording) "Stop Recording" else "Start Recording")
                        }
                    }
                    "Scan Document" -> {
                        Text("Document OCR Scanner", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Position document inside frame", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    "Flashcards" -> {
                        Text("Interactive Flashcards Deck", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LipiPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, LipiPrimary),
                            modifier = Modifier.fillMaxWidth().height(130.dp).clickable { cardFlipped = !cardFlipped }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (cardFlipped) "ANSWER:\nGradient descent calculates parameter updates using chain rule." else "QUESTION:\nWhat is Backpropagation in Neural Networks?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tap card to flip 🔄", fontSize = 10.sp, color = LipiPrimary)
                                }
                            }
                        }
                    }
                    else -> {
                        Text("Enter title or prompt for $actionName:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Title / Subject") },
                            placeholder = { Text("e.g. Chapter 4 Analysis") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val title = if (textInput.isNotBlank()) textInput else actionName
                    val template = when(actionName) {
                        "Import PDF" -> "pdf"
                        "Mind Map" -> "grid"
                        "Handwritten Note" -> "blank"
                        else -> "ruled"
                    }
                    val cover = when(actionName) {
                        "Flashcards" -> "subject_computer"
                        "Mind Map" -> "3d_tech"
                        "Voice Note" -> "3d_creative"
                        else -> "3d_academic"
                    }
                    
                    viewModel.createNewNoteWithDesign(
                        title = title,
                        templateType = template,
                        coverType = cover,
                        pageColor = 0xFFFFFFFF,
                        coverTitle = title,
                        coverSubtitle = "Quick Action",
                        coverAuthor = "Me",
                        coverExtra = actionName,
                        folder = "Quick Actions"
                    )
                    
                    onNavigateToNotesWithFilter(actionName)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LipiPrimary)
            ) {
                Text(if (actionName == "New Notebook") "Create" else "Save & Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@Composable
fun StudyProgressCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    progressPct: Int,
    timeFormatted: String,
    onStudyProgressClick: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val LipiPrimary = Color(0xFF5A4FCF)
    val LipiSecondary = Color(0xFF8B5CF6)
    val progressFraction = progressPct / 100f

    Card(
        modifier = modifier
            .height(180.dp)
            .springCardPress { onStudyProgressClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = LipiPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Study Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(115.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Blurred shadow for arc
                        Canvas(modifier = Modifier.fillMaxSize().blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)) {
                            val strokeW = 12.dp.toPx()
                            drawArc(
                                brush = Brush.sweepGradient(listOf(LipiSecondary, LipiPrimary, LipiSecondary)),
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                alpha = 0.7f
                            )
                        }
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 12.dp.toPx()
                            drawCircle(
                                color = LipiPrimary.copy(alpha = 0.1f),
                                style = Stroke(width = strokeW)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(LipiSecondary, LipiPrimary, LipiSecondary)),
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$progressPct%",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                    }
                }
                
                // Divider
                Divider(
                    color = textSecondary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(vertical = 4.dp)
                )
                
                // Right Stats Area
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(start = 24.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("This Week", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textPrimary.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(timeFormatted, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("of 30h goal", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    }
                    
                    // Mini Bar Chart
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 4.dp)
                    ) {
                        val heights = listOf(0.65f, 0.35f, 0.25f, 0.5f, 0.85f, 0.4f, 0.3f)
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        
                        heights.forEachIndexed { index, h ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(44.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Background Track
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(LipiPrimary.copy(alpha = 0.15f))
                                    )
                                    // Glow behind fill
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(h)
                                            .blur(6.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                            .background(LipiPrimary)
                                    )
                                    // Fill
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(h)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(LipiSecondary, Color(0xFF4C1D95))
                                                )
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(days[index], fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
