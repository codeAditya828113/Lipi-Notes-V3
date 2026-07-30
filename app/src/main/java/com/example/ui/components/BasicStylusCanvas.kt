package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BasicStroke(
    val path: Path,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f
)

fun exportCanvasToImage(
    context: android.content.Context,
    strokes: List<BasicStroke>,
    size: IntSize,
    bgColor: Int = android.graphics.Color.WHITE
) {
    if (size.width <= 0 || size.height <= 0) return
    try {
        val bitmap = android.graphics.Bitmap.createBitmap(size.width, size.height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(bgColor)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        
        for (stroke in strokes) {
            paint.color = stroke.color.toArgb()
            paint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path.asAndroidPath(), paint)
        }
        
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "canvas_export_${System.currentTimeMillis()}.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BasicStylusCanvas(modifier: Modifier = Modifier) {
    var strokes by remember { mutableStateOf(listOf<BasicStroke>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State to force recomposition when path changes
    var pathUpdateTrigger by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            val path = Path().apply {
                                moveTo(event.x, event.y)
                            }
                            currentPath = path
                            pathUpdateTrigger++
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            currentPath?.lineTo(event.x, event.y)
                            pathUpdateTrigger++
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            currentPath?.let { path ->
                                strokes = strokes + BasicStroke(path, currentColor, currentStrokeWidth)
                            }
                            currentPath = null
                            pathUpdateTrigger++
                            true
                        }
                        else -> false
                    }
                }
        ) {
            // Read the trigger to ensure recomposition
            val trigger = pathUpdateTrigger
            
            strokes.forEach { stroke ->
                drawPath(
                    path = stroke.path,
                    color = stroke.color,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            currentPath?.let { path ->
                drawPath(
                    path = path,
                    color = currentColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { 
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            exportCanvasToImage(context, strokes, canvasSize)
                        }
                        android.widget.Toast.makeText(context, "Saved to Photos", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export Canvas")
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            FloatingActionButton(
                onClick = { showClearDialog = true },
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Canvas")
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear Canvas") },
                text = { Text("Are you sure you want to clear the entire canvas? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            strokes = emptyList()
                            currentPath = null
                            pathUpdateTrigger++
                            showClearDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

