package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ==========================================
// LIPI COLOR SYSTEM (Android 16 M3 Expressive)
// ==========================================
private val LipiBgLight @Composable get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFF7F8FC)
private val LipiCardWhite @Composable get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFFFFFFF)
private val LipiPrimary = Color(0xFF5B6DFF)      // Royal Indigo
private val LipiSecondary = Color(0xFF8A7CFF)    // Lavender
private val LipiAccent = Color(0xFF4DA3FF)       // Sky Blue
private val LipiSuccess = Color(0xFF2ECC71)      // Emerald Green
private val LipiWarning = Color(0xFFFF9F43)      // Warm Amber
private val LipiError = Color(0xFFFF5C5C)        // Coral Red
private val LipiTextPrimary @Composable get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF1E293B)
private val LipiTextSecondary @Composable get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)
private val LipiBorder @Composable get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)
private val LipiSoftBg @Composable get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFF1F5F9)

/**
 * Redesigned "All Notes" screen matching the homepage's visual language.
 * Material 3 Expressive, floating 24dp cards, AI search bar, responsive grid,
 * right panel with AI suggestions and deadlines on tablet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedesignedAllNotesView(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    viewModel: NoteViewModel,
    isTablet: Boolean,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOpenMenu: () -> Unit,
    onSelectNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onRenameNote: (NoteEntity, String) -> Unit,
    onDuplicateNote: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isGridView by remember { mutableStateOf(true) }
    var selectedSortOption by remember { mutableStateOf("Date (Newest)") }
    var starredNoteIds by remember { mutableStateOf(setOf<Int>()) }

    // Drag and Drop state
    var draggedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var dragTouchPosition by remember { mutableStateOf(Offset.Zero) }
    var activeHoveredFolder by remember { mutableStateOf<String?>(null) }
    var activeHoveredSwapNoteId by remember { mutableStateOf<Int?>(null) }
    var showMoveToFolderNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val folderBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val noteBoundsMap = remember { mutableStateMapOf<Int, Rect>() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launchers for importing PDF and DOCX
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

    // Filter and sort notes
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
            selectedFilter in listOf("Handwritten", "Note") -> baseFiltered.filter { it.templateType in listOf("blank", "ruled", "grid") }
            selectedFilter in listOf("PDFs", "PDF", "Imported PDFs & Docs") -> baseFiltered.filter { it.templateType == "pdf" || it.templateType == "docx" || !it.pdfTitle.isNullOrEmpty() || it.title.contains(".pdf", ignoreCase = true) || it.title.contains("PDF", ignoreCase = true) }
            selectedFilter in listOf("Voice Note", "Voice Notes") -> baseFiltered.filter { it.tags.contains("voicenote", ignoreCase = true) || it.title.contains("Voice Note", ignoreCase = true) || !it.audioPath.isNullOrEmpty() || !it.audioTranscription.isNullOrEmpty() }
            selectedFilter in listOf("AI Summary", "Summaries") -> baseFiltered.filter { it.tags.contains("aisummary", ignoreCase = true) || it.title.contains("Summary", ignoreCase = true) || !it.summary.isNullOrEmpty() }
            selectedFilter in listOf("Flashcards", "Flashcard Deck") -> baseFiltered.filter { it.tags.contains("flashcards", ignoreCase = true) || it.title.contains("Flashcards", ignoreCase = true) || it.title.contains("Deck", ignoreCase = true) }
            selectedFilter in listOf("Mind Map", "Mind Maps") -> baseFiltered.filter { it.tags.contains("mindmap", ignoreCase = true) || it.title.contains("Mind Map", ignoreCase = true) }
            selectedFilter in listOf("Templates", "Folder", "Structural Templates") -> baseFiltered.filter { it.templateType in listOf("cornell", "meeting") }
            selectedFilter in listOf("Favorites", "Recent", "Starred") -> {
                if (selectedFilter == "Favorites" || selectedFilter == "Starred") baseFiltered.filter { starredNoteIds.contains(it.id) }
                else baseFiltered.sortedByDescending { it.lastModifiedTime }.take(8)
            }
            selectedFilter in listOf("Projects", "Work/Projects") -> baseFiltered.filter { it.tags.contains("work", ignoreCase = true) || it.title.contains("project", ignoreCase = true) || it.title.contains("work", ignoreCase = true) }
            selectedFilter in listOf("School", "School/Lectures") -> baseFiltered.filter { it.tags.contains("school", ignoreCase = true) || it.tags.contains("study", ignoreCase = true) || it.title.contains("lecture", ignoreCase = true) }
            selectedFilter in listOf("Personal", "Personal/Ideas") -> baseFiltered.filter { it.tags.contains("personal", ignoreCase = true) || it.tags.contains("ideas", ignoreCase = true) }
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
            "Custom Order" -> {
                if (viewModel.customNoteOrder.isNotEmpty()) {
                    val orderMap = viewModel.customNoteOrder.withIndex().associate { it.value to it.index }
                    categoryFiltered.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
                } else {
                    categoryFiltered
                }
            }
            "Date (Oldest)" -> categoryFiltered.sortedBy { it.lastModifiedTime }
            "Title (A to Z)" -> categoryFiltered.sortedBy { it.title.lowercase() }
            "Title (Z to A)" -> categoryFiltered.sortedByDescending { it.title.lowercase() }
            "Template Type" -> categoryFiltered.sortedBy { it.templateType }
            else -> {
                if (viewModel.customNoteOrder.isNotEmpty()) {
                    val orderMap = viewModel.customNoteOrder.withIndex().associate { it.value to it.index }
                    categoryFiltered.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
                } else {
                    categoryFiltered.sortedByDescending { it.lastModifiedTime }
                }
            }
        }
    }

    if (showMoveToFolderNote != null) {
        MoveToFolderDialog(
            note = showMoveToFolderNote!!,
            viewModel = viewModel,
            onDismissRequest = { showMoveToFolderNote = null },
            onFolderSelected = { targetFolder: String ->
                val noteTitle = showMoveToFolderNote!!.title
                viewModel.moveNoteToFolder(showMoveToFolderNote!!, targetFolder)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Moved '$noteTitle' to folder '$targetFolder'")
                }
            }
        )
    }

    if (showBatchMoveDialog) {
        BatchMoveToFolderDialog(
            selectedCount = viewModel.selectedNoteIds.size,
            viewModel = viewModel,
            onDismissRequest = { showBatchMoveDialog = false },
            onFolderSelected = { targetFolder ->
                val count = viewModel.selectedNoteIds.size
                viewModel.moveSelectedNotesToFolder(targetFolder)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Moved $count notebooks to folder '$targetFolder'")
                }
            }
        )
    }

    if (showBatchDeleteConfirm) {
        val count = viewModel.selectedNoteIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Delete $count Notebooks?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the $count selected notebooks?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedNotes()
                        showBatchDeleteConfirm = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted $count notebooks")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LipiError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LipiBgLight)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main Content Area (Header + Filters + Notebook Grid)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 16.dp)
            ) {
                // Header Bar & AI Search
                AllNotesHeaderSection(
                    searchKeyword = searchKeyword,
                    onSearchChange = onSearchChange,
                    onCreateNoteClick = onCreateNoteClick,
                    onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                    onImportDocxClick = { docxPickerLauncher.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") },
                    onMenuClick = onOpenMenu,
                    onHomeClick = onHomeClick,
                    viewModel = viewModel,
                    isGridView = isGridView,
                    onToggleGridView = { isGridView = !isGridView },
                    selectedSortOption = selectedSortOption,
                    onSortOptionSelected = { selectedSortOption = it },
                    isTablet = isTablet
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Expressive Filter Chips Row (acts as Drag & Drop target for folders)
                AllNotesFilterChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                    viewModel = viewModel,
                    activeHoveredFolder = activeHoveredFolder,
                    onFolderBoundsChanged = { label, rect ->
                        folderBoundsMap[label] = rect
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Batch selection status bar if active
                if (viewModel.isSelectionMode) {
                    AllNotesSelectionBar(
                        viewModel = viewModel,
                        onOpenBatchMove = { showBatchMoveDialog = true },
                        onBatchExportZip = {
                            viewModel.exportSelectedNotesAsZip(context) { msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onOpenBatchDeleteConfirm = { showBatchDeleteConfirm = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Drag & Drop Hint Banner when reordering or moving
                AnimatedVisibility(
                    visible = draggedNote == null && selectedFilter == "All Notes",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiPrimary.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Pro Tip: Long press and drag any notebook to reorder your library or drop into a folder chip above!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = LipiPrimary
                            )
                        }
                    }
                }

                // Notebook Cards Grid
                if (filteredNotes.isEmpty()) {
                    AllNotesEmptyState(
                        onCreateNoteClick = onCreateNoteClick,
                        onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") }
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedFilter to selectedSortOption,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)) +
                             slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> if (targetState.first != initialState.first) width / 8 else 0 } +
                             scaleIn(initialScale = 0.96f, animationSpec = tween(280)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(180))
                            )
                        },
                        label = "GridFilterTransition",
                        modifier = Modifier.weight(1f)
                    ) { _ ->
                        LazyVerticalGrid(
                            columns = if (isGridView) {
                                if (isTablet) GridCells.Adaptive(minSize = 220.dp) else GridCells.Fixed(2)
                            } else {
                                GridCells.Fixed(1)
                            },
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                val isHoveredForSwap = activeHoveredSwapNoteId == note.id
                                val isCardDragged = draggedNote?.id == note.id

                                RedesignedNotebookCard(
                                    note = note,
                                    isGridView = isGridView,
                                    isStarred = starredNoteIds.contains(note.id),
                                    onToggleStar = { id ->
                                        starredNoteIds = if (starredNoteIds.contains(id)) starredNoteIds - id else starredNoteIds + id
                                    },
                                    onSelectNote = {
                                        if (viewModel.isSelectionMode) {
                                            viewModel.toggleNoteSelection(note.id)
                                        } else {
                                            onSelectNote(note)
                                        }
                                    },
                                    onDeleteNote = { onDeleteNote(note) },
                                    onRenameNote = { newTitle -> onRenameNote(note, newTitle) },
                                    onDuplicateNote = { onDuplicateNote(note) },
                                    onOpenMoveToFolderDialog = { showMoveToFolderNote = note },
                                    viewModel = viewModel,
                                    isDragging = isCardDragged,
                                    isHoveredForSwap = isHoveredForSwap,
                                    onDragStart = { localOffset ->
                                        draggedNote = note
                                        dragTouchPosition = localOffset
                                    },
                                    onDrag = { dragAmount ->
                                        dragTouchPosition += dragAmount

                                        // Check if hovering over folder chips
                                        val hoveredFolder = folderBoundsMap.entries.firstOrNull { (_, bounds) ->
                                            bounds.contains(dragTouchPosition)
                                        }?.key

                                        if (hoveredFolder != null) {
                                            activeHoveredFolder = hoveredFolder
                                            activeHoveredSwapNoteId = null
                                        } else {
                                            activeHoveredFolder = null
                                            val hoveredNote = noteBoundsMap.entries.firstOrNull { (id, bounds) ->
                                                id != note.id && bounds.contains(dragTouchPosition)
                                            }?.key
                                            activeHoveredSwapNoteId = hoveredNote
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggedNote != null) {
                                            if (activeHoveredFolder != null) {
                                                val folderTarget = activeHoveredFolder!!
                                                val title = draggedNote!!.title
                                                viewModel.moveNoteToFolder(draggedNote!!, folderTarget)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Moved '$title' to folder '$folderTarget'")
                                                }
                                            } else if (activeHoveredSwapNoteId != null) {
                                                val currentIds = filteredNotes.map { it.id }.toMutableList()
                                                val fromIdx = currentIds.indexOf(draggedNote!!.id)
                                                val toIdx = currentIds.indexOf(activeHoveredSwapNoteId!!)
                                                if (fromIdx != -1 && toIdx != -1) {
                                                    currentIds.removeAt(fromIdx)
                                                    currentIds.add(toIdx, draggedNote!!.id)
                                                    viewModel.saveNoteOrder(currentIds)
                                                    selectedSortOption = "Custom Order"
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Reordered '${draggedNote!!.title}'")
                                                    }
                                                }
                                            }
                                        }
                                        draggedNote = null
                                        activeHoveredFolder = null
                                        activeHoveredSwapNoteId = null
                                    },
                                    onDragCancel = {
                                        draggedNote = null
                                        activeHoveredFolder = null
                                        activeHoveredSwapNoteId = null
                                    },
                                    modifier = Modifier
                                        .animateItem()
                                        .onGloballyPositioned { coordinates ->
                                            noteBoundsMap[note.id] = coordinates.boundsInWindow()
                                        }
                                )
                            }
                        }
                    }
                }
            }


        }

        // Floating Drag Overlay / Ghost Card Preview
        if (draggedNote != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragTouchPosition.x - 110.dp.toPx()).roundToInt(),
                                (dragTouchPosition.y - 70.dp.toPx()).roundToInt()
                            )
                        }
                        .width(220.dp)
                        .height(140.dp)
                        .graphicsLayer {
                            rotationZ = -3f
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                        .shadow(24.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = LipiCardWhite,
                    border = BorderStroke(2.dp, if (activeHoveredFolder != null) LipiSuccess else LipiPrimary)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NoteCardPreview(note = draggedNote!!, modifier = Modifier.fillMaxSize())
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (activeHoveredFolder != null) LipiSuccess else LipiPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeHoveredFolder != null) Icons.Default.Folder else Icons.Default.DragIndicator,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (activeHoveredFolder != null) "Drop to move to $activeHoveredFolder" else "Moving '${draggedNote!!.title}'",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Snackbar Host for feedback notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * Premium Page Header displaying Title, Subtitle, Large AI Search Bar, and Action Buttons.
 */
