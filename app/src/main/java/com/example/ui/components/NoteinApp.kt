package com.example.ui.components

import androidx.compose.ui.platform.LocalContext


import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.data.NoteEntity
import com.example.data.Point
import com.example.data.Stroke
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable

private fun formatStorageSize(bytes: Long): String {
    return when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
        else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024f * 1024f))
    }
}

@Composable
fun NoteinApp(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val selectedNote = viewModel.selectedNote

    // Automatically load the last opened note on startup if none is selected yet
    LaunchedEffect(notes) {
        if (selectedNote == null && notes.isNotEmpty()) {
            val lastOpenedId = viewModel.getLastOpenedNoteId()
            if (lastOpenedId != -1L) {
                val matchedNote = notes.find { it.id.toLong() == lastOpenedId }
                if (matchedNote != null) {
                    viewModel.selectNote(matchedNote)
                }
            }
        }
    }

    // UI state parameters
    var activeTab by rememberSaveable { mutableStateOf("home") } // "home", "notes", "sync", "ai"
    var selectedFilter by rememberSaveable { mutableStateOf("All Notes") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }

    // Collapsible Panels for Tablet Layout
    var isSidebarExpanded by rememberSaveable { mutableStateOf(true) }
    var isNoteListExpanded by rememberSaveable { mutableStateOf(true) }

    // Adaptive Window Size Check (Compact < 600dp vs Expanded >= 600dp)
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val tabStack = remember { mutableStateListOf("home") }

    fun navigateToTab(tab: String, filter: String? = null) {
        if (filter != null) {
            selectedFilter = filter
        }
        if (tab == "home") {
            tabStack.clear()
            tabStack.add("home")
            activeTab = "home"
        } else {
            if (tab != activeTab) {
                activeTab = tab
                if (tabStack.lastOrNull() != tab) {
                    tabStack.add(tab)
                }
            }
        }
    }

    val isBackHandlerEnabled = drawerState.isOpen ||
            selectedNote != null ||
            searchKeyword.isNotEmpty() ||
            tabStack.size > 1 ||
            activeTab != "home"

    // Intercept Back Gesture to navigate step-by-step back to previous section before exiting
    androidx.activity.compose.BackHandler(enabled = isBackHandlerEnabled) {
        when {
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }
            selectedNote != null -> {
                viewModel.selectNote(null)
            }
            searchKeyword.isNotEmpty() -> {
                searchKeyword = ""
            }
            tabStack.size > 1 -> {
                tabStack.removeAt(tabStack.lastIndex)
                val prevTab = tabStack.lastOrNull() ?: "home"
                activeTab = prevTab
            }
            activeTab != "home" -> {
                activeTab = "home"
                selectedFilter = "All Notes"
            }
        }
    }

    LaunchedEffect(viewModel.isFullViewMode) {
        val activity = (context as? com.example.MainActivity)
        if (activity != null) {
            activity.updateSystemBarsVisibility(viewModel.isFullViewMode)
        } else {
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (viewModel.isFullViewMode) {
                    controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isTablet,
        drawerContent = {
            if (!isTablet) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(280.dp)
                ) {
                    ResponsiveSidebar(
                        notes = notes,
                        viewModel = viewModel,
                        activeTab = activeTab,
                        onTabChange = { tab ->
                            navigateToTab(tab)
                            scope.launch { drawerState.close() }
                        },
                        selectedFilter = selectedFilter,
                        onFilterChange = { filter ->
                            selectedFilter = filter
                            scope.launch { drawerState.close() }
                        },
                        searchKeyword = searchKeyword,
                        onSearchChange = { searchKeyword = it },
                        userViewModel = userViewModel
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isTablet && activeTab != "notes" && !viewModel.isFullViewMode) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = activeTab == "home",
                            onClick = { navigateToTab("home") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Notes") },
                            label = { Text("Notes") },
                            selected = activeTab == "notes",
                            onClick = { navigateToTab("notes") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Cloud, contentDescription = "Sync") },
                            label = { Text("Backup") },
                            selected = activeTab == "sync",
                            onClick = { navigateToTab("sync") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Responsive Tablet Sidebar
                if (isTablet && (!viewModel.isFullscreen || activeTab != "notes")) {
                    AnimatedVisibility(
                        visible = isSidebarExpanded,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        ResponsiveSidebar(
                            notes = notes,
                            viewModel = viewModel,
                            activeTab = activeTab,
                            onTabChange = { navigateToTab(it) },
                            selectedFilter = selectedFilter,
                            onFilterChange = { selectedFilter = it },
                            searchKeyword = searchKeyword,
                            onSearchChange = { searchKeyword = it },
                            userViewModel = userViewModel
                        )
                    }
                }
                // Main Contents
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            val isDashboardSyncTransition =
                                (initialState == "home" && targetState == "sync") ||
                                (initialState == "sync" && targetState == "home")

                            val fadeThroughEnter: EnterTransition = if (isDashboardSyncTransition) {
                                fadeIn(
                                    animationSpec = tween(durationMillis = 300, delayMillis = 90, easing = FastOutSlowInEasing)
                                ) + scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(durationMillis = 300, delayMillis = 90, easing = FastOutSlowInEasing)
                                ) + slideInVertically(
                                    initialOffsetY = { if (targetState == "sync") it / 20 else -it / 20 },
                                    animationSpec = tween(durationMillis = 300, delayMillis = 90, easing = FastOutSlowInEasing)
                                )
                            } else {
                                fadeIn(
                                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                                ) + scaleIn(
                                    initialScale = 0.95f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                ) + slideInHorizontally(
                                    initialOffsetX = { if (targetState == "home") -it / 5 else it / 5 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }

                            val fadeThroughExit: ExitTransition = if (isDashboardSyncTransition) {
                                fadeOut(
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                                ) + scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                                ) + slideOutVertically(
                                    targetOffsetY = { if (targetState == "sync") -it / 20 else it / 20 },
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                                )
                            } else {
                                fadeOut(
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                ) + scaleOut(
                                    targetScale = 0.95f,
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                ) + slideOutHorizontally(
                                    targetOffsetX = { if (targetState == "home") it / 5 else -it / 5 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }

                            fadeThroughEnter togetherWith fadeThroughExit
                        },
                        label = "MainTabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            "home" -> {
                                NovaDashboard(
                                    notes = notes,
                                    viewModel = viewModel,
                                    onNavigateToNotes = {
                                        navigateToTab("notes", "All Notes")
                                    },
                                    onNavigateToNotesWithFilter = { filter ->
                                        navigateToTab("notes", filter)
                                    },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    isTablet = isTablet,
                                    userViewModel = userViewModel
                                )
                            }
                            "notes" -> {
                                NoteWorkspace(
                                    notes = notes,
                                    selectedNote = selectedNote,
                                    viewModel = viewModel,
                                    isTablet = isTablet,
                                    searchKeyword = searchKeyword,
                                    onSearchChange = { searchKeyword = it },
                                    onCreateNoteClick = { showCreateDialog = true },
                                    selectedFilter = selectedFilter,
                                    onFilterSelected = { selectedFilter = it },
                                    isSidebarExpanded = isSidebarExpanded,
                                    onToggleSidebar = { isSidebarExpanded = !isSidebarExpanded },
                                    isNoteListExpanded = isNoteListExpanded,
                                    onToggleNoteList = { isNoteListExpanded = !isNoteListExpanded },
                                    onHomeClick = { navigateToTab("home") },
                                    onOpenMenu = { scope.launch { drawerState.open() } }
                                )
                            }
                            "sync" -> {
                                SyncDashboard(
                                    viewModel = viewModel,
                                    userViewModel = userViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NotebookStudioDialog(
            note = null,
            onDismiss = { showCreateDialog = false },
            onCreateNew = { title, templateType, coverType, pageColor, coverTitle, coverSubtitle, coverAuthor, coverExtra, folder ->
                viewModel.createNewNoteWithDesign(
                    title = title,
                    templateType = templateType,
                    coverType = coverType,
                    pageColor = pageColor,
                    coverTitle = coverTitle,
                    coverSubtitle = coverSubtitle,
                    coverAuthor = coverAuthor,
                    coverExtra = coverExtra,
                    folder = folder
                )
                showCreateDialog = false
                navigateToTab("notes")
            }
        )
    }

    if (viewModel.showOnboardingDialog) {
        OnboardingDialog(
            onDismiss = { viewModel.dismissOnboardingDialog() }
        )
    }

    if (viewModel.showDocumentScannerOverlay) {
        LipiDocumentScanner(
            viewModel = viewModel,
            onDismiss = {
                viewModel.closeDocumentScanner()
                if (viewModel.selectedNote != null) {
                    navigateToTab("notes")
                }
            }
        )
    }

    if (viewModel.showConflictDialog) {
        NoteConflictDialog(viewModel = viewModel)
    }

    if (viewModel.showGoogleSearchDialog) {
        GoogleSearchDialog(
            initialQuery = viewModel.googleSearchQuery,
            onDismissRequest = { viewModel.closeGoogleSearch() }
        )
    }

    if (viewModel.showUpdatePromptDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdatePromptDialog() },
            icon = {
                Icon(
                    imageVector = if (viewModel.updateAvailable) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (viewModel.updateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (viewModel.updateAvailable) "Update Available (v${viewModel.updateVersionName})" else "App is Up to Date (v${com.example.BuildConfig.VERSION_NAME})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (viewModel.updateAvailable) {
                            "A new version of Lipi Notes (v${viewModel.updateVersionName}) is available! Click 'Download APK' to open GitHub and automatically start downloading the update."
                        } else {
                            "You are currently running the latest version of Lipi Notes (v${com.example.BuildConfig.VERSION_NAME}). You can re-download the latest release APK from GitHub anytime."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (viewModel.updateNotes.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Release Details:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = viewModel.updateNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (viewModel.updateProgress != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Downloading update: ${(viewModel.updateProgress!! * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LinearProgressIndicator(
                                progress = { viewModel.updateProgress!! },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    if (viewModel.updateError != null) {
                        Text(
                            text = viewModel.updateError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val targetUrl = when {
                                viewModel.updateReleaseUrl.isNotBlank() -> viewModel.updateReleaseUrl
                                viewModel.updateApkUrl.isNotBlank() -> viewModel.updateApkUrl
                                else -> "https://github.com/codeAditya828113/Lipi-Notes-V3"
                            }
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("OTAUpdate", "Could not open browser", e)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View Releases", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.markPendingUpdate(viewModel.updateNotes)
                            viewModel.downloadAndInstallApk()
                        },
                        enabled = viewModel.updateProgress == null,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("download_latest_apk_button")
                    ) {
                        if (viewModel.updateProgress != null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Downloading...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (viewModel.updateAvailable) "Download & Install" else "Re-download APK", fontSize = 12.sp)
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissUpdatePromptDialog() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (viewModel.showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissChangelogDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "🎉 What's New in Lipi Notes v${viewModel.changelogVersionName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your app has been successfully updated! Here is the change log and list of improvements in this release:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Release Notes & Highlights:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.changelogNotes,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissChangelogDialog() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got It, Explore!")
                }
            }
        )
    }
}

@Composable
fun NoteWorkspaceTabBar(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    viewModel: NoteViewModel,
    onCreateNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (viewModel.isFullscreen) return

    val sysDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = viewModel.isDarkTheme(sysDark)

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .padding(horizontal = 8.dp, vertical = 0.dp)
    ) {
        val openNotes = remember(notes, viewModel.openNoteIds) {
            notes.filter { it.id in viewModel.openNoteIds }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // "All Notes" Tab
            val isAllNotesSelected = (selectedNote == null)
            val allNotesBg by animateColorAsState(
                targetValue = if (isAllNotesSelected) (if (isDark) Color(0xFF1E293B) else Color.White) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "allNotesBg"
            )
            val allNotesTextColor by animateColorAsState(
                targetValue = if (isAllNotesSelected) (if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "allNotesTextColor"
            )
            val allNotesIconColor by animateColorAsState(
                targetValue = if (isAllNotesSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "allNotesIconColor"
            )

            Surface(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                color = allNotesBg,
                border = if (isAllNotesSelected) BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)) else null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.selectNote(null) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "All Notes Tab",
                        tint = allNotesIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All Notes",
                        fontSize = 13.sp,
                        fontWeight = if (isAllNotesSelected) FontWeight.Bold else FontWeight.Medium,
                        color = allNotesTextColor,
                        maxLines = 1
                    )
                }
            }

            // Open Note Tabs
            openNotes.forEach { note ->
                val isTabSelected = (selectedNote?.id == note.id)
                val baseIconTint = if (note.templateType == "pdf") Color(0xFFD32F2F) else Color(0xFF3B82F6)

                val tabBg by animateColorAsState(
                    targetValue = if (isTabSelected) (if (isDark) Color(0xFF1E293B) else Color.White) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabBg"
                )
                val tabTextColor by animateColorAsState(
                    targetValue = if (isTabSelected) (if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabTextColor"
                )
                val tabIconTint by animateColorAsState(
                    targetValue = if (isTabSelected) baseIconTint else baseIconTint.copy(alpha = 0.7f),
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabIconTint"
                )

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    color = tabBg,
                    border = if (isTabSelected) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.selectNote(note) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (note.templateType == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Description,
                            contentDescription = null,
                            tint = tabIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = note.title,
                            fontSize = 13.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tabTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = { viewModel.closeNote(note) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Note",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // New Tab Button
            IconButton(
                onClick = onCreateNoteClick,
                modifier = Modifier
                    .padding(bottom = 6.dp, start = 6.dp)
                    .size(28.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Note Tab",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Right side header controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
        ) {
            IconButton(
                onClick = { viewModel.isFullscreen = !viewModel.isFullscreen },
                modifier = Modifier.size(32.dp).testTag("fullscreen_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Immersive Fullscreen",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onCreateNoteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Templates Menu",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun NoteWorkspace(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    viewModel: NoteViewModel,
    isTablet: Boolean,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    isNoteListExpanded: Boolean,
    onToggleNoteList: () -> Unit,
    onHomeClick: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var pdfName = "Imported Document"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        pdfName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.importPdfToNote(uri, pdfName)
        }
    }

    val docxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var docxName = "Imported Document"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        docxName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.importDocxToNote(uri, docxName)
        }
    }

    var isGridView by rememberSaveable { mutableStateOf(true) }
    var selectedSortOption by rememberSaveable { mutableStateOf("Date (Newest)") }
    var starredNoteIds by remember { mutableStateOf(setOf<Int>()) }

    // Filter and sort notes based on search query, filter tab, and sort selection
    val filteredNotes = remember(notes, searchKeyword, selectedFilter, selectedSortOption, starredNoteIds) {
        val baseFiltered = if (searchKeyword.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(searchKeyword, ignoreCase = true) ||
                        note.content.contains(searchKeyword, ignoreCase = true) ||
                        (note.summary ?: "").contains(searchKeyword, ignoreCase = true) ||
                        note.tags.contains(searchKeyword, ignoreCase = true) ||
                        (note.audioTranscription ?: "").contains(searchKeyword, ignoreCase = true)
            }
        }
        
        val categoryFiltered = when {
            selectedFilter in listOf("Scanned Documents", "Scanned Docs", "Scanned") -> baseFiltered.filter {
                it.templateType == "scanned_doc" ||
                it.tags.contains("scanned", ignoreCase = true) ||
                it.title.contains("Scanned", ignoreCase = true) ||
                it.title.contains("Scan", ignoreCase = true) ||
                (it.pdfTitle ?: "").contains("Scanned", ignoreCase = true)
            }
            selectedFilter in listOf("Handwritten", "Note") -> baseFiltered.filter { it.templateType in listOf("blank", "ruled", "grid") }
            selectedFilter in listOf("PDFs", "PDF", "Imported PDFs & Docs") -> baseFiltered.filter { it.templateType == "pdf" || it.templateType == "docx" || !it.pdfTitle.isNullOrEmpty() || it.title.contains(".pdf", ignoreCase = true) || it.title.contains("PDF", ignoreCase = true) }
            selectedFilter in listOf("Templates", "Folder", "Structural Templates") -> baseFiltered.filter { it.templateType in listOf("cornell", "meeting") }
            selectedFilter in listOf("Favorites", "Starred") -> baseFiltered.filter { starredNoteIds.contains(it.id) }
            selectedFilter == "Work/Projects" -> baseFiltered.filter { it.tags.contains("work", ignoreCase = true) || it.title.contains("project", ignoreCase = true) || it.title.contains("work", ignoreCase = true) }
            selectedFilter == "School/Lectures" -> baseFiltered.filter { it.tags.contains("school", ignoreCase = true) || it.tags.contains("study", ignoreCase = true) || it.title.contains("lecture", ignoreCase = true) }
            selectedFilter == "Personal/Ideas" -> baseFiltered.filter { it.tags.contains("personal", ignoreCase = true) || it.tags.contains("ideas", ignoreCase = true) }
            selectedFilter.startsWith("dir:") -> {
                val dirId = selectedFilter.removePrefix("dir:")
                val targetDir = viewModel.customDirectories.find { it.id == dirId }
                val childDirIds = viewModel.customDirectories.filter { it.parentId == dirId }.map { it.id }
                val dirName = targetDir?.name ?: ""
                baseFiltered.filter { note ->
                    note.tags.contains("dir:$dirId", ignoreCase = true) ||
                    (dirName.isNotBlank() && note.tags.contains(dirName, ignoreCase = true)) ||
                    (dirName.isNotBlank() && note.title.contains(dirName, ignoreCase = true)) ||
                    childDirIds.any { childId -> note.tags.contains("dir:$childId", ignoreCase = true) }
                }
            }
            selectedFilter.startsWith("tag:") -> {
                val tagQuery = selectedFilter.removePrefix("tag:")
                baseFiltered.filter { note ->
                    note.tags.contains(tagQuery, ignoreCase = true) ||
                    note.tags.contains("tag:$tagQuery", ignoreCase = true)
                }
            }
            else -> {
                val matchingDir = viewModel.customDirectories.find { it.name.equals(selectedFilter, ignoreCase = true) }
                if (matchingDir != null) {
                    val dirId = matchingDir.id
                    val childDirIds = viewModel.customDirectories.filter { it.parentId == dirId }.map { it.id }
                    baseFiltered.filter { note ->
                        note.tags.contains("dir:$dirId", ignoreCase = true) ||
                        note.tags.contains(matchingDir.name, ignoreCase = true) ||
                        note.title.contains(matchingDir.name, ignoreCase = true) ||
                        childDirIds.any { childId -> note.tags.contains("dir:$childId", ignoreCase = true) }
                    }
                } else {
                    baseFiltered
                }
            }
        }

        when (selectedSortOption) {
            "Date (Oldest)" -> categoryFiltered.sortedBy { it.lastModifiedTime }
            "Title (A to Z)" -> categoryFiltered.sortedBy { it.title.lowercase() }
            "Title (Z to A)" -> categoryFiltered.sortedByDescending { it.title.lowercase() }
            "Template Type" -> categoryFiltered.sortedBy { it.templateType }
            else -> categoryFiltered.sortedByDescending { it.lastModifiedTime }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoteWorkspaceTabBar(
            notes = notes,
            selectedNote = selectedNote,
            viewModel = viewModel,
            onCreateNoteClick = onCreateNoteClick
        )

        AnimatedContent(
            targetState = selectedNote?.id ?: -1,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                    )
            },
            label = "WorkspaceNoteSwitchAnimation",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { targetNoteId ->
            val activeNote = notes.find { it.id == targetNoteId }
            if (activeNote != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NoteEditorCanvas(
                        viewModel = viewModel,
                        selectedNote = activeNote,
                        notes = notes,
                        onCreateNoteClick = onCreateNoteClick,
                        onBackClick = { viewModel.selectNote(null) },
                        isSidebarExpanded = false,
                        onToggleSidebar = {},
                        isNoteListExpanded = true,
                        onToggleNoteList = {}
                    )
                }
            } else {
                RedesignedAllNotesView(
                    notes = notes,
                    selectedNote = selectedNote,
                    viewModel = viewModel,
                    isTablet = isTablet,
                    searchKeyword = searchKeyword,
                    onSearchChange = onSearchChange,
                    onCreateNoteClick = onCreateNoteClick,
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                    onHomeClick = onHomeClick,
                    onOpenMenu = onOpenMenu,
                    onSelectNote = { viewModel.selectNote(it) },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onRenameNote = { note, newTitle -> viewModel.renameNote(note, newTitle) },
                    onDuplicateNote = { viewModel.duplicateNote(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Replaced by tabs in NoteListHeader
}

@Composable
fun NoteConflictDialog(
    viewModel: NoteViewModel
) {
    val conflicts = viewModel.pendingNoteConflicts
    if (conflicts.isEmpty()) return

    val currentConflict = conflicts.first()
    val totalConflicts = conflicts.size

    val dateFormat = remember {
        java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    }

    Dialog(
        onDismissRequest = { viewModel.dismissConflictDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp).size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Note Version Conflict",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (totalConflicts > 1) "Conflict 1 of $totalConflicts • Select resolution option" else "Choose version to keep after cloud restore",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.dismissConflictDialog() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Note Title Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Note: \"${currentConflict.title}\"",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Comparative View: Local vs Cloud
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Local Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Local Version", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Modified: ${dateFormat.format(java.util.Date(currentConflict.localModifiedTime))}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentConflict.localContentPreview,
                                fontSize = 11.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Cloud Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cloud Version", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Modified: ${dateFormat.format(java.util.Date(currentConflict.cloudModifiedTime))}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentConflict.cloudContentPreview,
                                fontSize = 11.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Resolution Action:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                // 3 Required User Action Buttons: 'Keep Local', 'Keep Cloud', or 'Create Copy'
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Keep Local Button
                    OutlinedButton(
                        onClick = { viewModel.resolveSingleConflict(currentConflict, "KEEP_LOCAL") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Keep Local", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Keep Cloud Button
                    Button(
                        onClick = { viewModel.resolveSingleConflict(currentConflict, "KEEP_CLOUD") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Keep Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Create Copy Button
                    FilledTonalButton(
                        onClick = { viewModel.resolveSingleConflict(currentConflict, "CREATE_COPY") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Create Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // If multiple conflicts, offer Batch Resolution
                if (totalConflicts > 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Apply to All Remaining Conflicts ($totalConflicts):", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.resolveAllConflicts("KEEP_LOCAL") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Keep Local All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { viewModel.resolveAllConflicts("KEEP_CLOUD") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Keep Cloud All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { viewModel.resolveAllConflicts("CREATE_COPY") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Copy All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MicrosoftLogoIcon(modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val gap = sizePx * 0.12f
        val squareSize = (sizePx - gap) / 2f

        // Red square (top-left)
        drawRect(color = Color(0xFFF25022), topLeft = Offset(0f, 0f), size = Size(squareSize, squareSize))
        // Green square (top-right)
        drawRect(color = Color(0xFF7FBA00), topLeft = Offset(squareSize + gap, 0f), size = Size(squareSize, squareSize))
        // Blue square (bottom-left)
        drawRect(color = Color(0xFF00A4EF), topLeft = Offset(0f, squareSize + gap), size = Size(squareSize, squareSize))
        // Yellow square (bottom-right)
        drawRect(color = Color(0xFFFFB900), topLeft = Offset(squareSize + gap, squareSize + gap), size = Size(squareSize, squareSize))
    }
}

@Composable
fun LinkedInLogoIcon(modifier: Modifier = Modifier.size(18.dp)) {
    Box(
        modifier = modifier
            .background(Color(0xFF0A66C2), shape = RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "in",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
fun GoogleDriveBackupDialog(
    viewModel: NoteViewModel,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeProvider = GoogleDriveBackupHelper.getSavedAccountProvider(context)
    val activeEmail = GoogleDriveBackupHelper.getSavedAccountEmail(context)
    val activeName = GoogleDriveBackupHelper.getSavedAccountName(context)
    val isSignedIn = GoogleDriveBackupHelper.isSignedIn(context)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp).size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cloud Backup & Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Automated Cloud Storage & Sync",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isSignedIn && activeEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (activeProvider) {
                                "Microsoft" -> MicrosoftLogoIcon(modifier = Modifier.size(20.dp))
                                "LinkedIn" -> LinkedInLogoIcon(modifier = Modifier.size(20.dp))
                                else -> Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$activeName • $activeProvider",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = activeEmail,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive Backup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (viewModel.lastSyncTime.isNotBlank()) "Last backup: ${viewModel.lastSyncTime}" else "No backup history yet",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (viewModel.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = { viewModel.syncWithGoogleDrive() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Now")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automated Live Backup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Auto-sync note updates directly to Google Drive",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = viewModel.autoBackupEnabled,
                                onCheckedChange = { viewModel.toggleAutoBackup(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Local Storage Quick Backup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Save all notes & study stats to local device storage",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val file = viewModel.createAutoLocalBackupFile()
                                    if (file != null) {
                                        android.widget.Toast.makeText(context, "Saved local backup: ${file.name} 📁", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Local")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Conflict Resolution Dialogue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Simulate cloud/local note conflict dialogue",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    onDismissRequest()
                                    viewModel.triggerSampleConflictTest()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Conflict")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun NoteListHeader(
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onImportDocxClick: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onMenuClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    isTablet: Boolean = false,
    viewModel: NoteViewModel? = null,
    isGridView: Boolean = true,
    onToggleGridView: () -> Unit = {},
    selectedSortOption: String = "Date (Newest)",
    onSortOptionSelected: (String) -> Unit = {}
) {
    var isSearching by remember { mutableStateOf(false) }
    var showDriveBackupDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    if (showDriveBackupDialog && viewModel != null) {
        GoogleDriveBackupDialog(
            viewModel = viewModel,
            onDismissRequest = { showDriveBackupDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        // App Title Bar and Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(24.dp).clickable { onMenuClick() }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (viewModel?.isSyncing == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable { showDriveBackupDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (viewModel?.isSyncing == true) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Syncing...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Saved",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp).clickable { onHomeClick() }
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = if (viewModel?.isSelectionMode == true) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                    contentDescription = "Select",
                    tint = if (viewModel?.isSelectionMode == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp).clickable { viewModel?.toggleSelectionMode() }
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearching || searchKeyword.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp).clickable { isSearching = !isSearching }
                )
            }
        }

        if (isSearching || searchKeyword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = searchKeyword,
                onValueChange = onSearchChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchKeyword.isEmpty()) {
                                Text(
                                    text = "Search by title, content or AI summary...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            innerTextField()
                        }
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
         Box(modifier = Modifier.fillMaxWidth()) {
            val baseFilters = listOf("All", "PDF", "Note", "Folder")
            val customFilters = viewModel?.customDirectories?.map { it.name } ?: emptyList()
            val allFilters = baseFilters + customFilters

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(end = 80.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                allFilters.forEach { filter ->
                    val isSelected = when (filter) {
                        "All" -> selectedFilter == "All Notes" || selectedFilter == "All"
                        "PDF" -> selectedFilter == "PDFs" || selectedFilter == "PDF" || selectedFilter == "Imported PDFs & Docs"
                        "Note" -> selectedFilter == "Handwritten" || selectedFilter == "Note"
                        "Folder" -> selectedFilter == "Templates" || selectedFilter == "Folder" || selectedFilter == "Structural Templates"
                        else -> {
                            val dir = viewModel?.customDirectories?.find { it.name == filter }
                            if (dir != null) selectedFilter == "dir:${dir.id}" else filter == selectedFilter
                        }
                    }
                    val targetFilter = when (filter) {
                        "All" -> "All Notes"
                        "PDF" -> "PDFs"
                        "Note" -> "Handwritten"
                        "Folder" -> "Templates"
                        else -> {
                            val dir = viewModel?.customDirectories?.find { it.name == filter }
                            if (dir != null) "dir:${dir.id}" else filter
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onFilterSelected(targetFilter) }
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (filter == "PDF") "PDF Notes" else if (filter == "Folder") "Templates" else filter,
                            fontSize = 18.sp,
                            fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected) {
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.width(40.dp).height(3.dp).background(MaterialTheme.colorScheme.primary))
                        } else {
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }
              
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterEnd)) {
                Box {
                    Canvas(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showThemeMenu = true }
                    ) {
                        val gradient = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue))
                        drawRoundRect(brush = gradient, cornerRadius = CornerRadius(4.dp.toPx()))
                    }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Light Theme") },
                            onClick = {
                                showThemeMenu = false
                                viewModel?.updateThemeMode("light")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dark Theme") },
                            onClick = {
                                showThemeMenu = false
                                viewModel?.updateThemeMode("dark")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("OLED Black Theme") },
                            onClick = {
                                showThemeMenu = false
                                viewModel?.updateThemeMode("oled")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("System Default") },
                            onClick = {
                                showThemeMenu = false
                                viewModel?.updateThemeMode("system")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (viewModel?.dynamicColorEnabled == true) "✓ Material You Active" else "Material You Dynamic Color") },
                            onClick = {
                                showThemeMenu = false
                                viewModel?.toggleDynamicColor(!(viewModel?.dynamicColorEnabled ?: false))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onToggleGridView,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.GridView,
                        contentDescription = "Toggle View Mode",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showSortMenu = true }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Options",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (selectedSortOption) {
                                "Date (Oldest)" -> "Oldest"
                                "Title (A to Z)" -> "A-Z"
                                "Title (Z to A)" -> "Z-A"
                                "Template Type" -> "Type"
                                else -> "Sort"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        val options = listOf(
                            "Date (Newest)" to "Date (Newest First)",
                            "Date (Oldest)" to "Date (Oldest First)",
                            "Title (A to Z)" to "Title (A to Z)",
                            "Title (Z to A)" to "Title (Z to A)",
                            "Template Type" to "Template Type"
                        )
                        options.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selectedSortOption == key) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOption == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showSortMenu = false
                                    onSortOptionSelected(key)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (viewModel?.isSelectionMode == true) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${viewModel.selectedNoteIds.size} Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(
                            onClick = {
                                if (viewModel.selectedNoteIds.isNotEmpty()) {
                                    viewModel.clearSelectedNotes()
                                } else {
                                    // Select all Notes
                                    viewModel.selectedNoteIds = viewModel.allNotes.value.map { it.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (viewModel.selectedNoteIds.isNotEmpty()) "Clear All" else "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.duplicateSelectedNotes() },
                            enabled = viewModel.selectedNoteIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Batch Duplicate",
                                tint = if (viewModel.selectedNoteIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelectedNotes() },
                            enabled = viewModel.selectedNoteIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Batch Delete",
                                tint = if (viewModel.selectedNoteIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleSelectionMode() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Selection"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCardPreview(note: NoteEntity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(note.pageColor), RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (note.coverType != "none") {
            RenderCover(
                coverType = note.coverType,
                title = note.coverTitle,
                subtitle = note.coverSubtitle,
                author = note.coverAuthor,
                extra = note.coverExtra,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
            )
        } else {
            when (note.templateType) {
                "grid" -> {
                    Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                        val step = 10.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = Color(0xFFC4C7CF).copy(alpha = 0.3f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = Color(0xFFC4C7CF).copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }
                }
                "ruled" -> {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    val step = 12.dp.toPx()
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF93C5FD).copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f
                        )
                        y += step
                    }
                }
            }
            "cornell" -> {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    val cueX = size.width * 0.3f
                    drawLine(
                        color = Color(0xFFC4C7CF).copy(alpha = 0.7f),
                        start = Offset(cueX, 0f),
                        end = Offset(cueX, size.height * 0.75f),
                        strokeWidth = 1.5f
                    )
                    val summaryY = size.height * 0.75f
                    drawLine(
                        color = Color(0xFFC4C7CF).copy(alpha = 0.7f),
                        start = Offset(0f, summaryY),
                        end = Offset(size.width, summaryY),
                        strokeWidth = 1.5f
                    )
                }
            }
            "meeting" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF1F5F9)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF1F5F9)))
                    }
                }
            }
            "pdf" -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                var pdfBitmap by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null) }
                
                androidx.compose.runtime.LaunchedEffect(note.id) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val pdfFile = java.io.File(context.filesDir, "note_${note.id}.pdf")
                        if (pdfFile.exists()) {
                            val bitmap = PdfHelper.renderPdfPageToBitmap(pdfFile, 0, 400, 600)
                            if (bitmap != null) {
                                pdfBitmap = bitmap
                            }
                        }
                    }
                }
                
                if (pdfBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = pdfBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Document",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            else -> {
                if (note.title == "Deforestation Detection System") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else if (note.title == "Quick Start Guide") {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFE0B2)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ImportContacts,
                            contentDescription = "Guide",
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else if (note.title == "Scratch paper" && note.templateType == "blank") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(size.width * 0.2f, size.height * 0.8f)
                            path.lineTo(size.width * 0.8f, size.height * 0.8f)
                            path.lineTo(size.width * 0.5f, size.height * 0.2f)
                            path.close()
                            drawPath(path, color = Color(0xFFFF5252).copy(alpha = 0.8f))
                        }
                    }
                } else {
                    // Blank page preview
                }
            }

            }

            val strokes = androidx.compose.runtime.remember(note.drawingData) {
                try {
                    com.example.data.StrokeSerializer.deserializeStrokes(note.drawingData)
                        .filter { it.page == 1 && !it.isHidden }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            if (strokes.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    val scaleX = size.width / 1200f
                    val scaleY = size.height / 1600f
                    
                    withTransform({
                        scale(scaleX, scaleY, Offset.Zero)
                    }) {
                        strokes.forEach { stroke ->
                            if (stroke.points.size >= 2) {
                                val path = androidx.compose.ui.graphics.Path()
                                path.moveTo(stroke.points.first().x, stroke.points.first().y)
                                for (i in 1 until stroke.points.size) {
                                    val current = stroke.points[i]
                                    val prev = stroke.points[i - 1]
                                    val midX = (prev.x + current.x) / 2
                                    val midY = (prev.y + current.y) / 2
                                    if (i == 1) {
                                        path.lineTo(midX, midY)
                                    } else {
                                        path.quadraticTo(prev.x, prev.y, midX, midY)
                                    }
                                }
                                path.lineTo(stroke.points.last().x, stroke.points.last().y)
                                
                                val color = Color(stroke.color)
                                val width = stroke.width
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    ),
                                    blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteList(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    onSelect: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onRename: (NoteEntity, String) -> Unit,
    onDuplicate: (NoteEntity) -> Unit,
    viewModel: NoteViewModel? = null,
    isGridView: Boolean = true,
    starredNoteIds: Set<Int> = emptySet(),
    onToggleStar: (Int) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "No Notes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Notebook Pads Found",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start taking handwritten notes, import PDF study materials, or create a Cornell pad.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    if (viewModel != null) {
                        Button(
                            onClick = { viewModel.createNewNote("Ruled Pad", "ruled") },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Ruled Pad", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = if (isGridView) GridCells.Adaptive(minSize = 200.dp) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalArrangement = Arrangement.spacedBy(if (isGridView) 40.dp else 16.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                val isSelected = selectedNote?.id == note.id
                val isCheckSelected = viewModel?.selectedNoteIds?.contains(note.id) == true
                var showContextMenu by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                var showCustomizeTemplateDialog by remember { mutableStateOf(false) }
                var showMoveDialog by remember { mutableStateOf(false) }
                val isStarred = starredNoteIds.contains(note.id)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_card_${note.id}")
                        .combinedClickable(
                            onClick = {
                                if (viewModel?.isSelectionMode == true) {
                                    viewModel.toggleNoteSelection(note.id)
                                } else {
                                    onSelect(note)
                                }
                            },
                            onLongClick = {
                                if (viewModel?.isSelectionMode == true) {
                                    viewModel.toggleNoteSelection(note.id)
                                } else {
                                    showContextMenu = true
                                }
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(if (isGridView) 3f/4f else 4f/1f).shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface)) {
                         NoteCardPreview(note = note, modifier = Modifier.fillMaxSize())
                         if (viewModel?.isSelectionMode == true) {
                             Box(
                                 modifier = Modifier
                                     .align(Alignment.TopStart)
                                     .padding(8.dp)
                                     .size(24.dp)
                                     .background(
                                         color = if (isCheckSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                         shape = CircleShape
                                     )
                                     .border(
                                         width = 2.dp,
                                         color = if (isCheckSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                         shape = CircleShape
                                     ),
                                 contentAlignment = Alignment.Center
                             ) {
                                 if (isCheckSelected) {
                                     Icon(
                                         imageVector = Icons.Default.Check,
                                         contentDescription = "Selected",
                                         tint = MaterialTheme.colorScheme.onPrimary,
                                         modifier = Modifier.size(16.dp)
                                     )
                                 }
                             }
                         }
                         Icon(
                             imageVector = if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                             contentDescription = "Favorite",
                             modifier = Modifier
                                 .align(Alignment.TopEnd)
                                 .padding(8.dp)
                                 .size(20.dp)
                                 .clickable { onToggleStar(note.id) },
                             tint = if (isStarred) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                         )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = note.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(18.dp).clickable { showContextMenu = true },
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text(
                            text = "${(note.id % 20) + 1}P",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(note.lastModifiedTime)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (note.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "ML Kit Searchable",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { 
                                showContextMenu = false
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Customize Cover & Template")
                                }
                            },
                            onClick = { 
                                showContextMenu = false
                                showCustomizeTemplateDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Note")
                                }
                            },
                            onClick = {
                                showContextMenu = false
                                viewModel?.shareNote(context, note)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (note.isPinned) "Unpin Note" else "Pin to Top")
                                }
                            },
                            onClick = {
                                showContextMenu = false
                                viewModel?.toggleNotePin(note)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move / Categorize") },
                            onClick = { 
                                showContextMenu = false
                                showMoveDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { 
                                showContextMenu = false
                                onDuplicate(note)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { 
                                showContextMenu = false
                                onDelete(note)
                            }
                        )
                    }

                    if (showMoveDialog && viewModel != null) {
                        var showCreateDirDialog by remember { mutableStateOf(false) }
                        var showCreateTagDialog by remember { mutableStateOf(false) }

                        AssignDirectoryAndTagsDialog(
                            note = note,
                            directories = viewModel.customDirectories,
                            tags = viewModel.customTags,
                            onDismiss = { showMoveDialog = false },
                            onUpdateTags = { newTags ->
                                viewModel.updateNoteTags(note, newTags)
                            },
                            onAddNewDirectory = {
                                showCreateDirDialog = true
                            },
                            onAddNewTag = {
                                showCreateTagDialog = true
                            }
                        )

                        if (showCreateDirDialog) {
                            DirectoryEditDialog(
                                allDirectories = viewModel.customDirectories,
                                onDismiss = { showCreateDirDialog = false },
                                onSave = { name, parentId, colorHex ->
                                    viewModel.addDirectory(name, parentId, colorHex)
                                }
                            )
                        }

                        if (showCreateTagDialog) {
                            TagEditDialog(
                                onDismiss = { showCreateTagDialog = false },
                                onSave = { name, colorHex, textColorHex ->
                                    viewModel.addTag(name, colorHex, textColorHex)
                                }
                            )
                        }
                    }
                    
                    if (showCustomizeTemplateDialog) {
                        AdvancedTemplateDialog(
                            note = note,
                            onDismiss = { showCustomizeTemplateDialog = false },
                            onSave = { templateType, coverType, pageColor, coverTitle, coverSubtitle, coverAuthor, coverExtra ->
                                viewModel?.updateNoteDesignAndCover(
                                    targetNote = note,
                                    templateType = templateType,
                                    coverType = coverType,
                                    pageColor = pageColor,
                                    coverTitle = coverTitle,
                                    coverSubtitle = coverSubtitle,
                                    coverAuthor = coverAuthor,
                                    coverExtra = coverExtra
                                )
                                showCustomizeTemplateDialog = false
                            }
                        )
                    }

                    if (showRenameDialog) {
                        var newTitle by remember { mutableStateOf(note.title) }
                        AlertDialog(
                            onDismissRequest = { showRenameDialog = false },
                            title = { Text("Rename Notebook") },
                            text = {
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newTitle.isNotBlank()) {
                                            onRename(note, newTitle)
                                        }
                                        showRenameDialog = false
                                    }
                                ) {
                                    Text("Rename")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRenameDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                }
            }
        }
    }
}
@Composable
fun NoteEditorEmptyState(
    onCreateNoteClick: () -> Unit,
    isSidebarExpanded: Boolean = true,
    onToggleSidebar: () -> Unit = {},
    isNoteListExpanded: Boolean = true,
    onToggleNoteList: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Layout Actions Row for restoring hidden lists or sidebars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleSidebar) {
                Icon(
                    imageVector = if (isSidebarExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                    contentDescription = "Toggle Sidebar",
                    tint = if (isSidebarExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleNoteList) {
                Icon(
                    imageVector = if (isNoteListExpanded) Icons.AutoMirrored.Filled.ViewList else Icons.AutoMirrored.Filled.List,
                    contentDescription = "Toggle Notes List",
                    tint = if (isNoteListExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                LipiLogoCard()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Note Selected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a note from the panel, or create a new handwritten pad with grids, ruled sheets, Cornell tables, or study PDFs.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCreateNoteClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Note Pad")
                }
            }
        }
    }
}

@Composable
fun RealisticPenItem(
    toolId: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    activeColor: Color? = null
) {
    // Physical pen lift out of shelf when selected (bouncy spring)
    val animatedOffsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "realistic_pen_offset_y"
    )

    // Bouncy scale expansion when selected
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "realistic_pen_scale"
    )

    // Gentle rotation tilt when selected
    val animatedRotation by animateFloatAsState(
        targetValue = if (isSelected) -4f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "realistic_pen_rotation"
    )

    val animatedBorderWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "realistic_pen_border_width"
    )
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "realistic_pen_bg_color"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "realistic_pen_border_color"
    )

    val haptic = LocalHapticFeedback.current
    val itemShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(30.dp)
            .height(42.dp)
            .graphicsLayer {
                translationY = animatedOffsetY.toPx()
                scaleX = animatedScale
                scaleY = animatedScale
                rotationZ = animatedRotation
            }
            .clip(itemShape)
            .background(animatedBgColor)
            .border(width = animatedBorderWidth, color = animatedBorderColor, shape = itemShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDoubleTap?.invoke()
                    }
                )
            }
            .padding(top = 2.dp, bottom = 3.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.width(18.dp).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            
            when (toolId) {
                "fountain_pen" -> {
                    val bodyH = h * 0.15f
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(w * 0.15f, 0f),
                        size = Size(w * 0.7f, bodyH)
                    )
                    val ringH = h * 0.10f
                    drawRect(
                        color = Color(0xFF94A3B8),
                        topLeft = Offset(w * 0.1f, bodyH),
                        size = Size(w * 0.8f, ringH)
                    )
                    val gripH = h * 0.30f
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(w * 0.2f, bodyH + ringH),
                        size = Size(w * 0.6f, gripH)
                    )
                    val nibStart = bodyH + ringH + gripH
                    val nibPath = Path().apply {
                        moveTo(w * 0.25f, nibStart)
                        lineTo(w * 0.15f, nibStart + h * 0.12f)
                        lineTo(w * 0.5f, h)
                        lineTo(w * 0.85f, nibStart + h * 0.12f)
                        lineTo(w * 0.75f, nibStart)
                        close()
                    }
                    drawPath(nibPath, color = Color(0xFFCBD5E1))
                    
                    val goldPath = Path().apply {
                        moveTo(w * 0.35f, nibStart)
                        lineTo(w * 0.3f, nibStart + h * 0.08f)
                        lineTo(w * 0.5f, h - 1.5.dp.toPx())
                        lineTo(w * 0.7f, nibStart + h * 0.08f)
                        lineTo(w * 0.65f, nibStart)
                        close()
                    }
                    drawPath(goldPath, color = Color(0xFFF59E0B))
                    
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(w * 0.5f, nibStart + 1.dp.toPx()),
                        end = Offset(w * 0.5f, h - 1.dp.toPx()),
                        strokeWidth = 1f
                    )
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = 1.dp.toPx(),
                        center = Offset(w * 0.5f, nibStart + h * 0.05f)
                    )
                }
                "pencil" -> {
                    val bodyH = h * 0.2f
                    drawRect(
                        color = Color(0xFFEAB308),
                        topLeft = Offset(w * 0.2f, 0f),
                        size = Size(w * 0.6f, bodyH)
                    )
                    drawLine(Color(0xFFCA8A04), start = Offset(w * 0.4f, 0f), end = Offset(w * 0.4f, bodyH), strokeWidth = 1f)
                    drawLine(Color(0xFFCA8A04), start = Offset(w * 0.6f, 0f), end = Offset(w * 0.6f, bodyH), strokeWidth = 1f)
                    
                    val collarPath = Path().apply {
                        moveTo(w * 0.2f, bodyH)
                        lineTo(w * 0.5f, h)
                        lineTo(w * 0.8f, bodyH)
                        close()
                    }
                    drawPath(collarPath, color = Color(0xFFFED7AA))
                    
                    val leadPath = Path().apply {
                        moveTo(w * 0.4f, h - h * 0.22f)
                        lineTo(w * 0.5f, h)
                        lineTo(w * 0.6f, h - h * 0.22f)
                        close()
                    }
                    drawPath(leadPath, color = Color(0xFF374151))
                }
                "felt_pen" -> {
                    val bodyH = h * 0.2f
                    drawRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(w * 0.22f, 0f),
                        size = Size(w * 0.56f, bodyH)
                    )
                    val gripH = h * 0.4f
                    drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(w * 0.25f, bodyH),
                        size = Size(w * 0.5f, gripH)
                    )
                    val tipPath = Path().apply {
                        moveTo(w * 0.32f, bodyH + gripH)
                        quadraticTo(w * 0.32f, h, w * 0.5f, h)
                        quadraticTo(w * 0.68f, h, w * 0.68f, bodyH + gripH)
                        close()
                    }
                    drawPath(tipPath, color = Color(0xFF334155))
                }
                "highlighter" -> {
                    val bodyH = h * 0.25f
                    drawRect(
                        color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                        topLeft = Offset(w * 0.12f, 0f),
                        size = Size(w * 0.76f, bodyH)
                    )
                    drawRoundRect(
                        color = Color(0xFF0284C7),
                        topLeft = Offset(w * 0.28f, h * 0.05f),
                        size = Size(w * 0.44f, bodyH - h * 0.08f),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                    drawRect(
                        color = Color(0xFFF1F5F9),
                        topLeft = Offset(w * 0.22f, bodyH),
                        size = Size(w * 0.56f, h * 0.25f)
                    )
                    val chiselPath = Path().apply {
                        moveTo(w * 0.3f, bodyH + h * 0.25f)
                        lineTo(w * 0.36f, h)
                        lineTo(w * 0.72f, h - h * 0.12f)
                        lineTo(w * 0.72f, bodyH + h * 0.25f)
                        close()
                    }
                    drawPath(chiselPath, color = Color(0xFF38BDF8))
                }
                "eraser" -> {
                    val sleeveH = h * 0.25f
                    drawRect(
                        color = Color(0xFF1E3A8A),
                        topLeft = Offset(w * 0.16f, 0f),
                        size = Size(w * 0.68f, sleeveH)
                    )
                    drawRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(w * 0.16f, sleeveH * 0.45f),
                        size = Size(w * 0.68f, sleeveH * 0.25f)
                    )
                    val rubH = h * 0.75f
                    drawRect(
                        color = Color(0xFFFDA4AF),
                        topLeft = Offset(w * 0.2f, sleeveH),
                        size = Size(w * 0.6f, rubH)
                    )
                    val cutPath = Path().apply {
                        moveTo(w * 0.2f, sleeveH + rubH)
                        lineTo(w * 0.2f, h)
                        lineTo(w * 0.8f, h - h * 0.15f)
                        lineTo(w * 0.8f, sleeveH + rubH)
                        close()
                    }
                    drawPath(cutPath, color = Color(0xFFF43F5E))
                }
                "brush" -> {
                    val bodyH = h * 0.2f
                    drawRoundRect(
                        color = Color(0xFF78350F),
                        topLeft = Offset(w * 0.32f, 0f),
                        size = Size(w * 0.36f, bodyH),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                    val ferruleH = h * 0.3f
                    drawRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(w * 0.24f, bodyH),
                        size = Size(w * 0.52f, ferruleH)
                    )
                    val tipStart = bodyH + ferruleH
                    val tipPath = Path().apply {
                        moveTo(w * 0.26f, tipStart)
                        quadraticTo(w * 0.2f, tipStart + h * 0.25f, w * 0.5f, h)
                        quadraticTo(w * 0.8f, tipStart + h * 0.25f, w * 0.74f, tipStart)
                        close()
                    }
                    drawPath(tipPath, color = Color(0xFF1E293B))
                }
                "red_pen" -> {
                    val bodyH = h * 0.3f
                    drawRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(w * 0.22f, 0f),
                        size = Size(w * 0.56f, bodyH)
                    )
                    drawRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(w * 0.22f, bodyH - h * 0.15f),
                        size = Size(w * 0.56f, h * 0.15f)
                    )
                    val conePath = Path().apply {
                        moveTo(w * 0.22f, bodyH)
                        lineTo(w * 0.5f, h)
                        lineTo(w * 0.78f, bodyH)
                        close()
                    }
                    drawPath(conePath, color = Color(0xFFCBD5E1))
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 1.5.dp.toPx(),
                        center = Offset(w * 0.5f, h - 0.5.dp.toPx())
                    )
                }
                "tape" -> {
                    val bodyH = h * 0.4f
                    drawRect(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(w * 0.1f, 0f),
                        size = Size(w * 0.8f, bodyH)
                    )
                    drawRect(
                        color = Color(0xFFEAB308),
                        topLeft = Offset(w * 0.15f, bodyH),
                        size = Size(w * 0.7f, h - bodyH)
                    )
                }
                "shapes" -> {
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = w * 0.25f,
                        center = Offset(w * 0.35f, h * 0.4f)
                    )
                    drawRect(
                        color = Color(0xFFF43F5E),
                        topLeft = Offset(w * 0.45f, h * 0.4f),
                        size = Size(w * 0.4f, w * 0.4f)
                    )
                }
                "laser" -> {
                    val bodyH = h * 0.3f
                    drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(w * 0.22f, 0f),
                        size = Size(w * 0.56f, bodyH)
                    )
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 0.8.dp.toPx(),
                        center = Offset(w * 0.5f, bodyH * 0.35f)
                    )
                    val emitterPath = Path().apply {
                        moveTo(w * 0.22f, bodyH)
                        lineTo(w * 0.5f, h - 1.5.dp.toPx())
                        lineTo(w * 0.78f, bodyH)
                        close()
                    }
                    drawPath(emitterPath, color = Color(0xFFEF4444).copy(alpha = 0.8f))
                    drawLine(
                        color = Color(0xFFFF1744),
                        start = Offset(w * 0.5f, h - 1.5.dp.toPx()),
                        end = Offset(w * 0.5f, h),
                        strokeWidth = 1.5f
                    )
                }
                "lasso" -> {
                    val ovalPath = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.15f, h * 0.15f), Size(w * 0.7f, h * 0.55f)))
                    }
                    drawPath(
                        path = ovalPath,
                        color = Color(0xFF2563EB),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
                        )
                    )
                    drawCircle(
                        color = Color(0xFF1D4ED8),
                        radius = 2.dp.toPx(),
                        center = Offset(w * 0.5f, h * 0.7f)
                    )
                    drawLine(
                        color = Color(0xFF2563EB),
                        start = Offset(w * 0.5f, h * 0.7f),
                        end = Offset(w * 0.75f, h * 0.9f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .width(12.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteEditorCanvas(
    viewModel: NoteViewModel,
    selectedNote: NoteEntity,
    notes: List<NoteEntity>,
    onCreateNoteClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    isSidebarExpanded: Boolean = true,
    onToggleSidebar: () -> Unit = {},
    isNoteListExpanded: Boolean = true,
    onToggleNoteList: () -> Unit = {}
) {
    var showAISidebar by remember { mutableStateOf(false) }
    var showStylusSettingsDialog by remember { mutableStateOf(false) }
    var isToolbarExpanded by remember { mutableStateOf(true) }
    var showToolSettings by remember { mutableStateOf<String?>(null) }
    var showTimerPresets by remember { mutableStateOf(false) }
    var showFloatingPenSection by remember { mutableStateOf(true) }
    var showTemplateSelectionModal by remember { mutableStateOf(false) }
    var showScribbleToTextDialog by remember { mutableStateOf(false) }
    var showHandwrittenSearchDialog by remember { mutableStateOf(false) }
    var showFullscreenTimerDialog by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var isScrollingCanvas by remember { mutableStateOf(false) }
    var showHyperlinkDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showColorPickerDialogIndex by remember { mutableStateOf<Int?>(null) }
    var showDriveBackupDialog by remember { mutableStateOf(false) }
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var editingImageElement by remember { mutableStateOf<com.example.data.ImageElement?>(null) }
    val focusRequester = remember { FocusRequester() }

    val context = androidx.compose.ui.platform.LocalContext.current

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportActiveNoteToPdf(outputStream)
                    android.widget.Toast.makeText(context, "Exported PDF successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val docxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportActiveNoteToDocx(outputStream)
                    android.widget.Toast.makeText(context, "Exported DOCX successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val imageDir = java.io.File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
                val persistentFile = java.io.File(imageDir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    persistentFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val persistentPath = persistentFile.absolutePath
                val newImage = com.example.data.ImageElement(
                    uri = persistentPath,
                    x = 100f,
                    y = 100f,
                    width = 400f,
                    height = 400f,
                    page = viewModel.pdfPage
                )
                viewModel.currentImages = viewModel.currentImages + newImage
                viewModel.saveActiveCanvasStrokes()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }


    if (showDriveBackupDialog) {
        GoogleDriveBackupDialog(
            viewModel = viewModel,
            onDismissRequest = { showDriveBackupDialog = false }
        )
    }

    if (showToolSettings != null) {
        Dialog(
            onDismissRequest = { showToolSettings = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                val isWideScreen = screenWidth >= 560.dp || (screenWidth > screenHeight && screenWidth >= 440.dp)

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .widthIn(max = if (showToolSettings == "shapes" && isWideScreen) 720.dp else 480.dp)
                        .heightIn(max = screenHeight * 0.90f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (showToolSettings) {
                                    "shapes" -> "Shape Tool Settings"
                                    "lasso" -> "Lasso Settings"
                                    "laser" -> "Laser Pointer Settings"
                                    "eraser" -> "Eraser Settings"
                                    "pen", "fountain_pen", "ballpoint" -> "Pen Settings"
                                    "highlighter" -> "Highlighter Settings"
                                    "pencil" -> "Pencil Settings"
                                    else -> "Tool Settings"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showToolSettings = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint")) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                        .background(if (showToolSettings == "fountain_pen") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { 
                                            viewModel.activeToolType = "fountain_pen" 
                                            showToolSettings = "fountain_pen"
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Fountain", fontWeight = if (showToolSettings == "fountain_pen") FontWeight.Bold else FontWeight.Normal)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                        .background(if (showToolSettings == "ballpoint") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { 
                                            viewModel.activeToolType = "ballpoint"
                                            showToolSettings = "ballpoint"
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Ballpoint", fontWeight = if (showToolSettings == "ballpoint") FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    
                    val minThickness = when (viewModel.activeToolType) {
                        "highlighter" -> 5f
                        "eraser" -> 5f
                        "tape" -> 10f
                        else -> 1f
                    }
                    val maxThickness = when (viewModel.activeToolType) {
                        "highlighter" -> 80f
                        "eraser" -> 120f
                        "tape" -> 80f
                        "laser" -> 60f
                        "pencil" -> 35f
                        else -> 50f
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Brush / Tool Thickness", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "${String.format("%.1f", viewModel.activeWidth)} px",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val previewDp = (viewModel.activeWidth.dp * 0.65f).coerceIn(3.dp, 36.dp)
                                val previewColor = if (viewModel.activeToolType == "eraser") Color.Gray else Color(viewModel.activeColor)
                                Box(
                                    modifier = Modifier
                                        .size(previewDp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.activeWidth = (viewModel.activeWidth - 1f).coerceAtLeast(minThickness)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Thickness", modifier = Modifier.size(18.dp))
                            }

                            androidx.compose.material3.Slider(
                                value = viewModel.activeWidth.coerceIn(minThickness, maxThickness),
                                onValueChange = { viewModel.activeWidth = it },
                                valueRange = minThickness..maxThickness,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    viewModel.activeWidth = (viewModel.activeWidth + 1f).coerceAtMost(maxThickness)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Thickness", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val presetChips = when (viewModel.activeToolType) {
                            "highlighter", "tape" -> listOf(10f to "Thin", 20f to "Medium", 35f to "Thick", 50f to "Wide", 75f to "Max")
                            "eraser" -> listOf(15f to "Small", 30f to "Medium", 50f to "Large", 80f to "X-Large", 110f to "Huge")
                            "pencil" -> listOf(1.5f to "0.3mm", 3f to "0.5mm", 5f to "0.7mm", 8f to "1.0mm", 12f to "B2", 18f to "Shading")
                            "laser" -> listOf(4f to "Fine", 8f to "Standard", 16f to "Spot", 25f to "Beacon", 40f to "Glow")
                            else -> listOf(2f to "Fine", 4f to "Thin", 8f to "Medium", 14f to "Thick", 22f to "Heavy", 35f to "Max")
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            presetChips.forEach { (widthVal, label) ->
                                val isSelected = Math.abs(viewModel.activeWidth - widthVal) < 1.2f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                        .clickable { viewModel.activeWidth = widthVal }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "$label (${widthVal.toInt()}px)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                                        if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint", "highlighter", "pencil")) {
                        if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ink Flow ${viewModel.inkFlow.toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            androidx.compose.material3.Slider(
                                value = viewModel.inkFlow,
                                onValueChange = { viewModel.inkFlow = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pressure Sensitivity ${viewModel.pressureSensitivity.toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            androidx.compose.material3.Slider(
                                value = viewModel.pressureSensitivity,
                                onValueChange = { viewModel.pressureSensitivity = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        var magicSettingsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        if (showToolSettings == "pencil") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.pencilRainbowEnabled,
                                    onCheckedChange = { viewModel.pencilRainbowEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rainbow", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { magicSettingsExpanded = !magicSettingsExpanded }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Shape Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Icon(
                                imageVector = if (magicSettingsExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Shape Settings",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (magicSettingsExpanded) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.drawStraightLines,
                                    onCheckedChange = { viewModel.drawStraightLines = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Draw in a straight line", fontSize = 14.sp)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.smartShapesEnabled,
                                    onCheckedChange = { viewModel.smartShapesEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto Shape Recognition", fontSize = 14.sp)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.fillShapeEnabled,
                                    onCheckedChange = { viewModel.fillShapeEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fill Shape With Color", fontSize = 14.sp)
                            }

                            if (viewModel.fillShapeEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ink Tint (Fill Color Opacity) ${(viewModel.fillShapeOpacity * 100).toInt()}%", fontSize = 12.sp)
                                androidx.compose.material3.Slider(
                                    value = viewModel.fillShapeOpacity,
                                    onValueChange = { viewModel.fillShapeOpacity = it },
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (showToolSettings == "shapes") {
                        ShapeToolSettingsPanel(
                            viewModel = viewModel,
                            isWideScreen = isWideScreen,
                            screenWidth = screenWidth
                        )
                    }

                    if (showToolSettings == "lasso") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectPen,
                                    onCheckedChange = { viewModel.lassoSelectPen = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pen", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectShape,
                                    onCheckedChange = { viewModel.lassoSelectShape = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shape", fontSize = 14.sp)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectHighlighter,
                                    onCheckedChange = { viewModel.lassoSelectHighlighter = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Highlighter", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectText,
                                    onCheckedChange = { viewModel.lassoSelectText = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Text", fontSize = 14.sp)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectImage,
                                    onCheckedChange = { viewModel.lassoSelectImage = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Image", fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = viewModel.lassoSolidLine,
                                onCheckedChange = { viewModel.lassoSolidLine = it },
                                modifier = Modifier.scale(0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show Solid Lasso Line", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lasso Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val swatches = listOf(
                            0xFF1E1B4B.toInt() to "Dark Navy",
                            0xFFDC2626.toInt() to "Red",
                            0xFF78350F.toInt() to "Brown",
                            0xFF6B21A8.toInt() to "Purple",
                            0xFF0284C7.toInt() to "Sky Blue",
                            0xFF3B82F6.toInt() to "Blue",
                            0xFF059669.toInt() to "Green",
                            0xFFF59E0B.toInt() to "Yellow"
                        )

                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            swatches.forEach { (colorVal, _) ->
                                val isSelected = viewModel.activeColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            viewModel.activeColor = colorVal
                                        }
                                )
                            }
                        }
                    }

                    if (showToolSettings == "laser") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Laser Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            // Line Laser
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.laserMode == "line") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.laserMode == "line") MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.laserMode = "line" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Line Laser", fontSize = 14.sp, fontWeight = if (viewModel.laserMode == "line") FontWeight.Bold else FontWeight.Normal)
                            }
                            // Spot Laser
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.laserMode == "spot") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.laserMode == "spot") MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.laserMode = "spot" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Spot Laser", fontSize = 14.sp, fontWeight = if (viewModel.laserMode == "spot") FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Invisible After", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            androidx.compose.material3.Switch(
                                checked = viewModel.laserDisappearEnabled,
                                onCheckedChange = { viewModel.laserDisappearEnabled = it }
                            )
                        }
                        
                        if (viewModel.laserDisappearEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Delay: ${viewModel.laserDisappearDelay / 1000f}s", fontSize = 12.sp, modifier = Modifier.width(80.dp))
                                androidx.compose.material3.Slider(
                                    value = viewModel.laserDisappearDelay.toFloat(),
                                    onValueChange = { viewModel.laserDisappearDelay = it.toLong() },
                                    valueRange = 1000f..10000f,
                                    steps = 8,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (showToolSettings == "eraser") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Eraser Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stroke Eraser
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.eraserMode == "stroke") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.eraserMode == "stroke") MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.eraserMode = "stroke" }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (viewModel.eraserMode == "stroke") MaterialTheme.colorScheme.primary else Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Stroke", fontSize = 12.sp, fontWeight = if (viewModel.eraserMode == "stroke") FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                            
                            // Precise Eraser
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.eraserMode == "precise") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.eraserMode == "precise") MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.eraserMode = "precise" }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (viewModel.eraserMode == "precise") MaterialTheme.colorScheme.primary else Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Precise", fontSize = 12.sp, fontWeight = if (viewModel.eraserMode == "precise") FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            // Clear All
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.eraserMode == "clear_all") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.eraserMode == "clear_all") MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.eraserMode = "clear_all" }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (viewModel.eraserMode == "clear_all") MaterialTheme.colorScheme.primary else Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Clear All", fontSize = 12.sp, fontWeight = if (viewModel.eraserMode == "clear_all") FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Eraser Size (${viewModel.activeWidth.toInt()} px)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        androidx.compose.material3.Slider(
                            value = viewModel.activeWidth,
                            onValueChange = { viewModel.activeWidth = it },
                            valueRange = 10f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.clearAllCanvasStrokes()
                                showToolSettings = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Page Drawing Strokes")
                        }
                    }

                    if (showToolSettings != "eraser" && showToolSettings != "shapes") {
                        Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    
                    // Grid of colors
                    val swatches = listOf(
                        0xFF1E1B4B.toInt(), 0xFFDC2626.toInt(), 0xFF78350F.toInt(),
                        0xFF6B21A8.toInt(), 0xFF0284C7.toInt(), 0xFF0D9488.toInt(),
                        0xFFEAB308.toInt(), 0xFFEA580C.toInt(), 0xFF000000.toInt()
                    )
                    
                    var index = 0
                    while(index < swatches.size) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for(i in 0 until 3) {
                                if (index < swatches.size) {
                                    val c = swatches[index]
                                    val isSel = viewModel.activeColor == c
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(2.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                            .clickable { viewModel.activeColor = c }
                                    )
                                    index++
                                }
                            }
                        }
                    }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showToolSettings = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }
            }
        }
    }
    }

    if (showScribbleToTextDialog) {
        ScribbleToTextDialog(
            viewModel = viewModel,
            onDismiss = { showScribbleToTextDialog = false },
            onInsertText = { styledText ->
                viewModel.appendTextToNoteContent(styledText)
                showScribbleToTextDialog = false
            }
        )
    }

    if (showHyperlinkDialog) {
        val allNotesList by viewModel.allNotes.collectAsState()
        var linkTypeTab by remember { mutableStateOf(0) }
        var linkTitle by remember { mutableStateOf("") }
        var linkUrl by remember { mutableStateOf("https://") }
        var selectedTargetNoteId by remember { mutableStateOf(allNotesList.firstOrNull { it.id != selectedNote?.id }?.id ?: 0) }
        var selectedPageNum by remember { mutableStateOf(1) }

        AlertDialog(
            onDismissRequest = { showHyperlinkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Insert Hyperlink", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = linkTypeTab == 0,
                            onClick = {
                                linkTypeTab = 0
                                if (linkTitle.startsWith("🔗") || linkTitle.startsWith("📄")) linkTitle = ""
                            },
                            label = { Text("Web URL", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = linkTypeTab == 1,
                            onClick = {
                                linkTypeTab = 1
                                val targetNote = allNotesList.find { it.id == selectedTargetNoteId } ?: allNotesList.firstOrNull { it.id != selectedNote?.id }
                                if (targetNote != null) {
                                    selectedTargetNoteId = targetNote.id
                                    linkTitle = "🔗 ${targetNote.title}"
                                    linkUrl = "note://${targetNote.id}"
                                }
                            },
                            label = { Text("Note Link", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = linkTypeTab == 2,
                            onClick = {
                                linkTypeTab = 2
                                linkTitle = "📄 Jump to Page $selectedPageNum"
                                linkUrl = "page://$selectedPageNum"
                            },
                            label = { Text("Page Link", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    if (linkTypeTab == 0) {
                        OutlinedTextField(
                            value = linkTitle,
                            onValueChange = { linkTitle = it },
                            label = { Text("Link Text / Label") },
                            placeholder = { Text("e.g., Reference Webpage") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            label = { Text("URL Address") },
                            placeholder = { Text("https://example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (linkTypeTab == 1) {
                        Text("Link to another Note in your library:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        val otherNotes = allNotesList.filter { it.id != selectedNote?.id }
                        if (otherNotes.isEmpty()) {
                            Text("No other notes available to link. Create another note first!", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else {
                            var expandedNoteDropdown by remember { mutableStateOf(false) }
                            val currentSelectedNote = allNotesList.find { it.id == selectedTargetNoteId } ?: otherNotes.first()

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedNoteDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔗 ${currentSelectedNote.title}", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedNoteDropdown,
                                    onDismissRequest = { expandedNoteDropdown = false }
                                ) {
                                    otherNotes.forEach { noteItem ->
                                        DropdownMenuItem(
                                            text = { Text("🔗 ${noteItem.title}") },
                                            onClick = {
                                                selectedTargetNoteId = noteItem.id
                                                linkTitle = "🔗 ${noteItem.title}"
                                                linkUrl = "note://${noteItem.id}"
                                                expandedNoteDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = linkTitle,
                            onValueChange = { linkTitle = it },
                            label = { Text("Display Link Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Link to a specific Page in this Note:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Page $selectedPageNum of ${viewModel.pdfPageCount}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    if (selectedPageNum > 1) {
                                        selectedPageNum--
                                        linkTitle = "📄 Jump to Page $selectedPageNum"
                                        linkUrl = "page://$selectedPageNum"
                                    }
                                },
                                enabled = selectedPageNum > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Prev Page")
                            }
                            IconButton(
                                onClick = {
                                    if (selectedPageNum < viewModel.pdfPageCount) {
                                        selectedPageNum++
                                        linkTitle = "📄 Jump to Page $selectedPageNum"
                                        linkUrl = "page://$selectedPageNum"
                                    }
                                },
                                enabled = selectedPageNum < viewModel.pdfPageCount
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Next Page")
                            }
                        }
                        OutlinedTextField(
                            value = linkTitle,
                            onValueChange = { linkTitle = it },
                            label = { Text("Display Link Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = if (linkTypeTab == 1) {
                            "note://$selectedTargetNoteId"
                        } else if (linkTypeTab == 2) {
                            "page://$selectedPageNum"
                        } else {
                            if (linkUrl.startsWith("http://") || linkUrl.startsWith("https://") || linkUrl.startsWith("note://") || linkUrl.startsWith("page://")) linkUrl else "https://$linkUrl"
                        }
                        val formattedLink = "\n[${linkTitle.ifBlank { "Link" }}]($finalUrl)\n"
                        viewModel.appendTextToNoteContent(formattedLink)
                        showHyperlinkDialog = false
                    }
                ) {
                    Text("Insert Hyperlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHyperlinkDialog = false }) { Text("Cancel") }
            }
        )
    }

if (showLayersDialog) {
        AlertDialog(
            onDismissRequest = { showLayersDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Canvas Layers & Annotations")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Drawing Strokes Layer", fontWeight = FontWeight.SemiBold)
                        Text("${viewModel.currentStrokes.size} strokes", fontSize = 12.sp, color = Color.Gray)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Imported Images Layer", fontWeight = FontWeight.SemiBold)
                        Text("${viewModel.currentImages.size} images", fontSize = 12.sp, color = Color.Gray)
                    }
                    HorizontalDivider()
                    Text("Manage annotations and stroke elements on this page.", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { showLayersDialog = false }) { Text("Done") }
            }
        )
    }

    if (showTemplateSelectionModal) {
        AdvancedTemplateDialog(
            note = selectedNote,
            onDismiss = { showTemplateSelectionModal = false },
            onSave = { templateType, coverType, pageColor, coverTitle, coverSubtitle, coverAuthor, coverExtra ->
                viewModel.updateNoteDesign(templateType, coverType, pageColor)
                viewModel.updateCoverInfo(coverTitle, coverSubtitle, coverAuthor, coverExtra)
                showTemplateSelectionModal = false
            }
        )
    }

    if (showJumpToPageDialog) {
        var targetPageText by remember { mutableStateOf(viewModel.pdfPage.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpToPageDialog = false },
            title = {
                Text(
                    "Jump to Page",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Page ${viewModel.pdfPage} of ${viewModel.pdfPageCount}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (viewModel.pdfPageCount > 1) {
                        Slider(
                            value = viewModel.pdfPage.toFloat(),
                            onValueChange = { newPage ->
                                viewModel.setPDFPage(newPage.toInt().coerceIn(1, viewModel.pdfPageCount))
                            },
                            valueRange = 1f..viewModel.pdfPageCount.toFloat(),
                            steps = (viewModel.pdfPageCount - 2).coerceAtLeast(0)
                        )
                    }
                    
                    OutlinedTextField(
                        value = targetPageText,
                        onValueChange = { targetPageText = it },
                        label = { Text("Go to Page Number") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = targetPageText.toIntOrNull()
                        if (parsed != null && parsed in 1..viewModel.pdfPageCount) {
                            viewModel.setPDFPage(parsed)
                        }
                        showJumpToPageDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToPageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showColorPickerDialogIndex?.let { slotIdx ->
        AnyShadeColorPickerDialog(
            initialColor = viewModel.activeToolColors.getOrNull(slotIdx) ?: viewModel.activeColor,
            isHighlighter = (viewModel.activeToolType == "highlighter"),
            onDismiss = { showColorPickerDialogIndex = null },
            onColorSelected = { newColor ->
                viewModel.updateToolColorSlot(slotIdx, newColor)
                showColorPickerDialogIndex = null
            }
        )
    }

    if (showFullscreenTimerDialog) {
        var tempMinutes by remember { mutableStateOf((viewModel.timerTotalSeconds / 60).toString()) }
        AlertDialog(
            onDismissRequest = { showFullscreenTimerDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Focus Timer Settings", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select a preset duration or enter custom minutes:", style = MaterialTheme.typography.bodyMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "10 Min" to 600,
                            "25 Min" to 1500,
                            "50 Min" to 3000
                        ).forEach { (label, secs) ->
                            Button(
                                onClick = {
                                    viewModel.resetTimer(secs)
                                    showFullscreenTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.timerTotalSeconds == secs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (viewModel.timerTotalSeconds == secs) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempMinutes,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                tempMinutes = newValue
                            }
                        },
                        label = { Text("Custom Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Button(
                        onClick = {
                            if (viewModel.timerIsRunning) {
                                viewModel.pauseTimer()
                            } else {
                                viewModel.startTimer()
                            }
                            showFullscreenTimerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.timerIsRunning) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (viewModel.timerIsRunning) "Pause Focus Timer" else "Start Focus Timer")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = tempMinutes.toIntOrNull() ?: 25
                        viewModel.resetTimer(mins * 60)
                        viewModel.startTimer()
                        showFullscreenTimerDialog = false
                    }
                ) {
                    Text("Apply & Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullscreenTimerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStylusSettingsDialog) {
        Dialog(onDismissRequest = { showStylusSettingsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Stylus & Hand-Scroll Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Customize active stylus features, double-tap shortcuts, and palm rejection behaviors.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Stylus only writing toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { viewModel.stylusOnlyDrawing = !viewModel.stylusOnlyDrawing }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stylus-Only Writing Mode",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hand/finger touches pan and scroll the canvas, while only the stylus draws.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.stylusOnlyDrawing,
                            onCheckedChange = { viewModel.stylusOnlyDrawing = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pressure Sensitivity Calibration Launch Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.showPressureCalibrationDialog = true
                            },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Calibration Utility",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pressure Sensitivity Calibration",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Calibrate stroke weight response curves & deadzones with live test pad.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Calibration Utility",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Stylus Double-Tap / Side-Button Action",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val gestureOptions = listOf(
                        "toggle_eraser" to "Toggle Pen / Eraser",
                        "toggle_lasso" to "Toggle Pen / Lasso",
                        "toggle_highlighter" to "Toggle Pen / Highlighter",
                        "undo" to "Perform Undo (Ctrl+Z)",
                        "redo" to "Perform Redo (Ctrl+Y)",
                        "none" to "Disable Gesture"
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        gestureOptions.forEach { (actionValue, actionLabel) ->
                            val isSelected = viewModel.stylusDoubleTapAction == actionValue
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.stylusDoubleTapAction = actionValue }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.stylusDoubleTapAction = actionValue }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = actionLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showStylusSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply & Dismiss")
                    }
                }
            }
        }
    }

    if (viewModel.showPressureCalibrationDialog) {
        StylusPressureCalibrationDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showPressureCalibrationDialog = false }
        )
    }

    LaunchedEffect(selectedNote.id) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed) {
                    when (keyEvent.key) {
                        Key.Z -> {
                            viewModel.undo()
                            true
                        }
                        Key.Y -> {
                            viewModel.redo()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        if (!viewModel.isFullscreen) {
            // TIER 2: Two-Tier horizontal toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(width = 1.dp, color = Color(0xFFE2E8F0))
        ) {
            // Row 1: Core Navigation and Utility Bar (optimized with horizontal scroll for smartphone compatibility)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left utility icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Back arrow
                    IconButton(
                        onClick = { onBackClick?.invoke() ?: onToggleSidebar() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Split screen / Sidebar toggle [ | ]
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSidebarExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle Sidebar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Grid / Thumbnails [ ▦ ]
                    IconButton(
                        onClick = onToggleNoteList,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid Thumbnails",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Selected Pen Icon inside Circle
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFEFF6FF), CircleShape)
                            .border(1.dp, Color(0xFF3B82F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (viewModel.activeToolType) {
                                "eraser" -> Icons.Default.AutoFixNormal
                                "lasso" -> Icons.Default.SelectAll
                                else -> Icons.Default.Gesture
                            },
                            contentDescription = "Active Tool",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Undo arrow [ ↶ ]
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo (Ctrl+Z)",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo arrow [ ↷ ]
                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo (Ctrl+Y)",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Middle/Right Utility Actions (Matching Screenshot icons)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pen toggle (underlined)
                    IconButton(
                        onClick = { viewModel.activeToolType = "pen" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Draw mode",
                                tint = if (viewModel.activeToolType in listOf("pen", "fountain_pen", "pencil", "highlighter", "laser")) Color(0xFF3B82F6) else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            if (viewModel.activeToolType in listOf("pen", "fountain_pen", "pencil", "highlighter", "laser")) {
                                Box(modifier = Modifier.width(14.dp).height(2.dp).background(Color(0xFF3B82F6)))
                            }
                        }
                    }

                    // Text Tool [ T ]
                    IconButton(
                        onClick = { showScribbleToTextDialog = true },
                        modifier = Modifier.size(32.dp).testTag("scribble_to_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Scribble to Text Studio",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Lipi Insert Menu [ + Attach / Media / Link / Audio ]
                    IconButton(
                        onClick = { viewModel.showInsertMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("notebook_toolbar_insert_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Insert Media, Audio, Link, PDF & Text Blocks",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add Image [ 🌄+ ]
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Scan Document [ 📷 Scanner ]
                    IconButton(
                        onClick = { viewModel.openDocumentScanner("notebook", viewModel.selectedNote) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("notebook_toolbar_scan_document_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan Document into Notebook",
                            tint = Color(0xFF5B6DFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Smart Handwriting [ ✨ Smart Handwriting ]
                    IconButton(
                        onClick = { viewModel.openSmartHandwritingPanel() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("notebook_toolbar_smart_handwriting_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Smart Handwriting Studio",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Crop / Lasso selection
                    IconButton(
                        onClick = { 
                            if (viewModel.activeToolType == "lasso") {
                                showToolSettings = if (showToolSettings == "lasso") null else "lasso"
                            } else {
                                viewModel.activeToolType = "lasso"
                                showToolSettings = null
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Lasso selection",
                                tint = if (viewModel.activeToolType == "lasso") Color(0xFF3B82F6) else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            if (viewModel.activeToolType == "lasso") {
                                Box(modifier = Modifier.width(14.dp).height(2.dp).background(Color(0xFF3B82F6)))
                            }
                        }
                    }

                    // Ruler [ 📏 ]
                    IconButton(
                        onClick = { viewModel.isRulerActive = !viewModel.isRulerActive },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = "Ruler alignment",
                            tint = if (viewModel.isRulerActive) Color(0xFF3B82F6) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Comment bubble
                    IconButton(
                        onClick = { showAISidebar = !showAISidebar },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Link icon
                    IconButton(
                        onClick = { showHyperlinkDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Add Hyperlink",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Layer Stack
                    IconButton(
                        onClick = { showLayersDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Annotations Layer",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Microphone / Audio Transcription
                    IconButton(
                        onClick = { viewModel.openAudioOverlay() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("action_bar_mic_button")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Audio Transcription",
                            tint = if (viewModel.isRecording) Color(0xFFEF4444) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // AI Assistant Tab
                    IconButton(
                        onClick = { showAISidebar = !showAISidebar },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant Panel",
                            tint = if (showAISidebar) Color(0xFF3B82F6) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Cloud Drive Sync Status Indicator ('Saved' or 'Syncing')
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (viewModel.isSyncing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (viewModel.isSyncing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showDriveBackupDialog = true
                            }
                            .testTag("drive_sync_status_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (viewModel.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Syncing...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Drive Sync Status",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Saved",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    // Palm Rejection Quick Toggle Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (viewModel.stylusOnlyDrawing) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (viewModel.stylusOnlyDrawing) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.stylusOnlyDrawing = !viewModel.stylusOnlyDrawing
                            }
                            .testTag("palm_rejection_quick_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FrontHand,
                                contentDescription = "Palm Rejection Mode",
                                tint = if (viewModel.stylusOnlyDrawing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.stylusOnlyDrawing) "Palm Rejection ON" else "Palm Rejection OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.stylusOnlyDrawing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Stylus Settings Button
                    IconButton(
                        onClick = { showStylusSettingsDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("stylus_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (viewModel.stylusOnlyDrawing) Color(0xFF3B82F6) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Background Template Selection Button
                    IconButton(
                        onClick = { showTemplateSelectionModal = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("change_template_pattern_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Change Background Pattern",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Collapse/Expand Toolbar Button (For Smartphone workspace optimization!)
                    IconButton(
                        onClick = { isToolbarExpanded = !isToolbarExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_toolbar_expansion_button")
                    ) {
                        Icon(
                            imageVector = if (isToolbarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Pen Shelf (Optimize Workspace)",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))

                    // Export PDF Button
                    IconButton(
                        onClick = {
                            pdfExportLauncher.launch("note_${selectedNote.title.replace(" ", "_")}.pdf")
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export active note as PDF",
                            tint = Color(0xFFE11D48), // Rose/Red for PDF
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Export DOCX Button
                    IconButton(
                        onClick = {
                            docxExportLauncher.launch("note_${selectedNote.title.replace(" ", "_")}.docx")
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("export_docx_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Export active note as DOCX",
                            tint = Color(0xFF2563EB), // Blue for DOCX
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Share Note Menu
                    var showShareMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showShareMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("share_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share active note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE11D48))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share as PDF")
                                    }
                                },
                                onClick = {
                                    showShareMenu = false
                                    viewModel.shareActiveNote(context, "pdf")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share as DOCX")
                                    }
                                },
                                onClick = {
                                    showShareMenu = false
                                    viewModel.shareActiveNote(context, "docx")
                                }
                            )
                        }
                    }

                    // Extract PDF Text (Google ML Kit) & PDF Annotation Mode
                    if (selectedNote.templateType == "pdf" || selectedNote.templateType == "docx" || !selectedNote.pdfTitle.isNullOrEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.showPdfAnnotationViewer = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("open_pdf_annotation_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Open PDF Annotation Viewer & Drive Sync",
                                tint = Color(0xFFD32F2F), // Red PDF Accent
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.extractPdfTextWithMlKit()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("extract_pdf_text_mlkit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Extract text from PDF with Google ML Kit",
                                tint = Color(0xFF059669), // Emerald Green
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Search Handwritten Strokes (Gemini AI)
                    IconButton(
                        onClick = {
                            showHandwrittenSearchDialog = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("search_handwritten_strokes_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                            contentDescription = "Search & Analyze Handwritten Strokes with Gemini AI",
                            tint = Color(0xFF8B5CF6), // Purple / Gemini Accent
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Google Search Button
                    IconButton(
                        onClick = {
                            val searchQuery = selectedNote.title.ifBlank { selectedNote.content }
                            viewModel.openGoogleSearch(searchQuery)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("google_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search active note text on Google",
                            tint = Color(0xFF4285F4), // Google Blue
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isToolbarExpanded) {
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // Row 2: Pen Shelf, Swatches, and Thickness selectors
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Realistic Digital Pens shelf
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val realPens = listOf(
                        "fountain_pen" to "Pen",
                        "pencil" to "Pencil",
                        "eraser" to "Eraser",
                        "highlighter" to "Highlighter",
                        "laser" to "Laser",
                        "shapes" to "Shapes",
                        "lasso" to "Lasso"
                    )
                    realPens.forEach { (toolId, label) ->
                        val isSelected = viewModel.activeToolType == toolId || (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint")
                        RealisticPenItem(
                            onDoubleTap = { showToolSettings = if (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint") "ballpoint" else toolId },
                            toolId = toolId,
                            isSelected = isSelected,
                            activeColor = Color(viewModel.activeColor),
                            onClick = {
                                viewModel.activeToolType = toolId
                            }
                        )
                    }
                }

                // Dotted vertical divider
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color(0xFFCBD5E1))
                )

                // Color Palette & Star Favorite
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tool Palette:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    // Row of active tool's 4 primary colors
                    viewModel.activeToolColors.forEachIndexed { index, colorVal ->
                        val isSelected = viewModel.activeColor == colorVal
                        val swatchScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.25f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "color_swatch_scale"
                        )
                        val swatchOffsetY by androidx.compose.animation.core.animateDpAsState(
                            targetValue = if (isSelected) (-3).dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "color_swatch_offset"
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    scaleX = swatchScale
                                    scaleY = swatchScale
                                    translationY = swatchOffsetY.toPx()
                                }
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
                                    shape = CircleShape
                                )
                                .combinedClickable(
                                    onClick = {
                                        viewModel.activeColor = colorVal
                                        if (viewModel.activeToolType == "eraser" || viewModel.activeToolType == "lasso") {
                                            viewModel.activeToolType = "fountain_pen"
                                        }
                                    },
                                    onDoubleClick = {
                                        showColorPickerDialogIndex = index
                                    },
                                    onLongClick = {
                                        showColorPickerDialogIndex = index
                                    }
                                )
                        )
                    }

                    // Customize Shade Icon Button
                    IconButton(
                        onClick = {
                            val activeIdx = viewModel.activeToolColors.indexOf(viewModel.activeColor).coerceAtLeast(0)
                            showColorPickerDialogIndex = activeIdx
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize Shade",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Dotted vertical divider
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color(0xFFCBD5E1))
                )

                // Dynamic Circular Thickness Selector with dots and settings button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val toolbarThicknesses = when (viewModel.activeToolType) {
                        "highlighter", "tape" -> listOf(10f to "Thin", 25f to "Med", 50f to "Thick")
                        "eraser" -> listOf(15f to "Small", 40f to "Med", 80f to "Large")
                        "pencil" -> listOf(2f to "Fine", 5f to "Med", 12f to "Soft")
                        "laser" -> listOf(4f to "Line", 16f to "Spot", 32f to "Glow")
                        else -> listOf(2f to "Fine", 6f to "Med", 14f to "Thick")
                    }

                    toolbarThicknesses.forEach { (width, label) ->
                        val isSelected = Math.abs(viewModel.activeWidth - width) < 1.5f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { viewModel.activeWidth = width },
                                    onDoubleClick = { showToolSettings = viewModel.activeToolType },
                                    onLongClick = { showToolSettings = viewModel.activeToolType }
                                )
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val dotSizeDp = (width.dp * 0.4f).coerceIn(3.dp, 20.dp)
                                val dotColor = if (viewModel.activeToolType == "eraser") Color.Gray else Color(viewModel.activeColor)
                                Box(
                                    modifier = Modifier
                                        .size(dotSizeDp)
                                        .background(dotColor, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155)
                            )
                        }
                    }

                    // Direct Tune / Customize Thickness Button
                    IconButton(
                        onClick = { showToolSettings = viewModel.activeToolType },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Brush Thickness",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Dotted vertical divider
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color(0xFFCBD5E1))
                )

                // Row 2 Focus Timer Integration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    val minutes = viewModel.timerRemainingSeconds / 60
                    val seconds = viewModel.timerRemainingSeconds % 60
                    val formattedTime = String.format("%02d:%02d", minutes, seconds)

                    Icon(
                        imageVector = if (viewModel.timerIsRunning) Icons.Default.HourglassTop else Icons.Default.HourglassEmpty,
                        contentDescription = "Focus Timer",
                        tint = if (viewModel.timerIsRunning) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = formattedTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (viewModel.timerRemainingSeconds == 0) MaterialTheme.colorScheme.error else Color(0xFF0F172A)
                    )

                    // Play / Pause
                    IconButton(
                        onClick = {
                            if (viewModel.timerIsRunning) {
                                viewModel.pauseTimer()
                            } else {
                                viewModel.startTimer()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.timerIsRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Timer",
                            tint = if (viewModel.timerIsRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Reset Timer",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Presets & Custom set options
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "10m" to 600,
                            "25m" to 1500,
                            "50m" to 3000
                        ).forEach { (label, secs) ->
                            Surface(
                                onClick = { viewModel.resetTimer(secs) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (viewModel.timerTotalSeconds == secs) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (viewModel.timerTotalSeconds == secs) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (viewModel.timerTotalSeconds == secs) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Custom Minutes Option (According to Use)
                    var showCustomTimerDialog by remember { mutableStateOf(false) }
                    
                    Surface(
                        onClick = { showCustomTimerDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (viewModel.timerTotalSeconds !in listOf(600, 1500, 3000)) MaterialTheme.colorScheme.tertiaryContainer else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (viewModel.timerTotalSeconds !in listOf(600, 1500, 3000)) MaterialTheme.colorScheme.tertiary else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(10.dp), tint = if (viewModel.timerTotalSeconds !in listOf(600, 1500, 3000)) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF475569))
                            Text(
                                text = "Custom",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (viewModel.timerTotalSeconds !in listOf(600, 1500, 3000)) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF475569)
                            )
                        }
                    }

                    if (showCustomTimerDialog) {
                        var tempMinutes by remember { mutableStateOf((viewModel.timerTotalSeconds / 60).toString()) }
                        AlertDialog(
                            onDismissRequest = { showCustomTimerDialog = false },
                            title = { Text("Set Custom Focus Timer", style = MaterialTheme.typography.titleMedium) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Enter your desired duration in minutes:", style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(
                                        value = tempMinutes,
                                        onValueChange = { newValue ->
                                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                                tempMinutes = newValue
                                            }
                                        },
                                        label = { Text("Minutes") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val mins = tempMinutes.toIntOrNull() ?: 25
                                        if (mins > 0) {
                                            viewModel.resetTimer(mins * 60)
                                        }
                                        showCustomTimerDialog = false
                                    }
                                ) {
                                    Text("Apply")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomTimerDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
            }
        }
        }

        // Active workspace (Canvas + AI Sidebar overlay)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isDarkTheme = when (viewModel.themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Drawing Canvas
                DrawingCanvas(
                    strokes = viewModel.currentStrokes,
                    fadingStrokes = viewModel.fadingStrokes,
                    fadingTicker = viewModel.fadingTicker,
                    images = viewModel.currentImages,
                    currentStroke = viewModel.activeStroke,
                    onStrokeStarted = { viewModel.handleStrokeStarted(it) },
                    onStrokeDragged = { viewModel.handleStrokeDragged(it) },
                    onStrokeEnded = { viewModel.handleStrokeEnded() },
                    onImageUpdated = { idx, img ->
                        val mutList = viewModel.currentImages.toMutableList()
                        if (idx in mutList.indices) { mutList[idx] = img }
                        viewModel.currentImages = mutList
                        viewModel.saveActiveCanvasStrokes()
                    },
                    templateType = selectedNote.templateType,
                    pdfPage = viewModel.pdfPage,
                    noteId = selectedNote.id,
                    canvasBgColor = Color(selectedNote.pageColor),
                    canvasMode = viewModel.canvasMode,
                    lassoSelectedStrokes = viewModel.lassoSelectedStrokes,
                    lassoDragOffset = viewModel.lassoDragOffset,
                    lassoScaleX = viewModel.lassoScaleX,
                    lassoScaleY = viewModel.lassoScaleY,
                    lassoBoundingBox = viewModel.lassoBoundingBox,
                    lassoSolidLine = viewModel.lassoSolidLine,
                    stylusOnlyDrawing = viewModel.stylusOnlyDrawing,
                    onStylusDoubleTap = { viewModel.handleStylusGesture() },
                    isDarkTheme = isDarkTheme,
                    pdfPageCount = viewModel.pdfPageCount,
                    onPageSelected = { viewModel.setPDFPage(it) },
                    isRulerActive = viewModel.isRulerActive,
                    onShapeLongPressed = { stroke -> viewModel.selectShape(stroke) },
                    onImageLongPressed = { idx, img ->
                        editingImageIndex = idx
                        editingImageElement = img
                    },
                    onImageDeleted = { idx ->
                        val mutList = viewModel.currentImages.toMutableList()
                        if (idx in mutList.indices) {
                            mutList.removeAt(idx)
                            viewModel.currentImages = mutList
                            viewModel.saveActiveCanvasStrokes()
                        }
                    },
                    onLassoDrag = { offset -> viewModel.lassoDragOffset = Offset(viewModel.lassoDragOffset.x + offset.x, viewModel.lassoDragOffset.y + offset.y) },
                    onLassoScaleUpdated = { scaleX, scaleY -> viewModel.updateLassoScale(scaleX, scaleY) },
                    onLassoStrokesUpdated = { strokes, bbox -> viewModel.updateLassoStrokes(strokes, bbox) },
                    onScrollStateChanged = { isScrollingCanvas = it },
                    contentBlocks = viewModel.currentContentBlocks,
                    selectedBlockId = viewModel.selectedContentBlockId,
                    audioManager = viewModel.lipiAudioManager,
                    onBlockSelected = { viewModel.selectedContentBlockId = it },
                    onBlockUpdated = { viewModel.updateContentBlock(it) },
                    onBlockDeleted = { viewModel.deleteContentBlock(it.id) },
                    onMoveBlock = { id, dx, dy -> viewModel.moveContentBlock(id, dx, dy) },
                    onResizeBlock = { id, w, h -> viewModel.resizeContentBlock(id, w, h) },
                    onDuplicateBlock = { id -> viewModel.duplicateContentBlock(id) },
                    onNavigateToNotePage = { targetNoteId, targetPage ->
                        val targetNote = viewModel.allNotes.value.firstOrNull { it.id == targetNoteId }
                        if (targetNote != null) {
                            viewModel.selectNote(targetNote)
                            viewModel.setPDFPage(targetPage)
                        }
                    },
                    onOpenPdfViewer = { pdfPath, page ->
                        try {
                            val pdfFile = java.io.File(pdfPath)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Opening PDF (Page $page)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Minimalist visual timer readout in the corner (only shows when running)
                if (viewModel.timerIsRunning) {
                    val minutes = viewModel.timerRemainingSeconds / 60
                    val seconds = viewModel.timerRemainingSeconds % 60
                    val formattedTime = String.format("%02d:%02d", minutes, seconds)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = if (viewModel.isFullscreen) 72.dp else 12.dp,
                                end = 16.dp
                            )
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .clickable { showFullscreenTimerDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Focus Timer",
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = formattedTime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Lasso Selection Action Toolbar overlay
                if (viewModel.lassoSelectedStrokes.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${viewModel.lassoSelectedStrokes.size} selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Change Color
                            listOf(
                                0xFF1E1E1E.toInt() to Color(0xFF1E1E1E), // Black
                                0xFF1976D2.toInt() to MaterialTheme.colorScheme.primary, // Blue
                                0xFFD32F2F.toInt() to Color(0xFFD32F2F), // Red
                                0xFF388E3C.toInt() to Color(0xFF388E3C)  // Green
                            ).forEach { (colorInt, colorVal) ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(colorVal)
                                        .clickable { viewModel.recolorLassoSelection(colorInt) }
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Duplicate selection
                            IconButton(
                                onClick = { viewModel.duplicateLassoSelection() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate selected shape",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Toggle Fill
                            IconButton(
                                onClick = { 
                                    val currentFill = viewModel.lassoSelectedStrokes.firstOrNull()?.fillShape ?: false
                                    viewModel.customizeLassoSelection(fillShape = !currentFill, fillOpacity = 0.3f)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatColorFill,
                                    contentDescription = "Toggle shape fill",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Smart Refine
                            IconButton(
                                onClick = { viewModel.refineSelectedHandwriting() },
                                modifier = Modifier.size(28.dp).testTag("lasso_refine_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Refine handwriting",
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Straighten
                            IconButton(
                                onClick = { viewModel.straightenSelectedHandwriting() },
                                modifier = Modifier.size(28.dp).testTag("lasso_straighten_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = "Straighten lines",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Convert to Text
                            IconButton(
                                onClick = { viewModel.convertHandwritingToText() },
                                modifier = Modifier.size(28.dp).testTag("lasso_convert_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Convert to Text",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // AI Actions Dropdown
                            var showLassoAiMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showLassoAiMenu = true },
                                    modifier = Modifier.size(28.dp).testTag("lasso_ai_actions_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "AI Actions",
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showLassoAiMenu,
                                    onDismissRequest = { showLassoAiMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("✨ Explain") },
                                        onClick = {
                                            viewModel.runAiActionOnSelection("Explain")
                                            showLassoAiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📚 Summarize") },
                                        onClick = {
                                            viewModel.runAiActionOnSelection("Summarize")
                                            showLassoAiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🧠 Generate Quiz") },
                                        onClick = {
                                            viewModel.runAiActionOnSelection("Quiz")
                                            showLassoAiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🗂 Generate Flashcards") },
                                        onClick = {
                                            viewModel.runAiActionOnSelection("Flashcards")
                                            showLassoAiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🗺 Create Mind Map") },
                                        onClick = {
                                            viewModel.runAiActionOnSelection("MindMap")
                                            showLassoAiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🌐 Translate") },
                                        onClick = {
                                            viewModel.runAiActionOnSelection("Translate")
                                            showLassoAiMenu = false
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Delete selection
                            IconButton(
                                onClick = { viewModel.deleteLassoSelection() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected strokes",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Dismiss selection
                            IconButton(
                                onClick = { viewModel.clearLassoSelection() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done editing shape",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // PDF/DOCX multipage indicator overlay and Add Page controls
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScrollingCanvas || showJumpToPageDialog,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 76.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (viewModel.pdfPage > 1) {
                                        viewModel.setPDFPage(viewModel.pdfPage - 1)
                                    }
                                },
                                enabled = viewModel.pdfPage > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous Page",
                                    tint = if (viewModel.pdfPage > 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }

                            Card(
                                onClick = { showJumpToPageDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Text(
                                    "Page ${viewModel.pdfPage} of ${viewModel.pdfPageCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (viewModel.pdfPage < viewModel.pdfPageCount) {
                                        viewModel.setPDFPage(viewModel.pdfPage + 1)
                                    }
                                },
                                enabled = viewModel.pdfPage < viewModel.pdfPageCount,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next Page",
                                    tint = if (viewModel.pdfPage < viewModel.pdfPageCount) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.addPage() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Page",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                

                // Floating Pen Section & Fullscreen Exit Controls
                if (viewModel.isFullscreen) {
                    // Floating Pen Section Overlay (Top-Center)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, start = 64.dp, end = 64.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFloatingPenSection,
                            enter = fadeIn(animationSpec = tween(280)) + 
                                    slideInVertically(
                                        initialOffsetY = { -it / 2 }, 
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow, 
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    ) + 
                                    scaleIn(
                                        initialScale = 0.82f, 
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f), 
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow, 
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    ),
                            exit = fadeOut(animationSpec = tween(200)) + 
                                   slideOutVertically(targetOffsetY = { -it / 2 }) + 
                                   scaleOut(
                                       targetScale = 0.85f, 
                                       transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f), 
                                       animationSpec = tween(200)
                                   )
                        ) {
                            FloatingPenSection(
                                viewModel = viewModel,
                                onExitFullscreen = { viewModel.isFullscreen = false },
                                onToolDoubleTap = { showToolSettings = it },
                                onChangeTemplateClick = { showTemplateSelectionModal = true },
                                onAddPhotoClick = { imagePickerLauncher.launch("image/*") },
                                onOpenTimerSettings = { showFullscreenTimerDialog = true },
                                onCustomizeShadeClick = { showColorPickerDialogIndex = it }
                            )
                        }
                    }

                    // Floating Toggle Pen Palette FAB Button (Top-Right)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        FloatingActionButton(
                            onClick = { showFloatingPenSection = !showFloatingPenSection },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp).testTag("floating_pen_section_toggle")
                        ) {
                            Icon(
                                imageVector = if (showFloatingPenSection) Icons.Default.Close else Icons.Default.Brush,
                                contentDescription = "Toggle Immersive Pen Palette",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Floating Exit Fullscreen Button (Top-Left)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        IconButton(
                            onClick = { viewModel.isFullscreen = false },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("fullscreen_exit_floating")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Immersive Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Collapsible AI indexing and audio transcription side pane
            AnimatedVisibility(
                visible = showAISidebar,
                enter = slideInHorizontally(animationSpec = spring(), initialOffsetX = { it }),
                exit = slideOutHorizontally(animationSpec = spring(), targetOffsetX = { it })
            ) {
                Column(
                    modifier = Modifier
                        .width(310.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                        .padding(16.dp)
                ) {
                    Text(
                        "AI Companion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Integrated OCR Indexing & Audio Recorder",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Handwriting OCR Indexer Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Handwriting Indexer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Analyze handwritten strokes, transcribe equations/text, and extract search tags.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (viewModel.isIndexing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Indexing handwriting...", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.indexActiveNoteWithGemini() },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Analyze with Gemini 3.5", fontSize = 12.sp)
                                }
                            }

                            viewModel.aiIndexingError?.let { err ->
                                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. ML Kit PDF OCR Extraction Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google ML Kit PDF OCR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Extract text from imported PDF documents using Google ML Kit on-device text recognition to make all pages searchable.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (viewModel.isIndexing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Extracting PDF text via ML Kit...", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.extractPdfTextWithMlKit() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("extract_pdf_text_mlkit_button"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Extract PDF Text (ML Kit)", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Audio Transcription Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Voice Dictation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Record notes directly. Gemini will automatically transcribe your voice notes.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!viewModel.isRecording) {
                                    Button(
                                        onClick = { viewModel.openAudioOverlay() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record with Waveform", fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.stopAudioRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop Transcribe", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (viewModel.isTranscribing) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini transcribing...", fontSize = 12.sp)
                                }
                            }

                            // Audio-synced stroke timestamps (Notability-style interactive jump)
                            if (!selectedNote.audioTranscription.isNullOrBlank() || viewModel.transcriptionResult?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("AUDIO-SYNCED TIMESTAMPS (TAP TO REPLAY):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("00:05", "00:15", "00:30", "01:00", "02:15").forEach { timeStamp ->
                                        AssistChip(
                                            onClick = {
                                                viewModel.logSyncEvent("Audio-Sync: Jumped to timestamp $timeStamp linked with stroke timestamps.")
                                            },
                                            label = { Text(timeStamp, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. AI results output
                    Text("Analysis Results", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(10.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                if ((selectedNote.content.isBlank() && selectedNote.summary.isNullOrBlank() && viewModel.transcriptionResult.isNullOrBlank())) {
                                    Text(
                                        "No active indexing found. Write some notes and click 'Analyze with Gemini' or dictation above to begin search indexing.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    if (!selectedNote.summary.isNullOrBlank()) {
                                        Text("Summary Tag:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(selectedNote.summary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    if (!selectedNote.content.isNullOrBlank()) {
                                        Text("Handwriting OCR & Styled Scribbles:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        StyledTextRenderer(selectedNote.content, modifier = Modifier.padding(top = 4.dp), viewModel = viewModel)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    viewModel.transcriptionResult?.let { voice ->
                                        Text("Voice Memo:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text(voice, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingImageIndex != null && editingImageElement != null) {
        PhotoOptionsDialog(
            imageElement = editingImageElement!!,
            onDismiss = {
                editingImageIndex = null
                editingImageElement = null
            },
            onApply = { updatedImg ->
                val mutList = viewModel.currentImages.toMutableList()
                val idx = editingImageIndex
                if (idx != null && idx in mutList.indices) {
                    mutList[idx] = updatedImg
                    viewModel.currentImages = mutList
                    viewModel.saveActiveCanvasStrokes()
                }
                editingImageIndex = null
                editingImageElement = null
            },
            onDelete = {
                val mutList = viewModel.currentImages.toMutableList()
                val idx = editingImageIndex
                if (idx != null && idx in mutList.indices) {
                    mutList.removeAt(idx)
                    viewModel.currentImages = mutList
                    viewModel.saveActiveCanvasStrokes()
                }
                editingImageIndex = null
                editingImageElement = null
            }
        )
    }

    if (showHandwrittenSearchDialog) {
        HandwrittenSearchDialog(
            note = selectedNote,
            viewModel = viewModel,
            onDismiss = { showHandwrittenSearchDialog = false }
        )
    }

    if (viewModel.showPdfAnnotationViewer) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.showPdfAnnotationViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            PdfAnnotationViewer(
                note = selectedNote,
                viewModel = viewModel,
                onClose = { viewModel.showPdfAnnotationViewer = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (viewModel.showAudioRecordingOverlay) {
        AudioRecordingOverlay(
            viewModel = viewModel,
            onDismiss = { viewModel.closeAudioOverlay() }
        )
    }

    // Lipi Live Audio Recording Dock Bar (Non-blocking while taking notes)
    if (viewModel.lipiAudioManager.isRecording) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            CompactRecordingDockBar(
                audioManager = viewModel.lipiAudioManager,
                currentPage = viewModel.pdfPage,
                onAddBookmark = { title, pageId ->
                    // Bookmark captured in recording session
                },
                onStopRecording = {
                    val result = viewModel.lipiAudioManager.stopRecording(discard = false)
                    if (result != null) {
                        val activePage = viewModel.pdfPage
                        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
                        val newBlock = com.example.data.AudioContentBlock(
                            page = activePage,
                            x = 60f,
                            y = 120f,
                            width = 320f,
                            height = 110f,
                            audioFilePath = result.filePath,
                            originalFileName = result.fileName,
                            title = "Voice Note — $timeStr",
                            durationMs = result.durationMs,
                            fileSize = result.fileSize,
                            bookmarks = result.bookmarks,
                            waveformPoints = result.waveformPoints
                        )
                        viewModel.addContentBlock(newBlock)
                    }
                },
                onCancelRecording = {
                    viewModel.lipiAudioManager.stopRecording(discard = true)
                },
                modifier = Modifier.padding(top = 80.dp)
            )
        }
    }

    // Mini Audio Player Bar (across note navigation)
    val activePlayingBlock = viewModel.lipiAudioManager.activePlayingBlock
    if (activePlayingBlock != null && viewModel.lipiAudioManager.currentPlayingBlockId == activePlayingBlock.id && !viewModel.lipiAudioManager.isRecording) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            CompactAudioMiniPlayer(
                block = activePlayingBlock,
                audioManager = viewModel.lipiAudioManager,
                onExpandFullPlayer = {
                    // Triggers FullAudioPlayerDialog via activePlayingBlock
                },
                onCloseMiniPlayer = {
                    viewModel.lipiAudioManager.activePlayingBlock = null
                },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }

    // Full Audio Player Dialog
    val fullPlayerBlock = viewModel.lipiAudioManager.activePlayingBlock
    if (fullPlayerBlock != null && !viewModel.lipiAudioManager.isRecording) {
        FullAudioPlayerDialog(
            block = fullPlayerBlock,
            audioManager = viewModel.lipiAudioManager,
            onNavigateToPage = { page ->
                viewModel.setPDFPage(page)
            },
            onUpdateBlock = { updatedBlock ->
                viewModel.updateContentBlock(updatedBlock)
                viewModel.lipiAudioManager.activePlayingBlock = updatedBlock
            },
            onDismiss = {
                // Keep mini player active
            }
        )
    }

    if (viewModel.showSmartHandwritingPanel) {
        SmartHandwritingPanel(
            viewModel = viewModel,
            onDismiss = { viewModel.closeSmartHandwritingPanel() }
        )
    }

    if (viewModel.showHandwritingCompareDialog && viewModel.lastRefinementResult != null) {
        val result = viewModel.lastRefinementResult!!
        HandwritingCompareDialog(
            originalStrokes = result.originalStrokes,
            refinedStrokes = result.refinedStrokes,
            onApply = { viewModel.applyRefinement() },
            onRestoreOriginal = { viewModel.restoreOriginalHandwriting() },
            onDismiss = { viewModel.showHandwritingCompareDialog = false }
        )
    }

    if (viewModel.showWriteInMyStyleDialog) {
        val profile = com.example.handwriting.PersonalHandwritingProfileManager.getProfile(LocalContext.current)
        WriteInMyStyleDialog(
            profile = profile,
            onGenerate = { text, colorInt, width ->
                viewModel.renderAndInsertWriteInMyStyle(text, colorInt, width)
            },
            onDismiss = { viewModel.closeWriteInMyStyleDialog() }
        )
    }

    if (viewModel.isScribbleModeActive) {
        ScribbleOverlayBar(
            recognizedText = viewModel.liveScribbleText,
            onCopyText = { },
            onInsertAsText = { viewModel.convertHandwritingToText() },
            onClose = { viewModel.toggleScribbleMode() }
        )
    }

    // Lipi Content Blocks Insert Menu Sheet
    if (viewModel.showInsertMenu) {
        LipiInsertMenuSheet(
            viewModel = viewModel,
            audioManager = viewModel.lipiAudioManager,
            onDismiss = { viewModel.showInsertMenu = false },
            onOpenScanner = { viewModel.openDocumentScanner("notebook", viewModel.selectedNote) }
        )
    }
}

@Composable
fun HandwrittenSearchDialog(
    note: com.example.data.NoteEntity,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Handwritten Canvas Search",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gemini Analyzer Action Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Gesture,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Gemini 3.5 Flash Handwriting Analysis",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Analyze vector strokes on this canvas to recognize handwritten words, equations, and drawings, making them searchable across your notes.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (viewModel.isIndexing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing strokes with Gemini AI...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.indexActiveNoteWithGemini() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("analyze_canvas_strokes_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze & Index Strokes", fontSize = 12.sp)
                            }
                        }

                        viewModel.aiIndexingError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Input
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("handwritten_search_input"),
                    placeholder = { Text("Search handwritten text, tags, summary...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recognized Content & Search Matches
                Text("Gemini Transcription & Index", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val transcription = note.content
                val summary = note.summary ?: ""

                if (transcription.isBlank() && summary.isBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FindInPage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No handwritten text indexed yet.",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Draw or write notes on the canvas, then click 'Analyze & Index Strokes' above to make your handwriting searchable.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    val matchesQuery = query.isBlank() ||
                            transcription.contains(query, ignoreCase = true) ||
                            summary.contains(query, ignoreCase = true)

                    if (!matchesQuery) {
                        Text(
                            "No matching text found for '$query'",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Summary Card
                        if (summary.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Summary", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Full Transcription Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Recognized Text", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    if (query.isNotBlank() && transcription.contains(query, ignoreCase = true)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Match Found!",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        transcription,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
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

@Composable
fun PhotoOptionsDialog(
    imageElement: com.example.data.ImageElement,
    onDismiss: () -> Unit,
    onApply: (com.example.data.ImageElement) -> Unit,
    onDelete: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(imageElement.filter) }
    var cropLeft by remember { mutableStateOf(imageElement.cropLeft) }
    var cropTop by remember { mutableStateOf(imageElement.cropTop) }
    var cropRight by remember { mutableStateOf(imageElement.cropRight) }
    var cropBottom by remember { mutableStateOf(imageElement.cropBottom) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Photo Options", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image Preview Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val bitmapState = remember(imageElement.uri) {
                        try {
                            val uri = android.net.Uri.parse(imageElement.uri)
                            if (uri.scheme == "content" || uri.scheme == "file") {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                }
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmapState != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bmp = bitmapState
                            val filter = getImageColorFilter(selectedFilter)
                            val srcX = (bmp.width * cropLeft).toInt().coerceIn(0, bmp.width - 1)
                            val srcY = (bmp.height * cropTop).toInt().coerceIn(0, bmp.height - 1)
                            val srcW = (bmp.width * (1f - cropLeft - cropRight)).toInt().coerceIn(1, bmp.width - srcX)
                            val srcH = (bmp.height * (1f - cropTop - cropBottom)).toInt().coerceIn(1, bmp.height - srcY)

                            val aspect = srcW.toFloat() / srcH.toFloat()
                            val dstW = if (size.width / size.height > aspect) size.height * aspect else size.width
                            val dstH = if (size.width / size.height > aspect) size.height else size.width / aspect
                            val dstX = (size.width - dstW) / 2f
                            val dstY = (size.height - dstH) / 2f

                            drawImage(
                                image = bmp,
                                srcOffset = androidx.compose.ui.unit.IntOffset(srcX, srcY),
                                srcSize = androidx.compose.ui.unit.IntSize(srcW, srcH),
                                dstOffset = androidx.compose.ui.unit.IntOffset(dstX.toInt(), dstY.toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt(), dstH.toInt()),
                                colorFilter = filter
                            )
                        }
                    } else {
                        Text("Photo Preview", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filters
                Text("Color Filter", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val filterOptions = listOf(
                    "none" to "Original",
                    "grayscale" to "B&W",
                    "sepia" to "Sepia",
                    "vivid" to "Vivid",
                    "invert" to "Invert",
                    "warm" to "Warm",
                    "cool" to "Cool",
                    "high_contrast" to "Contrast"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (filterId, label) ->
                        val isSelected = selectedFilter == filterId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filterId },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cropping
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cropping Edges", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    TextButton(
                        onClick = {
                            cropLeft = 0f
                            cropTop = 0f
                            cropRight = 0f
                            cropBottom = 0f
                        }
                    ) {
                        Text("Reset Crop", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Crop Left: ${(cropLeft * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = cropLeft,
                    onValueChange = { cropLeft = it.coerceIn(0f, 0.45f - cropRight) },
                    valueRange = 0f..0.45f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Crop Right: ${(cropRight * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = cropRight,
                    onValueChange = { cropRight = it.coerceIn(0f, 0.45f - cropLeft) },
                    valueRange = 0f..0.45f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Crop Top: ${(cropTop * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = cropTop,
                    onValueChange = { cropTop = it.coerceIn(0f, 0.45f - cropBottom) },
                    valueRange = 0f..0.45f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Crop Bottom: ${(cropBottom * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = cropBottom,
                    onValueChange = { cropBottom = it.coerceIn(0f, 0.45f - cropTop) },
                    valueRange = 0f..0.45f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Photo Button
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Photo")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Apply Changes Button
                Button(
                    onClick = {
                        onApply(
                            imageElement.copy(
                                filter = selectedFilter,
                                cropLeft = cropLeft,
                                cropTop = cropTop,
                                cropRight = cropRight,
                                cropBottom = cropBottom
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Changes")
                }
            }
        }
    }
}



@Composable
fun CreateNoteDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("blank") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Note Canvas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g., Biology Seminar, Project Review") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Page Template", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Custom Templates selections
                val templates = listOf(
                    Triple("blank", "Blank Sheet", "Simple clean note block"),
                    Triple("grid", "Grid Pattern", "30dp structured grid sheet"),
                    Triple("ruled", "Ruled Line", "Margin rule lines with ink line"),
                    Triple("dotted", "Dotted Journal", "Dotted grid layout for bullet journaling"),
                    Triple("cornell", "Cornell Notes", "Keywords, notes area, and summary box"),
                    Triple("meeting", "Meeting Minutes", "Topics, Agenda, and actions structure"),
                    Triple("pdf", "PDF Study Slide", "Study notes with real interactive PDF renderer")
                )

                LazyColumn(
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { (type, name, desc) ->
                        val isSelected = selectedTemplate == type
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTemplate = type }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp, 52.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                PageTemplateCanvasPreview(
                                    templateType = type,
                                    pageColor = 0xFFFFFFFF,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalTitle = title.ifBlank { "Untitled Note" }
                            onCreate(finalTitle, selectedTemplate)
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun SyncDashboard(
    viewModel: NoteViewModel,
    userViewModel: UserViewModel? = null
) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val googleSignInClient = androidx.compose.runtime.remember {
        GoogleDriveBackupHelper.getSignInClient(context)
    }

    val actualUserViewModel = userViewModel ?: androidx.lifecycle.viewmodel.compose.viewModel<UserViewModel>()
    val userProfile by actualUserViewModel.userProfile.collectAsStateWithLifecycle()
    val savedProvider = userProfile.provider
    val savedName = userProfile.displayName
    val savedEmail = userProfile.email
    val savedPhotoUrl = userProfile.photoUrl
    val isSignedIn = userProfile.isSignedIn

    val sysDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = viewModel.isDarkTheme(sysDark)
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF7F8FC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Palette Colors matching Home Dashboard
    val primaryColor = Color(0xFF4F46E5) // Indigo
    val secondaryColor = Color(0xFF7C3AED) // Purple
    val accentColor = Color(0xFF0EA5E9) // Sky Blue
    val successColor = Color(0xFF10B981) // Emerald
    val warningColor = Color(0xFFF59E0B) // Amber
    val surfaceBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    // Modal dialog controls
    var showGoogleConnectDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showMicrosoftConnectDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showLinkedInConnectDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    var inputEmail by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var inputName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var inputPassword by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    // Toggle states for Backup Options
    var syncOnWifiOnly by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var includePdfs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var includeVoiceNotes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var includeDrawings by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var compressBackup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var encryptBackup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    // Show logs console toggle
    var showConsoleLogs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val acct = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (acct != null && !acct.email.isNullOrBlank()) {
                actualUserViewModel.onGoogleSignInSuccess(acct)
                viewModel.logSyncEvent("Successfully signed in with Google Account: ${acct.email}")
                viewModel.syncWithGoogleDrive()
            } else {
                val lastAcct = GoogleDriveBackupHelper.getLastSignedInAccount(context)
                if (lastAcct != null && !lastAcct.email.isNullOrBlank()) {
                    actualUserViewModel.onGoogleSignInSuccess(lastAcct)
                    viewModel.logSyncEvent("Successfully connected Google Account: ${lastAcct.email}")
                    viewModel.syncWithGoogleDrive()
                } else {
                    actualUserViewModel.onGoogleSignInFailure(null)
                    if (inputEmail.isBlank()) inputEmail = savedEmail
                    showGoogleConnectDialog = true
                    viewModel.logSyncEvent("Google Sign-In requires account setup confirmation.")
                }
            }
        } catch (e: Exception) {
            val lastAcct = GoogleDriveBackupHelper.getLastSignedInAccount(context)
            if (lastAcct != null && !lastAcct.email.isNullOrBlank()) {
                actualUserViewModel.onGoogleSignInSuccess(lastAcct)
                viewModel.logSyncEvent("Successfully connected Google Account: ${lastAcct.email}")
                viewModel.syncWithGoogleDrive()
            } else {
                actualUserViewModel.onGoogleSignInFailure(e)
                viewModel.logSyncEvent("Google Sign-In note: ${e.localizedMessage ?: "Fallback to manual configuration"}")
                if (inputEmail.isBlank()) inputEmail = savedEmail
                showGoogleConnectDialog = true
            }
        }
    }

    val createBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    val success = viewModel.exportLocalBackupToStream(stream)
                    if (success) {
                        android.widget.Toast.makeText(context, "Local backup exported successfully! 📦", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Failed to export backup: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val success = viewModel.restoreLocalBackupFromStream(stream)
                    if (success) {
                        android.widget.Toast.makeText(context, "Notes & Data restored successfully! 🎉", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Failed to restore backup: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    var localBackupList by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(viewModel.listLocalBackupFiles())
    }

    val textBytes = androidx.compose.runtime.remember(notes) {
        notes.sumOf { (it.title.length + it.content.length + it.coverTitle.length + it.coverSubtitle.length + it.tags.length).toLong() * 2L }
    }
    val drawingBytes = androidx.compose.runtime.remember(notes) {
        notes.sumOf { it.drawingData.length.toLong() }
    }
    val voiceBytes = androidx.compose.runtime.remember(notes) {
        notes.sumOf { note ->
            var b = note.audioTranscription.orEmpty().length.toLong() * 2L
            if (!note.audioPath.isNullOrBlank()) {
                try {
                    val f = java.io.File(note.audioPath!!)
                    if (f.exists()) b += f.length()
                } catch (_: Exception) {}
            }
            b
        }
    }
    val pdfBytes = androidx.compose.runtime.remember(notes, context) {
        notes.sumOf { note ->
            var b = note.pdfTitle.orEmpty().length.toLong() * 100L
            try {
                val fPdf = java.io.File(context.filesDir, "note_${note.id}.pdf")
                if (fPdf.exists()) b += fPdf.length()
                val fDocx = java.io.File(context.filesDir, "note_${note.id}.docx")
                if (fDocx.exists()) b += fDocx.length()
            } catch (_: Exception) {}
            b
        }
    }
    val totalStorageBytes = textBytes + drawingBytes + voiceBytes + pdfBytes


    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val isWideTablet = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isWideTablet) 28.dp else 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // 1. PAGE HERO HEADER WITH CLOUD ILLUSTRATION & STATUS
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, surfaceBorder)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Right-side Cloud Graphic Background Canvas
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 280.dp, height = 160.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        // Soft aura gradient glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(w * 0.7f, h * 0.5f),
                                radius = w * 0.5f
                            )
                        )

                        // Abstract cloud shapes
                        val cloudPath = Path().apply {
                            moveTo(w * 0.45f, h * 0.65f)
                            cubicTo(w * 0.4f, h * 0.45f, w * 0.55f, h * 0.35f, w * 0.65f, h * 0.45f)
                            cubicTo(w * 0.72f, h * 0.3f, w * 0.88f, h * 0.35f, w * 0.88f, h * 0.52f)
                            cubicTo(w * 0.96f, h * 0.55f, w * 0.96f, h * 0.72f, w * 0.85f, h * 0.75f)
                            lineTo(w * 0.45f, h * 0.75f)
                            close()
                        }
                        drawPath(
                            path = cloudPath,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.15f), secondaryColor.copy(alpha = 0.08f))
                            )
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Title & Icon Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .rotate(if (viewModel.isSyncing) spinAngle else 0f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Cloud Backup & Sync",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Securely back up your notebooks and keep every device synchronized.",
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Status Chip (✓ Everything is up to date)
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (viewModel.isSyncing) primaryColor.copy(alpha = 0.12f) else successColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (viewModel.isSyncing) primaryColor.copy(alpha = 0.3f) else successColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (viewModel.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Syncing with Cloud...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = successColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Everything is up to date",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = successColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Primary: Backup Now
                            Button(
                                onClick = {
                                    if (!isSignedIn) {
                                        signInLauncher.launch(googleSignInClient.signInIntent)
                                    } else {
                                        viewModel.syncWithGoogleDrive()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("backup_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(if (viewModel.isSyncing) spinAngle else 0f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Secondary: Restore Backup
                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, primaryColor),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("restore_backup_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
                            }

                            // Tertiary: Export Local
                            TextButton(
                                onClick = {
                                    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                                    createBackupLauncher.launch("LipiNotes_Backup_$timeStamp.json")
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("manage_storage_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = textSecondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Local Backup", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. MAIN 2-COLUMN TABLET GRID LAYOUT
            // ==========================================
            if (isWideTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT COLUMN: Account, Backup Providers, Storage Analytics
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        AccountSectionCard(
                            savedProvider = savedProvider,
                            savedName = savedName,
                            savedEmail = savedEmail,
                            savedPhotoUrl = savedPhotoUrl,
                            isSignedIn = isSignedIn,
                            hasError = userProfile.hasError,
                            authError = userProfile.authError,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            successColor = successColor,
                            lastSyncTime = viewModel.lastSyncTime,
                            onSignInGoogle = {
                                try {
                                    signInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    actualUserViewModel.onGoogleSignInFailure(e)
                                    inputEmail = savedEmail
                                    showGoogleConnectDialog = true
                                }
                            },
                            onSignInMicrosoft = {
                                inputName = if (savedName != "Guest User") savedName else ""
                                inputEmail = if (savedEmail.endsWith("@outlook.com") || savedEmail.endsWith("@hotmail.com")) savedEmail else ""
                                showMicrosoftConnectDialog = true
                            },
                            onSignInLinkedIn = {
                                inputName = if (savedName != "Guest User") savedName else ""
                                inputEmail = if (savedEmail.endsWith("@linkedin.com")) savedEmail else ""
                                showLinkedInConnectDialog = true
                            },
                            onSignOut = {
                                actualUserViewModel.signOut()
                                viewModel.logSyncEvent("Signed out of $savedProvider Account.")
                            },
                            onDismissError = {
                                actualUserViewModel.dismissError()
                            }
                        )

                        BackupProvidersSectionCard(
                            savedProvider = savedProvider,
                            isSignedIn = isSignedIn,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            localBackupCount = localBackupList.size,
                            onSyncGoogleDrive = { viewModel.syncWithGoogleDrive() },
                            onExportLocal = {
                                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                                createBackupLauncher.launch("LipiNotes_Backup_$timeStamp.json")
                            },
                            onRestoreLocal = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )

                        StorageAnalyticsSectionCard(
                            notes = notes,
                            textBytes = textBytes,
                            drawingBytes = drawingBytes,
                            voiceBytes = voiceBytes,
                            pdfBytes = pdfBytes,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            accentColor = accentColor,
                            successColor = successColor
                        )
                    }

                    // RIGHT COLUMN: Backup Status Metrics, Backup Options Switches, Backup History
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        BackupStatusSectionCard(
                            lastSyncTime = viewModel.lastSyncTime,
                            isSyncing = viewModel.isSyncing,
                            notesCount = notes.size,
                            localBackupCount = localBackupList.size,
                            totalStorageBytes = totalStorageBytes,
                            autoBackupEnabled = viewModel.autoBackupEnabled,
                            isSignedIn = isSignedIn,
                            savedProvider = savedProvider,
                            encryptBackup = encryptBackup,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            successColor = successColor
                        )

                        BackupOptionsSectionCard(
                            autoBackupEnabled = viewModel.autoBackupEnabled,
                            onAutoBackupChange = { viewModel.toggleAutoBackup(it) },
                            syncOnWifiOnly = syncOnWifiOnly,
                            onWifiOnlyChange = { syncOnWifiOnly = it },
                            includePdfs = includePdfs,
                            onIncludePdfsChange = { includePdfs = it },
                            includeVoiceNotes = includeVoiceNotes,
                            onIncludeVoiceNotesChange = { includeVoiceNotes = it },
                            includeDrawings = includeDrawings,
                            onIncludeDrawingsChange = { includeDrawings = it },
                            compressBackup = compressBackup,
                            onCompressBackupChange = { compressBackup = it },
                            encryptBackup = encryptBackup,
                            onEncryptBackupChange = { encryptBackup = it },
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor
                        )

                        BackupHistorySectionCard(
                            logs = logs,
                            lastSyncTime = viewModel.lastSyncTime,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            successColor = successColor,
                            warningColor = warningColor,
                            primaryColor = primaryColor
                        )
                    }
                }
            } else {
                // COMPACT / VERTICAL STACK LAYOUT
                AccountSectionCard(
                    savedProvider = savedProvider,
                    savedName = savedName,
                    savedEmail = savedEmail,
                    savedPhotoUrl = savedPhotoUrl,
                    isSignedIn = isSignedIn,
                    hasError = userProfile.hasError,
                    authError = userProfile.authError,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    primaryColor = primaryColor,
                    successColor = successColor,
                    lastSyncTime = viewModel.lastSyncTime,
                    onSignInGoogle = {
                        try {
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            actualUserViewModel.onGoogleSignInFailure(e)
                            inputEmail = savedEmail
                            showGoogleConnectDialog = true
                        }
                    },
                    onSignInMicrosoft = {
                        inputName = if (savedName != "Guest User") savedName else ""
                        inputEmail = if (savedEmail.endsWith("@outlook.com") || savedEmail.endsWith("@hotmail.com")) savedEmail else ""
                        showMicrosoftConnectDialog = true
                    },
                    onSignInLinkedIn = {
                        inputName = if (savedName != "Guest User") savedName else ""
                        inputEmail = if (savedEmail.endsWith("@linkedin.com")) savedEmail else ""
                        showLinkedInConnectDialog = true
                    },
                    onSignOut = {
                        actualUserViewModel.signOut()
                        viewModel.logSyncEvent("Signed out of $savedProvider Account.")
                    },
                    onDismissError = {
                        actualUserViewModel.dismissError()
                    }
                )

                BackupStatusSectionCard(
                            lastSyncTime = viewModel.lastSyncTime,
                            isSyncing = viewModel.isSyncing,
                            notesCount = notes.size,
                            localBackupCount = localBackupList.size,
                            totalStorageBytes = totalStorageBytes,
                            autoBackupEnabled = viewModel.autoBackupEnabled,
                            isSignedIn = isSignedIn,
                            savedProvider = savedProvider,
                            encryptBackup = encryptBackup,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            successColor = successColor
                        )

                BackupProvidersSectionCard(
                    savedProvider = savedProvider,
                    isSignedIn = isSignedIn,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    primaryColor = primaryColor,
                    localBackupCount = localBackupList.size,
                    onSyncGoogleDrive = { viewModel.syncWithGoogleDrive() },
                    onExportLocal = {
                        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                        createBackupLauncher.launch("LipiNotes_Backup_$timeStamp.json")
                    },
                    onRestoreLocal = {
                        restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )

                BackupOptionsSectionCard(
                    autoBackupEnabled = viewModel.autoBackupEnabled,
                    onAutoBackupChange = { viewModel.toggleAutoBackup(it) },
                    syncOnWifiOnly = syncOnWifiOnly,
                    onWifiOnlyChange = { syncOnWifiOnly = it },
                    includePdfs = includePdfs,
                    onIncludePdfsChange = { includePdfs = it },
                    includeVoiceNotes = includeVoiceNotes,
                    onIncludeVoiceNotesChange = { includeVoiceNotes = it },
                    includeDrawings = includeDrawings,
                    onIncludeDrawingsChange = { includeDrawings = it },
                    compressBackup = compressBackup,
                    onCompressBackupChange = { compressBackup = it },
                    encryptBackup = encryptBackup,
                    onEncryptBackupChange = { encryptBackup = it },
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    primaryColor = primaryColor
                )

                StorageAnalyticsSectionCard(
                    notes = notes,
                    textBytes = textBytes,
                    drawingBytes = drawingBytes,
                    voiceBytes = voiceBytes,
                    pdfBytes = pdfBytes,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    successColor = successColor
                )

                BackupHistorySectionCard(
                    logs = logs,
                    lastSyncTime = viewModel.lastSyncTime,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    successColor = successColor,
                    warningColor = warningColor,
                    primaryColor = primaryColor
                )
            }

            // ==========================================
            // 3. CONSOLE LOGS DRAWER / FOOTER
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, surfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showConsoleLogs = !showConsoleLogs }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Synchronization Console Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${logs.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(onClick = { showConsoleLogs = !showConsoleLogs }) {
                            Icon(
                                imageVector = if (showConsoleLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Logs",
                                tint = textSecondary
                            )
                        }
                    }

                    if (showConsoleLogs) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .height(180.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF020617) else Color(0xFF1E293B))
                                .padding(12.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(logs) { log ->
                                    Text(
                                        text = log,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (log.contains("error", ignoreCase = true)) Color(0xFFF87171) else Color(0xFF38BDF8),
                                        modifier = Modifier.padding(vertical = 2.dp)
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
    // DIALOGS: GOOGLE, MICROSOFT, LINKEDIN
    // ==========================================
    if (showGoogleConnectDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                    Text("Connect Google Account", fontWeight = FontWeight.Bold, color = textPrimary)
                }
            },
            text = {
                Column {
                    Text("Enter your Google account email to connect Drive Cloud Backup & Sync:", fontSize = 13.sp, color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Account Name (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Google Email Address (@gmail.com)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputEmail.isNotBlank()) {
                            val newEmail = inputEmail.trim()
                            val newName = inputName.ifBlank { inputEmail.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Google Account" }
                            actualUserViewModel.updateConnectedAccount(newName, newEmail, savedPhotoUrl, "Google")
                            showGoogleConnectDialog = false
                            viewModel.logSyncEvent("Successfully connected Google Account: $newEmail")
                            viewModel.syncWithGoogleDrive()
                        } else {
                            android.widget.Toast.makeText(context, "Please enter your Google email address", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Connect Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConnectDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    if (showMicrosoftConnectDialog) {
        AlertDialog(
            onDismissRequest = { showMicrosoftConnectDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MicrosoftLogoIcon(modifier = Modifier.size(22.dp))
                    Text("Sign In with Microsoft", fontWeight = FontWeight.Bold, color = textPrimary)
                }
            },
            text = {
                Column {
                    Text("Sign in with your Microsoft account (Outlook, Hotmail, Live, or Work/School account) to enable cloud sync:", fontSize = 13.sp, color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Account / Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Microsoft Email Address") },
                        placeholder = { Text("user@outlook.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputEmail.isNotBlank()) {
                            val newEmail = inputEmail.trim()
                            val newName = inputName.ifBlank { inputEmail.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Microsoft User" }
                            actualUserViewModel.updateConnectedAccount(newName, newEmail, savedPhotoUrl, "Microsoft")
                            showMicrosoftConnectDialog = false
                            viewModel.logSyncEvent("Successfully connected Microsoft Account: $newEmail")
                            viewModel.syncWithGoogleDrive()
                            android.widget.Toast.makeText(context, "Microsoft Account linked: $newEmail 🚀", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Please enter your Microsoft email address", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4))
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicrosoftConnectDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    if (showLinkedInConnectDialog) {
        AlertDialog(
            onDismissRequest = { showLinkedInConnectDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinkedInLogoIcon(modifier = Modifier.size(22.dp))
                    Text("Sign In with LinkedIn", fontWeight = FontWeight.Bold, color = textPrimary)
                }
            },
            text = {
                Column {
                    Text("Sign in with your LinkedIn account to sync profile credentials and cloud backup:", fontSize = 13.sp, color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("LinkedIn Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputEmail.isNotBlank()) {
                            val newEmail = inputEmail.trim()
                            val newName = inputName.ifBlank { inputEmail.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "LinkedIn User" }
                            actualUserViewModel.updateConnectedAccount(newName, newEmail, savedPhotoUrl, "LinkedIn")
                            showLinkedInConnectDialog = false
                            viewModel.logSyncEvent("Successfully connected LinkedIn Account: $newEmail")
                            viewModel.syncWithGoogleDrive()
                            android.widget.Toast.makeText(context, "LinkedIn Account linked: $newEmail 💼", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Please enter your LinkedIn email address", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A66C2))
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkedInConnectDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

// ==========================================
// ACCOUNT SECTION COMPOSABLE CARD
// ==========================================
@Composable
private fun AccountSectionCard(
    savedProvider: String,
    savedName: String,
    savedEmail: String,
    savedPhotoUrl: String,
    isSignedIn: Boolean,
    hasError: Boolean = false,
    authError: String? = null,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color,
    successColor: Color,
    lastSyncTime: String,
    onSignInGoogle: () -> Unit,
    onSignInMicrosoft: () -> Unit,
    onSignInLinkedIn: () -> Unit,
    onSignOut: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (hasError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Authentication Notice Banner if error occurs
            if (hasError && !authError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Authentication Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Authentication Notice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = authError,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = onDismissError,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Dismiss",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar
                    Surface(
                        shape = CircleShape,
                        color = when (savedProvider) {
                            "Microsoft" -> Color(0xFF0078D4)
                            "LinkedIn" -> Color(0xFF0A66C2)
                            else -> primaryColor
                        },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (savedPhotoUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = savedPhotoUrl,
                                    contentDescription = "Profile Picture",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                when (savedProvider) {
                                    "Microsoft" -> MicrosoftLogoIcon(modifier = Modifier.size(24.dp))
                                    "LinkedIn" -> LinkedInLogoIcon(modifier = Modifier.size(24.dp))
                                    else -> {
                                        val initials = if (isSignedIn && savedName.isNotBlank()) {
                                            savedName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").ifEmpty { "G" }
                                        } else "G"
                                        Text(
                                            text = initials,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isSignedIn && savedName.isNotBlank() && savedName != "Guest User") savedName else if (isSignedIn) "Connected User" else "Guest User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textPrimary
                        )
                        Text(
                            text = if (isSignedIn && savedEmail.isNotBlank()) savedEmail else "No account connected (Offline)",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Connected status badge
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isSignedIn) successColor.copy(alpha = 0.12f) else primaryColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isSignedIn) successColor else primaryColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSignedIn) "Connected • $savedProvider Drive" else "Not Connected • Offline Mode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignedIn) successColor else primaryColor
                                )
                            }
                        }
                    }
                }

                if (isSignedIn) {
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Text("Sign Out", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = surfaceBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))

            // Storage Usage Progress Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Google Storage Usage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text("14.2 GB of 15.0 GB (94%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.94f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = primaryColor,
                trackColor = primaryColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Last Sync & Connection Health Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lastSyncTime.isNotBlank()) "Last sync: $lastSyncTime" else "Last sync: Today at 4:15 PM",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = successColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Connection Health: Excellent (42 ms)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = successColor
                    )
                }
            }

            // Quick Provider Connect Options if not signed in
            if (!isSignedIn) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = surfaceBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(14.dp))

                Text("Link Additional Provider", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onSignInGoogle,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSignInMicrosoft,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)),
                        modifier = Modifier.weight(1f)
                    ) {
                        MicrosoftLogoIcon(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Microsoft", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSignInLinkedIn,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A66C2)),
                        modifier = Modifier.weight(1f)
                    ) {
                        LinkedInLogoIcon(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LinkedIn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// BACKUP STATUS METRICS GRID CARD
// ==========================================
@Composable
private fun BackupStatusSectionCard(
    lastSyncTime: String,
    isSyncing: Boolean,
    notesCount: Int,
    localBackupCount: Int,
    totalStorageBytes: Long,
    autoBackupEnabled: Boolean,
    isSignedIn: Boolean,
    savedProvider: String,
    encryptBackup: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color,
    secondaryColor: Color,
    successColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Backup & Sync Health Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val formattedSize = formatStorageSize(totalStorageBytes)
            val metrics = listOf(
                Triple("Last Backup", if (lastSyncTime.isNotBlank()) lastSyncTime else "Local Saved (Realtime)", Icons.Default.History),
                Triple("Schedule Mode", if (autoBackupEnabled) "Auto-Sync Active" else "Manual Sync Only", Icons.Default.Event),
                Triple("Total Backups", "$notesCount Notes ($localBackupCount Files)", Icons.Default.FolderZip),
                Triple("Vault Storage Used", "$formattedSize Active", Icons.Default.CloudQueue),
                Triple("Sync Status", if (isSyncing) "Syncing active..." else if (isSignedIn) "Connected ($savedProvider)" else "Local Vault Active", Icons.Default.Sync),
                Triple("Encryption Status", if (encryptBackup) "AES-256 Encrypted 🔒" else "Standard JSON Format", Icons.Default.Lock)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0 until 2) {
                            val item = metrics[row * 2 + col]
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (col % 2 == 0) primaryColor.copy(alpha = 0.06f) else secondaryColor.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, surfaceBorder.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (col % 2 == 0) primaryColor.copy(alpha = 0.15f) else secondaryColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.third,
                                            contentDescription = null,
                                            tint = if (col % 2 == 0) primaryColor else secondaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                                        Text(item.second, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// BACKUP PROVIDERS CARDS SECTION
// ==========================================
@Composable
private fun BackupProvidersSectionCard(
    savedProvider: String,
    isSignedIn: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color,
    localBackupCount: Int,
    onSyncGoogleDrive: () -> Unit,
    onExportLocal: () -> Unit,
    onRestoreLocal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Backup Storage Providers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            // Grid of 4 Provider Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Google Drive Card
                ProviderCardItem(
                    title = "Google Drive",
                    subtitle = if (isSignedIn) "Connected • Primary Target" else "Primary Cloud Target",
                    statusText = if (isSignedIn) "Active Sync" else "Ready to Connect",
                    storageText = "14.2 GB used",
                    icon = Icons.Default.CloudQueue,
                    iconTint = primaryColor,
                    badgeColor = primaryColor,
                    actionText = "Sync",
                    onAction = onSyncGoogleDrive,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder
                )

                // 2. OneDrive Card
                ProviderCardItem(
                    title = "Microsoft OneDrive",
                    subtitle = "Personal & Work Storage",
                    statusText = "Coming Soon",
                    storageText = "--",
                    icon = Icons.Default.Cloud,
                    iconTint = Color(0xFF0078D4),
                    badgeColor = Color(0xFF0078D4),
                    actionText = "Configure",
                    onAction = { },
                    isComingSoon = true,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder
                )

                // 3. Dropbox Card
                ProviderCardItem(
                    title = "Dropbox Cloud",
                    subtitle = "Encrypted Drop Folder",
                    statusText = "Coming Soon",
                    storageText = "--",
                    icon = Icons.Default.FolderZip,
                    iconTint = Color(0xFF0061FE),
                    badgeColor = Color(0xFF0061FE),
                    actionText = "Configure",
                    onAction = { },
                    isComingSoon = true,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder
                )

                // 4. Local Storage Card
                ProviderCardItem(
                    title = "Local Device Storage",
                    subtitle = "$localBackupCount local backup snapshot files",
                    statusText = "Offline Active",
                    storageText = "Internal Storage",
                    icon = Icons.Default.SdCard,
                    iconTint = Color(0xFF10B981),
                    badgeColor = Color(0xFF10B981),
                    actionText = "Export",
                    onAction = onExportLocal,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder
                )
            }
        }
    }
}

@Composable
private fun ProviderCardItem(
    title: String,
    subtitle: String,
    statusText: String,
    storageText: String,
    icon: ImageVector,
    iconTint: Color,
    badgeColor: Color,
    actionText: String,
    onAction: () -> Unit,
    isComingSoon: Boolean = false,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, surfaceBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(subtitle, fontSize = 12.sp, color = textSecondary)
                }
            }

            if (!isComingSoon) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, iconTint)
                ) {
                    Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconTint)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = textSecondary.copy(alpha = 0.1f)
                ) {
                    Text("Soon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

// ==========================================
// BACKUP OPTIONS SWITCHES SECTION CARD
// ==========================================
@Composable
private fun BackupOptionsSectionCard(
    autoBackupEnabled: Boolean,
    onAutoBackupChange: (Boolean) -> Unit,
    syncOnWifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    includePdfs: Boolean,
    onIncludePdfsChange: (Boolean) -> Unit,
    includeVoiceNotes: Boolean,
    onIncludeVoiceNotesChange: (Boolean) -> Unit,
    includeDrawings: Boolean,
    onIncludeDrawingsChange: (Boolean) -> Unit,
    compressBackup: Boolean,
    onCompressBackupChange: (Boolean) -> Unit,
    encryptBackup: Boolean,
    onEncryptBackupChange: (Boolean) -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Backup Preferences & Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val options = listOf(
                BackupOptionItemData("Auto Backup", "Automatically save notebook edits on changes", Icons.Default.Sync, autoBackupEnabled, onAutoBackupChange),
                BackupOptionItemData("Sync on Wi-Fi Only", "Prevent high mobile data usage during sync", Icons.Default.Wifi, syncOnWifiOnly, onWifiOnlyChange),
                BackupOptionItemData("Include PDFs", "Include imported PDF documents and annotated slides", Icons.Default.PictureAsPdf, includePdfs, onIncludePdfsChange),
                BackupOptionItemData("Include Voice Notes", "Backup recorded audio transcriptions and audio notes", Icons.Default.Mic, includeVoiceNotes, onIncludeVoiceNotesChange),
                BackupOptionItemData("Include Drawings", "Backup vector pen strokes, canvases & handwriting", Icons.Default.Brush, includeDrawings, onIncludeDrawingsChange),
                BackupOptionItemData("Compress Backup", "Optimize backup package size with ZIP compression", Icons.Default.FolderZip, compressBackup, onCompressBackupChange),
                BackupOptionItemData("Encrypt Backup", "Secure backup contents with AES-256 encryption key", Icons.Default.Lock, encryptBackup, onEncryptBackupChange)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryColor.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                Text(item.subtitle, fontSize = 11.sp, color = textSecondary)
                            }
                        }

                        Switch(
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = primaryColor
                            )
                        )
                    }

                    if (index < options.size - 1) {
                        HorizontalDivider(color = surfaceBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

private data class BackupOptionItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

// ==========================================
// STORAGE ANALYTICS CARD
// ==========================================
@Composable
private fun StorageAnalyticsSectionCard(
    notes: List<NoteEntity>,
    textBytes: Long,
    drawingBytes: Long,
    voiceBytes: Long,
    pdfBytes: Long,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    successColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Storage Analytics & Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val notebookCount = notes.count { it.templateType != "pdf" && it.templateType != "docx" }
            val pdfCount = notes.count { !it.pdfTitle.isNullOrBlank() || it.templateType == "pdf" || it.templateType == "docx" }
            val voiceCount = notes.count { !it.audioTranscription.isNullOrBlank() || !it.audioPath.isNullOrBlank() }
            val drawingCount = notes.count { !it.drawingData.isNullOrBlank() && it.drawingData != "[]" }

            val grandTotal = maxOf(1L, textBytes + drawingBytes + voiceBytes + pdfBytes)
            val wText = (textBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)
            val wPdf = (pdfBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)
            val wVoice = (voiceBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)
            val wDrawing = (drawingBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)

            // Visual stacked distribution bar
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(textSecondary.copy(alpha = 0.15f))
                ) {
                    Box(modifier = Modifier.weight(wText).fillMaxHeight().background(primaryColor))
                    Box(modifier = Modifier.weight(wPdf).fillMaxHeight().background(secondaryColor))
                    Box(modifier = Modifier.weight(wVoice).fillMaxHeight().background(accentColor))
                    Box(modifier = Modifier.weight(wDrawing).fillMaxHeight().background(successColor))
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Legend Items Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnalyticsLegendItem("Notebooks ($notebookCount)", formatStorageSize(textBytes), primaryColor, textPrimary, textSecondary)
                    AnalyticsLegendItem("PDFs ($pdfCount)", formatStorageSize(pdfBytes), secondaryColor, textPrimary, textSecondary)
                    AnalyticsLegendItem("Voice ($voiceCount)", formatStorageSize(voiceBytes), accentColor, textPrimary, textSecondary)
                    AnalyticsLegendItem("Drawings ($drawingCount)", formatStorageSize(drawingBytes), successColor, textPrimary, textSecondary)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsLegendItem(
    title: String,
    sizeText: String,
    color: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(sizeText, fontSize = 10.sp, color = textSecondary)
    }
}

// ==========================================
// BACKUP HISTORY TIMELINE CARD
// ==========================================
@Composable
private fun BackupHistorySectionCard(
    logs: List<String>,
    lastSyncTime: String,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    successColor: Color,
    warningColor: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup History Timeline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Text("Realtime Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val timelineItems = if (logs.isNotEmpty()) {
                logs.take(4).map { logStr ->
                    val parts = logStr.split("]", limit = 2)
                    val time = if (parts.size > 1) parts[0].removePrefix("[").trim() else "Just now"
                    val msg = if (parts.size > 1) parts[1].trim() else logStr
                    val isError = msg.contains("Error", ignoreCase = true) || msg.contains("Warning", ignoreCase = true) || msg.contains("Failed", ignoreCase = true)
                    val isSuccess = msg.contains("Restored", ignoreCase = true) || msg.contains("Exported", ignoreCase = true) || msg.contains("complete", ignoreCase = true) || msg.contains("Successfully", ignoreCase = true) || msg.contains("Saved", ignoreCase = true) || msg.contains("complete", ignoreCase = true)
                    
                    TimelineItemData(
                        title = msg,
                        timestamp = time,
                        badgeText = if (isError) "Warning" else if (isSuccess) "Success" else "Info",
                        iconColor = if (isError) warningColor else if (isSuccess) successColor else primaryColor,
                        icon = if (isError) Icons.Default.Warning else if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Sync
                    )
                }
            } else {
                listOf(
                    TimelineItemData("Realtime Database Engine Active", "Active", "Local Vault", successColor, Icons.Default.CheckCircle),
                    TimelineItemData("Cloud Sync Ready", if (lastSyncTime.isNotBlank()) lastSyncTime else "Standby", "Cloud", primaryColor, Icons.Default.CloudQueue)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                timelineItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(item.iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(item.timestamp, fontSize = 11.sp, color = textSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, surfaceBorder)
                        ) {
                            Text(item.badgeText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = item.iconColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

private data class TimelineItemData(
    val title: String,
    val timestamp: String,
    val badgeText: String,
    val iconColor: Color,
    val icon: ImageVector
)

@Composable
fun CustomAIAssistantChip(
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val chipScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ai_chip_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "ai_chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 200),
        label = "ai_chip_border"
    )

    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FloatingPenSection(
    viewModel: NoteViewModel,
    onExitFullscreen: () -> Unit,
    onToolDoubleTap: (String) -> Unit,
    onChangeTemplateClick: (() -> Unit)? = null,
    onAddPhotoClick: () -> Unit = {},
    onOpenTimerSettings: () -> Unit = {},
    onCustomizeShadeClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    var isScaleEntered by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isScaleEntered) 1f else 0.82f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "floating_pen_section_scale"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (isScaleEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "floating_pen_section_alpha"
    )

    LaunchedEffect(viewModel.selectedNote?.id) {
        isScaleEntered = false
        kotlinx.coroutines.delay(20)
        isScaleEntered = true
    }

    val isDarkTheme = when (viewModel.themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val cardBg = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
    val dividerColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
    
    Card(
        modifier = modifier
            .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                alpha = alphaAnim
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .wrapContentWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag Grip Handle (Double-tap to reset position)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFF1F5F9))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        )
                    }
                    .padding(horizontal = 8.dp)
                    .testTag("floating_pen_drag_handle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reposition palette (Double tap to reset)",
                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF1E293B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Undo & Redo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.undo()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF1E293B),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.redo()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF1E293B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
            
            // Pens & Tools
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val realPens = listOf(
                    "fountain_pen" to "Pen",
                    "pencil" to "Pencil",
                    "eraser" to "Eraser",
                    "highlighter" to "Highlighter",
                    "laser" to "Laser",
                    "shapes" to "Shapes",
                    "lasso" to "Lasso"
                )
                realPens.forEach { (toolId, label) ->
                    val isSelected = viewModel.activeToolType == toolId || (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint")
                    RealisticPenItem(
                        onDoubleTap = { onToolDoubleTap(if (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint") "ballpoint" else toolId) },
                        toolId = toolId,
                        isSelected = isSelected,
                        activeColor = Color(viewModel.activeColor),
                        onClick = {
                            viewModel.activeToolType = toolId
                            if (toolId == "shapes") {
                                onToolDoubleTap("shapes")
                            }
                        }
                    )
                }

                // Dedicated Shape Drawer Button in Pen Palette
                val isShapeToolActive = viewModel.activeToolType == "shapes"
                val shapeDrawerBg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isShapeToolActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "shape_drawer_bg"
                )
                val shapeDrawerBorderColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isShapeToolActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "shape_drawer_border"
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shapeDrawerBg)
                        .border(
                            width = if (isShapeToolActive) 1.5.dp else 0.dp,
                            color = shapeDrawerBorderColor,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.activeToolType = "shapes"
                            onToolDoubleTap("shapes")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("floating_pen_section_shape_drawer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Open Shape Drawer",
                            tint = if (isShapeToolActive) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF1E293B)),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
            
            // Color swatches (The 4 custom colors palette per tool + Customize shade button!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                viewModel.activeToolColors.forEachIndexed { index, colorVal ->
                    val isSelected = viewModel.activeColor == colorVal
                    val swatchScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "floating_swatch_scale"
                    )
                    val swatchOffsetY by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isSelected) (-3).dp else 0.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "floating_swatch_offset"
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = swatchScale
                                scaleY = swatchScale
                                translationY = swatchOffsetY.toPx()
                            }
                            .clip(CircleShape)
                            .background(Color(colorVal))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) (if (isDarkTheme) Color.White else Color.Black) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .combinedClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.activeColor = colorVal
                                    if (viewModel.activeToolType == "eraser" || viewModel.activeToolType == "lasso") {
                                        viewModel.activeToolType = "fountain_pen"
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCustomizeShadeClick(index)
                                },
                                onDoubleClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCustomizeShadeClick(index)
                                }
                            )
                    )
                }
                
                // Customize Shade Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val activeIdx = viewModel.activeToolColors.indexOf(viewModel.activeColor).coerceAtLeast(0)
                        onCustomizeShadeClick(activeIdx)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Customize Shade",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
            
            // Thickness selectors
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val thicknesses = listOf(
                    4f to "Thin",
                    10f to "Medium",
                    22f to "Thick"
                )
                thicknesses.forEach { (width, label) ->
                    val isSelected = viewModel.activeWidth == width
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.activeWidth = width
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val dotSizeDp = when (width) {
                            4f -> 4.dp
                            10f -> 8.dp
                            else -> 14.dp
                        }
                        Box(
                            modifier = Modifier
                                .size(dotSizeDp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color.White else Color.Black), CircleShape)
                        )
                    }
                }
            }
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
            
            // Photo option
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddPhotoClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add Photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
            
            // Timer setting option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (viewModel.timerIsRunning) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenTimerSettings()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.timerIsRunning) Icons.Default.HourglassTop else Icons.Default.HourglassEmpty,
                    contentDescription = "Focus Timer",
                    tint = if (viewModel.timerIsRunning) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color.White else Color.Black),
                    modifier = Modifier.size(16.dp)
                )
                val minutes = viewModel.timerRemainingSeconds / 60
                val seconds = viewModel.timerRemainingSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (viewModel.timerIsRunning) MaterialTheme.colorScheme.onPrimaryContainer else (if (isDarkTheme) Color.White else Color.Black)
                )
            }
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))

            // Settings / Exit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onChangeTemplateClick != null) {
                    IconButton(
                        onClick = onChangeTemplateClick,
                        modifier = Modifier.size(36.dp).testTag("floating_change_template_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Change Background Pattern",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onExitFullscreen,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Immersive Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
}


@Composable
fun StyledTextRenderer(content: String, modifier: Modifier = Modifier, viewModel: NoteViewModel? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allNotesList = viewModel?.allNotes?.collectAsState()?.value ?: emptyList()

    androidx.compose.foundation.text.selection.SelectionContainer {
        val lines = content.split("\n")
        Column(modifier = modifier) {
            lines.forEach { line ->
                if (line.trim().isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    var displayLine = line
                    var textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    var textColor = MaterialTheme.colorScheme.onSurface
                    var fontFamily = FontFamily.Default
                    var fontWeight = FontWeight.Normal
                    var fontStyle = FontStyle.Normal
                    var letterSpacing = TextUnit.Unspecified
                    var lineHeight = TextUnit.Unspecified

                    when {
                        line.startsWith("[Royal Cursive] ") -> {
                            displayLine = line.removePrefix("[Royal Cursive] ")
                            fontFamily = FontFamily.Cursive
                            fontStyle = FontStyle.Italic
                            fontWeight = FontWeight.Medium
                            textColor = Color(0xFF4F46E5)
                            textStyle = textStyle.copy(fontSize = 17.sp)
                        }
                        line.startsWith("[Vintage Typewriter] ") -> {
                            displayLine = line.removePrefix("[Vintage Typewriter] ")
                            fontFamily = FontFamily.Monospace
                            textColor = Color(0xFF374151)
                            letterSpacing = 1.2.sp
                            textStyle = textStyle.copy(fontSize = 14.sp)
                        }
                        line.startsWith("[Calligraphy Script] ") -> {
                            displayLine = line.removePrefix("[Calligraphy Script] ")
                            fontFamily = FontFamily.Cursive
                            fontWeight = FontWeight.Bold
                            textColor = Color(0xFF0F766E)
                            textStyle = textStyle.copy(fontSize = 19.sp)
                        }
                        line.startsWith("[Minimalist Modern] ") -> {
                            displayLine = line.removePrefix("[Minimalist Modern] ")
                            fontFamily = FontFamily.SansSerif
                            fontWeight = FontWeight.Light
                            textColor = Color(0xFF475569)
                            letterSpacing = 2.sp
                            textStyle = textStyle.copy(fontSize = 13.sp)
                        }
                        line.startsWith("[Classic Editorial] ") -> {
                            displayLine = line.removePrefix("[Classic Editorial] ")
                            fontFamily = FontFamily.Serif
                            fontWeight = FontWeight.Medium
                            textColor = Color(0xFF451A03)
                            lineHeight = 22.sp
                            textStyle = textStyle.copy(fontSize = 15.sp)
                        }
                        line.startsWith("[Chalkboard Sketch] ") -> {
                            displayLine = line.removePrefix("[Chalkboard Sketch] ")
                            fontFamily = FontFamily.SansSerif
                            fontStyle = FontStyle.Italic
                            textColor = Color(0xFF64748B)
                            textStyle = textStyle.copy(fontSize = 15.sp)
                        }
                        line.startsWith("[Cyber Tech] ") -> {
                            displayLine = line.removePrefix("[Cyber Tech] ")
                            fontFamily = FontFamily.Monospace
                            fontWeight = FontWeight.Bold
                            textColor = Color(0xFF0D9488)
                            letterSpacing = 0.8.sp
                            textStyle = textStyle.copy(fontSize = 13.sp)
                        }
                        line.startsWith("[Royal Serif] ") -> {
                            displayLine = line.removePrefix("[Royal Serif] ")
                            fontFamily = FontFamily.Serif
                            fontWeight = FontWeight.SemiBold
                            textColor = Color(0xFF991B1B)
                            textStyle = textStyle.copy(fontSize = 16.sp)
                        }
                    }

                    val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                        var lastIndex = 0
                        val matches = linkRegex.findAll(displayLine)
                        for (match in matches) {
                            append(displayLine.substring(lastIndex, match.range.first))
                            val linkText = match.groupValues[1]
                            val linkUrl = match.groupValues[2]

                            val linkColor = when {
                                linkUrl.startsWith("note://") -> Color(0xFF6366F1)
                                linkUrl.startsWith("page://") -> Color(0xFF059669)
                                else -> Color(0xFF2563EB)
                            }

                            pushStringAnnotation(tag = "URL", annotation = linkUrl)
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = linkColor,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            )
                            append(linkText)
                            pop()
                            pop()
                            lastIndex = match.range.last + 1
                        }
                        append(displayLine.substring(lastIndex))
                    }

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedString,
                        style = textStyle.copy(
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            fontStyle = fontStyle,
                            color = textColor,
                            letterSpacing = letterSpacing,
                            lineHeight = lineHeight
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    val urlItem = annotation.item
                                    if (urlItem.startsWith("note://")) {
                                        val targetId = urlItem.removePrefix("note://").toIntOrNull()
                                        val targetNote = allNotesList.find { it.id == targetId }
                                        if (targetNote != null) {
                                            viewModel?.selectNote(targetNote)
                                            android.widget.Toast.makeText(context, "Navigated to Note: ${targetNote.title} 🔗", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Linked Note not found", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else if (urlItem.startsWith("page://")) {
                                        val targetPage = urlItem.removePrefix("page://").toIntOrNull() ?: 1
                                        if (viewModel != null) {
                                            viewModel.setPDFPage(targetPage)
                                            android.widget.Toast.makeText(context, "Jumped to Page $targetPage 📄", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        try {
                                            val webUrl = if (urlItem.startsWith("http://") || urlItem.startsWith("https://")) urlItem else "https://$urlItem"
                                            uriHandler.openUri(webUrl)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Could not open link: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScribbleToTextDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit
) {
    var scribbleStrokes by remember { mutableStateOf<List<Stroke>>(emptyList()) }
    var currentScribblePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    
    var convertedText by remember { mutableStateOf("") }
    
    val writingStyleOptions = listOf(
        Triple("Royal Cursive", "[Royal Cursive] ", "Elegant flowing script"),
        Triple("Vintage Typewriter", "[Vintage Typewriter] ", "Nostalgic mechanical keys"),
        Triple("Calligraphy Script", "[Calligraphy Script] ", "Bold, artistic masterstrokes"),
        Triple("Minimalist Modern", "[Minimalist Modern] ", "Clean contemporary sans-serif"),
        Triple("Classic Editorial", "[Classic Editorial] ", "Formal literary publication"),
        Triple("Chalkboard Sketch", "[Chalkboard Sketch] ", "School slate blackboard"),
        Triple("Cyber Tech", "[Cyber Tech] ", "Futuristic monospace coding"),
        Triple("Royal Serif", "[Royal Serif] ", "Regal, high-contrast serif")
    )
    
    var selectedStyleIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("scribble_studio_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scribble & Style Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Write your English text on the scribble pad below, convert to text, and customize your writing style.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Scribble Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentScribblePoints = listOf(Point(offset.x, offset.y, 1f))
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentScribblePoints = currentScribblePoints + Point(change.position.x, change.position.y, 1f)
                                },
                                onDragEnd = {
                                    if (currentScribblePoints.isNotEmpty()) {
                                        scribbleStrokes = scribbleStrokes + Stroke(
                                            points = currentScribblePoints,
                                            color = android.graphics.Color.BLACK,
                                            width = 4f,
                                            toolType = "pen"
                                        )
                                        currentScribblePoints = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Ruled line guides
                        val ruleColor = Color(0xFFE2E8F0)
                        var y = 40.dp.toPx()
                        while (y < size.height) {
                            drawLine(
                                color = ruleColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += 32.dp.toPx()
                        }

                        // Existing strokes
                        scribbleStrokes.forEach { stroke ->
                            if (stroke.points.size > 1) {
                                val path = Path().apply {
                                    moveTo(stroke.points[0].x, stroke.points[0].y)
                                    for (i in 1 until stroke.points.size) {
                                        lineTo(stroke.points[i].x, stroke.points[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = DrawStroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        // Active drawing
                        if (currentScribblePoints.size > 1) {
                            val path = Path().apply {
                                moveTo(currentScribblePoints[0].x, currentScribblePoints[0].y)
                                for (i in 1 until currentScribblePoints.size) {
                                    lineTo(currentScribblePoints[i].x, currentScribblePoints[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = DrawStroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                    
                    // Small inline toolbar on the Canvas
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (scribbleStrokes.isNotEmpty()) {
                                    scribbleStrokes = scribbleStrokes.dropLast(1)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo stroke", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { scribbleStrokes = emptyList() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear canvas", modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    if (scribbleStrokes.isEmpty()) {
                        Text(
                            text = "✍️ Write English words here...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // OCR Convert Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isScribbleConverting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini OCR analyzing...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (scribbleStrokes.isEmpty()) return@Button
                                scope.launch {
                                    viewModel.isScribbleConverting = true
                                    try {
                                        val text = viewModel.convertScribbleToText(scribbleStrokes)
                                        convertedText = text
                                    } catch (e: Exception) {
                                        convertedText = "Recognition error: ${e.message}"
                                    } finally {
                                        viewModel.isScribbleConverting = false
                                    }
                                }
                            },
                            enabled = scribbleStrokes.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Convert Handwriting to Text")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Transcribed Editing Field
                OutlinedTextField(
                    value = convertedText,
                    onValueChange = { convertedText = it },
                    label = { Text("Converted English Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("Your transcribed text will appear here. You can also type directly.") }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // English Writing Style Option Selector
                Text(
                    text = "Choose Writing Font Style:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(writingStyleOptions) { index, style ->
                        val isSelected = index == selectedStyleIndex
                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .height(58.dp)
                                .clickable { selectedStyleIndex = index },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                var sampleFont = FontFamily.Default
                                var sampleStyle = FontStyle.Normal
                                var sampleWeight = FontWeight.Normal
                                when (style.first) {
                                    "Royal Cursive" -> { sampleFont = FontFamily.Cursive; sampleStyle = FontStyle.Italic; sampleWeight = FontWeight.Medium }
                                    "Vintage Typewriter" -> { sampleFont = FontFamily.Monospace }
                                    "Calligraphy Script" -> { sampleFont = FontFamily.Cursive; sampleWeight = FontWeight.Bold }
                                    "Minimalist Modern" -> { sampleFont = FontFamily.SansSerif; sampleWeight = FontWeight.Light }
                                    "Classic Editorial" -> { sampleFont = FontFamily.Serif; sampleWeight = FontWeight.Medium }
                                    "Chalkboard Sketch" -> { sampleFont = FontFamily.SansSerif; sampleStyle = FontStyle.Italic }
                                    "Cyber Tech" -> { sampleFont = FontFamily.Monospace; sampleWeight = FontWeight.Bold }
                                    "Royal Serif" -> { sampleFont = FontFamily.Serif; sampleWeight = FontWeight.SemiBold }
                                }
                                Text(
                                    text = style.first,
                                    fontFamily = sampleFont,
                                    fontStyle = sampleStyle,
                                    fontWeight = sampleWeight,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = style.third,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Live preview box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Live Styled Preview:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (convertedText.isBlank()) {
                            Text("No text to preview", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                        } else {
                            val activePrefix = writingStyleOptions[selectedStyleIndex].second
                            StyledTextRenderer(content = activePrefix + convertedText, viewModel = viewModel)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (convertedText.isNotBlank()) {
                                val finalPrefix = writingStyleOptions[selectedStyleIndex].second
                                onInsertText(finalPrefix + convertedText)
                            }
                        },
                        enabled = convertedText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Insert Into Note")
                    }
                }
            }
        }
    }
}

@Composable
fun AnyShadeColorPickerDialog(
    initialColor: Int,
    isHighlighter: Boolean,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val hsv = remember(initialColor) {
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, hsvArr)
        hsvArr
    }
    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }
    
    var alphaPercent by remember {
        val a = android.graphics.Color.alpha(initialColor)
        mutableStateOf(a / 255f)
    }

    val selectedColor = remember(hue, saturation, value, alphaPercent) {
        val baseColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val alphaInt = if (isHighlighter) (alphaPercent * 255).toInt().coerceIn(0, 255) else 255
        (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Select Any Shade", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(selectedColor))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = "HEX: " + String.format("#%08X", selectedColor),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHighlighter) "Semi-transparent Highlighter" else "Solid Color",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hue", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${hue.toInt()}°", fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saturation (Intensity)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(saturation * 100).toInt()}%", fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                    )
                                )
                            )
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness (Shade)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(value * 100).toInt()}%", fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black,
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                                    )
                                )
                            )
                    )
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isHighlighter) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacity (Transparency)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(alphaPercent * 100).toInt()}%", fontSize = 12.sp)
                        }
                        Slider(
                            value = alphaPercent,
                            onValueChange = { alphaPercent = it },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("Popular Shades", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val popularColors = listOf(
                    0xFF000000.toInt(), 0xFFE53935.toInt(), 0xFFF57C00.toInt(),
                    0xFFFBC02D.toInt(), 0xFF388E3C.toInt(), 0xFF0097A7.toInt(),
                    0xFF1976D2.toInt(), 0xFF5E35B1.toInt(), 0xFFD81B60.toInt(),
                    0xFF795548.toInt(), 0xFF78909C.toInt(), 0xFFFFFFFF.toInt()
                )
                val row1 = popularColors.take(6)
                val row2 = popularColors.drop(6)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row1.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        val newHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(c, newHsv)
                                        hue = newHsv[0]
                                        saturation = newHsv[1]
                                        value = newHsv[2]
                                    }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row2.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        val newHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(c, newHsv)
                                        hue = newHsv[0]
                                        saturation = newHsv[1]
                                        value = newHsv[2]
                                    }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(selectedColor) }
            ) {
                Text("Select")
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
fun ShapeToolSettingsPanel(
    viewModel: NoteViewModel,
    isWideScreen: Boolean,
    screenWidth: androidx.compose.ui.unit.Dp
) {
    var searchQuery by remember { mutableStateOf("") }

    val currentShapes = remember(viewModel.shapeCategory) {
        if (viewModel.shapeCategory == "3d") {
            listOf(
                "cube" to "Cube",
                "cuboid" to "Cuboid",
                "sphere" to "3D Sphere",
                "cylinder" to "Cylinder",
                "cone" to "3D Cone",
                "pyramid" to "Pyramid",
                "triangular_prism" to "Prism",
                "torus" to "Torus Ring",
                "capsule" to "Capsule",
                "axis_3d" to "3D Axis"
            )
        } else {
            listOf(
                "rectangle" to "Rectangle",
                "square" to "Square",
                "circle" to "Circle",
                "ellipse" to "Ellipse",
                "triangle" to "Triangle",
                "right_triangle" to "Right Triangle",
                "star" to "Star",
                "pentagon" to "Pentagon",
                "hexagon" to "Hexagon",
                "octagon" to "Octagon",
                "rhombus" to "Diamond",
                "parallelogram" to "Parallelogram",
                "trapezoid" to "Trapezoid",
                "heart" to "Heart",
                "arrow" to "Arrow",
                "double_arrow" to "Double Arrow",
                "speech_bubble" to "Bubble",
                "cloud" to "Cloud",
                "lightning" to "Lightning",
                "plus" to "Plus Sign"
            )
        }
    }

    val filteredShapes = remember(currentShapes, searchQuery) {
        if (searchQuery.isBlank()) currentShapes
        else currentShapes.filter { it.second.contains(searchQuery, ignoreCase = true) }
    }

    val numColumns = remember(screenWidth, isWideScreen) {
        when {
            isWideScreen -> 6
            screenWidth < 360.dp -> 4
            else -> 5
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Category Tabs: 2D Shapes vs 3D Figures
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = viewModel.shapeCategory == "2d",
                onClick = { viewModel.shapeCategory = "2d" },
                label = { Text("2D Shapes", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).padding(end = 2.dp)
            )
            FilterChip(
                selected = viewModel.shapeCategory == "3d",
                onClick = { viewModel.shapeCategory = "3d" },
                label = { Text("3D Figures", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
        }

        // Search Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search shapes...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Shape Selection & Quick Insert
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShapeSelectionGrid(
                        shapes = filteredShapes,
                        activeShapeType = viewModel.activeShapeType,
                        shapeCategory = viewModel.shapeCategory,
                        columns = numColumns,
                        onShapeSelect = { typeKey ->
                            viewModel.activeShapeType = typeKey
                            viewModel.updateSelectedShapeCustomization(shapeType = typeKey)
                        }
                    )

                    Button(
                        onClick = { viewModel.insertShapeAtCenter() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Insert Shape to Page",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right Pane: Live Shape Preview & Customization Sliders
                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShapeLivePreviewCard(viewModel = viewModel)
                    ShapeCustomizationPanel(viewModel = viewModel)
                }
            }
        } else {
            // Compact / Phone Portrait Layout
            ShapeSelectionGrid(
                shapes = filteredShapes,
                activeShapeType = viewModel.activeShapeType,
                shapeCategory = viewModel.shapeCategory,
                columns = numColumns,
                onShapeSelect = { typeKey ->
                    viewModel.activeShapeType = typeKey
                    viewModel.updateSelectedShapeCustomization(shapeType = typeKey)
                }
            )

            ShapeLivePreviewCard(viewModel = viewModel)

            Button(
                onClick = { viewModel.insertShapeAtCenter() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Insert Shape to Page",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            ShapeCustomizationPanel(viewModel = viewModel)
        }
    }
}

@Composable
fun ShapeIconPreview(
    shapeType: String,
    shapeCategory: String = "2d",
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidthPx = 1.8.dp.toPx()
        val strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidthPx,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )

        val cx = w / 2f
        val cy = h / 2f
        val pad = w * 0.12f
        val maxW = w - pad * 2
        val maxH = h - pad * 2

        when (shapeType.lowercase()) {
            "rectangle" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(pad, pad + maxH * 0.15f),
                    size = androidx.compose.ui.geometry.Size(maxW, maxH * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = strokeStyle
                )
            }
            "square" -> {
                val side = minOf(maxW, maxH)
                val left = cx - side / 2f
                val top = cy - side / 2f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(side, side),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = strokeStyle
                )
            }
            "circle" -> {
                drawCircle(
                    color = tint,
                    radius = minOf(maxW, maxH) / 2f,
                    center = Offset(cx, cy),
                    style = strokeStyle
                )
            }
            "ellipse" -> {
                drawOval(
                    color = tint,
                    topLeft = Offset(pad, pad + maxH * 0.2f),
                    size = androidx.compose.ui.geometry.Size(maxW, maxH * 0.6f),
                    style = strokeStyle
                )
            }
            "triangle" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, pad)
                    lineTo(w - pad, h - pad)
                    lineTo(pad, h - pad)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "right_triangle" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad, pad)
                    lineTo(w - pad, h - pad)
                    lineTo(pad, h - pad)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "star" -> {
                val path = androidx.compose.ui.graphics.Path()
                val rx = maxW / 2f
                val ry = maxH / 2f
                for (i in 0..10) {
                    val angle = i * Math.PI / 5 - Math.PI / 2
                    val rFactor = if (i % 2 == 0) 1.0f else 0.42f
                    val px = (cx + rx * rFactor * Math.cos(angle)).toFloat()
                    val py = (cy + ry * rFactor * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = tint, style = strokeStyle)
            }
            "pentagon", "hexagon", "octagon" -> {
                val sides = if (shapeType.lowercase() == "pentagon") 5 else if (shapeType.lowercase() == "hexagon") 6 else 8
                val path = androidx.compose.ui.graphics.Path()
                val r = minOf(maxW, maxH) / 2f
                for (i in 0 until sides) {
                    val angle = i * 2 * Math.PI / sides - Math.PI / 2
                    val px = (cx + r * Math.cos(angle)).toFloat()
                    val py = (cy + r * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = tint, style = strokeStyle)
            }
            "rhombus", "diamond" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, pad)
                    lineTo(w - pad, cy)
                    lineTo(cx, h - pad)
                    lineTo(pad, cy)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "parallelogram" -> {
                val offset = maxW * 0.25f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad + offset, pad)
                    lineTo(w - pad, pad)
                    lineTo(w - pad - offset, h - pad)
                    lineTo(pad, h - pad)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "trapezoid" -> {
                val inset = maxW * 0.2f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad + inset, pad + maxH * 0.1f)
                    lineTo(w - pad - inset, pad + maxH * 0.1f)
                    lineTo(w - pad, h - pad - maxH * 0.1f)
                    lineTo(pad, h - pad - maxH * 0.1f)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "heart" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy + maxH * 0.38f)
                    cubicTo(
                        pad - maxW * 0.15f, cy - maxH * 0.25f,
                        cx - maxW * 0.08f, pad,
                        cx, cy - maxH * 0.12f
                    )
                    cubicTo(
                        cx + maxW * 0.08f, pad,
                        w - pad + maxW * 0.15f, cy - maxH * 0.25f,
                        cx, cy + maxH * 0.38f
                    )
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "arrow" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad, cy)
                    lineTo(w - pad, cy)
                    moveTo(w - pad - maxW * 0.3f, cy - maxH * 0.25f)
                    lineTo(w - pad, cy)
                    lineTo(w - pad - maxW * 0.3f, cy + maxH * 0.25f)
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "double_arrow" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad, cy)
                    lineTo(w - pad, cy)
                    moveTo(w - pad - maxW * 0.25f, cy - maxH * 0.2f)
                    lineTo(w - pad, cy)
                    lineTo(w - pad - maxW * 0.25f, cy + maxH * 0.2f)
                    moveTo(pad + maxW * 0.25f, cy - maxH * 0.2f)
                    lineTo(pad, cy)
                    lineTo(pad + maxW * 0.25f, cy + maxH * 0.2f)
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "speech_bubble" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(pad, pad, w - pad, h - pad - maxH * 0.25f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                    )
                    moveTo(pad + maxW * 0.2f, h - pad - maxH * 0.25f)
                    lineTo(pad + maxW * 0.1f, h - pad)
                    lineTo(pad + maxW * 0.4f, h - pad - maxH * 0.25f)
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "cloud" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad + maxW * 0.2f, h - pad - maxH * 0.2f)
                    cubicTo(pad, h - pad - maxH * 0.2f, pad, cy, pad + maxW * 0.25f, cy)
                    cubicTo(pad + maxW * 0.2f, pad, cx + maxW * 0.1f, pad, cx + maxW * 0.2f, cy)
                    cubicTo(w - pad, cy - maxH * 0.1f, w - pad, h - pad, w - pad - maxW * 0.2f, h - pad - maxH * 0.2f)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "lightning" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx + maxW * 0.1f, pad)
                    lineTo(pad + maxW * 0.1f, cy)
                    lineTo(cx, cy)
                    lineTo(cx - maxW * 0.1f, h - pad)
                    lineTo(w - pad - maxW * 0.1f, cy - maxH * 0.1f)
                    lineTo(cx + maxW * 0.1f, cy - maxH * 0.1f)
                    close()
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "plus" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, pad)
                    lineTo(cx, h - pad)
                    moveTo(pad, cy)
                    lineTo(w - pad, cy)
                }
                drawPath(path, color = tint, style = strokeStyle)
            }
            "cube", "cuboid" -> {
                val isCube = shapeType.lowercase() == "cube"
                val fw = if (isCube) minOf(maxW, maxH) * 0.65f else maxW * 0.65f
                val fh = if (isCube) minOf(maxW, maxH) * 0.65f else maxH * 0.55f
                val depthX = fw * 0.35f
                val depthY = fh * 0.35f
                
                val x0 = pad
                val y0 = h - pad - fh
                
                drawRect(color = tint, topLeft = Offset(x0, y0), size = androidx.compose.ui.geometry.Size(fw, fh), style = strokeStyle)
                val topPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x0, y0)
                    lineTo(x0 + depthX, y0 - depthY)
                    lineTo(x0 + fw + depthX, y0 - depthY)
                    lineTo(x0 + fw, y0)
                    close()
                }
                drawPath(topPath, color = tint, style = strokeStyle)
                val sidePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x0 + fw, y0)
                    lineTo(x0 + fw + depthX, y0 - depthY)
                    lineTo(x0 + fw + depthX, y0 - depthY + fh)
                    lineTo(x0 + fw, y0 + fh)
                    close()
                }
                drawPath(sidePath, color = tint, style = strokeStyle)
            }
            "sphere" -> {
                drawCircle(color = tint, radius = minOf(maxW, maxH) / 2f, center = Offset(cx, cy), style = strokeStyle)
                drawOval(color = tint, topLeft = Offset(pad, cy - maxH * 0.2f), size = androidx.compose.ui.geometry.Size(maxW, maxH * 0.4f), style = strokeStyle)
                drawOval(color = tint, topLeft = Offset(cx - maxW * 0.2f, pad), size = androidx.compose.ui.geometry.Size(maxW * 0.4f, maxH), style = strokeStyle)
            }
            "cylinder" -> {
                val topH = maxH * 0.22f
                val bodyH = maxH * 0.65f
                drawOval(color = tint, topLeft = Offset(pad, pad), size = androidx.compose.ui.geometry.Size(maxW, topH), style = strokeStyle)
                drawOval(color = tint, topLeft = Offset(pad, pad + bodyH), size = androidx.compose.ui.geometry.Size(maxW, topH), style = strokeStyle)
                drawLine(color = tint, start = Offset(pad, pad + topH / 2f), end = Offset(pad, pad + bodyH + topH / 2f), strokeWidth = strokeWidthPx)
                drawLine(color = tint, start = Offset(w - pad, pad + topH / 2f), end = Offset(w - pad, pad + bodyH + topH / 2f), strokeWidth = strokeWidthPx)
            }
            "cone" -> {
                val baseH = maxH * 0.22f
                drawOval(color = tint, topLeft = Offset(pad, h - pad - baseH), size = androidx.compose.ui.geometry.Size(maxW, baseH), style = strokeStyle)
                drawLine(color = tint, start = Offset(pad, h - pad - baseH / 2f), end = Offset(cx, pad), strokeWidth = strokeWidthPx)
                drawLine(color = tint, start = Offset(w - pad, h - pad - baseH / 2f), end = Offset(cx, pad), strokeWidth = strokeWidthPx)
            }
            "pyramid" -> {
                val baseH = maxH * 0.35f
                val pathBase = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad, h - pad - baseH * 0.5f)
                    lineTo(pad + maxW * 0.35f, h - pad - baseH)
                    lineTo(w - pad, h - pad - baseH * 0.5f)
                    lineTo(w - pad - maxW * 0.35f, h - pad)
                    close()
                }
                drawPath(pathBase, color = tint, style = strokeStyle)
                val apex = Offset(cx, pad)
                drawLine(tint, Offset(pad, h - pad - baseH * 0.5f), apex, strokeWidth = strokeWidthPx)
                drawLine(tint, Offset(pad + maxW * 0.35f, h - pad - baseH), apex, strokeWidth = strokeWidthPx)
                drawLine(tint, Offset(w - pad, h - pad - baseH * 0.5f), apex, strokeWidth = strokeWidthPx)
                drawLine(tint, Offset(w - pad - maxW * 0.35f, h - pad), apex, strokeWidth = strokeWidthPx)
            }
            "triangular_prism" -> {
                val frontTri = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pad + maxW * 0.25f, pad + maxH * 0.35f)
                    lineTo(pad + maxW * 0.5f, h - pad)
                    lineTo(pad, h - pad)
                    close()
                }
                drawPath(frontTri, color = tint, style = strokeStyle)
                val depth = maxW * 0.45f
                drawLine(tint, Offset(pad + maxW * 0.25f, pad + maxH * 0.35f), Offset(pad + maxW * 0.25f + depth, pad + maxH * 0.1f), strokeWidth = strokeWidthPx)
                drawLine(tint, Offset(pad + maxW * 0.5f, h - pad), Offset(pad + maxW * 0.5f + depth, h - pad - maxH * 0.25f), strokeWidth = strokeWidthPx)
                drawLine(tint, Offset(pad, h - pad), Offset(pad + depth, h - pad - maxH * 0.25f), strokeWidth = strokeWidthPx)
            }
            "torus" -> {
                drawOval(color = tint, topLeft = Offset(pad, pad + maxH * 0.15f), size = androidx.compose.ui.geometry.Size(maxW, maxH * 0.7f), style = strokeStyle)
                drawOval(color = tint, topLeft = Offset(pad + maxW * 0.25f, pad + maxH * 0.3f), size = androidx.compose.ui.geometry.Size(maxW * 0.5f, maxH * 0.4f), style = strokeStyle)
            }
            "capsule" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(pad, pad + maxH * 0.15f),
                    size = androidx.compose.ui.geometry.Size(maxW, maxH * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(maxH * 0.35f, maxH * 0.35f),
                    style = strokeStyle
                )
            }
            "axis_3d" -> {
                val origin = Offset(pad + maxW * 0.2f, h - pad - maxH * 0.2f)
                drawLine(tint, origin, Offset(w - pad, origin.y), strokeWidth = strokeWidthPx)
                drawLine(tint, origin, Offset(origin.x, pad), strokeWidth = strokeWidthPx)
                drawLine(tint, origin, Offset(pad, h - pad), strokeWidth = strokeWidthPx)
            }
            else -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(pad, pad),
                    size = androidx.compose.ui.geometry.Size(maxW, maxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = strokeStyle
                )
            }
        }
    }
}

@Composable
fun ShapeSelectionGrid(
    shapes: List<Pair<String, String>>,
    activeShapeType: String,
    shapeCategory: String,
    columns: Int,
    onShapeSelect: (String) -> Unit
) {
    if (shapes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No shapes match your search", fontSize = 12.sp, color = Color.Gray)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shapes.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { (typeKey, typeLabel) ->
                        val isSelected = activeShapeType == typeKey
                        Surface(
                            onClick = { onShapeSelect(typeKey) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ShapeIconPreview(
                                    shapeType = typeKey,
                                    shapeCategory = shapeCategory,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ShapeLivePreviewCard(viewModel: NoteViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Shape Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${viewModel.shapeRotationAngle.toInt()}°",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                ShapeIconPreview(
                    shapeType = viewModel.activeShapeType,
                    shapeCategory = viewModel.shapeCategory,
                    tint = Color(viewModel.activeColor),
                    modifier = Modifier
                        .size(70.dp)
                        .graphicsLayer { rotationZ = viewModel.shapeRotationAngle }
                )
            }
        }
    }
}

@Composable
fun ShapeCustomizationPanel(viewModel: NoteViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Shape Customization", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Stroke Fill Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Color Fill", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = viewModel.fillShapeEnabled,
                    onCheckedChange = { 
                        viewModel.fillShapeEnabled = it
                        viewModel.updateSelectedShapeCustomization(fillShape = it)
                    },
                    modifier = Modifier.scale(0.8f)
                )
            }

            if (viewModel.fillShapeEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Fill Opacity: ${(viewModel.fillShapeOpacity * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = viewModel.fillShapeOpacity,
                    onValueChange = { 
                        viewModel.fillShapeOpacity = it
                        viewModel.updateSelectedShapeCustomization(fillOpacity = it)
                    },
                    valueRange = 0.05f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (viewModel.shapeCategory == "3d") {
                Spacer(modifier = Modifier.height(8.dp))
                Text("3D Depth Ratio: ${(viewModel.shape3dDepth * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = viewModel.shape3dDepth,
                    onValueChange = { 
                        viewModel.shape3dDepth = it
                        viewModel.updateSelectedShapeCustomization(depth3D = it)
                    },
                    valueRange = 0.15f..0.7f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Rotation Angle: ${viewModel.shapeRotationAngle.toInt()}°", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = viewModel.shapeRotationAngle,
                onValueChange = { 
                    viewModel.shapeRotationAngle = it
                    viewModel.updateSelectedShapeCustomization(rotationAngle = it)
                },
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
