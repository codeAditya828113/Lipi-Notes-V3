package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class PageTemplateOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "Standard", "Study & Work", "Specialty"
    val icon: ImageVector,
    val defaultColor: Long = 0xFFFFFFFFL
)

val ALL_PAGE_TEMPLATES = listOf(
    // Standard
    PageTemplateOption("blank", "Blank Canvas", "Pure clean unlined page", "Standard", Icons.Default.CropPortrait),
    PageTemplateOption("ruled", "Standard Ruled", "Lined with header & left margin", "Standard", Icons.Default.FormatAlignLeft),
    PageTemplateOption("grid", "Square Grid (5mm)", "Math & precision graph paper", "Standard", Icons.Default.GridOn),
    PageTemplateOption("dotted", "Dot Grid", "5mm dots for bullet journaling", "Standard", Icons.Default.Grain),
    PageTemplateOption("engineering", "Engineering Grid", "Accent major/minor grid lines", "Standard", Icons.Default.BorderAll),

    // Study & Work
    PageTemplateOption("cornell", "Cornell Notes", "Cue column, notes, and summary footer", "Study & Work", Icons.Default.School),
    PageTemplateOption("meeting", "Meeting Minutes", "Date, agenda, decisions & action items", "Study & Work", Icons.Default.Group),
    PageTemplateOption("daily_planner", "Daily Planner", "Hourly timeline & prioritized tasks", "Study & Work", Icons.Default.CalendarToday),
    PageTemplateOption("two_column", "Two-Column Split", "Comparison & vocabulary columns", "Study & Work", Icons.Default.ViewColumn),

    // Specialty
    PageTemplateOption("legal", "Legal Pad", "Canary yellow with red double margin", "Specialty", Icons.Default.Description, 0xFFFFFDE7L),
    PageTemplateOption("music", "Music Manuscript", "5-line musical staves for notation", "Specialty", Icons.Default.MusicNote),
    PageTemplateOption("blackboard", "Dark Blackboard", "Chalkboard dark slate canvas", "Specialty", Icons.Default.DarkMode, 0xFF1E293BL)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LipiPageOverviewDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val note = viewModel.selectedNote ?: return
    val totalPages = viewModel.pdfPageCount
    val currentPage = viewModel.pdfPage

    var filterMode by remember { mutableStateOf("all") } // "all", "bookmarked"
    var showAddPageModal by remember { mutableStateOf(false) }
    var insertTargetIndex by remember { mutableIntStateOf(totalPages + 1) }
    var pageToChangeTemplate by remember { mutableStateOf<Int?>(null) }
    var pageToDeleteConfirm by remember { mutableStateOf<Int?>(null) }
    var pageToClearConfirm by remember { mutableStateOf<Int?>(null) }

    val allPageNumbers = remember(totalPages) { (1..totalPages).toList() }
    val filteredPages = remember(allPageNumbers, filterMode, viewModel.bookmarkedPagesSet) {
        if (filterMode == "bookmarked") {
            allPageNumbers.filter { viewModel.isPageBookmarked(it) }
        } else {
            allPageNumbers
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Overview",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Page Overview",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${note.title} • $totalPages ${if (totalPages == 1) "Page" else "Pages"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Add Page Action Button
                            Button(
                                onClick = {
                                    insertTargetIndex = totalPages + 1
                                    showAddPageModal = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("overview_add_page_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Page", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = filterMode == "all",
                                onClick = { filterMode = "all" },
                                label = { Text("All Pages ($totalPages)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ViewModule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            FilterChip(
                                selected = filterMode == "bookmarked",
                                onClick = { filterMode = "bookmarked" },
                                label = { Text("Bookmarked (${viewModel.bookmarkedPagesSet.size})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (viewModel.bookmarkedPagesSet.isNotEmpty()) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (viewModel.bookmarkedPagesSet.isNotEmpty()) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }

                // Grid Body
                if (filteredPages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No bookmarked pages found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap the bookmark ribbon on any page card to save it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredPages, key = { it }) { pageNum ->
                            val templateName = viewModel.getPageTemplate(pageNum)
                            val isCurrent = pageNum == currentPage
                            val isBookmarked = viewModel.isPageBookmarked(pageNum)
                            val strokeCount = viewModel.currentStrokes.count { it.page == pageNum }
                            val imageCount = viewModel.currentImages.count { it.page == pageNum }
                            val blockCount = viewModel.currentContentBlocks.count { it.page == pageNum }

                            PageThumbnailCard(
                                pageNum = pageNum,
                                totalPages = totalPages,
                                isCurrent = isCurrent,
                                isBookmarked = isBookmarked,
                                templateName = templateName,
                                strokeCount = strokeCount,
                                imageCount = imageCount,
                                blockCount = blockCount,
                                pageBgColor = Color(note.pageColor),
                                onClick = {
                                    viewModel.setPDFPage(pageNum)
                                    onDismiss()
                                },
                                onToggleBookmark = {
                                    viewModel.togglePageBookmark(pageNum)
                                },
                                onInsertAfter = {
                                    insertTargetIndex = pageNum + 1
                                    showAddPageModal = true
                                },
                                onDuplicate = {
                                    viewModel.duplicatePage(pageNum)
                                },
                                onChangeTemplate = {
                                    pageToChangeTemplate = pageNum
                                },
                                onMoveLeft = {
                                    if (pageNum > 1) {
                                        viewModel.reorderPage(pageNum, pageNum - 1)
                                    }
                                },
                                onMoveRight = {
                                    if (pageNum < totalPages) {
                                        viewModel.reorderPage(pageNum, pageNum + 1)
                                    }
                                },
                                onClearContent = {
                                    pageToClearConfirm = pageNum
                                },
                                onDelete = {
                                    pageToDeleteConfirm = pageNum
                                }
                            )
                        }

                        // Add Page card at the end
                        item {
                            AddPageCard(
                                onClick = {
                                    insertTargetIndex = totalPages + 1
                                    showAddPageModal = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Add New Page with Template Picker
    if (showAddPageModal) {
        LipiAddPageTemplateDialog(
            defaultInsertIndex = insertTargetIndex,
            totalPages = totalPages,
            currentNoteDefaultTemplate = note.templateType,
            onDismiss = { showAddPageModal = false },
            onPageCreated = { targetIdx, chosenTemplate ->
                viewModel.addPage(atIndex = targetIdx, template = chosenTemplate)
                showAddPageModal = false
            }
        )
    }

    // Modal: Change Template for specific existing page
    if (pageToChangeTemplate != null) {
        val targetPage = pageToChangeTemplate!!
        val currentTpl = viewModel.getPageTemplate(targetPage)

        LipiSelectPageTemplateSheet(
            title = "Change Template for Page $targetPage",
            currentTemplate = currentTpl,
            onDismiss = { pageToChangeTemplate = null },
            onTemplateSelected = { newTpl ->
                viewModel.setPageTemplate(targetPage, newTpl)
                pageToChangeTemplate = null
            }
        )
    }

    // Confirmation: Delete Page
    if (pageToDeleteConfirm != null) {
        val pageNum = pageToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { pageToDeleteConfirm = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Page $pageNum?") },
            text = {
                Text(
                    if (totalPages <= 1) {
                        "This is the only page in the notebook. Deleting it will clear all drawings and content on this page."
                    } else {
                        "Are you sure you want to permanently delete Page $pageNum? All drawings, text, and images on this page will be removed."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePage(pageNum)
                        pageToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Clear Page Content
    if (pageToClearConfirm != null) {
        val pageNum = pageToClearConfirm!!
        AlertDialog(
            onDismissRequest = { pageToClearConfirm = null },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Page $pageNum Content?") },
            text = { Text("All strokes, drawings, and images on Page $pageNum will be erased. The page itself will be kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPageContent(pageNum)
                        pageToClearConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Ink")
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToClearConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PageThumbnailCard(
    pageNum: Int,
    totalPages: Int,
    isCurrent: Boolean,
    isBookmarked: Boolean,
    templateName: String,
    strokeCount: Int,
    imageCount: Int,
    blockCount: Int,
    pageBgColor: Color,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onInsertAfter: () -> Unit,
    onDuplicate: () -> Unit,
    onChangeTemplate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onClearContent: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isCurrent) {
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Badge Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Number + Current indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Page $pageNum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isCurrent) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Bookmark Icon Button
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Toggle Bookmark",
                        tint = if (isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Paper Preview Visual Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(pageBgColor)
                    .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            ) {
                // Vector Template Pattern Simulation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val lineColor = Color(0xFF94A3B8).copy(alpha = 0.4f)
                    val marginColor = Color(0xFFEF4444).copy(alpha = 0.5f)
                    val accentColor = Color(0xFF3B82F6).copy(alpha = 0.4f)

                    val norm = templateName.lowercase().trim()
                    when {
                        norm == "ruled" -> {
                            val marginX = w * 0.2f
                            drawLine(marginColor, Offset(marginX, 0f), Offset(marginX, h), strokeWidth = 1.5f)
                            drawLine(accentColor, Offset(0f, 20f), Offset(w, 20f), strokeWidth = 1.5f)
                            var y = 35f
                            while (y < h - 10f) {
                                drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                                y += 16f
                            }
                        }
                        norm == "grid" || norm == "square" || norm == "engineering" -> {
                            val spacing = 14f
                            var x = spacing
                            while (x < w) {
                                drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.8f)
                                x += spacing
                            }
                            var y = spacing
                            while (y < h) {
                                drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                                y += spacing
                            }
                        }
                        norm == "dotted" -> {
                            val spacing = 16f
                            var x = spacing
                            while (x < w) {
                                var y = spacing
                                while (y < h) {
                                    drawCircle(lineColor, radius = 1.2f, center = Offset(x, y))
                                    y += spacing
                                }
                                x += spacing
                            }
                        }
                        norm == "cornell" -> {
                            val splitX = w * 0.32f
                            val summaryY = h * 0.78f
                            drawLine(accentColor, Offset(0f, 20f), Offset(w, 20f), strokeWidth = 1.5f)
                            drawLine(accentColor, Offset(splitX, 20f), Offset(splitX, summaryY), strokeWidth = 1.5f)
                            drawLine(accentColor, Offset(0f, summaryY), Offset(w, summaryY), strokeWidth = 1.5f)
                            var y = 35f
                            while (y < summaryY) {
                                drawLine(lineColor, Offset(splitX, y), Offset(w, y), strokeWidth = 0.8f)
                                y += 14f
                            }
                        }
                        norm == "meeting" -> {
                            val splitX = w * 0.45f
                            drawLine(accentColor, Offset(0f, 22f), Offset(w, 22f), strokeWidth = 1.5f)
                            drawLine(accentColor, Offset(splitX, 22f), Offset(splitX, h), strokeWidth = 1.5f)
                            var y = 35f
                            while (y < h - 10f) {
                                drawLine(lineColor, Offset(0f, y), Offset(splitX - 6f, y), strokeWidth = 0.8f)
                                y += 16f
                            }
                        }
                        norm == "daily_planner" -> {
                            val splitX = w * 0.38f
                            drawLine(accentColor, Offset(0f, 20f), Offset(w, 20f), strokeWidth = 1.5f)
                            drawLine(accentColor, Offset(splitX, 20f), Offset(splitX, h), strokeWidth = 1.5f)
                            var y = 32f
                            while (y < h - 10f) {
                                drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                                y += 15f
                            }
                        }
                        norm == "legal" -> {
                            drawLine(marginColor, Offset(w * 0.18f, 0f), Offset(w * 0.18f, h), strokeWidth = 1.5f)
                            drawLine(marginColor, Offset(w * 0.22f, 0f), Offset(w * 0.22f, h), strokeWidth = 0.8f)
                            var y = 24f
                            while (y < h - 10f) {
                                drawLine(lineColor, Offset(w * 0.22f, y), Offset(w, y), strokeWidth = 0.8f)
                                y += 14f
                            }
                        }
                        norm == "music" -> {
                            val staffY = listOf(30f, 75f, 120f)
                            staffY.forEach { sy ->
                                for (i in 0..4) {
                                    val ly = sy + i * 5f
                                    if (ly < h - 5f) {
                                        drawLine(Color.DarkGray, Offset(10f, ly), Offset(w - 10f, ly), strokeWidth = 1f)
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay stats in bottom corner of preview
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (strokeCount > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Gesture, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Text("$strokeCount", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (imageCount > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Text("$imageCount", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (blockCount > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Text("$blockCount", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Actions & Template label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = templateName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // 3-Dots Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Page Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Insert Page After") },
                            leadingIcon = { Icon(Icons.Default.PostAdd, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onInsertAfter()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Duplicate Page") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Change Template") },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onChangeTemplate()
                            }
                        )

                        if (pageNum > 1) {
                            DropdownMenuItem(
                                text = { Text("Move Left (Up)") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMoveLeft()
                                }
                            )
                        }

                        if (pageNum < totalPages) {
                            DropdownMenuItem(
                                text = { Text("Move Right (Down)") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMoveRight()
                                }
                            )
                        }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Clear Ink on Page") },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onClearContent()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete Page", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPageCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Page",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = "Add New Page",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Choose template & paper style",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LipiAddPageTemplateDialog(
    defaultInsertIndex: Int,
    totalPages: Int,
    currentNoteDefaultTemplate: String,
    onDismiss: () -> Unit,
    onPageCreated: (Int, String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(currentNoteDefaultTemplate) }
    var selectedPositionMode by remember { mutableStateOf("end") } // "end", "after_current", "beginning"
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Standard", "Study & Work", "Specialty")
    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") ALL_PAGE_TEMPLATES
        else ALL_PAGE_TEMPLATES.filter { it.category == selectedCategory }
    }

    val computedInsertIndex = when (selectedPositionMode) {
        "beginning" -> 1
        "after_current" -> defaultInsertIndex.coerceIn(1, totalPages + 1)
        else -> totalPages + 1
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add New Page",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a paper style and position in document",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Insertion Position Selector
            Text(
                text = "Insert Location",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPositionMode == "end",
                    onClick = { selectedPositionMode = "end" },
                    label = { Text("At End (Page ${totalPages + 1})") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedPositionMode == "after_current",
                    onClick = { selectedPositionMode = "after_current" },
                    label = { Text("After Page ${defaultInsertIndex - 1}") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedPositionMode == "beginning",
                    onClick = { selectedPositionMode = "beginning" },
                    label = { Text("At Start (Page 1)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Template Category Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Template Options Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTemplates, key = { it.id }) { item ->
                    val isSelected = selectedTemplate == item.id

                    Surface(
                        onClick = { selectedTemplate = item.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = {
                    onPageCreated(computedInsertIndex, selectedTemplate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_create_page_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insert Page at Position $computedInsertIndex",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LipiSelectPageTemplateSheet(
    title: String,
    currentTemplate: String,
    onDismiss: () -> Unit,
    onTemplateSelected: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Standard", "Study & Work", "Specialty")
    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") ALL_PAGE_TEMPLATES
        else ALL_PAGE_TEMPLATES.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a paper pattern for this page",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTemplates, key = { it.id }) { item ->
                    val isSelected = currentTemplate.equals(item.id, ignoreCase = true)

                    Surface(
                        onClick = {
                            onTemplateSelected(item.id)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