@Composable
private fun AllNotesHeaderSection(
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onImportDocxClick: () -> Unit,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    viewModel: NoteViewModel,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    selectedSortOption: String,
    onSortOptionSelected: (String) -> Unit,
    isTablet: Boolean
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showDriveBackupDialog by remember { mutableStateOf(false) }

    if (showDriveBackupDialog) {
        GoogleDriveBackupDialog(
            viewModel = viewModel,
            onDismissRequest = { showDriveBackupDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top Navigation & Sync Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isTablet) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LipiCardWhite)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = LipiTextPrimary)
                    }
                }
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LipiCardWhite)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = LipiPrimary)
                }

                // Cloud Sync Status Indicator
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (viewModel.isSyncing) LipiPrimary.copy(alpha = 0.12f) else if (isDark) Color(0xFF132E1D) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (viewModel.isSyncing) LipiPrimary.copy(alpha = 0.3f) else if (isDark) Color(0xFF166534) else Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showDriveBackupDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (viewModel.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = LipiPrimary
                            )
                            Text(
                                text = "Syncing Drive...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LipiPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Saved",
                                tint = LipiSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cloud Saved",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LipiSuccess
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Batch Selection Toggle
                IconButton(
                    onClick = { viewModel.toggleSelectionMode() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isSelectionMode) LipiPrimary.copy(alpha = 0.15f) else LipiCardWhite)
                ) {
                    Icon(
                        imageVector = if (viewModel.isSelectionMode) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Select Notes",
                        tint = if (viewModel.isSelectionMode) LipiPrimary else LipiTextSecondary
                    )
                }

                // Scan Document Button
                IconButton(
                    onClick = { viewModel.openDocumentScanner("all_notes") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LipiCardWhite)
                        .testTag("all_notes_scan_document_button")
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Document", tint = LipiPrimary)
                }

                // Import PDF/Doc Button
                IconButton(
                    onClick = onImportPdfClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LipiCardWhite)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Import PDF", tint = LipiSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title Header: 👋 All Notes & Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👋 All Notes",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = LipiTextPrimary,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Find and organize your notebooks, PDFs, handwritten notes and projects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LipiTextSecondary,
                    fontSize = 14.sp
                )
            }

            // Primary + New Notebook Action Button
            Button(
                onClick = onCreateNoteClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LipiPrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                modifier = Modifier.testTag("create_new_notebook_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Notebook", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Large AI Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = LipiCardWhite,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Search",
                        tint = LipiPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchKeyword.isEmpty()) {
                            Text(
                                text = "Ask Lipi AI... Search handwriting • PDFs • voice notes • diagrams",
                                fontSize = 14.sp,
                                color = LipiTextSecondary
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchKeyword,
                            onValueChange = onSearchChange,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LipiTextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchKeyword.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = LipiTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prompt Hint Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val promptPills = listOf(
                        "🔍 Search handwriting",
                        "📄 Search PDFs",
                        "🎙️ Voice notes",
                        "📊 Search diagrams",
                        "💡 Ask questions"
                    )
                    promptPills.forEach { pill ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LipiBgLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val term = pill.substringAfter(" ").trim()
                                    onSearchChange(term)
                                }
                        ) {
                            Text(
                                text = pill,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = LipiTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Toolbar: Sort, View Toggle, Filter indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "YOUR NOTEBOOKS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LipiTextSecondary,
                letterSpacing = 1.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // View Mode Toggle Button
                IconButton(
                    onClick = onToggleGridView,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LipiCardWhite)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.FormatListBulleted else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List View",
                        tint = LipiPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Sort Options Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LipiCardWhite,
                        border = BorderStroke(1.dp, LipiBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showSortMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, tint = LipiPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedSortOption) {
                                    "Date (Oldest)" -> "Sort: Oldest"
                                    "Title (A to Z)" -> "Sort: A-Z"
                                    "Title (Z to A)" -> "Sort: Z-A"
                                    "Template Type" -> "Sort: Type"
                                    else -> "Sort: Newest"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LipiTextPrimary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = LipiTextSecondary)
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        val sortOptions = listOf(
                            "Date (Newest)" to "Date (Newest First)",
                            "Date (Oldest)" to "Date (Oldest First)",
                            "Title (A to Z)" to "Title (A to Z)",
                            "Title (Z to A)" to "Title (Z to A)",
                            "Template Type" to "Template Type"
                        )
                        sortOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selectedSortOption == key) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOption == key) LipiPrimary else LipiTextPrimary
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
    }
}

