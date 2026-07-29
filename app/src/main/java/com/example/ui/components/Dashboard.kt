package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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
    val todayDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    // Dynamic greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // 1. Title bar & Greeting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isTablet) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = "Lipi",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "by Aditya Kumar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val isSignedIn = GoogleDriveBackupHelper.isSignedIn(context)
                        val userFirstName = if (isSignedIn) {
                            GoogleDriveBackupHelper.getSavedAccountName(context).split(" ").firstOrNull() ?: ""
                        } else ""
                        val greetingText = if (userFirstName.isNotBlank()) "$greeting, $userFirstName" else greeting
                        Text(
                            text = greetingText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = todayDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. High-End Customizable Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERFORMANCE & GOALS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = { showCustomizeGoalsModal = true },
                    modifier = Modifier.testTag("customize_goals_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Customize Goals", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dailyGoalMins = viewModel.dailyGoalTargetMinutes
            val studySecs = viewModel.dailyStudySeconds
            val studyMinsDone = studySecs / 60
            val dailyPercent = if (dailyGoalMins > 0) ((studySecs.toFloat() / (dailyGoalMins * 60f)) * 100f).coerceAtMost(100f).toInt() else 0

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = if (isTablet) 3 else 2
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Pads",
                    value = "${notes.size} Pads",
                    subtitle = "${notes.count { it.templateType == "ruled" }} Ruled • ${notes.count { it.templateType == "cornell" }} Cornell",
                    icon = Icons.Default.Book,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateToNotes() },
                    onCustomizeClick = { showCustomizeGoalsModal = true }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Goal",
                    value = "$dailyPercent%",
                    subtitle = "${studyMinsDone}m of ${dailyGoalMins}m target",
                    progress = (studySecs.toFloat() / (dailyGoalMins * 60f)).coerceAtMost(1f),
                    icon = Icons.Default.TrendingUp,
                    tint = Color(0xFF00B0FF),
                    onClick = { showCustomizeGoalsModal = true },
                    onCustomizeClick = { showCustomizeGoalsModal = true }
                )
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title = "Study Streak",
                    value = "${viewModel.studyStreakDays} Days 🔥",
                    subtitle = if (viewModel.studyStreakDays >= 7) "Legendary streak!" else "Keep studying daily",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = Color(0xFFFF5722),
                    onClick = { showCustomizeGoalsModal = true },
                    onCustomizeClick = { showCustomizeGoalsModal = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Quick Action Banners Grid (2 Columns on tablet, Stacked on phone)
            val pdfNotesCount = notes.count { it.templateType == "pdf" || it.templateType == "docx" || !it.pdfTitle.isNullOrEmpty() || it.title.contains(".pdf", ignoreCase = true) || it.title.contains("PDF", ignoreCase = true) }
            Text(
                text = "QUICK STUDY ACTIONS & FOLDERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "PDF Notes & Documents",
                        subtitle = "$pdfNotesCount PDF files imported • Tap to open PDF folder",
                        icon = Icons.Default.PictureAsPdf,
                        color = Color(0xFFFFEBEE),
                        onClick = {
                            if (onNavigateToNotesWithFilter != null) {
                                onNavigateToNotesWithFilter("PDFs")
                            } else {
                                onNavigateToNotes()
                            }
                        }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Ruled Study Notebook",
                        subtitle = "Classic handwritten sheets with customizable margins",
                        icon = Icons.Default.DriveFileRenameOutline,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            viewModel.createNewNote("Ruled Pad Notes", "ruled")
                            if (onNavigateToNotesWithFilter != null) {
                                onNavigateToNotesWithFilter("Handwritten")
                            } else {
                                onNavigateToNotes()
                            }
                        }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Cornell Academic Pad",
                        subtitle = "Standardized recall layout for lecture memorization",
                        icon = Icons.Default.ListAlt,
                        color = Color(0xFFE0F7FA),
                        onClick = {
                            viewModel.createNewNote("Cornell Note", "cornell")
                            if (onNavigateToNotesWithFilter != null) {
                                onNavigateToNotesWithFilter("Templates")
                            } else {
                                onNavigateToNotes()
                            }
                        }
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "PDF Notes & Documents Folder",
                        subtitle = "$pdfNotesCount PDF files imported • Tap to open PDF folder",
                        icon = Icons.Default.PictureAsPdf,
                        color = Color(0xFFFFEBEE),
                        onClick = {
                            if (onNavigateToNotesWithFilter != null) {
                                onNavigateToNotesWithFilter("PDFs")
                            } else {
                                onNavigateToNotes()
                            }
                        }
                    )
                    ActionCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Ruled Study Notebook",
                        subtitle = "Classic handwritten sheets with customizable layout margins",
                        icon = Icons.Default.DriveFileRenameOutline,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            viewModel.createNewNote("Ruled Pad Notes", "ruled")
                            if (onNavigateToNotesWithFilter != null) {
                                onNavigateToNotesWithFilter("Handwritten")
                            } else {
                                onNavigateToNotes()
                            }
                        }
                    )
                    ActionCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Cornell Academic Pad",
                        subtitle = "Standardized recall layout perfect for lecture memorization",
                        icon = Icons.Default.ListAlt,
                        color = Color(0xFFE0F7FA),
                        onClick = {
                            viewModel.createNewNote("Cornell Note", "cornell")
                            if (onNavigateToNotesWithFilter != null) {
                                onNavigateToNotesWithFilter("Templates")
                            } else {
                                onNavigateToNotes()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Two Major Interactive Workspaces (Tasks & Pomodoro side-by-side or stacked)
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        DashboardTasksWidget()
                    }
                    Box(modifier = Modifier.weight(0.8f)) {
                        DashboardPomodoroWidget(viewModel)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    DashboardTasksWidget()
                    DashboardPomodoroWidget(viewModel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Recent/Pinned Notes Carousel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTINUE WORKING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onNavigateToNotes) {
                    Text("View All", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }

            if (notes.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No notes created yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap on 'Ruled Notebook' or 'Cornell Academic Pad' above to get started!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(notes.take(6)) { note ->
                        RecentNoteCard(
                            note = note,
                            onClick = {
                                viewModel.selectNote(note)
                                onNavigateToNotes()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. AI Smart Recommendations Hub
            Text(
                text = "PERSONALIZED AI SUGGESTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AISuggestionRow()

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
    onCustomizeClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "statCardScale"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "statCardProgress"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title.uppercase(),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = value,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (onCustomizeClick != null) {
                    IconButton(
                        onClick = onCustomizeClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = tint,
                    trackColor = tint.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
fun CustomizeGoalsModal(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    var tempGoalMins by remember { mutableStateOf(viewModel.dailyGoalTargetMinutes.toString()) }
    var tempTaskGoal by remember { mutableStateOf(viewModel.dailyTaskGoalTarget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                // 1. Daily Study Time Goal (Minutes)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF00B0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Study Time Target", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                    color = if (isSel) Color(0xFF00B0FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF00B0FF) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { tempGoalMins = mins.toString() }
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSel) Color(0xFF00B0FF) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tempGoalMins,
                            onValueChange = { tempGoalMins = it.filter { char -> char.isDigit() } },
                            label = { Text("Target Minutes / Day", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                    }
                }

                // 2. Automatic Study Streak Information
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Current Streak: ${viewModel.studyStreakDays} Days 🔥", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚡ Automatic Streak Tracker: Your study streak automatically increments each day you study in the app! If you don't study for 2 consecutive days, your streak resets to 0.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                // 3. Daily Task Target
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Tasks Goal Target", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempTaskGoal,
                            onValueChange = { tempTaskGoal = it.filter { char -> char.isDigit() } },
                            label = { Text("Target Tasks to Complete / Day", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goalVal = tempGoalMins.toIntOrNull() ?: 30
                    val taskVal = tempTaskGoal.toIntOrNull() ?: 3
                    
                    viewModel.updateDailyGoalMinutes(goalVal)
                    viewModel.updateDailyTaskGoalTarget(taskVal)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Custom Goals")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "actionCardScale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RecentNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "recentNoteCardScale"
    )

    val dateString = remember(note.lastModifiedTime) {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        sdf.format(Date(note.lastModifiedTime))
    }

    val icon = when (note.templateType) {
        "pdf" -> Icons.Default.PictureAsPdf
        "cornell" -> Icons.Default.ListAlt
        "ruled" -> Icons.Default.DriveFileRenameOutline
        "grid" -> Icons.Default.GridView
        else -> Icons.Default.Edit
    }

    val tint = when (note.templateType) {
        "pdf" -> Color(0xFFEF5350)
        "cornell" -> Color(0xFF26A69A)
        "ruled" -> Color(0xFF5C6BC0)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .width(200.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            if (note.coverType != "none") {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    RenderCover(
                        coverType = note.coverType,
                        title = note.coverTitle,
                        subtitle = note.coverSubtitle,
                        author = note.coverAuthor,
                        extra = note.coverExtra,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = note.templateType.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    modifier = Modifier
                        .background(tint.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = note.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Last active: $dateString",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        }
    }
}

@Composable
fun DashboardPomodoroWidget(viewModel: NoteViewModel) {
    val isTimerRunning = viewModel.timerIsRunning
    val timeLeftSeconds = viewModel.timerRemainingSeconds
    
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "POMODORO FOCUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isTimerRunning) "ACTIVE" else "READY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTimerRunning) Color(0xFF10B981) else Color(0xFFFF5722),
                    modifier = Modifier
                        .background(
                            if (isTimerRunning) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timer display
            Text(
                text = formattedTime,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset duration quick picks
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "10m" to 600,
                    "25m" to 1500,
                    "50m" to 3000
                ).forEach { (label, secs) ->
                    val isSelected = viewModel.timerTotalSeconds == secs
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFFF5722).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF5722) else Color.Transparent),
                        modifier = Modifier.clickable {
                            viewModel.resetTimer(secs)
                        }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isTimerRunning) {
                            viewModel.pauseTimer()
                        } else {
                            viewModel.startTimer()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else Color(0xFFFF5722)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pomodoro_focus_button")
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTimerRunning) "Pause" else "Focus Now")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetTimer(viewModel.timerTotalSeconds)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardTasksWidget() {
    var taskText by remember { mutableStateOf("") }
    val tasks = remember {
        mutableStateListOf(
            TaskItem("Prepare for Compiler Exam", true),
            TaskItem("Re-read machine learning pdf booklet", false),
            TaskItem("Draft sketch notes on neural nets", false)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TODAY'S STUDY TASKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${tasks.count { it.done }} of ${tasks.size} done",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task list container
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tasks.forEachIndexed { idx, t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tasks[idx] = t.copy(done = !t.done) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (t.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (t.done) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (t.done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { tasks.removeAt(idx) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input task
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 0.dp),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            if (taskText.isEmpty()) {
                                Text(
                                    text = "Add study item...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                innerTextField()
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (taskText.isNotBlank()) {
                            tasks.add(TaskItem(taskText.trim(), false))
                            taskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

data class TaskItem(val text: String, val done: Boolean)

@Composable
fun AISuggestionRow() {
    val suggestions = listOf(
        SuggestionData(
            title = "Semantic Smart Tags",
            desc = "Auto-cluster 'Cornell Notes' into #exam-prep tag groups",
            accent = Color(0xFF00B0FF),
            icon = Icons.Default.AutoAwesome
        ),
        SuggestionData(
            title = "Study Quiz Flashcards",
            desc = "Generate 5 critical flashcards from recent handwriting OCR",
            accent = Color(0xFFFF9100),
            icon = Icons.Default.Quiz
        ),
        SuggestionData(
            title = "Lectures Audio Cleanup",
            desc = "Run Gemini summaries on voice audio transcription",
            accent = Color(0xFF00E676),
            icon = Icons.Default.KeyboardVoice
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        suggestions.forEach { sug ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sug.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = sug.accent
                        )
                        Icon(
                            imageVector = sug.icon,
                            contentDescription = null,
                            tint = sug.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sug.desc,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 3,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

data class SuggestionData(val title: String, val desc: String, val accent: Color, val icon: ImageVector)
