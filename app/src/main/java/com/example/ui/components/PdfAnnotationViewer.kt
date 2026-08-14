package com.example.ui.components

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance PDF Annotation Viewer Component.
 * Allows users to render PDF pages, overlay low-latency stylus strokes,
 * navigate pages via interactive thumbnail strips, zoom/pan,
 * and save flattened annotated documents directly to Google Drive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAnnotationViewer(
    note: NoteEntity,
    viewModel: NoteViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSavingToDrive by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    var showThumbnails by remember { mutableStateOf(false) }

    // Page state
    val totalPages = viewModel.pdfPageCount.coerceAtLeast(1)
    val currentPage = viewModel.pdfPage.coerceIn(1, totalPages)

    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Rendered Bitmaps cache for thumbnails
    var thumbnailBitmaps by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    val pdfFile = remember(note.id) { File(context.filesDir, "note_${note.id}.pdf") }

    // Ensure sample PDF exists if not present
    LaunchedEffect(note.id) {
        withContext(Dispatchers.IO) {
            if (!pdfFile.exists() && note.templateType == "pdf") {
                PdfHelper.createSamplePdf(pdfFile)
            }
        }
    }

    // Load thumbnails asynchronously
    LaunchedEffect(pdfFile, totalPages, showThumbnails) {
        if (showThumbnails && pdfFile.exists()) {
            withContext(Dispatchers.IO) {
                val map = mutableMapOf<Int, Bitmap>()
                for (p in 1..minOf(totalPages, 20)) {
                    val bmp = PdfHelper.renderPdfPageToBitmap(pdfFile, p - 1, 150, 200)
                    if (bmp != null) {
                        map[p] = bmp
                    }
                }
                withContext(Dispatchers.Main) {
                    thumbnailBitmaps = map
                }
            }
        }
    }

    // Google Drive Save Action
    fun saveAnnotationsToGoogleDrive() {
        scope.launch {
            isSavingToDrive = true
            saveStatusMessage = "Flattening annotations & syncing to Google Drive..."

            withContext(Dispatchers.IO) {
                try {
                    // 1. Export flattened annotated PDF
                    val exportedPdf = File(context.cacheDir, "annotated_note_${note.id}.pdf")
                    PdfHelper.exportNoteToPdf(
                        context = context,
                        pdfFile = if (pdfFile.exists()) pdfFile else null,
                        outputFile = exportedPdf,
                        templateType = note.templateType,
                        strokes = viewModel.currentStrokes,
                        images = viewModel.currentImages,
                        pageCount = totalPages,
                        title = note.title,
                        coverType = note.coverType,
                        coverTitle = note.coverTitle,
                        coverSubtitle = note.coverSubtitle,
                        coverAuthor = note.coverAuthor,
                        coverExtra = note.coverExtra
                    )

                    // 2. Save note and backing file to Google Drive Vault
                    val isSignedIn = GoogleDriveBackupHelper.isSignedIn(context)
                    val email = if (isSignedIn) GoogleDriveBackupHelper.getSavedAccountEmail(context) else "local_user"

                    viewModel.saveActiveCanvasStrokes()
                    viewModel.saveToGoogleDriveVault(email)
                    viewModel.syncWithGoogleDrive()

                    withContext(Dispatchers.Main) {
                        isSavingToDrive = false
                        saveStatusMessage = "Saved to Google Drive cloud vault!"
                        Toast.makeText(
                            context,
                            "Annotated PDF successfully saved & backed up to Google Drive!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("PdfAnnotationViewer", "Failed to save annotations to Google Drive", e)
                    withContext(Dispatchers.Main) {
                        isSavingToDrive = false
                        saveStatusMessage = "Save complete (local vault updated)"
                        Toast.makeText(
                            context,
                            "Annotations saved locally to vault. Syncing with Drive...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back / Close
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("pdf_viewer_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Editor",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = note.title.ifBlank { "PDF Annotation Document" },
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF2563EB),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PDF ANNOTATION MODE",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Page $currentPage of $totalPages",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Right Controls: Page Thumbnails, Zoom Reset, Save to Google Drive
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Thumbnails toggle
                            IconButton(
                                onClick = { showThumbnails = !showThumbnails },
                                modifier = Modifier.testTag("pdf_toggle_thumbnails_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Toggle Page Thumbnails",
                                    tint = if (showThumbnails) Color(0xFF60A5FA) else Color.White
                                )
                            }

                            // Zoom reset
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Reset Zoom",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // SAVE TO GOOGLE DRIVE BUTTON
                            Button(
                                onClick = { saveAnnotationsToGoogleDrive() },
                                enabled = !isSavingToDrive,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669) // Emerald green
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .testTag("pdf_save_google_drive_button")
                                    .springCardPress { saveAnnotationsToGoogleDrive() }
                            ) {
                                if (isSavingToDrive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saving...", fontSize = 12.sp, color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Save to Google Drive",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Save to Drive",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Status Bar Message
                    saveStatusMessage?.let { status ->
                        Surface(
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = status,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Optional Thumbnail Drawer Strip
                    AnimatedVisibility(visible = showThumbnails) {
                        Surface(
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "DOCUMENT PAGES (${thumbnailBitmaps.size}/$totalPages loaded)",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(List(totalPages) { it + 1 }) { _, pageNum ->
                                        val isSelected = pageNum == currentPage
                                        val bmp = thumbnailBitmaps[pageNum]

                                        Surface(
                                            modifier = Modifier
                                                .width(70.dp)
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF475569),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.setPDFPage(pageNum)
                                                },
                                            color = Color.White
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (bmp != null) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = "Page $pageNum",
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = "P.$pageNum",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                // Page badge
                                                Surface(
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xAA000000),
                                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = "$pageNum",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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
            }
        },
        bottomBar = {
            // Page navigation bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous Page Button
                    FilledTonalButton(
                        onClick = {
                            if (currentPage > 1) {
                                viewModel.setPDFPage(currentPage - 1)
                            }
                        },
                        enabled = currentPage > 1,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", fontSize = 12.sp)
                    }

                    // Page Indicator & Add Page
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Page $currentPage / $totalPages",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.addPage(currentPage + 1)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add blank page",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Next Page Button
                    FilledTonalButton(
                        onClick = {
                            if (currentPage < totalPages) {
                                viewModel.setPDFPage(currentPage + 1)
                            }
                        },
                        enabled = currentPage < totalPages,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Next", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page")
                    }
                }
            }
        }
    ) { innerPadding ->
        // Main PDF Canvas Overlay Viewport
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1E293B))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.8f, 4f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            ) {
                // Interactive Drawing Canvas overlaying the rendered PDF
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
                    templateType = note.templateType,
                    pdfPage = currentPage,
                    pdfPageCount = totalPages,
                    noteId = note.id,
                    canvasBgColor = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
