package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var isAiToolsExpanded by remember { mutableStateOf(true) }

    // Directory & Tag Management Dialog state
    var directoryToEdit by remember { mutableStateOf<DirectoryItem?>(null) }
    var isCreatingDirectory by remember { mutableStateOf(false) }
    var defaultParentForNewDir by remember { mutableStateOf<String?>(null) }
    var tagToEdit by remember { mutableStateOf<TagItem?>(null) }
    var isCreatingTag by remember { mutableStateOf(false) }

    // Counts for Folder Badges
    val allCount = notes.size
    val handwrittenCount = notes.count { it.templateType in listOf("blank", "ruled", "grid") }
    val pdfCount = notes.count { it.templateType in listOf("pdf", "docx") }
    val templatesCount = notes.count { it.templateType in listOf("cornell", "meeting") }
    val backupPendingCount = notes.count { !it.isSynced }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .testTag("responsive_sidebar"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // 1. Sidebar Header & Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Lipi Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 6.dp)
                )
                Column {
                    Text(
                        text = "Lipi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "by Aditya Kumar",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // 2. User Account Card (Custom Dynamic Session)
            val context = androidx.compose.ui.platform.LocalContext.current
            val isSignedIn = GoogleDriveBackupHelper.isSignedIn(context)
            val accountName = GoogleDriveBackupHelper.getSavedAccountName(context)
            val accountEmail = GoogleDriveBackupHelper.getSavedAccountEmail(context)
            val photoUrl = GoogleDriveBackupHelper.getSavedPhotoUrl(context)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onTabChange("sync") }
                    .testTag("user_account_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
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
                                    accountName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").ifEmpty { "G" }
                                } else "G"
                                Text(
                                    text = initials,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isSignedIn) accountName else "Guest User",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSignedIn) Color(0xFF4CAF50) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSignedIn && accountEmail.isNotBlank()) accountEmail else "Tap to Sign In",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. AI Search Tools (Quick Input + AI Tags)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAiToolsExpanded = !isAiToolsExpanded }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI Semantic Search",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val rotation by animateFloatAsState(targetValue = if (isAiToolsExpanded) 180f else 0f)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isAiToolsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            // AI Query Input Box with zero clipping & full vertical alignment
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchKeyword,
                                onValueChange = onSearchChange,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (searchKeyword.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(20.dp)
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
                                            contentDescription = "Search",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                            if (searchKeyword.isEmpty()) {
                                                Text(
                                                    text = "Search drawing, voice & notes...",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    style = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic)
                                                )
                                            }
                                            innerTextField()
                                        }
                                        if (searchKeyword.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onSearchChange("") },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick AI Filters
                            Text(
                                text = "QUICK SMART INDEXES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Has AI summary tag
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (searchKeyword == "summary") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            onSearchChange(if (searchKeyword == "summary") "" else "summary")
                                        }
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Summaries",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (searchKeyword == "summary") MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Has voice dictation tag
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (searchKeyword == "voice") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            onSearchChange(if (searchKeyword == "voice") "" else "voice")
                                        }
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Voice Dict",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (searchKeyword == "voice") MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Note folders/categories list
            Text(
                text = "FOLDERS & LABELS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    FolderItem(
                        icon = Icons.Default.Home,
                        label = "Home Dashboard",
                        isSelected = activeTab == "home",
                        onClick = {
                            onTabChange("home")
                            onFilterChange("All Notes")
                        }
                    )
                }
                item {
                    FolderItem(
                        icon = Icons.Default.Folder,
                        label = "All Notes",
                        count = allCount,
                        isSelected = activeTab == "notes" && selectedFilter == "All Notes",
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("All Notes")
                            viewModel.selectNote(null)
                        }
                    )
                }
                item {
                    FolderItem(
                        icon = Icons.Default.Edit,
                        label = "Handwritten",
                        count = handwrittenCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Handwritten",
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Handwritten")
                            viewModel.selectNote(null)
                        }
                    )
                }
                item {
                    FolderItem(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Imported PDFs & Docs",
                        count = pdfCount,
                        isSelected = activeTab == "notes" && selectedFilter == "PDFs",
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("PDFs")
                            viewModel.selectNote(null)
                        }
                    )
                }
                item {
                    FolderItem(
                        icon = Icons.Default.Description,
                        label = "Structural Templates",
                        count = templatesCount,
                        isSelected = activeTab == "notes" && selectedFilter == "Templates",
                        onClick = {
                            onTabChange("notes")
                            onFilterChange("Templates")
                            viewModel.selectNote(null)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NESTED DIRECTORIES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        IconButton(
                            onClick = {
                                defaultParentForNewDir = null
                                isCreatingDirectory = true
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add Directory",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Render Root level directories and their nested subdirectories
                val rootDirectories = viewModel.customDirectories.filter { it.parentId == null }
                rootDirectories.forEach { rootDir ->
                    item(key = "dir_${rootDir.id}") {
                        CustomDirectorySidebarRow(
                            directory = rootDir,
                            parentDirectoryName = null,
                            notes = notes,
                            isSelected = activeTab == "notes" && (selectedFilter == "dir:${rootDir.id}" || selectedFilter == rootDir.name),
                            onSelect = {
                                onTabChange("notes")
                                onFilterChange("dir:${rootDir.id}")
                                viewModel.selectNote(null)
                            },
                            onAddNote = {
                                viewModel.addNoteToDirectory(rootDir)
                                onTabChange("notes")
                            },
                            onAddSubdirectory = {
                                defaultParentForNewDir = rootDir.id
                                isCreatingDirectory = true
                            },
                            onEdit = {
                                directoryToEdit = rootDir
                            }
                        )
                    }

                    val childDirectories = viewModel.customDirectories.filter { it.parentId == rootDir.id }
                    childDirectories.forEach { childDir ->
                        item(key = "dir_${childDir.id}") {
                            CustomDirectorySidebarRow(
                                directory = childDir,
                                parentDirectoryName = rootDir.name,
                                notes = notes,
                                isSelected = activeTab == "notes" && (selectedFilter == "dir:${childDir.id}" || selectedFilter == childDir.name),
                                onSelect = {
                                    onTabChange("notes")
                                    onFilterChange("dir:${childDir.id}")
                                    viewModel.selectNote(null)
                                },
                                onAddNote = {
                                    viewModel.addNoteToDirectory(childDir)
                                    onTabChange("notes")
                                },
                                onAddSubdirectory = {
                                    defaultParentForNewDir = childDir.id
                                    isCreatingDirectory = true
                                },
                                onEdit = {
                                    directoryToEdit = childDir
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLORED TAGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        IconButton(
                            onClick = { isCreatingTag = true },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Tag",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

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

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "INTEGRATIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                }

                item {
                    FolderItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "AI Summaries Center",
                        count = notes.count { !it.summary.isNullOrBlank() },
                        isSelected = activeTab == "ai",
                        onClick = { onTabChange("ai") }
                    )
                }

                item {
                    FolderItem(
                        icon = Icons.Default.Cloud,
                        label = "Cloud Backup & Sync",
                        count = backupPendingCount,
                        isSelected = activeTab == "sync",
                        badgeColor = if (backupPendingCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        badgeTextColor = if (backupPendingCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { onTabChange("sync") }
                    )
                }

                item {
                    FolderItem(
                        icon = Icons.Default.Info,
                        label = "App Tour & Guide",
                        isSelected = false,
                        onClick = { viewModel.showOnboardingFlowManually() }
                    )
                }
            }

            // 5. Stylus Smart settings panel (Integrated inside the sidebar!)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
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
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Engine Settings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val rotation by animateFloatAsState(targetValue = if (isSettingsExpanded) 180f else 0f)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Settings",
                            modifier = Modifier
                                .size(18.dp)
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
                                    Text("Smart Shapes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Snap sketches to polygons", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = viewModel.smartShapesEnabled,
                                    onCheckedChange = { viewModel.smartShapesEnabled = it },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Infinite Canvas toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Infinite Canvas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Unlimited draw workspace", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = viewModel.canvasMode == "infinite",
                                    onCheckedChange = {
                                        viewModel.canvasMode = if (it) "infinite" else "fixed"
                                    },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Theme Mode Selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Theme Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = when (viewModel.themeMode) {
                                            "dark" -> "Dark Mode"
                                            "light" -> "Light Mode"
                                            else -> "System Mode"
                                        },
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateThemeMode("light") },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (viewModel.themeMode == "light") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = "Light Mode",
                                            tint = if (viewModel.themeMode == "light") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateThemeMode("dark") },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (viewModel.themeMode == "dark") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NightsStay,
                                            contentDescription = "Dark Mode",
                                            tint = if (viewModel.themeMode == "dark") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateThemeMode("system") },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (viewModel.themeMode == "system") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "System Theme",
                                            tint = if (viewModel.themeMode == "system") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Settings Info
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pen Color", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(viewModel.activeColor))
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text("Pen Thickness", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${viewModel.activeWidth.toInt()} px", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // App Updates title
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
                                Text("Over-The-Air Updates", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Installed: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Update URL input field
                            var showUrlInput by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Configure OTA Server", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (showUrlInput) "Hide" else "Show",
                                    fontSize = 9.sp,
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
                                    label = { Text("Update JSON or APK URL", fontSize = 8.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.saveUpdateUrlSetting(tempUrl) }) {
                                            Icon(Icons.Default.Save, contentDescription = "Save URL", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Supports JSON manifests or direct APK download links.",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Status message
                            Text(
                                text = viewModel.updateStatusMessage,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (viewModel.updateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Progress bar
                            viewModel.updateProgress?.let { progress ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Column {
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}% downloaded",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.checkForUpdates() },
                                    enabled = !viewModel.updateChecking && viewModel.updateProgress == null,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    if (viewModel.updateChecking) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    } else {
                                        Text("Check", fontSize = 10.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.showChangelogManually() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.2f).height(32.dp)
                                ) {
                                    Text("What's New", fontSize = 10.sp)
                                }

                                if (viewModel.updateAvailable) {
                                    Button(
                                        onClick = { viewModel.downloadAndInstallApk() },
                                        enabled = viewModel.updateProgress == null,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1.2f).height(32.dp)
                                    ) {
                                        Text("Install v${viewModel.updateVersionName}", fontSize = 10.sp)
                                    }
                                } else if (viewModel.updateUrlSetting.endsWith(".apk", ignoreCase = true)) {
                                    Button(
                                        onClick = { viewModel.downloadAndInstallApk() },
                                        enabled = viewModel.updateProgress == null,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1.2f).height(32.dp)
                                    ) {
                                        Text("Download APK", fontSize = 10.sp)
                                    }
                                }
                            }

                            if (viewModel.updateAvailable && viewModel.updateNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("What's New:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(viewModel.updateNotes, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
    parentDirectoryName: String? = null,
    notes: List<NoteEntity>,
    isSelected: Boolean,
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
    val indent = if (parentDirectoryName != null) 16.dp else 0.dp

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = if (parentDirectoryName != null) Icons.Default.SubdirectoryArrowRight else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(directory.colorHex),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = directory.name,
                fontSize = 12.sp,
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
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "Add Note to Directory",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Directory Options",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
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

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(tag.colorHex))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "#${tag.name}",
                    color = Color(tag.textColorHex),
                    fontSize = 11.sp,
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
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "Add Note with Tag",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Tag",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun FolderItem(
    icon: ImageVector,
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    badgeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 12.sp,
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
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else badgeColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
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