/**
 * Expressive Material 3 Filter Chips Row with Drag & Drop target detection.
 */
@Composable
private fun AllNotesFilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    viewModel: NoteViewModel,
    activeHoveredFolder: String? = null,
    onFolderBoundsChanged: (String, Rect) -> Unit = { _, _ -> }
) {
    var isShrunk by remember { mutableStateOf(false) }

    val baseFilters = listOf("All", "Recent", "Favorites", "Handwritten", "PDF", "Projects", "Templates", "Personal", "School")
    val customDirs = viewModel.customDirectories.map { it.name }
    val allChipLabels = baseFilters + customDirs

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Shrink / Expand Toggle Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isShrunk) LipiPrimary.copy(alpha = 0.12f) else LipiCardWhite,
            border = BorderStroke(1.dp, LipiPrimary.copy(alpha = 0.3f)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { isShrunk = !isShrunk }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isShrunk) Icons.Default.FilterList else Icons.Default.UnfoldLess,
                    contentDescription = if (isShrunk) "Expand Folders & Filters" else "Shrink Folders & Filters",
                    tint = LipiPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isShrunk) "Filters" else "Shrink",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LipiPrimary
                )
            }
        }

        if (isShrunk) {
            val activeLabel = when {
                selectedFilter == "All Notes" || selectedFilter == "All" -> "All Notes"
                selectedFilter.startsWith("dir:") -> {
                    val dirId = selectedFilter.removePrefix("dir:")
                    viewModel.customDirectories.find { it.id == dirId }?.name ?: selectedFilter
                }
                selectedFilter.startsWith("tag:") -> selectedFilter.removePrefix("tag:")
                else -> selectedFilter
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LipiPrimary,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Active: $activeLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                allChipLabels.forEach { label ->
                    val isSelected = when (label) {
                        "All" -> selectedFilter == "All Notes" || selectedFilter == "All"
                        "Recent" -> selectedFilter == "Recent"
                        "Favorites" -> selectedFilter == "Favorites" || selectedFilter == "Starred"
                        "Handwritten" -> selectedFilter == "Handwritten" || selectedFilter == "Note"
                        "PDF" -> selectedFilter == "PDFs" || selectedFilter == "PDF"
                        "Projects" -> selectedFilter == "Projects" || selectedFilter == "Work/Projects"
                        "Templates" -> selectedFilter == "Templates" || selectedFilter == "Folder"
                        "Personal" -> selectedFilter == "Personal" || selectedFilter == "Personal/Ideas"
                        "School" -> selectedFilter == "School" || selectedFilter == "School/Lectures"
                        else -> {
                            val dir = viewModel.customDirectories.find { it.name == label }
                            if (dir != null) selectedFilter == "dir:${dir.id}" else label == selectedFilter
                        }
                    }

                    val isHoveredByDrag = activeHoveredFolder == label

                    val targetFilter = when (label) {
                        "All" -> "All Notes"
                        "Recent" -> "Recent"
                        "Favorites" -> "Favorites"
                        "Handwritten" -> "Handwritten"
                        "PDF" -> "PDFs"
                        "Projects" -> "Projects"
                        "Templates" -> "Templates"
                        "Personal" -> "Personal"
                        "School" -> "School"
                        else -> {
                            val dir = viewModel.customDirectories.find { it.name == label }
                            if (dir != null) "dir:${dir.id}" else label
                        }
                    }

                    val scale by animateFloatAsState(
                        targetValue = if (isHoveredByDrag) 1.15f else if (isSelected) 1.04f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "ChipScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isHoveredByDrag) LipiSuccess else if (isSelected) LipiPrimary else LipiCardWhite,
                        shadowElevation = if (isHoveredByDrag) 8.dp else if (isSelected) 4.dp else 1.dp,
                        border = BorderStroke(
                            width = if (isHoveredByDrag) 2.dp else 1.dp,
                            color = if (isHoveredByDrag) LipiSuccess else if (isSelected) LipiPrimary else LipiBorder
                        ),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .onGloballyPositioned { coordinates ->
                                onFolderBoundsChanged(label, coordinates.boundsInWindow())
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onFilterSelected(targetFilter) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val iconVector = when {
                                isHoveredByDrag -> Icons.Default.Folder
                                label == "All" -> Icons.Default.AllInclusive
                                label == "Recent" -> Icons.Default.Schedule
                                label == "Favorites" -> Icons.Default.Star
                                label == "Handwritten" -> Icons.Default.Draw
                                label == "PDF" -> Icons.Default.PictureAsPdf
                                label == "Projects" -> Icons.Default.FolderSpecial
                                label == "Templates" -> Icons.Default.DashboardCustomize
                                label == "Personal" -> Icons.Default.Person
                                label == "School" -> Icons.Default.School
                                else -> Icons.Default.Folder
                            }

                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = if (isHoveredByDrag || isSelected) Color.White else LipiTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = if (isHoveredByDrag) "Drop into $label" else label,
                                fontSize = 13.sp,
                                fontWeight = if (isHoveredByDrag || isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isHoveredByDrag || isSelected) Color.White else LipiTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expressive Batch Selection Bar with Delete, Move, ZIP Export, and Duplicate capabilities.
 */
@Composable
private fun AllNotesSelectionBar(
    viewModel: NoteViewModel,
    onOpenBatchMove: () -> Unit,
    onBatchExportZip: () -> Unit,
    onOpenBatchDeleteConfirm: () -> Unit
) {
    val selectedCount = viewModel.selectedNoteIds.size
    val totalCount = viewModel.allNotes.value.size
    val isAllSelected = selectedCount > 0 && selectedCount == totalCount

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LipiPrimary.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, LipiPrimary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LipiPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$selectedCount Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LipiPrimary
                    )
                    TextButton(
                        onClick = {
                            if (isAllSelected) {
                                viewModel.clearSelectedNotes()
                            } else {
                                viewModel.selectAllNotes(viewModel.allNotes.value)
                            }
                        }
                    ) {
                        Text(
                            text = if (isAllSelected) "Deselect All" else "Select All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Selection Mode", tint = LipiTextSecondary)
                }
            }

            if (selectedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = LipiPrimary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Batch Move
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenBatchMove() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Move", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Batch Export ZIP
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBatchExportZip() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = "Export ZIP", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Export ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Batch Duplicate
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.duplicateSelectedNotes() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Duplicate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Batch Delete
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiError,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenBatchDeleteConfirm() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to batch move selected notebooks to a chosen folder.
 */
@Composable
private fun BatchMoveToFolderDialog(
    selectedCount: Int,
    viewModel: NoteViewModel,
    onDismissRequest: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    var showCreateNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    if (showCreateNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNewFolderDialog = false },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.addDirectory(newFolderName)
                            onFolderSelected(newFolderName)
                            showCreateNewFolderDialog = false
                            onDismissRequest()
                        }
                    }
                ) { Text("Create & Move") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    val availableFolders = remember(viewModel.customDirectories) {
        val base = listOf("Projects", "School", "Personal", "Templates")
        val custom = viewModel.customDirectories.map { it.name }
        (base + custom).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = LipiPrimary)
                Text("Move $selectedCount Notebooks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select target folder for selected notebooks:", fontSize = 13.sp, color = LipiTextSecondary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                availableFolders.forEach { folder ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LipiSoftBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFolderSelected(folder)
                                onDismissRequest()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = LipiPrimary)
                            Text(folder, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LipiTextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showCreateNewFolderDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Folder", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Redesigned Notebook Card featuring physical notebook cover, badges, metadata,
 * drag-and-drop long-press gesture, and opening animation.
 */
@Composable
fun RedesignedNotebookCard(
    note: NoteEntity,
    isGridView: Boolean,
    isStarred: Boolean,
    onToggleStar: (Int) -> Unit,
    onSelectNote: () -> Unit,
    onDeleteNote: () -> Unit,
    onRenameNote: (String) -> Unit,
    onDuplicateNote: () -> Unit,
    onOpenMoveToFolderDialog: () -> Unit = {},
    viewModel: NoteViewModel,
    isDragging: Boolean = false,
    isHoveredForSwap: Boolean = false,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val isCheckSelected = viewModel.selectedNoteIds.contains(note.id)

    val cardScale by animateFloatAsState(
        targetValue = if (isDragging) 0.92f else if (isHoveredForSwap) 1.06f else if (isHovered) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "NotebookCardScale"
    )

    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(note.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Notebook", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Notebook Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRenameNote(newTitle)
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = if (isDragging) 0.35f else 1.0f
            }
            .clip(RoundedCornerShape(24.dp))
            .shadow(elevation = if (isHoveredForSwap) 12.dp else if (isHovered) 8.dp else 4.dp, shape = RoundedCornerShape(24.dp))
            .pointerInput(note.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(offset) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
            .clickable { onSelectNote() }
            .testTag("notebook_card_${note.id}"),
        shape = RoundedCornerShape(24.dp),
        color = LipiCardWhite,
        border = BorderStroke(
            width = if (isHoveredForSwap) 2.dp else 1.dp,
            color = if (isHoveredForSwap) LipiSuccess else if (isCheckSelected) LipiPrimary else LipiBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Notebook Cover Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isGridView) 1.25f else 3.5f)
                    .background(LipiSoftBg)
            ) {
                // Main Cover / Template Preview
                NoteCardPreview(note = note, modifier = Modifier.fillMaxSize())

                // Physical Notebook Left Spine Detail Accent
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF334155),
                                    Color(0xFF64748B),
                                    Color(0xFF94A3B8).copy(alpha = 0.4f)
                                )
                            )
                        )
                        .align(Alignment.CenterStart)
                )

                // Drag Handle Indicator Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 18.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DragHandle, contentDescription = "Drag to reorder", tint = LipiTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // Pinned / Starred Badge
                if (isStarred) {
                    Surface(
                        shape = CircleShape,
                        color = LipiWarning,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 10.dp, start = 48.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = "Starred", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // AI Indexed Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LipiPrimary.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 10.dp, start = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("AI Indexed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Selection Checkbox or Favorite Toggle Button
                if (viewModel.isSelectionMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCheckSelected) LipiPrimary else Color.White.copy(alpha = 0.95f),
                        border = BorderStroke(2.dp, if (isCheckSelected) LipiPrimary else LipiTextSecondary),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.toggleNoteSelection(note.id) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCheckSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    IconButton(
                        onClick = { onToggleStar(note.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isStarred) LipiError else LipiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Metadata Info Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LipiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Three-dot Menu Action Button
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = LipiTextSecondary, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Notebook") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onSelectNote()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Folder...") },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = LipiPrimary) },
                                onClick = {
                                    showMenu = false
                                    onOpenMoveToFolderDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicateNote()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = LipiError) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = LipiError) },
                                onClick = {
                                    showMenu = false
                                    onDeleteNote()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tag Pill & Page Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tagLabel = when {
                        note.templateType == "pdf" -> "PDF Document"
                        note.templateType == "cornell" -> "Cornell Notes"
                        note.templateType == "ruled" -> "Ruled Pad"
                        note.templateType == "grid" -> "Grid Pad"
                        note.tags.isNotBlank() -> note.tags.split(",").firstOrNull() ?: "Handwritten"
                        else -> "Handwritten"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LipiPrimary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "#$tagLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LipiPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    val dateStr = try {
                        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        sdf.format(Date(note.lastModifiedTime))
                    } catch (e: Exception) {
                        "Edited recently"
                    }

                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = LipiTextSecondary
                    )
                }
            }
        }
    }
}



