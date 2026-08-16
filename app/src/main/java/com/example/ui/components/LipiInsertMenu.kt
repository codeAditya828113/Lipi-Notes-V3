package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.audio.LipiAudioManager
import com.example.data.*
import com.example.pdf.LipiPdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified Insert Menu Sheet & Flow Controller
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LipiInsertMenuSheet(
    viewModel: NoteViewModel,
    audioManager: LipiAudioManager,
    onDismiss: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showRecordAudioDialog by remember { mutableStateOf(false) }
    var showWebLinkDialog by remember { mutableStateOf(false) }
    var showInternalLinkDialog by remember { mutableStateOf(false) }
    var showStickyTextDialog by remember { mutableStateOf(false) }
    var showTextBoxDialog by remember { mutableStateOf(false) }
    var showPdfPagePickerSheet by remember { mutableStateOf(false) }
    var selectedPdfFileForPageInsert by remember { mutableStateOf<File?>(null) }
    var selectedPdfTitle by remember { mutableStateOf("") }

    // Audio File Import Launcher (MP3, M4A, WAV, AAC, OGG, FLAC)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val imported = audioManager.importAudioFile(uri)
                if (imported != null) {
                    val activePage = viewModel.pdfPage
                    val newBlock = AudioContentBlock(
                        page = activePage,
                        x = 60f,
                        y = 100f,
                        width = 240f,
                        height = 48f,
                        audioFilePath = imported.localFilePath,
                        originalFileName = imported.originalFileName,
                        title = imported.title,
                        durationMs = imported.durationMs
                    )
                    viewModel.addContentBlock(newBlock)
                    Toast.makeText(context, "Audio file attached to Page $activePage", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Failed to import audio file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // PDF Attachment Import Launcher
    val pdfAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val imported = LipiPdfManager.importPdfFile(context, uri)
                if (imported != null) {
                    val activePage = viewModel.pdfPage
                    val newBlock = PdfAttachmentContentBlock(
                        page = activePage,
                        x = 60f,
                        y = 120f,
                        width = 270f,
                        height = 80f,
                        pdfFilePath = imported.localFilePath,
                        originalFileName = imported.originalFileName,
                        pageCount = imported.pageCount,
                        fileSizeFormatted = imported.fileSizeFormatted,
                        previewThumbnailPath = imported.thumbnailPath ?: ""
                    )
                    viewModel.addContentBlock(newBlock)
                    Toast.makeText(context, "PDF attached to note", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Failed to attach PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // PDF File Picker for Page Extraction / Insertion Launcher
    val pdfPagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val imported = LipiPdfManager.importPdfFile(context, uri)
                if (imported != null) {
                    selectedPdfFileForPageInsert = File(imported.localFilePath)
                    selectedPdfTitle = imported.originalFileName
                    showPdfPagePickerSheet = true
                } else {
                    Toast.makeText(context, "Could not load PDF pages", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Image Gallery Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addImageFromUri(uri)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Insert & Attach",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Embed multimedia, links, and documents directly onto note pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Grid
            val insertItems = listOf(
                InsertOption(
                    title = "Record Audio",
                    subtitle = "Voice note with live waveform",
                    icon = Icons.Default.Mic,
                    color = Color(0xFFEF4444),
                    onClick = { showRecordAudioDialog = true }
                ),
                InsertOption(
                    title = "Import Audio",
                    subtitle = "MP3, M4A, WAV, AAC, OGG",
                    icon = Icons.Default.Audiotrack,
                    color = Color(0xFFF59E0B),
                    onClick = { audioPickerLauncher.launch("audio/*") }
                ),
                InsertOption(
                    title = "Web Hyperlink",
                    subtitle = "Embed web URL preview card",
                    icon = Icons.Default.Language,
                    color = Color(0xFF3B82F6),
                    onClick = { showWebLinkDialog = true }
                ),
                InsertOption(
                    title = "Link to Note / Page",
                    subtitle = "Fast jump between notes",
                    icon = Icons.Default.MenuBook,
                    color = Color(0xFF8B5CF6),
                    onClick = { showInternalLinkDialog = true }
                ),
                InsertOption(
                    title = "Attach PDF",
                    subtitle = "Multi-page document attachment",
                    icon = Icons.Default.PictureAsPdf,
                    color = Color(0xFFEC4899),
                    onClick = { pdfAttachmentLauncher.launch("application/pdf") }
                ),
                InsertOption(
                    title = "Insert PDF Page",
                    subtitle = "Embed page & write over it",
                    icon = Icons.Default.Layers,
                    color = Color(0xFF06B6D4),
                    onClick = { pdfPagePickerLauncher.launch("application/pdf") }
                ),
                InsertOption(
                    title = "Photo / Image",
                    subtitle = "Insert image onto page",
                    icon = Icons.Default.Image,
                    color = Color(0xFF10B981),
                    onClick = { imagePickerLauncher.launch("image/*") }
                ),
                InsertOption(
                    title = "Scan Document",
                    subtitle = "Auto-crop scanner & OCR",
                    icon = Icons.Default.DocumentScanner,
                    color = Color(0xFF6366F1),
                    onClick = {
                        onDismiss()
                        onOpenScanner()
                    }
                ),
                InsertOption(
                    title = "Text Box",
                    subtitle = "Editable multiline text block",
                    icon = Icons.Default.TextFields,
                    color = Color(0xFF14B8A6),
                    onClick = { showTextBoxDialog = true }
                ),
                InsertOption(
                    title = "Sticky Note",
                    subtitle = "Colored text note",
                    icon = Icons.Default.StickyNote2,
                    color = Color(0xFFEAB308),
                    onClick = { showStickyTextDialog = true }
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(insertItems) { item ->
                    Card(
                        onClick = item.onClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Record Audio Dialog
    if (showRecordAudioDialog) {
        LipiRecordAudioDialog(
            audioManager = audioManager,
            onDismiss = { showRecordAudioDialog = false },
            onSaveRecording = { result, title ->
                val activePage = viewModel.pdfPage
                val newBlock = AudioContentBlock(
                    page = activePage,
                    x = 60f,
                    y = 100f,
                    width = 240f,
                    height = 48f,
                    audioFilePath = result.filePath,
                    originalFileName = result.fileName,
                    title = title.ifBlank { "Voice Note" },
                    durationMs = result.durationMs
                )
                viewModel.addContentBlock(newBlock)
                Toast.makeText(context, "Voice recording saved to Audio Player section!", Toast.LENGTH_SHORT).show()
                showRecordAudioDialog = false
                onDismiss()
                viewModel.openAudioPlayerLibrary()
            }
        )
    }

    // Web Link Dialog
    if (showWebLinkDialog) {
        LipiWebLinkDialog(
            onDismiss = { showWebLinkDialog = false },
            onConfirm = { url, title ->
                val activePage = viewModel.pdfPage
                val displayTitle = title.ifBlank { url }
                val (autoW, autoH) = calculateAutoBlockDimensions(url = url, title = displayTitle, isWebLink = true)
                val newBlock = WebLinkContentBlock(
                    page = activePage,
                    x = 60f,
                    y = 120f,
                    width = autoW,
                    height = autoH,
                    url = url,
                    title = displayTitle
                )
                viewModel.addContentBlock(newBlock)
                showWebLinkDialog = false
                onDismiss()
            }
        )
    }

    // Internal Note Link Dialog
    if (showInternalLinkDialog) {
        val notes by viewModel.allNotes.collectAsState()
        LipiInternalLinkDialog(
            notes = notes,
            currentNoteId = viewModel.selectedNote?.id ?: -1,
            onDismiss = { showInternalLinkDialog = false },
            onConfirm = { targetNoteId, targetNoteTitle, page, label ->
                val activePage = viewModel.pdfPage
                val displayLabel = label.ifBlank { "$targetNoteTitle (P$page)" }
                val (autoW, autoH) = calculateAutoBlockDimensions(title = displayLabel, isInternalLink = true)
                val newBlock = InternalLinkContentBlock(
                    page = activePage,
                    x = 60f,
                    y = 120f,
                    width = autoW,
                    height = autoH,
                    targetNoteId = targetNoteId,
                    targetNoteTitle = targetNoteTitle,
                    targetPage = page,
                    label = displayLabel
                )
                viewModel.addContentBlock(newBlock)
                showInternalLinkDialog = false
                onDismiss()
            }
        )
    }

    // Text Box Dialog
    if (showTextBoxDialog) {
        LipiTextBoxDialog(
            onDismiss = { showTextBoxDialog = false },
            onConfirm = { text ->
                val activePage = viewModel.pdfPage
                val (autoW, autoH) = calculateAutoBlockDimensions(text = text, isTextBox = true)
                val newBlock = TextContentBlock(
                    page = activePage,
                    x = 60f,
                    y = 120f,
                    width = autoW,
                    height = autoH,
                    text = text,
                    backgroundColor = 0xFFFFFFFFL,
                    textColor = 0xFF1E293BL,
                    isStickyNote = false
                )
                viewModel.addContentBlock(newBlock)
                showTextBoxDialog = false
                onDismiss()
            }
        )
    }

    // Sticky Text Note Dialog
    if (showStickyTextDialog) {
        LipiStickyTextDialog(
            onDismiss = { showStickyTextDialog = false },
            onConfirm = { text, bgColor, textColor ->
                val activePage = viewModel.pdfPage
                val (autoW, autoH) = calculateAutoBlockDimensions(text = text, isStickyNote = true)
                val newBlock = TextContentBlock(
                    page = activePage,
                    x = 60f,
                    y = 120f,
                    width = autoW,
                    height = autoH,
                    text = text,
                    backgroundColor = bgColor,
                    textColor = textColor,
                    isStickyNote = true
                )
                viewModel.addContentBlock(newBlock)
                showStickyTextDialog = false
                onDismiss()
            }
        )
    }

    // PDF Page Picker Sheet
    if (showPdfPagePickerSheet && selectedPdfFileForPageInsert != null) {
        LipiPdfPagePickerSheet(
            pdfFile = selectedPdfFileForPageInsert!!,
            pdfTitle = selectedPdfTitle,
            onDismiss = { showPdfPagePickerSheet = false },
            onPageSelected = { pageIndex ->
                val activePage = viewModel.pdfPage
                val newBlock = PdfPageContentBlock(
                    page = activePage,
                    x = 40f,
                    y = 40f,
                    width = 520f,
                    height = 700f,
                    pdfFilePath = selectedPdfFileForPageInsert!!.absolutePath,
                    pdfPageIndex = pageIndex,
                    sourcePdfTitle = selectedPdfTitle
                )
                viewModel.addContentBlock(newBlock)
                showPdfPagePickerSheet = false
                onDismiss()
            },
            onPagesSelected = { pageIndices ->
                val activePage = viewModel.pdfPage
                pageIndices.forEachIndexed { idx, pageIndex ->
                    val newBlock = PdfPageContentBlock(
                        page = activePage,
                        x = 40f + (idx * 15f),
                        y = 40f + (idx * 15f),
                        width = 520f,
                        height = 700f,
                        pdfFilePath = selectedPdfFileForPageInsert!!.absolutePath,
                        pdfPageIndex = pageIndex,
                        sourcePdfTitle = selectedPdfTitle
                    )
                    viewModel.addContentBlock(newBlock)
                }
                showPdfPagePickerSheet = false
                Toast.makeText(context, "Embedded ${pageIndices.size} PDF page(s) on Page $activePage", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }
}

private data class InsertOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

/**
 * Live Audio Recording Dialog
 */
@Composable
fun LipiRecordAudioDialog(
    audioManager: LipiAudioManager,
    onDismiss: () -> Unit,
    onSaveRecording: (LipiAudioManager.RecordingResult, String) -> Unit
) {
    val context = LocalContext.current
    var recordingTitle by remember { mutableStateOf("Lecture / Meeting Audio") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            audioManager.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            audioManager.startRecording()
        }
    }

    Dialog(
        onDismissRequest = {
            audioManager.stopRecording(discard = true)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(if (audioManager.isRecording && !audioManager.isRecordingPaused) Color(0xFFEF4444) else Color(0xFFF59E0B), CircleShape)
                        )
                        Text(
                            text = if (audioManager.isRecordingPaused) "Recording Paused" else "Recording Audio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            audioManager.stopRecording(discard = true)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF94A3B8))
                    }
                }

                // Title field
                OutlinedTextField(
                    value = recordingTitle,
                    onValueChange = { recordingTitle = it },
                    label = { Text("Audio Title", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Timer readout
                Text(
                    text = audioManager.formatDuration(audioManager.recordingDurationMs),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                // Live Audio Waveform Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val amplitudes = audioManager.liveAmplitudes
                    for (i in 0 until 35) {
                        val amp = amplitudes.getOrNull(amplitudes.size - 1 - i) ?: 0.1f
                        val barHeight = (amp * 44.dp.value).coerceIn(4f, 44f)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight.dp)
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                // Recording Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause / Resume Button
                    IconButton(
                        onClick = {
                            if (audioManager.isRecordingPaused) {
                                audioManager.resumeRecording()
                            } else {
                                audioManager.pauseRecording()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF334155), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (audioManager.isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (audioManager.isRecordingPaused) "Resume" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Done / Save Button
                    Button(
                        onClick = {
                            val result = audioManager.stopRecording(discard = false)
                            if (result != null) {
                                onSaveRecording(result, recordingTitle)
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Attach", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Web Hyperlink Dialog
 */
@Composable
fun LipiWebLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, title: String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Web Hyperlink", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Web URL (e.g. https://wikipedia.org)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Display Title (Optional)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlText.isNotBlank()) {
                        onConfirm(urlText.trim(), titleText.trim())
                    }
                },
                enabled = urlText.isNotBlank()
            ) {
                Text("Insert Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Internal Note / Page Link Dialog
 */
@Composable
fun LipiInternalLinkDialog(
    notes: List<NoteEntity>,
    currentNoteId: Int,
    onDismiss: () -> Unit,
    onConfirm: (targetNoteId: Int, targetNoteTitle: String, page: Int, label: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf(notes.firstOrNull { it.id != currentNoteId } ?: notes.firstOrNull()) }
    var selectedPage by remember { mutableIntStateOf(1) }
    var customLabel by remember { mutableStateOf("") }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link to Lipi Note / Page", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notebooks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Destination Notebook:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    items(filteredNotes) { note ->
                        val isSelected = selectedNote?.id == note.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedNote = note }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = note.title.ifBlank { "Untitled Note" },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (note.id == currentNoteId) {
                                Text("(This Note)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Page Number Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Page Number:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { if (selectedPage > 1) selectedPage-- },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Page")
                        }
                        Text(
                            text = "Page $selectedPage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = { selectedPage++ },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Page")
                        }
                    }
                }

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Link Label (e.g. 'See Chapter 3')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedNote != null) {
                        onConfirm(
                            selectedNote!!.id,
                            selectedNote!!.title,
                            selectedPage,
                            customLabel.trim()
                        )
                    }
                },
                enabled = selectedNote != null
            ) {
                Text("Create Note Link")
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
fun LipiTextBoxDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text Box", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type text here...") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Insert Text")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Sticky Text Note Creation Dialog
 */
@Composable
fun LipiStickyTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, bgColor: Long, textColor: Long) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    val stickyColors = listOf(
        0xFFFEF3C7L to 0xFF78350FL, // Yellow
        0xFFE0F2FEL to 0xFF0369A1L, // Sky Blue
        0xFFDCFCE7L to 0xFF15803DL, // Green
        0xFFFCE7F3L to 0xFFBE185DL, // Pink
        0xFFF3E8FFL to 0xFF6B21A8L  // Lavender
    )
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sticky Note", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Color Palette
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    stickyColors.forEachIndexed { index, (bg, _) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(bg))
                                .border(
                                    width = if (selectedColorIndex == index) 3.dp else 1.dp,
                                    color = if (selectedColorIndex == index) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Write note text here...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val (bg, textCol) = stickyColors[selectedColorIndex]
                    onConfirm(noteText.trim(), bg, textCol)
                },
                enabled = noteText.isNotBlank()
            ) {
                Text("Add Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Visual PDF Page Picker Sheet (Shows thumbnails for all pages of PDF)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LipiPdfPagePickerSheet(
    pdfFile: File,
    pdfTitle: String,
    onDismiss: () -> Unit,
    onPageSelected: (pageIndex: Int) -> Unit,
    onPagesSelected: (pageIndices: List<Int>) -> Unit = { pageIdx -> onPageSelected(pageIdx.firstOrNull() ?: 0) }
) {
    val context = LocalContext.current
    val pageCount = remember(pdfFile) { LipiPdfManager.getPdfPageCount(pdfFile) }
    val selectedPages = remember { mutableStateListOf<Int>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Choose PDF Pages to Embed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$pdfTitle ($pageCount pages)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            if (selectedPages.size == pageCount) {
                                selectedPages.clear()
                            } else {
                                selectedPages.clear()
                                selectedPages.addAll(0 until pageCount)
                            }
                        }
                    ) {
                        Text(if (selectedPages.size == pageCount) "Deselect All" else "Select All")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(pageCount) { pageIdx ->
                    val isSelected = selectedPages.contains(pageIdx)
                    PdfPageThumbnailItem(
                        pdfFile = pdfFile,
                        pageIndex = pageIdx,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedPages.remove(pageIdx)
                            } else {
                                selectedPages.add(pageIdx)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedPages.isNotEmpty()) {
                        onPagesSelected(selectedPages.sorted())
                    } else {
                        Toast.makeText(context, "Please select at least 1 page", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedPages.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (selectedPages.isEmpty()) "Select Pages to Embed" else "Insert ${selectedPages.size} Selected Page${if (selectedPages.size > 1) "s" else ""}",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PdfPageThumbnailItem(
    pdfFile: File,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfFile, pageIndex) {
        withContext(Dispatchers.IO) {
            val bmp = LipiPdfManager.renderPageToBitmap(pdfFile, pageIndex, 300, 400)
            bitmap = bmp
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null && !bitmap!!.isRecycled) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            // Checkbox Indicator
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
            )

            // Page badge
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Universal Edit Dialog for Lipi Content Blocks
 */
@Composable
fun LipiContentBlockEditDialog(
    block: LipiContentBlock,
    onDismiss: () -> Unit,
    onSave: (LipiContentBlock) -> Unit,
    onDelete: (LipiContentBlock) -> Unit
) {
    when (block) {
        is AudioContentBlock -> {
            var title by remember { mutableStateOf(block.title) }
            var transcription by remember { mutableStateOf(block.transcription) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Audio Recording", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = transcription,
                            onValueChange = { transcription = it },
                            label = { Text("Transcription / Notes") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onSave(block.copy(title = title.trim(), transcription = transcription.trim()))
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onDelete(block) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
        is WebLinkContentBlock -> {
            var url by remember { mutableStateOf(block.url) }
            var title by remember { mutableStateOf(block.title) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Web Hyperlink", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmedUrl = url.trim()
                            val trimmedTitle = title.trim()
                            val (autoW, autoH) = calculateAutoBlockDimensions(url = trimmedUrl, title = trimmedTitle, isWebLink = true)
                            onSave(block.copy(
                                url = trimmedUrl,
                                title = trimmedTitle,
                                width = autoW,
                                height = autoH
                            ))
                        },
                        enabled = url.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onDelete(block) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
        is InternalLinkContentBlock -> {
            var label by remember { mutableStateOf(block.label) }
            var targetPage by remember { mutableIntStateOf(block.targetPage) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Note Link", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Link Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Page:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (targetPage > 1) targetPage-- }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text("Page $targetPage", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { targetPage++ }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val trimmedLabel = label.trim()
                        val (autoW, autoH) = calculateAutoBlockDimensions(title = trimmedLabel, isInternalLink = true)
                        onSave(block.copy(
                            label = trimmedLabel,
                            targetPage = targetPage,
                            width = autoW,
                            height = autoH
                        ))
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onDelete(block) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
        is TextContentBlock -> {
            var text by remember { mutableStateOf(block.text) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (block.isStickyNote) "Edit Sticky Note" else "Edit Text Box", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmedText = text.trim()
                            val (autoW, autoH) = calculateAutoBlockDimensions(
                                text = trimmedText,
                                isStickyNote = block.isStickyNote,
                                isTextBox = !block.isStickyNote
                            )
                            onSave(block.copy(
                                text = trimmedText,
                                width = autoW,
                                height = autoH
                            ))
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onDelete(block) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Manage Attachment", fontWeight = FontWeight.Bold) },
                text = { Text("Do you want to remove this attachment from the note?") },
                confirmButton = {
                    Button(
                        onClick = { onDelete(block) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
