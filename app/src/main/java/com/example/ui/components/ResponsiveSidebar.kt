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
import com.example.data.NoteEntity

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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "by Aditya Kumar",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 2. User Account Card (Custom Dynamic Session)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onTabChange("sync") }
                    .testTag("user_account_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(12.dp)
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
                            Text(
                                text = "RC",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ramprit Choudhary",
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
                                    .background(Color(0xFF4CAF50)) // Connected green dot
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "rampritchoudhary16281@gmail.com",
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
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
                        }
                    )
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
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    if (viewModel.updateChecking) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    } else {
                                        Text("Check", fontSize = 10.sp)
                                    }
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
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null,
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
                        .width(3.dp)
                        .height(18.dp)
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
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
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
