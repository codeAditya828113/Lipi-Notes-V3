package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.NoteEntity
import com.example.pdf.LipiPdfManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Data representation of a scanned page
 */
data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val rawBitmap: Bitmap,
    var displayBitmap: Bitmap = rawBitmap,
    var corners: List<Offset> = listOf(
        Offset(0.12f, 0.12f),
        Offset(0.88f, 0.12f),
        Offset(0.88f, 0.88f),
        Offset(0.12f, 0.88f)
    ),
    var filter: String = "Auto",
    var rotationDegrees: Int = 0,
    var qualityWarning: String? = null,
    var ocrText: String? = null
)

/**
 * ScanSession Object maintaining full audit/state metadata
 */
data class ScanSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val sourceNotebookId: Int? = null,
    val pages: List<ScannedPage> = emptyList(),
    var status: String = "In Progress",
    var ocrStatus: String = "Pending",
    var suggestedTitle: String = ""
)

private enum class ScannerScreenMode {
    CAMERA,
    CROP_FILTER_ADJUST,
    REVIEW_PAGES,
    FINISH_DESTINATION
}

/**
 * Main Lipi Document Scanner Component
 * Supports Google ML Kit Document Scanner API natively, with on-device CameraX fallback.
 */
@Composable
fun LipiDocumentScanner(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("lipi_scanner_prefs", Context.MODE_PRIVATE) }

    // First use guidance dialog state
    var showFirstUseIntro by remember {
        mutableStateOf(prefs.getBoolean("show_first_use_intro", true))
    }

    // Auto capture preference
    var isAutoCaptureMode by remember {
        mutableStateOf(prefs.getBoolean("auto_capture_enabled", true))
    }

    // Camera State
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isCameraFront by remember { mutableStateOf(false) }

    // Scanned Pages State
    val scannedPages = remember { mutableStateListOf<ScannedPage>() }
    var activePageIndex by remember { mutableIntStateOf(-1) }
    var currentMode by remember { mutableStateOf(ScannerScreenMode.CAMERA) }

    // OCR State
    var isOcrRunning by remember { mutableStateOf(false) }
    var aggregatedOcrText by remember { mutableStateOf("") }
    var isOcrSearchableEnabled by remember { mutableStateOf(true) }

    // Selected Page Insert Indexes
    val selectedPageIndexesForInsert = remember { mutableStateListOf<Int>() }

    // Permissions check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to scan documents", Toast.LENGTH_SHORT).show()
        }
    }

    // Google ML Kit Document Scanner Intent Launcher
    val gmsScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val scanResult = try {
                    GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                } catch (e: Throwable) {
                    Log.w("LipiScanner", "Failed to parse GMS Document Scanning Result", e)
                    null
                }
                if (scanResult != null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val pageJpegs = scanResult.pages
                            val pdfUri = scanResult.pdf?.uri

                            if (pdfUri != null && pageJpegs.isNullOrEmpty()) {
                                // PDF returned
                                val tempPdfFile = File(context.cacheDir, "gms_scanned_${System.currentTimeMillis()}.pdf")
                                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                                    tempPdfFile.outputStream().use { output -> input.copyTo(output) }
                                }
                                val title = "Scanned Document ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}"
                                withContext(Dispatchers.Main) {
                                    viewModel.saveScannedPdfToNotebook(
                                        pdfFile = tempPdfFile,
                                        pdfTitle = title,
                                        targetNote = viewModel.activeNoteForScanner
                                    ) {
                                        Toast.makeText(context, "Scanned PDF saved to notebook!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            } else if (!pageJpegs.isNullOrEmpty()) {
                                // Pages returned
                                val newPages = mutableListOf<ScannedPage>()
                                for (page in pageJpegs) {
                                    val bitmap = PdfHelper.loadSoftwareBitmap(context, page.imageUri.toString())
                                    if (bitmap != null) {
                                        val filtered = PdfHelper.applyScanFilter(bitmap, "Auto")
                                        newPages.add(ScannedPage(rawBitmap = bitmap, displayBitmap = filtered))
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    if (newPages.isNotEmpty()) {
                                        scannedPages.clear()
                                        scannedPages.addAll(newPages)
                                        activePageIndex = scannedPages.size - 1
                                        currentMode = ScannerScreenMode.FINISH_DESTINATION
                                    } else {
                                        currentMode = ScannerScreenMode.CAMERA
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    currentMode = ScannerScreenMode.CAMERA
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e("LipiScanner", "Error processing GMS scan result", e)
                            withContext(Dispatchers.Main) {
                                currentMode = ScannerScreenMode.CAMERA
                            }
                        }
                    }
                } else {
                    currentMode = ScannerScreenMode.CAMERA
                }
            } else {
                currentMode = ScannerScreenMode.CAMERA
            }
        } catch (e: Throwable) {
            Log.e("LipiScanner", "GMS scanner activity result exception", e)
            currentMode = ScannerScreenMode.CAMERA
        }
    }

    // Function to launch Google Play Services ML Kit Scanner
    fun launchGmsScanner() {
        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(100)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                )
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()

            val scannerClient = GmsDocumentScanning.getClient(options)
            val activity = context as? ComponentActivity
            if (activity != null) {
                scannerClient.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        try {
                            gmsScannerLauncher.launch(
                                IntentSenderRequest.Builder(intentSender).build()
                            )
                        } catch (e: Throwable) {
                            Log.e("LipiScanner", "Failed to launch GMS scanner intent", e)
                            currentMode = ScannerScreenMode.CAMERA
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("LipiScanner", "GmsDocumentScanner launch failed, falling back to Camera", e)
                        currentMode = ScannerScreenMode.CAMERA
                    }
            } else {
                currentMode = ScannerScreenMode.CAMERA
            }
        } catch (e: Throwable) {
            Log.e("LipiScanner", "GmsDocumentScanner launch failed, falling back to CameraX", e)
            currentMode = ScannerScreenMode.CAMERA
        }
    }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val addedPages = mutableListOf<ScannedPage>()
                for (uri in uris) {
                    val bitmap = PdfHelper.loadSoftwareBitmap(context, uri.toString())
                    if (bitmap != null) {
                        val filtered = PdfHelper.applyScanFilter(bitmap, "Auto")
                        val page = ScannedPage(rawBitmap = bitmap, displayBitmap = filtered)
                        addedPages.add(page)
                    }
                }
                withContext(Dispatchers.Main) {
                    if (addedPages.isNotEmpty()) {
                        scannedPages.addAll(addedPages)
                        activePageIndex = scannedPages.size - 1
                        currentMode = ScannerScreenMode.CROP_FILTER_ADJUST
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .testTag("lipi_document_scanner_fullscreen")
        ) {
            if (showFirstUseIntro) {
                FirstUseIntroCard(
                    onGotIt = {
                        prefs.edit().putBoolean("show_first_use_intro", false).apply()
                        showFirstUseIntro = false
                        launchGmsScanner()
                    }
                )
            } else if (!hasCameraPermission) {
                // Permission Card
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 420.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Camera Access Needed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Lipi uses the camera to detect, straighten, and scan documents into multi-page PDFs.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { onDismiss() }) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grant Access")
                                }
                            }
                        }
                    }
                }
            } else {
                when (currentMode) {
                    ScannerScreenMode.CAMERA -> {
                        CameraScannerScreen(
                            isAutoCapture = isAutoCaptureMode,
                            flashMode = flashMode,
                            isFrontCamera = isCameraFront,
                            scannedCount = scannedPages.size,
                            onToggleAutoMode = {
                                isAutoCaptureMode = !isAutoCaptureMode
                                prefs.edit().putBoolean("auto_capture_enabled", isAutoCaptureMode).apply()
                            },
                            onToggleFlash = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            },
                            onToggleCamera = { isCameraFront = !isCameraFront },
                            onGalleryClick = { galleryLauncher.launch("image/*") },
                            onLaunchSystemScanner = { launchGmsScanner() },
                            onPageCaptured = { bitmap ->
                                val processed = PdfHelper.applyScanFilter(bitmap, "Auto")
                                val newPage = ScannedPage(rawBitmap = bitmap, displayBitmap = processed)
                                scannedPages.add(newPage)
                                activePageIndex = scannedPages.size - 1
                                currentMode = ScannerScreenMode.CROP_FILTER_ADJUST
                            },
                            onThumbnailStripClick = {
                                if (scannedPages.isNotEmpty()) {
                                    currentMode = ScannerScreenMode.REVIEW_PAGES
                                }
                            },
                            onFinishClick = {
                                if (scannedPages.isNotEmpty()) {
                                    currentMode = ScannerScreenMode.FINISH_DESTINATION
                                } else {
                                    onDismiss()
                                }
                            },
                            onCloseClick = { onDismiss() }
                        )
                    }

                    ScannerScreenMode.CROP_FILTER_ADJUST -> {
                        val activePage = scannedPages.getOrNull(activePageIndex)
                        if (activePage != null) {
                            CropFilterAdjustScreen(
                                scannedPage = activePage,
                                pageNumber = activePageIndex + 1,
                                totalPages = scannedPages.size,
                                onKeepScan = { updatedPage ->
                                    scannedPages[activePageIndex] = updatedPage
                                    currentMode = ScannerScreenMode.CAMERA
                                },
                                onDone = { updatedPage ->
                                    scannedPages[activePageIndex] = updatedPage
                                    currentMode = ScannerScreenMode.FINISH_DESTINATION
                                },
                                onRetake = {
                                    scannedPages.removeAt(activePageIndex)
                                    if (scannedPages.isEmpty()) {
                                        currentMode = ScannerScreenMode.CAMERA
                                    } else {
                                        currentMode = ScannerScreenMode.REVIEW_PAGES
                                    }
                                },
                                onClose = {
                                    currentMode = ScannerScreenMode.CAMERA
                                }
                            )
                        } else {
                            currentMode = ScannerScreenMode.CAMERA
                        }
                    }

                    ScannerScreenMode.REVIEW_PAGES -> {
                        ReviewPagesScreen(
                            pages = scannedPages,
                            onEditPage = { index ->
                                activePageIndex = index
                                currentMode = ScannerScreenMode.CROP_FILTER_ADJUST
                            },
                            onDeletePage = { index ->
                                scannedPages.removeAt(index)
                                if (scannedPages.isEmpty()) {
                                    currentMode = ScannerScreenMode.CAMERA
                                }
                            },
                            onReorderPages = { from, to ->
                                val item = scannedPages.removeAt(from)
                                scannedPages.add(to, item)
                            },
                            onAddMore = {
                                currentMode = ScannerScreenMode.CAMERA
                            },
                            onFinish = {
                                currentMode = ScannerScreenMode.FINISH_DESTINATION
                            },
                            onClose = {
                                currentMode = ScannerScreenMode.CAMERA
                            }
                        )
                    }

                    ScannerScreenMode.FINISH_DESTINATION -> {
                        FinishDestinationScreen(
                            viewModel = viewModel,
                            scannedPages = scannedPages,
                            isOcrRunning = isOcrRunning,
                            aggregatedOcrText = aggregatedOcrText,
                            isSearchableEnabled = isOcrSearchableEnabled,
                            onToggleSearchable = { isOcrSearchableEnabled = !isOcrSearchableEnabled },
                            selectedPageIndexes = selectedPageIndexesForInsert,
                            onRunOcr = {
                                isOcrRunning = true
                                scope.launch(Dispatchers.IO) {
                                    val sb = StringBuilder()
                                    // Multi-script OCR: Latin / English + Devanagari / Hindi
                                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                    val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

                                    for ((idx, page) in scannedPages.withIndex()) {
                                        try {
                                            val image = InputImage.fromBitmap(page.displayBitmap, 0)
                                            val latinResult = com.google.android.gms.tasks.Tasks.await(latinRecognizer.process(image))
                                            val devResult = com.google.android.gms.tasks.Tasks.await(devanagariRecognizer.process(image))

                                            val combinedText = buildString {
                                                if (latinResult.text.isNotBlank()) append(latinResult.text).append("\n")
                                                if (devResult.text.isNotBlank() && devResult.text != latinResult.text) {
                                                    append(devResult.text)
                                                }
                                            }.trim()

                                            if (combinedText.isNotBlank()) {
                                                sb.append("--- PAGE ${idx + 1} ---\n")
                                                sb.append(combinedText).append("\n\n")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LipiScanner", "OCR error on page ${idx + 1}", e)
                                        }
                                    }
                                    val finalOcr = sb.toString().trim()
                                    withContext(Dispatchers.Main) {
                                        isOcrRunning = false
                                        aggregatedOcrText = if (finalOcr.isNotBlank()) finalOcr else "No text recognized in scanned document."
                                    }
                                }
                            },
                            onSaveToTarget = { note, customTitle ->
                                scope.launch(Dispatchers.IO) {
                                    val pdfFile = File(context.cacheDir, "scanned_doc_${System.currentTimeMillis()}.pdf")
                                    PdfHelper.createPdfFromBitmaps(pdfFile, scannedPages.map { it.displayBitmap })
                                    val title = customTitle.ifBlank {
                                        "Scanned Doc ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}"
                                    }

                                    withContext(Dispatchers.Main) {
                                        viewModel.saveScannedPdfToNotebook(
                                            pdfFile = pdfFile,
                                            pdfTitle = title,
                                            targetNote = note,
                                            ocrText = if (isOcrSearchableEnabled) aggregatedOcrText.ifBlank { null } else null
                                        ) {
                                            Toast.makeText(context, "Scanned PDF inserted successfully!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            onInsertPagesToCurrentNote = { selectedIndexes ->
                                scope.launch(Dispatchers.IO) {
                                    for (index in selectedIndexes) {
                                        val page = scannedPages.getOrNull(index)
                                        if (page != null) {
                                            val imageFile = File(context.filesDir, "scan_page_${System.currentTimeMillis()}_$index.jpg")
                                            page.displayBitmap.compress(Bitmap.CompressFormat.JPEG, 90, imageFile.outputStream())
                                            withContext(Dispatchers.Main) {
                                                viewModel.addImageFromUri(Uri.fromFile(imageFile))
                                            }
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Inserted ${selectedIndexes.size} scanned page(s) into note!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            },
                            onEditPages = {
                                currentMode = ScannerScreenMode.REVIEW_PAGES
                            },
                            onDismiss = { onDismiss() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * First-use intro guide dialog
 */
@Composable
private fun FirstUseIntroCard(onGotIt: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF5B6DFF).copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = Color(0xFF5B6DFF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Lipi Document Scanner",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Automatic edge detection, high-contrast document filters, multi-page scanning, and instant on-device OCR searchability.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onGotIt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B6DFF))
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

enum class ScannerState {
    INITIALIZING,
    SEARCHING,
    DOCUMENT_DETECTED,
    STABILIZING,
    READY_TO_CAPTURE,
    CAPTURING,
    PROCESSING
}

data class DetectionResult(
    val state: ScannerState,
    val corners: List<Offset>,
    val confidence: Float,
    val statusText: String
)

/**
 * Computer-Vision Frame Analyzer for CameraX
 * Performs luminance variance, Sobel edge magnitude, and quadrilateral contour analysis.
 */
private class DocumentImageAnalyzer(
    private val onResult: (DetectionResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var previousCorners: List<Offset>? = null
    private var stableFrameCount = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        try {
            val planes = image.planes
            if (planes.isEmpty()) {
                imageProxy.close()
                return
            }

            val yBuffer = planes[0].buffer
            val width = image.width
            val height = image.height
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride

            val sampleW = 120
            val sampleH = 90
            val scaleX = width.toFloat() / sampleW
            val scaleY = height.toFloat() / sampleH

            val grid = IntArray(sampleW * sampleH)
            var sumLuminance = 0L

            for (y in 0 until sampleH) {
                val origY = (y * scaleY).toInt().coerceIn(0, height - 1)
                for (x in 0 until sampleW) {
                    val origX = (x * scaleX).toInt().coerceIn(0, width - 1)
                    val index = origY * rowStride + origX * pixelStride
                    if (index < yBuffer.capacity()) {
                        val lum = yBuffer.get(index).toInt() and 0xFF
                        grid[y * sampleW + x] = lum
                        sumLuminance += lum
                    }
                }
            }

            val totalPixels = sampleW * sampleH
            val meanLuminance = sumLuminance.toFloat() / totalPixels

            var sumVariance = 0.0
            for (i in 0 until totalPixels) {
                val diff = grid[i] - meanLuminance
                sumVariance += diff * diff
            }
            val stdDevLuminance = kotlin.math.sqrt(sumVariance / totalPixels)

            // False positive rejection rules:
            // Walls, ceilings, featureless tables, beds, pitch dark rooms have low variance or extreme light
            if (meanLuminance < 18f || meanLuminance > 242f || stdDevLuminance < 18.0) {
                stableFrameCount = 0
                previousCorners = null
                onResult(
                    DetectionResult(
                        state = ScannerState.SEARCHING,
                        corners = listOf(
                            Offset(0.15f, 0.20f),
                            Offset(0.85f, 0.20f),
                            Offset(0.85f, 0.80f),
                            Offset(0.15f, 0.80f)
                        ),
                        confidence = 0f,
                        statusText = "Looking for a document..."
                    )
                )
                imageProxy.close()
                return
            }

            // Sobel Edge Gradient calculation across grid
            val edgeMag = FloatArray(sampleW * sampleH)
            var totalEdgeMag = 0.0f

            for (y in 1 until sampleH - 1) {
                for (x in 1 until sampleW - 1) {
                    val p00 = grid[(y - 1) * sampleW + (x - 1)]
                    val p01 = grid[(y - 1) * sampleW + x]
                    val p02 = grid[(y - 1) * sampleW + (x + 1)]
                    val p10 = grid[y * sampleW + (x - 1)]
                    val p12 = grid[y * sampleW + (x + 1)]
                    val p20 = grid[(y + 1) * sampleW + (x - 1)]
                    val p21 = grid[(y + 1) * sampleW + x]
                    val p22 = grid[(y + 1) * sampleW + (x + 1)]

                    val gx = (p02 + 2 * p12 + p22) - (p00 + 2 * p10 + p20)
                    val gy = (p20 + 2 * p21 + p22) - (p00 + 2 * p01 + p02)
                    val mag = kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toFloat()

                    val idx = y * sampleW + x
                    edgeMag[idx] = mag
                    totalEdgeMag += mag
                }
            }

            val avgEdgeMag = totalEdgeMag / totalPixels

            // Bounding box around primary edge density
            var minX = sampleW
            var maxX = 0
            var minY = sampleH
            var maxY = 0
            val edgeThreshold = (avgEdgeMag * 1.5f).coerceAtLeast(35f)

            var edgePointCount = 0
            for (y in 2 until sampleH - 2) {
                for (x in 2 until sampleW - 2) {
                    if (edgeMag[y * sampleW + x] >= edgeThreshold) {
                        edgePointCount++
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            val boxW = (maxX - minX).coerceAtLeast(0)
            val boxH = (maxY - minY).coerceAtLeast(0)
            val boxArea = boxW * boxH
            val areaFraction = boxArea.toFloat() / totalPixels
            val aspectRatio = if (boxH > 0) boxW.toFloat() / boxH.toFloat() else 0f

            // Document detection criteria check (supports books, A4 paper, worksheets, notebooks):
            val isGenuineQuad = edgePointCount > (totalPixels * 0.04f) &&
                    areaFraction in 0.14f..0.82f &&
                    aspectRatio in 0.45f..2.3f

            if (!isGenuineQuad) {
                stableFrameCount = 0
                previousCorners = null
                onResult(
                    DetectionResult(
                        state = ScannerState.SEARCHING,
                        corners = listOf(
                            Offset(0.15f, 0.20f),
                            Offset(0.85f, 0.20f),
                            Offset(0.85f, 0.80f),
                            Offset(0.15f, 0.80f)
                        ),
                        confidence = 0f,
                        statusText = "Looking for a document..."
                    )
                )
            } else {
                var bestTLX = minX
                var bestTLY = minY
                var minSum = Int.MAX_VALUE

                var bestTRX = maxX
                var bestTRY = minY
                var maxDiff = Int.MIN_VALUE

                var bestBRX = maxX
                var bestBRY = maxY
                var maxSum = Int.MIN_VALUE

                var bestBLX = minX
                var bestBLY = maxY
                var minDiff = Int.MAX_VALUE

                for (y in minY..maxY) {
                    for (x in minX..maxX) {
                        if (edgeMag[y * sampleW + x] >= edgeThreshold) {
                            val sum = x + y
                            val diff = x - y

                            if (sum < minSum) {
                                minSum = sum
                                bestTLX = x
                                bestTLY = y
                            }
                            if (diff > maxDiff) {
                                maxDiff = diff
                                bestTRX = x
                                bestTRY = y
                            }
                            if (sum > maxSum) {
                                maxSum = sum
                                bestBRX = x
                                bestBRY = y
                            }
                            if (diff < minDiff) {
                                minDiff = diff
                                bestBLX = x
                                bestBLY = y
                            }
                        }
                    }
                }

                val normTL = Offset((bestTLX.toFloat() / sampleW).coerceIn(0.02f, 0.98f), (bestTLY.toFloat() / sampleH).coerceIn(0.02f, 0.98f))
                val normTR = Offset((bestTRX.toFloat() / sampleW).coerceIn(0.02f, 0.98f), (bestTRY.toFloat() / sampleH).coerceIn(0.02f, 0.98f))
                val normBR = Offset((bestBRX.toFloat() / sampleW).coerceIn(0.02f, 0.98f), (bestBRY.toFloat() / sampleH).coerceIn(0.02f, 0.98f))
                val normBL = Offset((bestBLX.toFloat() / sampleW).coerceIn(0.02f, 0.98f), (bestBLY.toFloat() / sampleH).coerceIn(0.02f, 0.98f))
                val currentCorners = listOf(normTL, normTR, normBR, normBL)

                val prev = previousCorners
                if (prev != null && prev.size == 4) {
                    val maxShift = currentCorners.zip(prev).maxOf { (curr, pr) ->
                        kotlin.math.abs(curr.x - pr.x) + kotlin.math.abs(curr.y - pr.y)
                    }

                    if (maxShift < 0.05f) {
                        stableFrameCount++
                    } else {
                        stableFrameCount = 0
                    }
                } else {
                    stableFrameCount = 0
                }
                previousCorners = currentCorners

                val state = when {
                    stableFrameCount >= 6 -> ScannerState.READY_TO_CAPTURE
                    stableFrameCount >= 2 -> ScannerState.STABILIZING
                    else -> ScannerState.DOCUMENT_DETECTED
                }

                val statusText = when (state) {
                    ScannerState.READY_TO_CAPTURE -> "Ready"
                    ScannerState.STABILIZING -> "Hold steady..."
                    ScannerState.DOCUMENT_DETECTED -> "Document detected"
                    else -> "Looking for a document..."
                }

                onResult(
                    DetectionResult(
                        state = state,
                        corners = currentCorners,
                        confidence = (stableFrameCount / 6f).coerceIn(0.3f, 1f),
                        statusText = statusText
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("LipiScanner", "Document analysis exception", e)
        } finally {
            imageProxy.close()
        }
    }
}

/**
 * CAMERA SCANNER SCREEN
 */
@Composable
private fun CameraScannerScreen(
    isAutoCapture: Boolean,
    flashMode: Int,
    isFrontCamera: Boolean,
    scannedCount: Int,
    onToggleAutoMode: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleCamera: () -> Unit,
    onGalleryClick: () -> Unit,
    onLaunchSystemScanner: () -> Unit,
    onPageCaptured: (Bitmap) -> Unit,
    onThumbnailStripClick: () -> Unit,
    onFinishClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var currentScannerState by remember { mutableStateOf(ScannerState.SEARCHING) }
    var statusText by remember { mutableStateOf("Looking for a document...") }
    var isDocumentDetected by remember { mutableStateOf(false) }
    var isStableForCapture by remember { mutableStateOf(false) }
    var cameraBindError by remember { mutableStateOf<String?>(null) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var detectedCorners by remember { mutableStateOf<List<Offset>?>(null) }

    // Animated document bounds
    val animatedTopLeft = remember { Animatable(Offset(0.15f, 0.20f), Offset.VectorConverter) }
    val animatedTopRight = remember { Animatable(Offset(0.85f, 0.20f), Offset.VectorConverter) }
    val animatedBottomRight = remember { Animatable(Offset(0.85f, 0.80f), Offset.VectorConverter) }
    val animatedBottomLeft = remember { Animatable(Offset(0.15f, 0.80f), Offset.VectorConverter) }

    var captureTriggered by remember { mutableStateOf(false) }

    fun performRealCapture() {
        val ic = imageCapture
        if (ic == null) {
            Toast.makeText(context, "Couldn't capture the document. Camera not ready.", Toast.LENGTH_SHORT).show()
            captureTriggered = false
            return
        }

        isCapturingPhoto = true
        currentScannerState = ScannerState.CAPTURING
        statusText = "Capturing..."

        val photoFile = File(context.cacheDir, "scan_raw_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        ic.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    statusText = "Cleaning document..."
                    scope.launch(Dispatchers.IO) {
                        try {
                            val rawBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            var bitmap = rawBitmap
                            if (rawBitmap != null && rawBitmap.width > 0 && rawBitmap.height > 0) {
                                try {
                                    val exif = android.media.ExifInterface(photoFile.absolutePath)
                                    val orientation = exif.getAttributeInt(
                                        android.media.ExifInterface.TAG_ORIENTATION,
                                        android.media.ExifInterface.ORIENTATION_UNDEFINED
                                    )
                                    val rotationDegrees = when (orientation) {
                                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                                        else -> 0
                                    }
                                    if (rotationDegrees != 0) {
                                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                        bitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                    }
                                } catch (e: Exception) {
                                    Log.w("LipiScanner", "Exif read failed: ${e.message}")
                                }

                                Log.d("LipiScanner", "Captured real document photo: ${bitmap.width}x${bitmap.height}")
                                val corners = detectedCorners
                                val cropped = if (corners != null && corners.size == 4) {
                                    PdfHelper.cropBitmapPerspective(bitmap, corners)
                                } else bitmap

                                val filtered = PdfHelper.applyScanFilter(cropped, "Auto")

                                withContext(Dispatchers.Main) {
                                    isCapturingPhoto = false
                                    captureTriggered = false
                                    currentScannerState = ScannerState.SEARCHING
                                    onPageCaptured(filtered)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isCapturingPhoto = false
                                    captureTriggered = false
                                    currentScannerState = ScannerState.SEARCHING
                                    Toast.makeText(context, "Couldn't capture the document. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e("LipiScanner", "Error processing photo capture", e)
                            withContext(Dispatchers.Main) {
                                isCapturingPhoto = false
                                captureTriggered = false
                                currentScannerState = ScannerState.SEARCHING
                                Toast.makeText(context, "Couldn't capture the document. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("LipiScanner", "Camera capture error", exc)
                    isCapturingPhoto = false
                    captureTriggered = false
                    currentScannerState = ScannerState.SEARCHING
                    Toast.makeText(context, "Couldn't capture the document. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            boundCameraProvider?.unbindAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Preview View with ImageAnalysis
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            boundCameraProvider = cameraProvider
                            val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                            if (cameraProvider.hasCamera(cameraSelector)) {
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setFlashMode(flashMode)
                                    .setTargetRotation(previewView.display.rotation)
                                    .build()
                                imageCapture = capture

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setTargetResolution(android.util.Size(640, 480))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(
                                    ContextCompat.getMainExecutor(ctx),
                                    DocumentImageAnalyzer { result ->
                                        if (!isCapturingPhoto) {
                                            currentScannerState = result.state
                                            statusText = result.statusText
                                            isDocumentDetected = (result.state != ScannerState.SEARCHING)
                                            isStableForCapture = (result.state == ScannerState.READY_TO_CAPTURE)

                                            if (result.corners.size == 4) {
                                                detectedCorners = result.corners
                                                scope.launch {
                                                    animatedTopLeft.animateTo(result.corners[0], tween(100))
                                                    animatedTopRight.animateTo(result.corners[1], tween(100))
                                                    animatedBottomRight.animateTo(result.corners[2], tween(100))
                                                    animatedBottomLeft.animateTo(result.corners[3], tween(100))
                                                }
                                            }

                                            if (isAutoCapture && result.state == ScannerState.READY_TO_CAPTURE && !captureTriggered && cameraBindError == null) {
                                                captureTriggered = true
                                                performRealCapture()
                                            }
                                        }
                                    }
                                )

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture,
                                    imageAnalysis
                                )
                            } else {
                                cameraBindError = "Camera not available on this device"
                            }
                        } catch (e: Throwable) {
                            Log.e("LipiScanner", "CameraX binding error", e)
                            cameraBindError = "Camera preview unavailable"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                } catch (e: Throwable) {
                    Log.e("LipiScanner", "ProcessCameraProvider error", e)
                    cameraBindError = "Camera provider unavailable"
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (cameraBindError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraEnhance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Preview Unavailable",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Physical camera hardware is limited in the current environment. You can select document images from your Gallery or simulate a document scan.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onGalleryClick() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import Document from Gallery", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { onPageCaptured(createSampleDocumentBitmap()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Sample Document Scan", fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = { onCloseClick() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close Scanner")
                            }
                        }
                    }
                }
            }
        }

        // Darkened Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        radius = 1200f
                    )
                )
        )

        // Document Edge Boundary Overlay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val tl = Offset(animatedTopLeft.value.x * w, animatedTopLeft.value.y * h)
            val tr = Offset(animatedTopRight.value.x * w, animatedTopRight.value.y * h)
            val br = Offset(animatedBottomRight.value.x * w, animatedBottomRight.value.y * h)
            val bl = Offset(animatedBottomLeft.value.x * w, animatedBottomLeft.value.y * h)

            val path = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }

            val strokeColor = when {
                isStableForCapture -> Color(0xFF10B981) // Green
                isDocumentDetected -> Color(0xFF5B6DFF) // Lipi Blue
                else -> Color.White.copy(alpha = 0.5f)
            }

            // Fill tint
            drawPath(
                path = path,
                color = strokeColor.copy(alpha = 0.12f)
            )

            // Outer stroke
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 3.dp.toPx())
            )

            fun drawCornerBrackets(center: Offset) {
                drawCircle(color = strokeColor, radius = 6.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }

            drawCornerBrackets(tl)
            drawCornerBrackets(tr)
            drawCornerBrackets(br)
            drawCornerBrackets(bl)
        }

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onCloseClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Status Pill & Mode Switcher
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isStableForCapture) Color(0xFF10B981) else Color(0xFF5B6DFF),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    VerticalDivider(modifier = Modifier.height(14.dp), color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isAutoCapture) "AUTO" else "MANUAL",
                        color = Color(0xFF4DA3FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onToggleAutoMode() }
                    )
                }
            }

            // Flash & Switch Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onToggleFlash() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "Flash",
                            tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) Color(0xFFFFD166) else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Capture & Controls Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Picker Button
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable { onGalleryClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Shutter / Capture Button
                Box(contentAlignment = Alignment.Center) {
                    if (isStableForCapture && isAutoCapture) {
                        PulsingAutoCaptureRing()
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, Color(0xFF5B6DFF), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                performRealCapture()
                            }
                            .testTag("camera_shutter_capture_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color(0xFF5B6DFF), CircleShape)
                            )
                        }
                    }
                }

                // Page Count Thumbnail Badge / Done Button
                if (scannedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF5B6DFF),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onFinishClick() }
                            .testTag("finish_scanned_pdf_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Done ($scannedCount)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(52.dp))
                }
            }
        }
    }
}

/**
 * CROP & FILTER ADJUSTMENT SCREEN
 */
@Composable
private fun CropFilterAdjustScreen(
    scannedPage: ScannedPage,
    pageNumber: Int,
    totalPages: Int,
    onKeepScan: (ScannedPage) -> Unit,
    onDone: (ScannedPage) -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(scannedPage.filter) }
    var currentRotation by remember { mutableIntStateOf(scannedPage.rotationDegrees) }

    // Display Bitmap
    val displayBitmap = remember(selectedFilter, currentRotation) {
        var bmp = PdfHelper.applyScanFilter(scannedPage.rawBitmap, selectedFilter)
        if (currentRotation != 0) {
            val matrix = Matrix().apply { postRotate(currentRotation.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }
        bmp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = "Adjust Page $pageNumber of $totalPages",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            IconButton(onClick = { currentRotation = (currentRotation + 90) % 360 }) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
            }
        }

        // Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = "Page Preview",
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
        }

        // Filters Selector Row
        Text(
            text = "SCAN FILTER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val filters = listOf("Auto", "Clean Shadow", "High Contrast B&W", "Grayscale", "Vibrant Color", "Original")
            itemsIndexed(filters) { _, filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF5B6DFF) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF5B6DFF) else Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Bottom Action Bar - 3 Options: Retake, Keep Scanning, Done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Option 1: Retake
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retake", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Option 2: Keep Scanning
            OutlinedButton(
                onClick = {
                    onKeepScan(
                        scannedPage.copy(
                            displayBitmap = displayBitmap,
                            filter = selectedFilter,
                            rotationDegrees = currentRotation
                        )
                    )
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF4DA3FF)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4DA3FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Keep Scanning", color = Color(0xFF4DA3FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Option 3: Done
            Button(
                onClick = {
                    onDone(
                        scannedPage.copy(
                            displayBitmap = displayBitmap,
                            filter = selectedFilter,
                            rotationDegrees = currentRotation
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B6DFF)),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * REVIEW MULTI-PAGE SCREEN
 */
@Composable
private fun ReviewPagesScreen(
    pages: List<ScannedPage>,
    onEditPage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onReorderPages: (Int, Int) -> Unit,
    onAddMore: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = "${pages.size} Scanned Page${if (pages.size > 1) "s" else ""}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            TextButton(onClick = onFinish) {
                Text("Done", color = Color(0xFF4DA3FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (isLandscape) {
            // Responsive Tablet Landscape Split View
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Thumbnail Rail
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Page", fontSize = 13.sp)
                    }

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(pages) { index, page ->
                            val isSelected = index == selectedIndex
                            Box(
                                modifier = Modifier
                                    .size(120.dp, 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        2.dp,
                                        if (isSelected) Color(0xFF5B6DFF) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedIndex = index }
                            ) {
                                Image(
                                    bitmap = page.displayBitmap.asImageBitmap(),
                                    contentDescription = "Thumbnail ${index + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(bottomStart = 8.dp),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Selected Page Preview
                val currentSelectedPage = pages.getOrNull(selectedIndex)
                if (currentSelectedPage != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = currentSelectedPage.displayBitmap.asImageBitmap(),
                            contentDescription = "Selected Page",
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
        } else {
            // Portrait View
            val currentPage = pages.getOrNull(selectedIndex) ?: pages.firstOrNull()
            if (currentPage != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentPage.displayBitmap.asImageBitmap(),
                        contentDescription = "Current Page",
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Bottom Thumbnail Rail
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .size(70.dp, 90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAddMore() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Page", tint = Color.White)
                            Text("Add Page", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                itemsIndexed(pages) { index, page ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(70.dp, 90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                2.dp,
                                if (isSelected) Color(0xFF5B6DFF) else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedIndex = index }
                    ) {
                        Image(
                            bitmap = page.displayBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Control Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onEditPage(selectedIndex) }) {
                    Icon(Icons.Default.Tune, contentDescription = "Edit", tint = Color.White)
                }

                IconButton(onClick = { onDeletePage(selectedIndex) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                }

                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B6DFF))
                ) {
                    Text("Done (${pages.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * FINISH DESTINATION SCREEN
 */
@Composable
private fun FinishDestinationScreen(
    viewModel: NoteViewModel,
    scannedPages: List<ScannedPage>,
    isOcrRunning: Boolean,
    aggregatedOcrText: String,
    isSearchableEnabled: Boolean,
    onToggleSearchable: () -> Unit,
    selectedPageIndexes: List<Int>,
    onRunOcr: () -> Unit,
    onSaveToTarget: (NoteEntity?, String) -> Unit,
    onInsertPagesToCurrentNote: (List<Int>) -> Unit,
    onEditPages: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeNote = viewModel.selectedNote
    var documentTitle by remember {
        mutableStateOf(
            if (activeNote != null) "${activeNote.title} (Scanned)"
            else "Scanned Doc ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}"
        )
    }

    LaunchedEffect(Unit) {
        if (aggregatedOcrText.isBlank()) {
            onRunOcr()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    "${scannedPages.size} Page${if (scannedPages.size > 1) "s" else ""} Scanned",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                TextButton(onClick = onEditPages) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF4DA3FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Pages", color = Color(0xFF4DA3FF), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val firstThumb = scannedPages.firstOrNull()?.displayBitmap
                        if (firstThumb != null) {
                            Image(
                                bitmap = firstThumb.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp, 72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEditPages() }
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(56.dp, 72.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF5B6DFF).copy(alpha = 0.2f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF5B6DFF))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = documentTitle,
                                onValueChange = { documentTitle = it },
                                label = { Text("Document Title", color = Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF5B6DFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${scannedPages.size} Scanned Page(s)",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )

                                OutlinedButton(
                                    onClick = onEditPages,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF4DA3FF).copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF4DA3FF), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Pages", color = Color(0xFF4DA3FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // OCR Searchable Toggle ("Make Searchable")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF4DA3FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Make Searchable (ML Kit OCR)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    if (isOcrRunning) "Recognizing text on device..." else "Enables offline search in Lipi",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isSearchableEnabled,
                            onCheckedChange = { onToggleSearchable() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF5B6DFF))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Destination Actions: Add to Notebook / Save PDF
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeNote != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSaveToTarget(activeNote, documentTitle) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5B6DFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Add to Notebook", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Attach PDF to '${activeNote.title}'", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSaveToTarget(null, documentTitle) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF4DA3FF), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Save PDF & Create Notebook", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Exports searchable multi-page PDF to library", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }

                if (activeNote != null && scannedPages.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onInsertPagesToCurrentNote(scannedPages.indices.toList())
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Insert Pages onto Note Canvas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Write & annotate over scanned page images", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Privacy Assurance Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Processed 100% on device • No cloud uploads",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PulsingAutoCaptureRing() {
    val transition = rememberInfiniteTransition(label = "AutoCapturePulse")
    val scale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(scale)
            .border(3.dp, Color(0xFF10B981), CircleShape)
    )
}

/**
 * Creates a high-legibility sample document bitmap for fallback simulation
 */
private fun createSampleDocumentBitmap(): Bitmap {
    val w = 800
    val h = 1100
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

    paint.color = android.graphics.Color.rgb(91, 109, 255)
    paint.textSize = 56f
    paint.isFakeBoldText = true
    canvas.drawText("LIPI SCANNED DOCUMENT", 80f, 160f, paint)

    paint.color = android.graphics.Color.LTGRAY
    paint.strokeWidth = 4f
    canvas.drawLine(80f, 200f, w - 80f, 200f, paint)

    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 32f
    paint.isFakeBoldText = false

    var y = 280f
    val sampleLines = listOf(
        "Subject: Lecture Notes & Vector Drawing Algorithms",
        "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
        "",
        "1. Executive Summary & Overview:",
        "This document was scanned using Lipi's native Android",
        "Document Scanner with automatic edge detection and ML Kit OCR.",
        "",
        "2. Key Takeaways & Action Items:",
        "• Automatic perspective correction and corner straightening",
        "• Enhanced contrast for high-legibility handwriting scanning",
        "• Multi-page PDF generation & instant notebook embedding",
        "• Local text extraction and notebook searchability",
        "",
        "3. Stylus & Vector Drawing Notes:",
        "Superposed strokes are rendered with sub-pixel precision.",
        "Smooth palm rejection enabled across all tablet screens."
    )

    for (line in sampleLines) {
        canvas.drawText(line, 80f, y, paint)
        y += 54f
    }

    return bitmap
}
