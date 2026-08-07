package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DirectoryItem
import com.example.data.NoteEntity
import com.example.data.TagItem

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
    modifier: Modifier = Modifier
) {
    var isCollapsed by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var isAiToolsExpanded by remember { mutableStateOf(true) }
    var isNestedDirectoriesExpanded by remember { mutableStateOf(true) }
    var isColoredTagsExpanded by remember { mutableStateOf(true) }
    var expandedDirectories by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Directory & Tag Management Dialog state
    var directoryToEdit by remember { mutableStateOf<DirectoryItem?>(null) }
    var isCreatingDirectory by remember { mutableStateOf(false) }
    var defaultParentForNewDir by remember { mutableStateOf<String?>(null) }
    var tagToEdit by remember { mutableStateOf<TagItem?>(null) }
    var isCreatingTag by remember { mutableStateOf(false) }

    // Dialog state for Feedback & About
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Counts for Folder Badges
    val allCount = notes.size
    val handwrittenCount = notes.count { it.templateType in listOf("blank", "ruled", "grid") }
    val pdfCount = notes.count { it.templateType in listOf("pdf", "docx") }
    val templatesCount = notes.count { it.templateType in listOf("cornell", "meeting") }
    val flashcardCount = notes.count { it.title.contains("flashcard", ignoreCase = true) || it.tags.contains("flashcard", ignoreCase = true) }
    val plannerCount = notes.count { it.templateType == "planner" || it.title.contains("planner", ignoreCase = true) }
    val mindMapCount = notes.count { it.drawingData.isNotBlank() }
    val backupPendingCount = notes.count { !it.isSynced }

    val context = LocalContext.current
    val isSignedIn = GoogleDriveBackupHelper.isSignedIn(context)
    val accountName = GoogleDriveBackupHelper.getSavedAccountName(context)
    val accountEmail = GoogleDriveBackupHelper.getSavedAccountEmail(context)
    val photoUrl = GoogleDriveBackupHelper.getSavedPhotoUrl(context)

    // Dynamic calculated storage
    val calculatedStorageBytes = remember(notes) {
        notes.sumOf { (it.title.length + it.content.length + it.drawingData.length + (it.summary?.length ?: 0)).toLong() * 2000L + 500000L }
    }
    val usedStorageMb = (calculatedStorageBytes / (1024f * 1024f)).coerceAtLeast(23.8f)

    val isDark = isSystemInDarkTheme() || viewModel.themeMode == "dark"
    val sidebarBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val animatedWidth by animateDpAsState(
        targetValue = if (isCollapsed) 78.dp else 312.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "SidebarWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(animatedWidth)
            .testTag("responsive_sidebar"),
        color = sidebarBg,
        tonalElevation = 1.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        border = BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isCollapsed) 8.dp else 14.dp, vertical = 16.dp)
        ) {
            // ==========================================
            // 1. BRANDING & COLLAPSE HEADER
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (!isCollapsed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTabChange("home") }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Lipi Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lipi",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary,
                                    letterSpacing = (-0.5).sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Expressive Note Studio",
                                fontSize = 10.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onTabChange("home") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Lipi Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isCollapsed = !isCollapsed },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(1.dp, borderStrokeColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.MenuOpen,
                        contentDescription = if (isCollapsed) "Expand Sidebar" else "Collapse Sidebar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ==========================================
            // 2. PROFILE SECTION (FLOATING PROFILE CARD)
            // ==========================================
            if (!isCollapsed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clickable { onTabChange("sync") }
                        .testTag("user_account_card"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, borderStrokeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Large Avatar with status ring
                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSignedIn && photoUrl.isNotBlank()) {
                                            coil.compose.AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "Profile Picture",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        } else {
                                            val initials = if (isSignedIn && accountName.isNotBlank()) {
                                                accountName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").ifEmpty { "A" }
                                            } else "AK"
                                            Text(
                                                text = initials,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(if (isSignedIn) Color(0xFF10B981) else Color(0xFFF59E0B))
                                        .border(2.dp, cardBg, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSignedIn && accountName.isNotBlank()) accountName else "Aditya Kumar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSignedIn && accountEmail.isNotBlank()) accountEmail else "aditya.lipi@google.com",
                                    fontSize = 10.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = if (isSignedIn) "Google Connected ✓" else "Tap to Sign In",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSignedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onTabChange("sync") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Account Settings",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats & Badges Row (Streak + Storage + Plan)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Streak Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF7ED),
                                border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = "Streak",
                                        tint = Color(0xFFF97316),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "12 Day Streak",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C)
                                    )
                                }
                            }

                            // Pro Plan Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Pro Plan",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Pro Plan",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3730A3)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mini Storage indicator
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Storage",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary
                                )
                                Text(
                                    text = "${String.format("%.1f", usedStorageMb)} MB / 50 GB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = (usedStorageMb / 50000f).coerceIn(0.05f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = borderStrokeColor
                            )
                        }
                    }
                }
            } else {
                // Collapsed Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .size(48.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        .clickable { onTabChange("sync") }
                        .align(Alignment.CenterHorizontally)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSignedIn && photoUrl.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = "AK",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. HERO AI SEARCH FIELD
            // ==========================================
            var isSearchFocused by remember { mutableStateOf(false) }

            if (!isCollapsed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(
                        width = if (isSearchFocused || searchKeyword.isNotEmpty()) 1.5.dp else 1.dp,
                        brush = if (isSearchFocused) Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ) else Brush.linearGradient(listOf(borderStrokeColor, borderStrokeColor))
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSearchFocused) 4.dp else 0.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Header Search Input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(sidebarBg, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Search Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchKeyword.isEmpty()) {
                                    Text(
                                        text = "Ask Lipi AI...",
                                        fontSize = 12.sp,
                                        color = textSecondary,
                                        style = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic)
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchKeyword,
                                    onValueChange = onSearchChange,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textPrimary
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isSearchFocused = it.isFocused }
                                        .testTag("sidebar_search")
                                )
                            }
                            if (searchKeyword.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // AI Chips Quick Filter Bar
                        Text(
                            text = "SMART INDEXES",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )

                        val quickChips = listOf(
                            "handwritten" to "Handwriting",
                            "pdf" to "PDFs",
                            "voice" to "Voice Notes",
                            "diagram" to "Diagrams",
                            "question" to "Ask Questions"
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 72.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    quickChips.forEach { (key, label) ->
                                        val isSelected = searchKeyword.contains(key, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else sidebarBg,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else borderStrokeColor
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    onSearchChange(if (isSelected) "" else key)
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else textSecondary)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Collapsed Search Icon
                IconButton(
                    onClick = {
                        isCollapsed = false
                        onTabChange("notes")
                    },
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(1.dp, borderStrokeColor, CircleShape)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ==========================================
            // 4. MAIN NAVIGATION SCROLLABLE CONTENT
            // ==========================================
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ---------------- WORKSPACES SECTION ----------------
                item {
                    if (!isCollapsed) {
                        SectionLabel("WORKSPACES")
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Home,
                        outlinedIcon = Icons.Outlined.Home,
                        label = "Home Dashboard",
                        isSelected = activeTab == "home",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("home")
                            onFilterChange("All Notes")
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Folder,
                        outlinedIcon = Icons.Outlined.Folder,
                        label = "All Notes",
                        count = allCount,
                        isSelected = activeTab == "notes" && selectedFilter == "All Notes",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("All Notes")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Edit,
                        outlinedIcon = Icons.Outlined.Edit,
                        label = "Handwritten",
                        count = handwrittenCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Handwritten",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Handwritten")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.PictureAsPdf,
                        outlinedIcon = Icons.Outlined.PictureAsPdf,
                        label = "PDF Notes & Docs",
                        count = pdfCount,
                        isSelected = activeTab == "notes" && selectedFilter == "PDFs",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("PDFs")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Description,
                        outlinedIcon = Icons.Outlined.Description,
                        label = "Templates",
                        count = templatesCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Templates",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Templates")
                            viewModel.selectNote(null)
                        }
                    )
                }

                // ---------------- LIBRARIES & TOOLS SECTION ----------------
                item {
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("LIBRARIES & TOOLS")
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Style,
                        outlinedIcon = Icons.Outlined.Style,
                        label = "Flashcards",
                        count = flashcardCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Flashcards",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Flashcards")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.DateRange,
                        outlinedIcon = Icons.Outlined.DateRange,
                        label = "Planner",
                        count = plannerCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Planner",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Planner")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.AccountTree,
                        outlinedIcon = Icons.Outlined.AccountTree,
                        label = "Mind Maps",
                        count = mindMapCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Mind Maps",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Mind Maps")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.AutoAwesome,
                        outlinedIcon = Icons.Outlined.AutoAwesome,
                        label = "AI Assistant",
                        count = notes.count { !it.summary.isNullOrBlank() },
                        isSelected = activeTab == "ai",
                        isCollapsed = isCollapsed,
                        badgeColor = MaterialTheme.colorScheme.primaryContainer,
                        badgeTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { onTabChange("ai") }
                    )
                }

                // ---------------- PROJECTS & FOLDER TREE ----------------
                item {
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isNestedDirectoriesExpanded = !isNestedDirectoriesExpanded }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (isNestedDirectoriesExpanded) 180f else 0f,
                                    label = "DirRotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle Folders",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp).rotate(rotation)
                                )
                                Text(
                                    text = "PROJECTS & FOLDERS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    defaultParentForNewDir = null
                                    isCreatingDirectory = true
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "New Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                if (!isCollapsed && isNestedDirectoriesExpanded) {
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
                                onEdit = { directoryToEdit = dir }
                            )
                        }
                    }
                }

                // ---------------- COLORED TAGS ----------------
                item {
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isColoredTagsExpanded = !isColoredTagsExpanded }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (isColoredTagsExpanded) 180f else 0f,
                                    label = "TagRotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle Tags",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp).rotate(rotation)
                                )
                                Text(
                                    text = "TAGS & LABELS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            IconButton(
                                onClick = { isCreatingTag = true },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Tag",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                if (!isCollapsed && isColoredTagsExpanded) {
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
                                onEdit = { tagToEdit = tag }
                            )
                        }
                    }
                }

                // ---------------- SYSTEM & ANALYTICS ----------------
                item {
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("SYSTEM & ANALYTICS")
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Analytics,
                        outlinedIcon = Icons.Outlined.Analytics,
                        label = "Analytics",
                        isSelected = activeTab == "home" && selectedFilter == "Analytics",
                        isCollapsed = isCollapsed,
                        onClick = {
                            onTabChange("home")
                            onFilterChange("Analytics")
                        }
                    )
                }

                item {
                    NavCardItem(
                        icon = Icons.Filled.Cloud,
                        outlinedIcon = Icons.Outlined.Cloud,
                        label = "Backup & Sync",
                        count = backupPendingCount,
                        isSelected = activeTab == "sync",
                        isCollapsed = isCollapsed,
                        badgeColor = if (backupPendingCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        badgeTextColor = if (backupPendingCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { onTabChange("sync") }
                    )
                }
            }

            // ==========================================
            // 5. ENGINE SETTINGS CARD
            // ==========================================
            if (!isCollapsed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderStrokeColor),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSettingsExpanded = !isSettingsExpanded }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Engine Settings",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "ENGINE SETTINGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                            val rotation by animateFloatAsState(targetValue = if (isSettingsExpanded) 180f else 0f)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Expand Settings",
                                modifier = Modifier.size(18.dp).rotate(rotation),
                                tint = textSecondary
                            )
                        }

                        AnimatedVisibility(
                            visible = isSettingsExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Smart Shapes toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Smart Shapes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text("Auto-snap sketches to polygons", fontSize = 9.sp, color = textSecondary)
                                    }
                                    Switch(
                                        checked = viewModel.smartShapesEnabled,
                                        onCheckedChange = { viewModel.smartShapesEnabled = it },
                                        modifier = Modifier.scale(0.7f)
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
                                        Text("Infinite Canvas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text("Unlimited draw workspace", fontSize = 9.sp, color = textSecondary)
                                    }
                                    Switch(
                                        checked = viewModel.canvasMode == "infinite",
                                        onCheckedChange = { viewModel.canvasMode = if (it) "infinite" else "fixed" },
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Theme Mode Selector
                                Column {
                                    Text("Theme Mode", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(sidebarBg, RoundedCornerShape(12.dp))
                                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (mode, label) ->
                                            val isSelected = viewModel.themeMode == mode
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { viewModel.updateThemeMode(mode) }
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else textSecondary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Active Pen Color & Thickness
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Pen Color:", fontSize = 10.sp, color = textSecondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(viewModel.activeColor))
                                                .border(1.dp, borderStrokeColor, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = "Size: ${viewModel.activeWidth.toInt()}px",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 6. STORAGE WIDGET & FOOTER
            // ==========================================
            if (!isCollapsed) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = borderStrokeColor)
                Spacer(modifier = Modifier.height(8.dp))

                // Minimal Footer Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSignedIn) Color(0xFF10B981) else Color(0xFF64748B))
                        )
                        Text(
                            text = if (isSignedIn) "Cloud Connected" else "Offline Mode",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Help",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showHelpDialog = true }
                        )
                        Text(
                            text = "About",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showAboutDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lipi Studio v1.2.0 • Android 16 M3",
                    fontSize = 8.5.sp,
                    color = textSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Help & Support Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Lipi Workspace Guide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Tap any note to open or edit instantly.")
                    Text("• Use AI Search to query handwriting, PDFs & voice transcriptions.")
                    Text("• Organize with nested directories and colored tags.")
                    Text("• Backup & Sync your notes directly to Google Drive.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Lipi Expressive Studio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Lipi is designed with Google Material 3 Expressive guidelines for high-productivity Android tablet note-taking.")
                    Text("Version: 1.2.0 (Build 12)")
                    Text("Designed by Aditya Kumar")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Directory & Tag Management Dialogs
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
    val indent = (indentLevel * 14).dp

    val containerBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "DirRowBg"
    )

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 1.dp, bottom = 1.dp)
            .height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = { onToggleExpand?.invoke() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = "Toggle Folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(32.dp))
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
                        .padding(horizontal = 7.dp, vertical = 2.dp)
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "Add Note to Directory",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Directory Options",
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
                                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
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
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
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
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
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

    val containerBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "TagRowBg"
    )

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(tag.colorHex))
                    .padding(horizontal = 7.dp, vertical = 3.5.dp)
            ) {
                Text(
                    text = "#${tag.name}",
                    color = Color(tag.textColorHex),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Spacer(modifier = Modifier.weight(1f))

            if (noteCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(tag.colorHex).copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "Add Note with Tag",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Tag",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun NavCardItem(
    icon: ImageVector,
    outlinedIcon: ImageVector = icon,
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    isCollapsed: Boolean,
    badgeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    val containerBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "NavItemBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "NavItemColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerBg,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCollapsed) 48.dp else 44.dp)
            .padding(vertical = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.Start,
            modifier = Modifier.padding(horizontal = if (isCollapsed) 0.dp else 10.dp)
        ) {
            if (isSelected && !isCollapsed) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = if (isSelected) icon else outlinedIcon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(20.dp)
            )

            if (!isCollapsed) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else contentColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (count != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else badgeColor)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else badgeTextColor
                        )
                    }
                }
            }
        }
    }
}
