package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.DirectoryItem
import com.example.data.NoteEntity
import com.example.data.TagItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Production-Grade Responsive Navigation Sidebar for Lipi (Tablet & Foldable Workspace).
 *
 * Adheres to Material 3 Expressive guidelines, high-touch tablet ergonomics (48dp targets),
 * fluid spring physics transitions, and pixel-precise visual hierarchy.
 */
@Composable
fun ResponsiveSidebar(
    notes: List<NoteEntity>,
    viewModel: NoteViewModel,
    activeTab: String,
    onTabChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel? = null
) {
    var isRailMode by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(true) }
    var isNestedDirectoriesExpanded by remember { mutableStateOf(true) }
    var isColoredTagsExpanded by remember { mutableStateOf(true) }
    var expandedDirectories by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Directory & Tag Dialog States
    var directoryToEdit by remember { mutableStateOf<DirectoryItem?>(null) }
    var isCreatingDirectory by remember { mutableStateOf(false) }
    var defaultParentForNewDir by remember { mutableStateOf<String?>(null) }
    var tagToEdit by remember { mutableStateOf<TagItem?>(null) }
    var isCreatingTag by remember { mutableStateOf(false) }

    // Smooth Spring Transition for Sidebar Width
    val targetWidth = if (isRailMode) 76.dp else 316.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SidebarWidthAnimation"
    )

    // Accurate category dynamic counters
    val allCount = notes.size
    val handwrittenCount = notes.count { it.templateType in listOf("blank", "ruled", "grid") }
    val pdfCount = notes.count { it.templateType in listOf("pdf", "docx") }
    val scannedCount = notes.count { 
        it.templateType == "scanned_doc" || 
        it.tags.contains("scanned", ignoreCase = true) || 
        it.title.contains("Scanned", ignoreCase = true) || 
        it.title.contains("Scan", ignoreCase = true) || 
        (it.pdfTitle ?: "").contains("Scanned", ignoreCase = true) 
    }
    val templatesCount = notes.count { it.templateType in listOf("cornell", "meeting") }
    val plannerCount = notes.count { it.tags.contains("planner", ignoreCase = true) || it.tags.contains("calendar", ignoreCase = true) }
    val mindMapsCount = notes.count { it.tags.contains("mindmap", ignoreCase = true) || it.templateType == "grid" }
    val backupPendingCount = notes.count { !it.isSynced }
    val syncedCount = notes.count { it.isSynced }

    // Dynamic User Profile & Account Data from UserViewModel
    val context = LocalContext.current
    val actualUserViewModel = userViewModel ?: androidx.lifecycle.viewmodel.compose.viewModel<UserViewModel>()
    val userProfile by actualUserViewModel.userProfile.collectAsStateWithLifecycle()
    val isSignedIn = userProfile.isSignedIn
    val accountName = userProfile.formattedName
    val accountEmail = userProfile.formattedEmail
    val photoUrl = userProfile.photoUrl
    val initials = userProfile.initials

    // Realtime storage computation
    val textBytes = remember(notes) {
        notes.sumOf { (it.title.length + it.content.length + it.coverTitle.length + it.coverSubtitle.length + it.tags.length).toLong() * 2L }
    }
    val drawingBytes = remember(notes) {
        notes.sumOf { it.drawingData.length.toLong() }
    }
    val voiceBytes = remember(notes) {
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
    val pdfBytes = remember(notes, context) {
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

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(animatedWidth)
            .testTag("responsive_sidebar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.5.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isRailMode) 8.dp else 12.dp,
                    vertical = 12.dp
                )
        ) {
            // ==========================================
            // 1. BRAND HEADER & EXPAND/COLLAPSE TOGGLE
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRailMode) Arrangement.Center else Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 6.dp)
            ) {
                if (!isRailMode) {
                    LipiBrandHeader(
                        iconSize = 34.dp,
                        showTagline = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { isRailMode = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .testTag("collapse_sidebar_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                            contentDescription = "Collapse Sidebar to Rail",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .clickable(
                                onClickLabel = "Expand Sidebar",
                                role = Role.Button
                            ) { isRailMode = false }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LipiLogoIcon(modifier = Modifier.size(28.dp))
                    }
                }
            }

            // ==========================================
            // 2. HERO PROFILE CARD
            // ==========================================
            if (!isRailMode) {
                HeroProfileCard(
                    isSignedIn = isSignedIn,
                    accountName = accountName,
                    accountEmail = accountEmail,
                    photoUrl = photoUrl,
                    initials = initials,
                    hasError = userProfile.hasError,
                    authError = userProfile.authError,
                    streakDays = viewModel.studyStreakDays,
                    totalStorageBytes = totalStorageBytes,
                    notesCount = notes.size,
                    pendingCount = backupPendingCount,
                    isSyncing = viewModel.isSyncing,
                    onClick = { onTabChange("sync") }
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (userProfile.hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable(
                                onClickLabel = "View Account & Cloud Sync",
                                role = Role.Button
                            ) { onTabChange("sync") }
                            .border(
                                2.dp,
                                if (userProfile.hasError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSignedIn && photoUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "User Profile Avatar",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = initials,
                                    color = if (userProfile.hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ==========================================
            // 3. SEARCH BOX
            // ==========================================
            if (!isRailMode) {
                SidebarQuickSearchBox(
                    notes = notes,
                    searchKeyword = searchKeyword,
                    onSearchChange = onSearchChange,
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = { isSearchExpanded = !isSearchExpanded }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isRailMode = false },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Notes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ==========================================
            // 4. MAIN SCROLLABLE NAVIGATION LIST
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                // ------------------------------------------
                // SECTION: WORKSPACES
                // ------------------------------------------
                if (!isRailMode) {
                    item {
                        SidebarSectionHeader(title = "WORKSPACES")
                    }
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.Home,
                        label = "Home Dashboard",
                        isSelected = activeTab == "home",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("home")
                            onFilterChange("All Notes")
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.Folder,
                        label = "All Notes",
                        count = allCount,
                        isSelected = activeTab == "notes" && selectedFilter == "All Notes",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("All Notes")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.Draw,
                        label = "Handwritten",
                        count = handwrittenCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Handwritten",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Handwritten")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.PictureAsPdf,
                        label = "PDF Notes & Docs",
                        count = pdfCount,
                        isSelected = activeTab == "notes" && selectedFilter == "PDFs",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("PDFs")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.DocumentScanner,
                        label = "Scanned Documents",
                        count = scannedCount,
                        isSelected = activeTab == "notes" && (selectedFilter == "Scanned Documents" || selectedFilter == "Scanned Docs"),
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Scanned Documents")
                            viewModel.selectNote(null)
                            viewModel.openDocumentScanner("sidebar_section")
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Scan Document",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Scanned Documents")
                            viewModel.openDocumentScanner("sidebar")
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.Description,
                        label = "Structural Templates",
                        count = templatesCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Templates",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Templates")
                            viewModel.selectNote(null)
                        }
                    )
                }

                // ------------------------------------------
                // SECTION: LIBRARIES & STUDY
                // ------------------------------------------
                if (!isRailMode) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SidebarSectionHeader(title = "LIBRARIES & STUDY")
                    }
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Planner & Calendar",
                        count = if (plannerCount > 0) plannerCount else null,
                        isSelected = activeTab == "notes" && selectedFilter == "Planner",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Planner")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.AccountTree,
                        label = "Mind Maps",
                        count = if (mindMapsCount > 0) mindMapsCount else null,
                        isSelected = activeTab == "notes" && selectedFilter == "Mind Maps",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Mind Maps")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.Insights,
                        label = "Study Insights",
                        isSelected = activeTab == "notes" && selectedFilter == "Analytics",
                        isRailMode = isRailMode,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Analytics")
                            viewModel.selectNote(null)
                        }
                    )
                }

                // ------------------------------------------
                // SECTION: PROJECTS & FOLDERS (EXPANDABLE TREE)
                // ------------------------------------------
                if (!isRailMode) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isNestedDirectoriesExpanded = !isNestedDirectoriesExpanded }
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (isNestedDirectoriesExpanded) 180f else 0f,
                                    label = "DirExpandRotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = if (isNestedDirectoriesExpanded) "Collapse Folders" else "Expand Folders",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(rotation)
                                )
                                Text(
                                    text = "PROJECTS & FOLDERS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    defaultParentForNewDir = null
                                    isCreatingDirectory = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "Create New Directory",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isNestedDirectoriesExpanded) {
                        fun flattenDirectories(
                            directories: List<DirectoryItem>,
                            parentId: String?,
                            level: Int,
                            expandedDirs: Set<String>
                        ): List<Pair<DirectoryItem, Int>> {
                            val result = mutableListOf<Pair<DirectoryItem, Int>>()
                            val children = directories.filter { it.parentId == parentId }
                            for (child in children) {
                                result.add(child to level)
                                if (expandedDirs.contains(child.id)) {
                                    result.addAll(flattenDirectories(directories, child.id, level + 1, expandedDirs))
                                }
                            }
                            return result
                        }

                        val flatList = flattenDirectories(viewModel.customDirectories, null, 0, expandedDirectories)

                        if (flatList.isEmpty()) {
                            item {
                                Text(
                                    text = "No custom folders. Tap + to organize notes.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 4.dp)
                                )
                            }
                        } else {
                            flatList.forEach { (dir, level) ->
                                val childDirectories = viewModel.customDirectories.filter { it.parentId == dir.id }
                                val isExpanded = expandedDirectories.contains(dir.id)

                                item(key = "dir_${dir.id}") {
                                    CustomDirectorySidebarRow(
                                        directory = dir,
                                        indentLevel = level,
                                        notes = notes,
                                        isSelected = activeTab == "notes" && (selectedFilter == "dir:${dir.id}" || selectedFilter == dir.name),
                                        hasChildren = childDirectories.isNotEmpty(),
                                        isExpanded = isExpanded,
                                        onToggleExpand = {
                                            expandedDirectories = if (isExpanded) {
                                                expandedDirectories - dir.id
                                            } else {
                                                expandedDirectories + dir.id
                                            }
                                        },
                                        onSelect = {
                                            onTabChange("notes")
                                            onFilterChange("dir:${dir.id}")
                                            viewModel.selectNote(null)
                                        },
                                        onAddNote = {
                                            viewModel.addNoteToDirectory(dir)
                                            onTabChange("notes")
                                        },
                                        onAddSubdirectory = {
                                            defaultParentForNewDir = dir.id
                                            isCreatingDirectory = true
                                            expandedDirectories = expandedDirectories + dir.id
                                        },
                                        onEdit = {
                                            directoryToEdit = dir
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // SECTION: COLORED TAGS
                    // ------------------------------------------
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isColoredTagsExpanded = !isColoredTagsExpanded }
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (isColoredTagsExpanded) 180f else 0f,
                                    label = "TagExpandRotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = if (isColoredTagsExpanded) "Collapse Tags" else "Expand Tags",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(rotation)
                                )
                                Text(
                                    text = "COLORED TAGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { isCreatingTag = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Tag",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isColoredTagsExpanded) {
                        if (viewModel.customTags.isEmpty()) {
                            item {
                                Text(
                                    text = "No custom tags yet. Tap + to create tags.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 4.dp)
                                )
                            }
                        } else {
                            viewModel.customTags.forEach { tag ->
                                item(key = "tag_${tag.id}") {
                                    CustomTagSidebarRow(
                                        tag = tag,
                                        notes = notes,
                                        isSelected = activeTab == "notes" && selectedFilter == "tag:${tag.name}",
                                        onSelect = {
                                            onTabChange("notes")
                                            onFilterChange("tag:${tag.name}")
                                            viewModel.selectNote(null)
                                        },
                                        onAddNote = {
                                            viewModel.addNoteWithTag(tag)
                                            onTabChange("notes")
                                        },
                                        onEdit = {
                                            tagToEdit = tag
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ------------------------------------------
                // SECTION: TOOLS & CLOUD
                // ------------------------------------------
                if (!isRailMode) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SidebarSectionHeader(title = "TOOLS & CLOUD")
                    }
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.CloudSync,
                        label = "Backup & Cloud Sync",
                        count = if (backupPendingCount > 0) backupPendingCount else null,
                        isSelected = activeTab == "sync",
                        isRailMode = isRailMode,
                        badgeColor = if (backupPendingCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        badgeTextColor = if (backupPendingCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { onTabChange("sync") }
                    )
                }

                item {
                    ExpressiveNavItem(
                        icon = Icons.Default.Explore,
                        label = "App Tour & Guide",
                        isRailMode = isRailMode,
                        onClick = { viewModel.showOnboardingFlowManually() }
                    )
                }

                // ------------------------------------------
                // SECTION: ENGINE SETTINGS & STORAGE WIDGETS
                // ------------------------------------------
                if (!isRailMode) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        EngineSettingsCard(
                            viewModel = viewModel,
                            isSettingsExpanded = isSettingsExpanded,
                            onToggleExpanded = { isSettingsExpanded = !isSettingsExpanded }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        RealtimeStorageWidgetCard(
                            textBytes = textBytes,
                            drawingBytes = drawingBytes,
                            voiceBytes = voiceBytes,
                            pdfBytes = pdfBytes,
                            totalStorageBytes = totalStorageBytes,
                            notesCount = notes.size,
                            syncedCount = syncedCount,
                            pendingCount = backupPendingCount,
                            isSyncing = viewModel.isSyncing,
                            lastSyncTime = viewModel.lastSyncTime,
                            onSyncClick = { viewModel.syncWithGoogleDrive() },
                            onManageClick = { onTabChange("sync") }
                        )
                    }
                }
            }

            // ==========================================
            // 5. BOTTOM FOOTER & CONTROLS
            // ==========================================
            if (!isRailMode) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(6.dp))
                SidebarFooter(
                    versionName = BuildConfig.VERSION_NAME,
                    onTourClick = { viewModel.showOnboardingFlowManually() },
                    onChangelogClick = { viewModel.showChangelogManually() }
                )
            } else {
                // Collapsed footer: Expand button with generous touch target (48dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isRailMode = false },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Expand Sidebar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS: DIRECTORY & TAG MANAGEMENT
    // ==========================================
    if (isCreatingDirectory || directoryToEdit != null) {
        DirectoryEditDialog(
            initialDirectory = directoryToEdit,
            defaultParentId = defaultParentForNewDir,
            allDirectories = viewModel.customDirectories,
            onDismiss = {
                isCreatingDirectory = false
                directoryToEdit = null
                defaultParentForNewDir = null
            },
            onSave = { name, parentId, colorHex ->
                if (directoryToEdit == null) {
                    viewModel.addDirectory(name, parentId, colorHex)
                } else {
                    viewModel.updateDirectory(directoryToEdit!!.id, name, parentId, colorHex)
                }
            },
            onDelete = { dirId ->
                viewModel.deleteDirectory(dirId)
            }
        )
    }

    if (isCreatingTag || tagToEdit != null) {
        TagEditDialog(
            initialTag = tagToEdit,
            onDismiss = {
                isCreatingTag = false
                tagToEdit = null
            },
            onSave = { name, colorHex, textColorHex ->
                if (tagToEdit == null) {
                    viewModel.addTag(name, colorHex, textColorHex)
                } else {
                    viewModel.updateTag(tagToEdit!!.id, name, colorHex, textColorHex)
                }
            },
            onDelete = { tagId ->
                viewModel.deleteTag(tagId)
            }
        )
    }
}

// ==========================================================
// SUB-COMPONENTS: HERO PROFILE CARD
// ==========================================================
private fun formatSidebarBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
private fun HeroProfileCard(
    isSignedIn: Boolean,
    accountName: String,
    accountEmail: String,
    photoUrl: String,
    initials: String,
    hasError: Boolean = false,
    authError: String? = null,
    streakDays: Int,
    totalStorageBytes: Long,
    notesCount: Int,
    pendingCount: Int,
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "Open Cloud Account Details",
                role = Role.Button
            )
            .testTag("user_account_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            1.dp,
            if (hasError) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with crisp border
                Surface(
                    shape = CircleShape,
                    color = if (hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(38.dp)
                        .border(
                            1.5.dp,
                            if (hasError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else Color(0xFF6366F1).copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSignedIn && photoUrl.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Picture",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = initials,
                                color = if (hasError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accountName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasError) Color(0xFFEF4444)
                                    else if (isSyncing) Color(0xFF3B82F6)
                                    else if (isSignedIn && pendingCount == 0) Color(0xFF10B981)
                                    else if (isSignedIn) Color(0xFFF59E0B)
                                    else Color(0xFF94A3B8)
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasError) "Auth Issue • Tap to fix"
                            else if (isSyncing) "Syncing..."
                            else if (isSignedIn && pendingCount == 0) "Google Connected ✓"
                            else if (isSignedIn) "$pendingCount Pending Sync"
                            else "Local Device Storage",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasError) Color(0xFFDC2626)
                            else if (isSyncing) Color(0xFF2563EB)
                            else if (isSignedIn && pendingCount == 0) Color(0xFF059669)
                            else if (isSignedIn) Color(0xFFD97706)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Cloud Status Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (hasError) Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF87171)))
                            else if (isSignedIn) Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                            else Brush.horizontalGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                        )
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = if (hasError) "ALERT" else if (isSignedIn) "CLOUD" else "LOCAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-status bar: Streak & Real Storage stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (streakDays > 0) "🔥 $streakDays Day Streak" else "🌱 Start Streak",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }

                Text(
                    text = "${formatSidebarBytes(totalStorageBytes)} • $notesCount notes",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================================
// SUB-COMPONENTS: SIDEBAR QUICK SEARCH
// ==========================================================
@Composable
private fun SidebarQuickSearchBox(
    notes: List<NoteEntity>,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit
) {
    val pdfCount = notes.count { it.templateType in listOf("pdf", "docx") }
    val audioCount = notes.count { !it.audioTranscription.isNullOrBlank() || !it.audioPath.isNullOrBlank() }
    val handwrittenCount = notes.count { it.templateType in listOf("blank", "ruled", "grid") }

    val placeholders = listOf(
        "Search notes...",
        "Search handwriting...",
        "Search PDFs & docs...",
        "Search audio recordings..."
    )
    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholders.size
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_hero_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clickable(
                        onClick = onToggleSearch,
                        onClickLabel = if (isSearchExpanded) "Collapse Search Filter" else "Expand Search Filter",
                        role = Role.Button
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Search",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val rotation by animateFloatAsState(
                    targetValue = if (isSearchExpanded) 180f else 0f,
                    label = "SearchRotation"
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isSearchExpanded) "Collapse Search" else "Expand Search",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // BasicTextField with minimum 44dp height for ergonomic tablet typing
                    BasicTextField(
                        value = searchKeyword,
                        onValueChange = onSearchChange,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (searchKeyword.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp)
                            .testTag("sidebar_search"),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = if (searchKeyword.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchKeyword.isEmpty()) {
                                        Text(
                                            text = placeholders[currentPlaceholderIndex],
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            style = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchKeyword.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Filter Chips with 32dp height
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SidebarFilterChip(
                            label = "📄 PDFs ($pdfCount)",
                            isSelected = searchKeyword == "pdf",
                            onClick = { onSearchChange(if (searchKeyword == "pdf") "" else "pdf") },
                            modifier = Modifier.weight(1f)
                        )
                        SidebarFilterChip(
                            label = "🎙️ Audio ($audioCount)",
                            isSelected = searchKeyword == "voice",
                            onClick = { onSearchChange(if (searchKeyword == "voice") "" else "voice") },
                            modifier = Modifier.weight(1f)
                        )
                        SidebarFilterChip(
                            label = "✍️ Draw ($handwrittenCount)",
                            isSelected = searchKeyword == "handwritten",
                            onClick = { onSearchChange(if (searchKeyword == "handwritten") "" else "handwritten") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = "Filter by $label"
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==========================================================
// SUB-COMPONENTS: SECTION HEADERS
// ==========================================================
@Composable
private fun SidebarSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
    )
}

// ==========================================================
// SUB-COMPONENTS: MATERIAL 3 EXPRESSIVE NAV ITEM
// ==========================================================
@Composable
private fun ExpressiveNavItem(
    icon: ImageVector,
    label: String,
    count: Int? = null,
    isSelected: Boolean = false,
    isRailMode: Boolean = false,
    badgeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "NavItemScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .scale(scale)
            .testTag("nav_item_${label.lowercase().replace(' ', '_')}")
            .semantics {
                this.contentDescription = "$label, ${if (isSelected) "Selected" else "Not selected"}${if (count != null) ", $count items" else ""}"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isRailMode) Arrangement.Center else Arrangement.Start,
            modifier = Modifier.padding(horizontal = if (isRailMode) 6.dp else 10.dp)
        ) {
            // Left Accent Bar Indicator
            if (isSelected && !isRailMode) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Tinted Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            // Label & Count Badge
            if (!isRailMode) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (count != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else badgeColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else badgeTextColor
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================
// SUB-COMPONENTS: DIRECTORY & TAG HIERARCHY ROWS
// ==========================================================
@Composable
fun CustomDirectorySidebarRow(
    directory: DirectoryItem,
    indentLevel: Int = 0,
    notes: List<NoteEntity>,
    isSelected: Boolean,
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onSelect: () -> Unit,
    onAddNote: () -> Unit,
    onAddSubdirectory: () -> Unit,
    onEdit: () -> Unit
) {
    val dirId = directory.id
    val dirName = directory.name
    val noteCount = notes.count { note ->
        note.tags.contains("dir:$dirId", ignoreCase = true) ||
        note.tags.contains(dirName, ignoreCase = true) ||
        note.title.contains(dirName, ignoreCase = true)
    }
    val indent = (indentLevel * 12).dp

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(42.dp)
            .semantics {
                this.contentDescription = "Folder ${directory.name}, $noteCount notes"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = { onToggleExpand?.invoke() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse Folder" else "Expand Folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }

            Icon(
                imageVector = if (indentLevel > 0) Icons.Default.SubdirectoryArrowRight else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(directory.colorHex),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = directory.name,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color(directory.colorHex),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (noteCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(directory.colorHex).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = noteCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(directory.colorHex)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(
                onClick = onAddNote,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = "Create note in ${directory.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options for ${directory.name}",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Add Note Here")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onAddNote()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Add Subdirectory")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onAddSubdirectory()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Edit / Change Color")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTagSidebarRow(
    tag: TagItem,
    notes: List<NoteEntity>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddNote: () -> Unit,
    onEdit: () -> Unit
) {
    val tagKey = tag.name
    val noteCount = notes.count { note ->
        note.tags.contains(tagKey, ignoreCase = true) ||
        note.tags.contains("tag:$tagKey", ignoreCase = true)
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                this.contentDescription = "Tag #${tag.name}, $noteCount notes"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(tag.colorHex))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "#${tag.name}",
                    color = Color(tag.textColorHex),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (noteCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(tag.colorHex).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = noteCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(tag.colorHex)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(
                onClick = onAddNote,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = "Create note tagged #${tag.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Tag #${tag.name}",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// ==========================================================
// SUB-COMPONENTS: ENGINE SETTINGS (MODERN CARD)
// ==========================================================
@Composable
private fun EngineSettingsCard(
    viewModel: NoteViewModel,
    isSettingsExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clickable(
                        onClick = onToggleExpanded,
                        onClickLabel = if (isSettingsExpanded) "Collapse Engine Settings" else "Expand Engine Settings",
                        role = Role.Button
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Engine Settings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val rotation by animateFloatAsState(
                    targetValue = if (isSettingsExpanded) 180f else 0f,
                    label = "SettingsRotation"
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isSettingsExpanded) "Collapse Settings" else "Expand Settings",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isSettingsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Smart Shapes toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Shapes", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("Snap sketches to polygons", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = viewModel.smartShapesEnabled,
                            onCheckedChange = { viewModel.smartShapesEnabled = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Infinite Canvas toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Infinite Canvas", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("Unlimited draw workspace", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = viewModel.canvasMode == "infinite",
                            onCheckedChange = {
                                viewModel.canvasMode = if (it) "infinite" else "fixed"
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme Mode Selector
                    Text("Theme Appearance", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemePillButton(
                            label = "☀️ Light",
                            isSelected = viewModel.themeMode == "light",
                            onClick = { viewModel.updateThemeMode("light") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemePillButton(
                            label = "🌙 Dark",
                            isSelected = viewModel.themeMode == "dark",
                            onClick = { viewModel.updateThemeMode("dark") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemePillButton(
                            label = "⚙️ Auto",
                            isSelected = viewModel.themeMode == "system",
                            onClick = { viewModel.updateThemeMode("system") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Pen Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Active Pen Color", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.outline)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(Color(viewModel.activeColor))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text("Active Stroke Width", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.outline)
                        Text("${viewModel.activeWidth.toInt()} px", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Over The Air Updates Widget
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Over-The-Air Updates", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    var showUrlInput by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure OTA Server", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (showUrlInput) "Hide" else "Show",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showUrlInput = !showUrlInput }
                        )
                    }

                    if (showUrlInput) {
                        Spacer(modifier = Modifier.height(4.dp))
                        var tempUrl by remember { mutableStateOf(viewModel.updateUrlSetting) }
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            label = { Text("Update JSON or APK URL", fontSize = 9.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.saveUpdateUrlSetting(tempUrl) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Save URL", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }

                    if (viewModel.updateStatusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.updateStatusMessage,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (viewModel.updateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    viewModel.updateProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = !viewModel.updateChecking && viewModel.updateProgress == null,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            if (viewModel.updateChecking) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                            } else {
                                Text("Check", fontSize = 10.5.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.showChangelogManually() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(34.dp)
                        ) {
                            Text("What's New", fontSize = 10.5.sp)
                        }

                        if (viewModel.updateAvailable) {
                            Button(
                                onClick = { viewModel.downloadAndInstallApk() },
                                enabled = viewModel.updateProgress == null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(34.dp)
                            ) {
                                Text("Install", fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePillButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = "Select $label theme"
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================================
// SUB-COMPONENTS: REALTIME STORAGE & CLOUD WIDGET
// ==========================================================
@Composable
private fun RealtimeStorageWidgetCard(
    textBytes: Long,
    drawingBytes: Long,
    voiceBytes: Long,
    pdfBytes: Long,
    totalStorageBytes: Long,
    notesCount: Int,
    syncedCount: Int,
    pendingCount: Int,
    isSyncing: Boolean,
    lastSyncTime: String,
    onSyncClick: () -> Unit,
    onManageClick: () -> Unit
) {
    // Proportional progress (assuming 50 MB local app sandbox quota)
    val maxQuotaBytes = 50L * 1024L * 1024L
    val progress = (totalStorageBytes.toFloat() / maxQuotaBytes.toFloat()).coerceIn(0.01f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onManageClick,
                role = Role.Button,
                onClickLabel = "Manage Storage and Cloud Sync"
            )
            .testTag("storage_widget_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Realtime Storage",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatSidebarBytes(totalStorageBytes),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-color breakdown bar
            val total = totalStorageBytes.coerceAtLeast(1L).toFloat()
            val textFraction = (textBytes.toFloat() / total).coerceIn(0f, 1f)
            val drawingFraction = (drawingBytes.toFloat() / total).coerceIn(0f, 1f)
            val voiceFraction = (voiceBytes.toFloat() / total).coerceIn(0f, 1f)
            val pdfFraction = (pdfBytes.toFloat() / total).coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (textFraction > 0f) {
                    Box(modifier = Modifier.weight(textFraction.coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFF3B82F6)))
                }
                if (drawingFraction > 0f) {
                    Box(modifier = Modifier.weight(drawingFraction.coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFF10B981)))
                }
                if (voiceFraction > 0f) {
                    Box(modifier = Modifier.weight(voiceFraction.coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFFF59E0B)))
                }
                if (pdfFraction > 0f) {
                    Box(modifier = Modifier.weight(pdfFraction.coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFFEC4899)))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Storage Legend Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageLegendItem(color = Color(0xFF3B82F6), label = "Text", sizeStr = formatSidebarBytes(textBytes))
                StorageLegendItem(color = Color(0xFF10B981), label = "Draw", sizeStr = formatSidebarBytes(drawingBytes))
                StorageLegendItem(color = Color(0xFFF59E0B), label = "Audio", sizeStr = formatSidebarBytes(voiceBytes))
                StorageLegendItem(color = Color(0xFFEC4899), label = "Docs", sizeStr = formatSidebarBytes(pdfBytes))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Sync Status & Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Syncing now...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (pendingCount > 0) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$pendingCount pending sync",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD97706)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$syncedCount/$notesCount notes backed up",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669)
                        )
                    }
                }

                Surface(
                    onClick = onSyncClick,
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isSyncing) "Syncing" else "Sync",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    sizeStr: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$label: $sizeStr",
            fontSize = 8.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================================
// SUB-COMPONENTS: MINIMAL FOOTER
// ==========================================================
@Composable
private fun SidebarFooter(
    versionName: String,
    onTourClick: () -> Unit,
    onChangelogClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Cloud Active",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "v$versionName",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.clickable(
                    onClick = onChangelogClick,
                    role = Role.Button,
                    onClickLabel = "View Changelog"
                )
            )
            Text(
                text = "Tour",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    onClick = onTourClick,
                    role = Role.Button,
                    onClickLabel = "Open App Tour"
                )
            )
        }
    }
}