@Composable
fun MoveToFolderDialog(
    note: com.example.data.NoteEntity,
    viewModel: com.example.ui.components.NoteViewModel,
    onDismissRequest: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    var showCreateNewFolderDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newFolderName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    if (showCreateNewFolderDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateNewFolderDialog = false },
            title = { androidx.compose.material3.Text("Create New Folder", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { androidx.compose.material3.Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.addDirectory(newFolderName)
                            onFolderSelected(newFolderName)
                            showCreateNewFolderDialog = false
                            onDismissRequest()
                        }
                    }
                ) { androidx.compose.material3.Text("Create & Move") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateNewFolderDialog = false }) { androidx.compose.material3.Text("Cancel") }
            }
        )
    }

    val availableFolders = androidx.compose.runtime.remember(viewModel.customDirectories) {
        val base = listOf("Projects", "School", "Personal", "Templates")
        val custom = viewModel.customDirectories.map { it.name }
        (base + custom).distinct()
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.DriveFileMove, contentDescription = null, tint = LipiPrimary)
                androidx.compose.material3.Text("Move \"${note.title}\"", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text("Select a folder destination:", color = LipiTextSecondary)
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = androidx.compose.ui.Modifier.heightIn(max = 240.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(availableFolders.size) { index ->
                        val folder = availableFolders[index]
                        androidx.compose.material3.Surface(
                            onClick = {
                                onFolderSelected(folder)
                                onDismissRequest()
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (note.tags.contains(folder, ignoreCase = true)) LipiPrimary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                            ) {
                                androidx.compose.material3.Text(folder, fontWeight = if (note.tags.contains(folder, ignoreCase = true)) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
                                if (note.tags.contains(folder, ignoreCase = true)) {
                                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Current", tint = LipiPrimary, modifier = androidx.compose.ui.Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = { showCreateNewFolderDialog = true }) {
                androidx.compose.material3.Text("New Folder")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                androidx.compose.material3.Text("Cancel")
            }
        }
    )
}

@Composable
fun AllNotesEmptyState(onCreateNoteClick: () -> Unit, onImportPdfClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text("No notebooks found", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onCreateNoteClick) {
            androidx.compose.material3.Text("Create Note")
        }
    }
}
