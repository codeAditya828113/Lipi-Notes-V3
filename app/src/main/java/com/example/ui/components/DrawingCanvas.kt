package com.example.ui.components

import androidx.compose.animation.*
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.example.data.Point
import com.example.data.Stroke
import com.example.data.FadingStroke
import com.example.ui.components.NoteViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfPageSize(val width: Float, val height: Float)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    strokes: List<Stroke>,
    fadingStrokes: List<com.example.data.FadingStroke> = emptyList(),
    fadingTicker: Long = 0L,
    images: List<com.example.data.ImageElement> = emptyList(),
    currentStroke: Stroke?,
    onStrokeStarted: (Point) -> Unit,
    onStrokeDragged: (List<Point>) -> Unit,
    onStrokeEnded: () -> Unit,
    onImageUpdated: (Int, com.example.data.ImageElement) -> Unit = {_,_->},
    templateType: String,
    pdfPage: Int, // 1..pdfPageCount
    noteId: Int? = null,
    modifier: Modifier = Modifier,
    canvasBgColor: Color = Color(0xFFFAF9F6), // Soft ivory paper background
    canvasMode: String = "fixed", // "fixed", "infinite"
    lassoSelectedStrokes: List<Stroke> = emptyList(),
    lassoDragOffset: Offset = Offset.Zero,
    lassoScaleX: Float = 1f,
    lassoScaleY: Float = 1f,
    lassoBoundingBox: Rect? = null,
    lassoSolidLine: Boolean = false,
    stylusOnlyDrawing: Boolean = false,
    onStylusDoubleTap: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    pdfPageCount: Int = 1,
    onPageSelected: (Int) -> Unit = {},
    isRulerActive: Boolean = false,
    onShapeLongPressed: (Stroke) -> Unit = {},
    onLassoDrag: (Offset) -> Unit = {},
    onLassoScaleUpdated: (Float, Float) -> Unit = {_,_->}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val pdfFile = remember(noteId) {
        if (noteId != null) File(context.filesDir, "note_$noteId.pdf") else null
    }

    // Infinite/Scrollable Canvas Offset and Scale
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isZoomLocked by remember { mutableStateOf(false) }

    // Ruler States
    var rulerOffset by remember { mutableStateOf(Offset(400f, 600f)) }
    var rulerAngle by remember { mutableStateOf(0f) }
    var activeRulerInteraction by remember { mutableStateOf<String?>(null) } // "drag", "rotate", or null
    val rulerW = 180f
    val rulerH = 1000f

    // Multi-touch tracking states
    var activeLassoInteraction by remember { mutableStateOf<String?>(null) } // "move", "resize"
    var lastLassoTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var initialLassoScaleX by remember { mutableStateOf(1f) }
    var initialLassoScaleY by remember { mutableStateOf(1f) }
    var potentialShapeStroke by remember { mutableStateOf<Stroke?>(null) }
    var lastShapeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var imageDragOffset by remember { mutableStateOf(Offset.Zero) }
    var imageResizeScale by remember { mutableStateOf(1f) }
    var activeImageInteraction by remember { mutableStateOf<String?>(null) } // "drag", "resize", null

    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var potentialImageIndex by remember { mutableStateOf<Int?>(null) }
    var pendingStrokeDownPoint by remember { mutableStateOf<com.example.data.Point?>(null) }
    var pendingStrokeTouchedPage by remember { mutableStateOf(1) }
    var strokeStartedPage by remember { mutableStateOf(1) }

    var isZooming by remember { mutableStateOf(false) }
    var showZoomIndicator by remember { mutableStateOf(false) }
    var initialSpacing by remember { mutableStateOf(0f) }
    var initialScale by remember { mutableStateOf(1f) }
    var initialPivot by remember { mutableStateOf(Offset.Zero) }
    var initialOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale, isZooming) {
        if (isZooming) {
            showZoomIndicator = true
        } else {
            showZoomIndicator = true
            delay(2000)
            showZoomIndicator = false
        }
    }

    // Stylus double-tap / gesture and Finger panning states
    var lastFingerDragPoint by remember { mutableStateOf<Offset?>(null) }
    var lastStylusDownTime by remember { mutableStateOf(0L) }
    var lastStylusDownX by remember { mutableStateOf(0f) }
    var lastStylusDownY by remember { mutableStateOf(0f) }
    var isWritingStartedOnPage by remember { mutableStateOf(false) }

    // Reset translation if switched back to fixed page mode (unless it's a PDF note, which scrolls)
    LaunchedEffect(canvasMode) {
        if (canvasMode == "fixed" && templateType != "pdf" && templateType != "docx") {
            scale = 1f
            offset = Offset.Zero
        }
    }

    val actualBgColor = if (isDarkTheme && canvasBgColor == Color(0xFFFFFFFF)) Color(0xFF121620) else canvasBgColor
    val isDarkPaperCanvas = isDarkTheme || (0.299f * actualBgColor.red + 0.587f * actualBgColor.green + 0.114f * actualBgColor.blue) < 0.45f
    BoxWithConstraints(modifier = modifier.background(actualBgColor)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val heightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1)

        // Cache management for PDF bitmaps and sizes
        var pdfPageSizes by remember { mutableStateOf<List<PdfPageSize>>(emptyList()) }
        var pdfBitmaps by remember { mutableStateOf<Map<Int, android.graphics.Bitmap>>(emptyMap()) }

        LaunchedEffect(pdfFile) {
            if (pdfFile != null && pdfFile.exists() && (templateType == "pdf" || templateType == "docx")) {
                withContext(Dispatchers.IO) {
                    try {
                        val input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = android.graphics.pdf.PdfRenderer(input)
                        val list = mutableListOf<PdfPageSize>()
                        for (i in 0 until renderer.pageCount) {
                            val page = renderer.openPage(i)
                            list.add(PdfPageSize(page.width.toFloat(), page.height.toFloat()))
                            page.close()
                        }
                        renderer.close()
                        input.close()
                        pdfPageSizes = list
                    } catch (e: Exception) {
                        Log.e("DrawingCanvas", "Failed to get PDF page sizes", e)
                        pdfPageSizes = emptyList()
                    }
                }
            } else {
                pdfPageSizes = emptyList()
            }
        }

        // Compute which pages are currently visible on screen (plus a padding buffer)
        val visiblePages = remember(offset, scale, pdfPageCount, heightPx, pdfPageSizes, widthPx) {
            val visible = mutableSetOf<Int>()
            val visibleStart = -offset.y / scale
            val visibleEnd = (-offset.y + heightPx) / scale
            for (p in 1..pdfPageCount) {
                var pTop = 0f
                for (i in 1 until p) {
                    val originalSize = pdfPageSizes.getOrNull(i - 1)
                    val pH = if (originalSize != null) {
                        val scaleX = widthPx.toFloat() / originalSize.width
                        val scaleY = heightPx.toFloat() / originalSize.height
                        val s = kotlin.math.min(scaleX, scaleY)
                        originalSize.height * s
                    } else {
                        heightPx.toFloat()
                    }
                    pTop += pH
                }
                
                val originalSize = pdfPageSizes.getOrNull(p - 1)
                val pH = if (originalSize != null) {
                    val scaleX = widthPx.toFloat() / originalSize.width
                    val scaleY = heightPx.toFloat() / originalSize.height
                    val s = kotlin.math.min(scaleX, scaleY)
                    originalSize.height * s
                } else {
                    heightPx.toFloat()
                }

                val buffer = 400f // load pages 400px before/after they enter visible viewport
                if (pTop + pH >= visibleStart - buffer && pTop <= visibleEnd + buffer) {
                    visible.add(p)
                }
            }
            if (visible.isEmpty() && pdfPageCount >= 1) {
                visible.add(1)
            }
            visible
        }

        // Lazy render/load visible pages, remove pages that scrolled out of view safely
        LaunchedEffect(visiblePages, pdfFile, widthPx, heightPx) {
            if (pdfFile == null || !pdfFile.exists() || (templateType != "pdf" && templateType != "docx")) {
                pdfBitmaps = emptyMap()
                return@LaunchedEffect
            }

            withContext(Dispatchers.IO) {
                val updatedBitmaps = pdfBitmaps.toMutableMap()
                
                // 1. Remove bitmaps for pages that are no longer visible/needed
                val iterator = updatedBitmaps.iterator()
                var removedAny = false
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val p = entry.key
                    val isNearVisible = visiblePages.contains(p) || 
                                       visiblePages.contains(p - 1) || 
                                       visiblePages.contains(p + 1)
                    if (!isNearVisible) {
                        iterator.remove()
                        removedAny = true
                    }
                }
                
                // 2. Render new visible pages asynchronously
                var changed = false
                for (p in visiblePages) {
                    if (!updatedBitmaps.containsKey(p)) {
                        val bitmap = PdfHelper.renderPdfPageToBitmap(pdfFile, p - 1, widthPx, heightPx)
                        if (bitmap != null && !bitmap.isRecycled) {
                            updatedBitmaps[p] = bitmap
                            changed = true
                        }
                    }
                }
                
                if (changed || removedAny || updatedBitmaps.size != pdfBitmaps.size) {
                    pdfBitmaps = updatedBitmaps
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                pdfBitmaps = emptyMap()
            }
        }

        val strokePathCache = remember { HashMap<Stroke, Path>() }
        val imageBitmaps = rememberImageBitmaps(images)
        LaunchedEffect(widthPx, heightPx, templateType, pdfPageCount) {
            strokePathCache.clear()
        }

        val getPageWidth: (Int) -> Float = { p ->
            val originalSize = pdfPageSizes.getOrNull(p - 1)
            if (originalSize != null && originalSize.width > 0f) {
                val scaleX = widthPx.toFloat() / originalSize.width
                val scaleY = heightPx.toFloat() / originalSize.height
                val s = kotlin.math.min(scaleX, scaleY)
                originalSize.width * s
            } else {
                if (widthPx > heightPx) {
                    kotlin.math.min(widthPx.toFloat(), heightPx.toFloat() * (600f / 800f))
                } else {
                    widthPx.toFloat()
                }
            }
        }
        val getPageHeight: (Int) -> Float = { p ->
            val originalSize = pdfPageSizes.getOrNull(p - 1)
            if (originalSize != null && originalSize.width > 0f) {
                val scaleX = widthPx.toFloat() / originalSize.width
                val scaleY = heightPx.toFloat() / originalSize.height
                val s = kotlin.math.min(scaleX, scaleY)
                originalSize.height * s
            } else {
                getPageWidth(p) * (800f / 600f)
            }
        }
        val pdfW = getPageWidth(1)
        val pdfH = getPageHeight(1)
        val getPageLeft: (Int) -> Float = { p ->
            (widthPx.toFloat() - getPageWidth(p)) / 2f
        }
        val getPageTop: (Int) -> Float = { p ->
            var top = 0f
            for (i in 1 until p) {
                top += getPageHeight(i)
            }
            top
        }
        val getNormH: (Int) -> Float = { p ->
            val originalSize = pdfPageSizes.getOrNull(p - 1)
            if (originalSize != null && originalSize.width > 0f) {
                600f * (originalSize.height / originalSize.width)
            } else {
                800f
            }
        }
        val toNormalizedX: (Float, Int) -> Float = { xVal, p ->
            val pW = getPageWidth(p)
            val pL = getPageLeft(p)
            val localX = xVal - pL
            (localX / pW) * 600f
        }
        val toNormalizedY: (Float, Int) -> Float = { yVal, p ->
            val pH = getPageHeight(p)
            val pTop = getPageTop(p)
            val localY = yVal - pTop
            (localY / pH) * getNormH(p)
        }
        val fromNormalizedX: (Float, Int) -> Float = { nx, p ->
            val pW = getPageWidth(p)
            val pL = getPageLeft(p)
            (nx / 600f) * pW + pL
        }
        val fromNormalizedY: (Float, Int) -> Float = { ny, p ->
            val pH = getPageHeight(p)
            val pTop = getPageTop(p)
            (ny / getNormH(p)) * pH + pTop
        }

        // Sync visible page center to viewmodel selection
        LaunchedEffect(offset.y, pdfPageCount) {
            val pageHVal = getPageHeight(1)
            if (pageHVal > 0f) {
                val visiblePage = ((-offset.y + heightPx / 2f) / pageHVal).toInt() + 1
                val coercedPage = visiblePage.coerceIn(1, pdfPageCount)
                onPageSelected(coercedPage)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInteropFilter { motionEvent ->
                    // Multi-pointer & Palm Rejection logic
                    var activePointerIndex = 0
                    var stylusPointerIndex: Int? = null
                    for (p in 0 until motionEvent.pointerCount) {
                        val tool = motionEvent.getToolType(p)
                        if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) {
                            stylusPointerIndex = p
                            break
                        }
                    }

                    val isStylus = stylusPointerIndex != null
                    if (isStylus) {
                        activePointerIndex = stylusPointerIndex!!
                    } else {
                        activePointerIndex = 0
                    }

                    val primaryTool = motionEvent.getToolType(activePointerIndex)
                    val isFinger = primaryTool == MotionEvent.TOOL_TYPE_FINGER || primaryTool == MotionEvent.TOOL_TYPE_UNKNOWN

                    val x = motionEvent.getX(activePointerIndex)
                    val y = motionEvent.getY(activePointerIndex)
                    
                    val action = motionEvent.actionMasked
                    val isMultiPage = templateType == "pdf" || templateType == "docx" || pdfPageCount > 1
                    val isNormalizedCoords = true

                    // Palm Rejection & Palm Contact Area Detection
                    val touchMajor = try { motionEvent.getTouchMajor(activePointerIndex) } catch (e: Exception) { 0f }
                    val touchMinor = try { motionEvent.getTouchMinor(activePointerIndex) } catch (e: Exception) { 0f }
                    val isLargePalmContact = isFinger && (touchMajor > 38f || touchMinor > 38f)

                    if (isLargePalmContact && !isStylus) {
                        // Absorb and suppress large palm rest contact
                        return@pointerInteropFilter true
                    }

                    // 1. Ruler Touch Interaction (Move / Rotate)
                    if (isRulerActive) {
                        val dxR = x - rulerOffset.x
                        val dyR = y - rulerOffset.y
                        val angleRad = Math.toRadians(rulerAngle.toDouble())
                        val cosR = Math.cos(-angleRad).toFloat()
                        val sinR = Math.sin(-angleRad).toFloat()
                        val localXR = dxR * cosR - dyR * sinR
                        val localYR = dxR * sinR + dyR * cosR

                        if (action == MotionEvent.ACTION_DOWN) {
                            if (localXR in (-rulerW / 2)..rulerW / 2 && localYR in (-rulerH / 2)..rulerH / 2) {
                                // Check if touch is near center pivot circle (say within 60px) for rotation
                                val distCenter = kotlin.math.hypot(localXR, localYR)
                                if (distCenter < 60f) {
                                    activeRulerInteraction = "rotate"
                                } else {
                                    activeRulerInteraction = "drag"
                                }
                                lastFingerDragPoint = Offset(x, y)
                                return@pointerInteropFilter true
                            }
                        }
                    }

                    if (isRulerActive && activeRulerInteraction != null) {
                        if (action == MotionEvent.ACTION_MOVE) {
                            if (activeRulerInteraction == "drag") {
                                val lastPoint = lastFingerDragPoint ?: Offset(x, y)
                                rulerOffset = rulerOffset + Offset(x - lastPoint.x, y - lastPoint.y)
                                lastFingerDragPoint = Offset(x, y)
                            } else if (activeRulerInteraction == "rotate") {
                                val currentAngle = Math.toDegrees(Math.atan2((y - rulerOffset.y).toDouble(), (x - rulerOffset.x).toDouble())).toFloat()
                                rulerAngle = currentAngle
                            }
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            activeRulerInteraction = null
                            lastFingerDragPoint = null
                        }
                        return@pointerInteropFilter true
                    }

                    // Image Interaction (Select, Move, Resize, Delete via LongPress / Corner drag)
                    if (action == MotionEvent.ACTION_DOWN) {
                        // Find if an image is touched
                        var touchedImageIndex: Int? = null
                        var isResize = false
                        for (i in images.indices.reversed()) {
                            val img = images[i]
                            val imgPage = img.page.coerceIn(1, pdfPageCount)
                            val renderX = fromNormalizedX(img.x, imgPage)
                            val renderY = fromNormalizedY(img.y, imgPage)
                            val renderW = (img.width / 600f) * getPageWidth(imgPage)
                            val renderH = (img.height / getNormH(imgPage)) * getPageHeight(imgPage)
                            
                            // apply canvas offset and scale to touch point to get world coordinates
                            val worldX = (x - widthPx / 2f - offset.x) / scale + widthPx / 2f
                            val worldY = (y - offset.y) / scale
                            
                            // Check resize handle (bottom right corner 40x40 area)
                            val handleSize = 40f
                            if (worldX >= renderX + renderW - handleSize && worldX <= renderX + renderW + handleSize &&
                                worldY >= renderY + renderH - handleSize && worldY <= renderY + renderH + handleSize) {
                                touchedImageIndex = i
                                isResize = true
                                break
                            } else if (worldX >= renderX && worldX <= renderX + renderW &&
                                worldY >= renderY && worldY <= renderY + renderH) {
                                touchedImageIndex = i
                                isResize = false
                                break
                            }
                        }
                        
                        if (touchedImageIndex != null) {
                            if (isResize) {
                                selectedImageIndex = touchedImageIndex
                                activeImageInteraction = "resize"
                                lastFingerDragPoint = Offset(x, y)
                                return@pointerInteropFilter true
                            }
                            
                            if (selectedImageIndex == touchedImageIndex) {
                                // Already selected, drag immediately
                                activeImageInteraction = "drag"
                                lastFingerDragPoint = Offset(x, y)
                                return@pointerInteropFilter true
                            } else {
                                // Potential long press
                                potentialImageIndex = touchedImageIndex
                                lastFingerDragPoint = Offset(x, y)
                                
                                // Compute stroke start parameters manually for delayed execution
                                val pivotX = widthPx / 2f
                                val mappedX = (x - pivotX - offset.x) / scale + pivotX
                                val mappedY = (y - offset.y) / scale
                                
                                val touchedPage = if (isMultiPage) {
                                    var pageIdx = 1
                                    var accumulatedHeight = 0f
                                    for (p in 1..pdfPageCount) {
                                        val pH = getPageHeight(p)
                                        if (mappedY >= accumulatedHeight && mappedY < accumulatedHeight + pH) {
                                            pageIdx = p
                                            break
                                        }
                                        accumulatedHeight += pH
                                        if (p == pdfPageCount) {
                                            pageIdx = pdfPageCount
                                        }
                                    }
                                    pageIdx.coerceIn(1, pdfPageCount)
                                } else {
                                    pdfPage
                                }
                                val finalX = toNormalizedX(mappedX, touchedPage)
                                val finalY = toNormalizedY(mappedY, touchedPage)
                                val toolType = motionEvent.getToolType(0)
                                val pressure = if (toolType == MotionEvent.TOOL_TYPE_STYLUS) motionEvent.pressure else 1.0f
                                
                                pendingStrokeDownPoint = com.example.data.Point(finalX, finalY, pressure)
                                pendingStrokeTouchedPage = touchedPage
                                
                                longPressJob?.cancel()
                                longPressJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(400)
                                    if (potentialImageIndex == touchedImageIndex) {
                                        selectedImageIndex = touchedImageIndex
                                        activeImageInteraction = "drag"
                                        potentialImageIndex = null
                                        pendingStrokeDownPoint = null
                                    }
                                }
                                return@pointerInteropFilter true
                            }
                        } else {
                            potentialImageIndex = null
                            selectedImageIndex = null
                        }
                    }
                    
                    if (potentialImageIndex != null && action == MotionEvent.ACTION_MOVE) {
                        val lastPoint = lastFingerDragPoint ?: Offset(x, y)
                        val dx = x - lastPoint.x
                        val dy = y - lastPoint.y
                        if (kotlin.math.hypot(dx, dy) > 10f) {
                            // Moved too much before long press triggered - fallback to drawing
                            potentialImageIndex = null
                            longPressJob?.cancel()
                            
                            if (pendingStrokeDownPoint != null) {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                isZooming = false
                                strokeStartedPage = pendingStrokeTouchedPage
                                onPageSelected(pendingStrokeTouchedPage)
                                onStrokeStarted(pendingStrokeDownPoint!!)
                                pendingStrokeDownPoint = null
                            }
                            // DO NOT return true, fall through to ACTION_MOVE drawing logic!
                        } else {
                            return@pointerInteropFilter true
                        }
                    }
                    if (potentialImageIndex != null && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)) {
                        potentialImageIndex = null
                        longPressJob?.cancel()
                        if (pendingStrokeDownPoint != null) {
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                            strokeStartedPage = pendingStrokeTouchedPage
                            onPageSelected(pendingStrokeTouchedPage)
                            onStrokeStarted(pendingStrokeDownPoint!!)
                            onStrokeEnded()
                            pendingStrokeDownPoint = null
                        }
                        return@pointerInteropFilter true
                    }
                    
                    if (selectedImageIndex != null && activeImageInteraction != null) {
                        val i = selectedImageIndex!!
                        if (action == MotionEvent.ACTION_MOVE) {
                            val lastPoint = lastFingerDragPoint ?: Offset(x, y)
                            val dx = (x - lastPoint.x) / scale
                            val dy = (y - lastPoint.y) / scale
                            val img = images[i]
                            val imgPage = img.page.coerceIn(1, pdfPageCount)
                            val pW = getPageWidth(imgPage)
                            val pH = getPageHeight(imgPage)
                            
                            val virtualDx = (dx / pW) * 600f
                            val virtualDy = (dy / pH) * getNormH(imgPage)
                            
                            if (activeImageInteraction == "drag") {
                                onImageUpdated(i, img.copy(x = img.x + virtualDx, y = img.y + virtualDy))
                            } else if (activeImageInteraction == "resize") {
                                onImageUpdated(i, img.copy(width = maxOf(50f, img.width + virtualDx), height = maxOf(50f, img.height + virtualDy)))
                            }
                            lastFingerDragPoint = Offset(x, y)
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            activeImageInteraction = null
                            lastFingerDragPoint = null
                        }
                        return@pointerInteropFilter true
                    }

                    // Active Lasso Shape Interaction (Move or Resize via Corner Handles)
                    if (lassoSelectedStrokes.isNotEmpty() && lassoBoundingBox != null) {
                        val box = lassoBoundingBox!!
                        val cX = (box.left + box.right) / 2f
                        val cY = (box.top + box.bottom) / 2f
                        val halfW = ((box.right - box.left) / 2f) * lassoScaleX
                        val halfH = ((box.bottom - box.top) / 2f) * lassoScaleY

                        val leftPx = fromNormalizedX(cX - halfW + lassoDragOffset.x, pdfPage)
                        val rightPx = fromNormalizedX(cX + halfW + lassoDragOffset.x, pdfPage)
                        val topPx = fromNormalizedY(cY - halfH + lassoDragOffset.y, pdfPage)
                        val bottomPx = fromNormalizedY(cY + halfH + lassoDragOffset.y, pdfPage)

                        val touchPxX = x
                        val touchPxY = y

                        if (action == MotionEvent.ACTION_DOWN) {
                            val handleRadius = 45f
                            val isCorner = kotlin.math.hypot(touchPxX - leftPx, touchPxY - topPx) <= handleRadius ||
                                           kotlin.math.hypot(touchPxX - rightPx, touchPxY - topPx) <= handleRadius ||
                                           kotlin.math.hypot(touchPxX - leftPx, touchPxY - bottomPx) <= handleRadius ||
                                           kotlin.math.hypot(touchPxX - rightPx, touchPxY - bottomPx) <= handleRadius

                            if (isCorner) {
                                activeLassoInteraction = "resize"
                                lastLassoTouchPoint = Offset(touchPxX, touchPxY)
                                initialLassoScaleX = lassoScaleX
                                initialLassoScaleY = lassoScaleY
                                return@pointerInteropFilter true
                            } else if (touchPxX in (leftPx - 15f)..(rightPx + 15f) && touchPxY in (topPx - 15f)..(bottomPx + 15f)) {
                                activeLassoInteraction = "move"
                                lastLassoTouchPoint = Offset(touchPxX, touchPxY)
                                return@pointerInteropFilter true
                            }
                        } else if (action == MotionEvent.ACTION_MOVE && activeLassoInteraction != null) {
                            val lastPoint = lastLassoTouchPoint ?: Offset(x, y)
                            if (activeLassoInteraction == "move") {
                                val dxPx = x - lastPoint.x
                                val dyPx = y - lastPoint.y
                                val pageW = getPageWidth(pdfPage)
                                val pageH = getPageHeight(pdfPage)
                                val normDx = (dxPx / scale / pageW) * 600f
                                val normDy = (dyPx / scale / pageH) * getNormH(pdfPage)
                                onLassoDrag(Offset(normDx, normDy))
                                lastLassoTouchPoint = Offset(x, y)
                            } else if (activeLassoInteraction == "resize") {
                                val centerPxX = (leftPx + rightPx) / 2f
                                val centerPxY = (topPx + bottomPx) / 2f
                                val initialDist = kotlin.math.hypot(lastPoint.x - centerPxX, lastPoint.y - centerPxY).coerceAtLeast(10f)
                                val currentDist = kotlin.math.hypot(x - centerPxX, y - centerPxY)
                                val factor = currentDist / initialDist
                                val newScaleX = (initialLassoScaleX * factor).coerceIn(0.15f, 6f)
                                val newScaleY = (initialLassoScaleY * factor).coerceIn(0.15f, 6f)
                                onLassoScaleUpdated(newScaleX, newScaleY)
                                lastLassoTouchPoint = Offset(x, y)
                            }
                            return@pointerInteropFilter true
                        } else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && activeLassoInteraction != null) {
                            activeLassoInteraction = null
                            lastLassoTouchPoint = null
                            return@pointerInteropFilter true
                        }
                    }

                    // Check Long Press on Canvas Shapes/Strokes to select & customize
                    if (action == MotionEvent.ACTION_DOWN) {
                        val pivotX = widthPx / 2f
                        val mappedX = (x - pivotX - offset.x) / scale + pivotX
                        val mappedY = (y - offset.y) / scale
                        val worldX = toNormalizedX(mappedX, pdfPage)
                        val worldY = toNormalizedY(mappedY, pdfPage)

                        var matched: Stroke? = null
                        for (s in strokes.reversed()) {
                            val bbox = SmartInkEngine.getBoundingBox(s) ?: continue
                            val margin = 35f
                            if (worldX >= bbox.left - margin && worldX <= bbox.right + margin &&
                                worldY >= bbox.top - margin && worldY <= bbox.bottom + margin) {
                                if (s.toolType == "shapes" || s.fillShape || s.points.any { pt -> kotlin.math.hypot(pt.x - worldX, pt.y - worldY) <= 35f }) {
                                    matched = s
                                    break
                                }
                            }
                        }

                        if (matched != null) {
                            potentialShapeStroke = matched
                            lastShapeTouchPoint = Offset(x, y)
                            val toolType = motionEvent.getToolType(0)
                            val pressure = if (toolType == MotionEvent.TOOL_TYPE_STYLUS) motionEvent.pressure else 1.0f
                            pendingStrokeDownPoint = com.example.data.Point(worldX, worldY, pressure)
                            pendingStrokeTouchedPage = pdfPage

                            longPressJob?.cancel()
                            longPressJob = coroutineScope.launch {
                                kotlinx.coroutines.delay(350)
                                if (potentialShapeStroke == matched) {
                                    onShapeLongPressed(matched)
                                    potentialShapeStroke = null
                                    pendingStrokeDownPoint = null
                                }
                            }
                            return@pointerInteropFilter true
                        }
                    }

                    if (potentialShapeStroke != null && action == MotionEvent.ACTION_MOVE) {
                        val lastPoint = lastShapeTouchPoint ?: Offset(x, y)
                        if (kotlin.math.hypot(x - lastPoint.x, y - lastPoint.y) > 12f) {
                            longPressJob?.cancel()
                            potentialShapeStroke = null
                            if (pendingStrokeDownPoint != null) {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                isZooming = false
                                strokeStartedPage = pendingStrokeTouchedPage
                                onPageSelected(pendingStrokeTouchedPage)
                                onStrokeStarted(pendingStrokeDownPoint!!)
                                pendingStrokeDownPoint = null
                            }
                        } else {
                            return@pointerInteropFilter true
                        }
                    }

                    if (potentialShapeStroke != null && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)) {
                        longPressJob?.cancel()
                        potentialShapeStroke = null
                        if (pendingStrokeDownPoint != null) {
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                            strokeStartedPage = pendingStrokeTouchedPage
                            onPageSelected(pendingStrokeTouchedPage)
                            onStrokeStarted(pendingStrokeDownPoint!!)
                            onStrokeEnded()
                            pendingStrokeDownPoint = null
                        }
                        return@pointerInteropFilter true
                    }

                    // Multi-touch gesture processing for ALL templates to allow zooming & vertical scrolling
                    if (motionEvent.pointerCount >= 2) {
                        when (action) {
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                val x0 = motionEvent.getX(0)
                                val y0 = motionEvent.getY(0)
                                val x1 = motionEvent.getX(1)
                                val y1 = motionEvent.getY(1)
                                initialSpacing = kotlin.math.hypot(x0 - x1, y0 - y1)
                                initialScale = scale
                                initialOffset = offset
                                initialPivot = Offset((x0 + x1) / 2f, (y0 + y1) / 2f)
                                isZooming = true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (isZooming && motionEvent.pointerCount >= 2) {
                                    val x0 = motionEvent.getX(0)
                                    val y0 = motionEvent.getY(0)
                                    val x1 = motionEvent.getX(1)
                                    val y1 = motionEvent.getY(1)
                                    val currentSpacing = kotlin.math.hypot(x0 - x1, y0 - y1)
                                    val currentPivot = Offset((x0 + x1) / 2f, (y0 + y1) / 2f)
                                    
                                    if (initialSpacing > 0f && !isZoomLocked) {
                                        scale = ((currentSpacing / initialSpacing) * initialScale).coerceIn(0.5f, 3.5f)
                                    }
                                    
                                    val rawOffset = initialOffset + (currentPivot - initialPivot)
                                    
                                    val pageCount = if (isMultiPage) pdfPageCount else 1
                                    val pageH = if (isNormalizedCoords) pdfH else heightPx.toFloat()
                                    val maxScrollY = -((pageCount * pageH * scale - heightPx).coerceAtLeast(0f))
                                    
                                    val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                    
                                    offset = Offset(
                                        rawOffset.x.coerceIn(-maxScrollX, maxScrollX),
                                        rawOffset.y.coerceIn(maxScrollY, 0f)
                                    )
                                }
                            }
                            MotionEvent.ACTION_POINTER_UP -> {
                                isZooming = false
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    // Map screen coordinates back to canvas space using scale and centered pivot
                    val pivotX = widthPx / 2f
                    val mappedX = (x - pivotX - offset.x) / scale + pivotX
                    val mappedY = (y - offset.y) / scale

                    // Snap mapped touch coordinate to ruler edge if close
                    var snappedX = mappedX
                    var snappedY = mappedY

                    if (isRulerActive && activeRulerInteraction == null) {
                        val dxR = mappedX - rulerOffset.x
                        val dyR = mappedY - rulerOffset.y
                        val angleRad = Math.toRadians(rulerAngle.toDouble())
                        val cosR = Math.cos(-angleRad).toFloat()
                        val sinR = Math.sin(-angleRad).toFloat()
                        val localXR = dxR * cosR - dyR * sinR
                        val localYR = dxR * sinR + dyR * cosR

                        if (localYR in (-rulerH / 2)..rulerH / 2) {
                            val snapThreshold = 45f
                            val distToLeft = Math.abs(localXR - (-rulerW / 2))
                            val distToRight = Math.abs(localXR - (rulerW / 2))
                            if (distToLeft < snapThreshold || distToRight < snapThreshold) {
                                val snappedLocalXR = if (distToLeft < distToRight) -rulerW / 2 else rulerW / 2
                                val cosRot = Math.cos(angleRad).toFloat()
                                val sinRot = Math.sin(angleRad).toFloat()
                                snappedX = snappedLocalXR * cosRot - localYR * sinRot + rulerOffset.x
                                snappedY = snappedLocalXR * sinRot + localYR * cosRot + rulerOffset.y
                            }
                        }
                    }

                    // Stylus Gesture (Double tap or button secondary click) Detection
                    if (isStylus && action == MotionEvent.ACTION_DOWN) {
                        val currentTime = System.currentTimeMillis()
                        val timeDiff = currentTime - lastStylusDownTime
                        val dx = x - lastStylusDownX
                        val dy = y - lastStylusDownY
                        val distance = kotlin.math.hypot(dx, dy)
                        val isButtonSecondary = (motionEvent.buttonState and MotionEvent.BUTTON_SECONDARY) != 0

                        if ((timeDiff < 350 && distance < 50f) || isButtonSecondary) {
                            onStylusDoubleTap()
                            lastStylusDownTime = 0L
                        } else {
                            lastStylusDownTime = currentTime
                            lastStylusDownX = x
                            lastStylusDownY = y
                        }
                    }

                    // Hand/Finger scrolling when Stylus-Only Drawing is active OR viewing scrollable PDF
                    if ((stylusOnlyDrawing || isMultiPage) && isFinger) {
                        when (action) {
                            MotionEvent.ACTION_DOWN -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                lastFingerDragPoint = Offset(x, y)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                val lastPoint = lastFingerDragPoint
                                if (lastPoint != null) {
                                    val dx = x - lastPoint.x
                                    val dy = y - lastPoint.y
                                    val rawOffset = Offset(offset.x + dx, offset.y + dy)
                                    
                                    var totalHeight = 0f
                                    for (p in 1..pdfPageCount) {
                                        totalHeight += getPageHeight(p)
                                    }
                                    val maxScrollY = -((totalHeight * scale - heightPx).coerceAtLeast(0f))
                                    
                                    val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                    
                                    offset = Offset(
                                        rawOffset.x.coerceIn(-maxScrollX, maxScrollX),
                                        rawOffset.y.coerceIn(maxScrollY, 0f)
                                    )
                                    lastFingerDragPoint = Offset(x, y)
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                lastFingerDragPoint = null
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    // Map to local PDF coordinate space to support vertical scrolling & rotation/split-screen perfectly
                    val touchedPage = if (isMultiPage) {
                        var pageIdx = 1
                        var accumulatedHeight = 0f
                        for (p in 1..pdfPageCount) {
                            val pH = getPageHeight(p)
                            if (snappedY >= accumulatedHeight && snappedY < accumulatedHeight + pH) {
                                pageIdx = p
                                break
                            }
                            accumulatedHeight += pH
                            if (p == pdfPageCount) {
                                pageIdx = pdfPageCount
                            }
                        }
                        pageIdx.coerceIn(1, pdfPageCount)
                    } else {
                        pdfPage
                    }

                    val finalX = toNormalizedX(snappedX, touchedPage)
                    val finalY = toNormalizedY(snappedY, touchedPage)

                    // Detect stylus pressure if available, fallback to 1.0
                    val pressure = if (isStylus) {
                        try { motionEvent.getPressure(activePointerIndex) } catch (e: Exception) { motionEvent.pressure }
                    } else {
                        1.0f
                    }

                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            val pageL = getPageLeft(touchedPage)
                            val pageT = getPageTop(touchedPage)
                            val pageW = getPageWidth(touchedPage)
                            val pageH = getPageHeight(touchedPage)
                            val isInsidePage = snappedX >= pageL && snappedX <= pageL + pageW && snappedY >= pageT && snappedY <= pageT + pageH

                            if (isInsidePage) {
                                isWritingStartedOnPage = true
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                isZooming = false
                                strokeStartedPage = touchedPage
                                onPageSelected(touchedPage)
                                
                                val startX = toNormalizedX(snappedX, touchedPage).coerceIn(0f, 600f)
                                val startY = toNormalizedY(snappedY, touchedPage).coerceIn(0f, getNormH(touchedPage))
                                onStrokeStarted(Point(startX, startY, pressure))
                            } else {
                                isWritingStartedOnPage = false
                            }
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (isWritingStartedOnPage) {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                val pointsList = mutableListOf<Point>()
                                val historySize = motionEvent.historySize
                                val normH = getNormH(strokeStartedPage)
                                for (i in 0 until historySize) {
                                    val hx = try { motionEvent.getHistoricalX(activePointerIndex, i) } catch (e: Exception) { motionEvent.getHistoricalX(i) }
                                    val hy = try { motionEvent.getHistoricalY(activePointerIndex, i) } catch (e: Exception) { motionEvent.getHistoricalY(i) }
                                    val hp = if (isStylus) {
                                        try { motionEvent.getHistoricalPressure(activePointerIndex, i) } catch (e: Exception) { motionEvent.getHistoricalPressure(i) }
                                    } else 1.0f
                                    val pivotX = widthPx / 2f
                                    val mHx = (hx - pivotX - offset.x) / scale + pivotX
                                    val mHy = (hy - offset.y) / scale
                                    
                                    var snappedHx = mHx
                                    var snappedHy = mHy

                                    if (isRulerActive && activeRulerInteraction == null) {
                                        val dxR = mHx - rulerOffset.x
                                        val dyR = mHy - rulerOffset.y
                                        val angleRad = Math.toRadians(rulerAngle.toDouble())
                                        val cosR = Math.cos(-angleRad).toFloat()
                                        val sinR = Math.sin(-angleRad).toFloat()
                                        val localXR = dxR * cosR - dyR * sinR
                                        val localYR = dxR * sinR + dyR * cosR

                                        if (localYR in (-rulerH / 2)..rulerH / 2) {
                                            val snapThreshold = 45f
                                            val distToLeft = Math.abs(localXR - (-rulerW / 2))
                                            val distToRight = Math.abs(localXR - (rulerW / 2))
                                            if (distToLeft < snapThreshold || distToRight < snapThreshold) {
                                                val snappedLocalXR = if (distToLeft < distToRight) -rulerW / 2 else rulerW / 2
                                                val cosRot = Math.cos(angleRad).toFloat()
                                                val sinRot = Math.sin(angleRad).toFloat()
                                                snappedHx = snappedLocalXR * cosRot - localYR * sinRot + rulerOffset.x
                                                snappedHy = snappedLocalXR * sinRot + localYR * cosRot + rulerOffset.y
                                            }
                                        }
                                    }

                                    val fHx = toNormalizedX(snappedHx, strokeStartedPage).coerceIn(0f, 600f)
                                    val fHy = toNormalizedY(snappedHy, strokeStartedPage).coerceIn(0f, normH)
                                    pointsList.add(Point(fHx, fHy, hp))
                                }
                                val finalXVal = toNormalizedX(snappedX, strokeStartedPage).coerceIn(0f, 600f)
                                val finalYVal = toNormalizedY(snappedY, strokeStartedPage).coerceIn(0f, normH)
                                pointsList.add(Point(finalXVal, finalYVal, pressure))
                                if (pointsList.isNotEmpty()) {
                                    onStrokeDragged(pointsList)
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isWritingStartedOnPage) {
                                onStrokeEnded()
                                isWritingStartedOnPage = false
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true }) {
            // Apply canvas panning and zooming transformations if infinite mode is active, hand scroll is enabled, or scrollable PDF is shown
            val isMultiPage = templateType == "pdf" || templateType == "docx" || pdfPageCount > 1
            val isNormalizedCoords = true
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset(size.width / 2f, 0f))
            }) {
                var pdfOffset = Offset.Zero

                // Compute visible range in page Y coordinates for frustum culling
                val visibleTop = (-offset.y / scale) - 200f
                val visibleBottom = ((-offset.y + heightPx) / scale) + 200f

                // 1. Draw Template/PDF Background First
                when (templateType) {
                    "pdf", "docx" -> {
                        for (p in 1..pdfPageCount) {
                            val top = getPageTop(p)
                            val pH = getPageHeight(p)
                            if (top + pH >= visibleTop && top <= visibleBottom) {
                                val bitmap = pdfBitmaps[p]
                                val pW = getPageWidth(p)
                                val left = getPageLeft(p)
                                if (bitmap != null && !bitmap.isRecycled) {
                                    drawImage(
                                        image = bitmap.asImageBitmap(),
                                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                                        dstSize = androidx.compose.ui.unit.IntSize(pW.toInt(), pH.toInt()),
                                        colorFilter = if (isDarkTheme) {
                                            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                                androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                                                    -1f, 0f, 0f, 0f, 255f,
                                                    0f, -1f, 0f, 0f, 255f,
                                                    0f, 0f, -1f, 0f, 255f,
                                                    0f, 0f, 0f, 1f, 0f
                                                ))
                                            )
                                        } else null
                                    )
                                } else {
                                    drawRect(color = actualBgColor, topLeft = Offset(left, top), size = Size(pW, pH))
                                }
                            }
                        }
                    }
                    else -> {
                        for (p in 1..pdfPageCount) {
                            val topOffset = getPageTop(p)
                            val pageH = getPageHeight(p)
                            if (topOffset + pageH >= visibleTop && topOffset <= visibleBottom) {
                                val pageW = getPageWidth(p)
                                val pageL = getPageLeft(p)

                                drawRect(
                                    color = actualBgColor,
                                    topLeft = Offset(pageL, topOffset),
                                    size = Size(pageW, pageH)
                                )

                                withTransform({ translate(pageL, topOffset) }) {
                                    when (templateType) {
                                        "grid" -> {
                                            val gridSpacing = 30.dp.toPx()
                                            val gridColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.35f)
                                            for (gx in 0..pageW.toInt() step gridSpacing.toInt()) {
                                                drawLine(gridColor, start = Offset(gx.toFloat(), 0f), end = Offset(gx.toFloat(), pageH), strokeWidth = 1f)
                                            }
                                            for (gy in 0..pageH.toInt() step gridSpacing.toInt()) {
                                                drawLine(gridColor, start = Offset(0f, gy.toFloat()), end = Offset(pageW, gy.toFloat()), strokeWidth = 1f)
                                            }
                                        }
                                        "dotted" -> {
                                            val dotSpacing = 24.dp.toPx()
                                            val dotColor = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.45f)
                                            val dotRadius = 1.5.dp.toPx()
                                            for (gx in dotSpacing.toInt()..pageW.toInt() step dotSpacing.toInt()) {
                                                for (gy in dotSpacing.toInt()..pageH.toInt() step dotSpacing.toInt()) {
                                                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(gx.toFloat(), gy.toFloat()))
                                                }
                                            }
                                        }
                                        "ruled" -> {
                                            val lineSpacing = 40.dp.toPx()
                                            val ruledColor = if (isDarkTheme) Color(0xFF64748B).copy(alpha = 0.6f) else Color(0xFF94A3B8).copy(alpha = 0.75f)
                                            for (ry in lineSpacing.toInt()..pageH.toInt() step lineSpacing.toInt()) {
                                                drawLine(ruledColor, start = Offset(0f, ry.toFloat()), end = Offset(pageW, ry.toFloat()), strokeWidth = 1f)
                                            }
                                        }
                                        "cornell" -> {
                                            val splitX = pageW * 0.28f
                                            val summaryY = pageH * 0.82f
                                            val lineColor = if (isDarkTheme) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFFBBDEFB).copy(alpha = 0.4f)
                                            val divisionColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF90A4AE)
                                            val lineSpacing = 28.dp.toPx()
                                            for (cy in lineSpacing.toInt()..summaryY.toInt() step lineSpacing.toInt()) {
                                                drawLine(lineColor, start = Offset(splitX, cy.toFloat()), end = Offset(pageW, cy.toFloat()), strokeWidth = 1f)
                                            }
                                            drawLine(divisionColor, start = Offset(splitX, 0f), end = Offset(splitX, summaryY), strokeWidth = 3f)
                                            drawLine(divisionColor, start = Offset(0f, summaryY), end = Offset(pageW, summaryY), strokeWidth = 3f)
                                            drawIntoCanvas { canvas ->
                                                val paint = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.GRAY
                                                    textSize = 36f
                                                    isAntiAlias = true
                                                }
                                                canvas.nativeCanvas.drawText("Cue / Keywords", 30f, 60f, paint)
                                                canvas.nativeCanvas.drawText("Notes Canvas", splitX + 30f, 60f, paint)
                                                canvas.nativeCanvas.drawText("Summary block", 30f, summaryY + 50f, paint)
                                            }
                                        }
                                        "meeting" -> {
                                            val cardBg = Color.White
                                            val borderColor = Color.LightGray.copy(alpha = 0.5f)
                                            drawRoundRect(color = cardBg, topLeft = Offset(20.dp.toPx(), 20.dp.toPx()), size = Size(pageW * 0.45f - 30.dp.toPx(), pageH * 0.35f), cornerRadius = CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
                                            drawRoundRect(color = borderColor, topLeft = Offset(20.dp.toPx(), 20.dp.toPx()), size = Size(pageW * 0.45f - 30.dp.toPx(), pageH * 0.35f), cornerRadius = CornerRadius(8.dp.toPx()), style = DrawStroke(width = 2f))
                                            drawRoundRect(color = cardBg, topLeft = Offset(20.dp.toPx(), pageH * 0.35f + 40.dp.toPx()), size = Size(pageW * 0.45f - 30.dp.toPx(), pageH * 0.55f - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
                                            drawRoundRect(color = borderColor, topLeft = Offset(20.dp.toPx(), pageH * 0.35f + 40.dp.toPx()), size = Size(pageW * 0.45f - 30.dp.toPx(), pageH * 0.55f - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = DrawStroke(width = 2f))
                                            drawRoundRect(color = cardBg, topLeft = Offset(pageW * 0.45f + 10.dp.toPx(), 20.dp.toPx()), size = Size(pageW * 0.55f - 30.dp.toPx(), pageH - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
                                            drawRoundRect(color = borderColor, topLeft = Offset(pageW * 0.45f + 10.dp.toPx(), 20.dp.toPx()), size = Size(pageW * 0.55f - 30.dp.toPx(), pageH - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = DrawStroke(width = 2f))
                                            drawIntoCanvas { canvas ->
                                                val titlePaint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 32f; isFakeBoldText = true; isAntiAlias = true }
                                                canvas.nativeCanvas.drawText("Agenda", 20.dp.toPx() + 20f, 20.dp.toPx() + 40f, titlePaint)
                                                canvas.nativeCanvas.drawText("Action Items", 20.dp.toPx() + 20f, pageH * 0.35f + 40.dp.toPx() + 40f, titlePaint)
                                                canvas.nativeCanvas.drawText("Meeting Minutes", pageW * 0.45f + 10.dp.toPx() + 20f, 20.dp.toPx() + 40f, titlePaint)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 1.5 Draw Images (with viewport culling)
                images.forEach { img ->
                    val imgPage = img.page.coerceIn(1, pdfPageCount)
                    val pageTop = getPageTop(imgPage)
                    val pageH = getPageHeight(imgPage)
                    if (pageTop + pageH >= visibleTop && pageTop <= visibleBottom) {
                        imageBitmaps[img.uri]?.let { bmp ->
                            val renderX = fromNormalizedX(img.x, imgPage)
                            val renderY = fromNormalizedY(img.y, imgPage)
                            val renderW = (img.width / 600f) * getPageWidth(imgPage)
                            val renderH = (img.height / getNormH(imgPage)) * getPageHeight(imgPage)

                            if (renderY + renderH >= visibleTop && renderY <= visibleBottom) {
                                drawImage(
                                    image = bmp,
                                    dstOffset = androidx.compose.ui.unit.IntOffset(renderX.toInt(), renderY.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(renderW.toInt(), renderH.toInt())
                                )
                                if (selectedImageIndex == images.indexOf(img)) {
                                    drawRect(
                                        color = Color.Blue,
                                        topLeft = Offset(renderX, renderY),
                                        size = Size(renderW, renderH),
                                        style = DrawStroke(2f)
                                    )
                                    drawCircle(
                                        color = Color.Blue,
                                        radius = 15f,
                                        center = Offset(renderX + renderW, renderY + renderH)
                                    )
                                }
                            }
                        }
                    }
                }

                val drawSingleStroke: (com.example.data.Stroke, Boolean, Float) -> Unit = { stroke, isLassoed, alphaMult ->
                    if (!stroke.isHidden) {
                        val strokePage = stroke.page.coerceIn(1, pdfPageCount)
                        val pTop = getPageTop(strokePage)
                        val pH = getPageHeight(strokePage)
                        val pLeft = getPageLeft(strokePage)
                        val pW = getPageWidth(strokePage)
                        val isVisible = pTop + pH >= visibleTop && pTop <= visibleBottom

                        if (isVisible) {
                            clipRect(left = pLeft, top = pTop, right = pLeft + pW, bottom = pTop + pH) {
                                val rawPoints = stroke.points
                                if (rawPoints.isNotEmpty()) {
                                val bbox = lassoBoundingBox
                                val cX = if (bbox != null) (bbox.left + bbox.right) / 2f else 0f
                                val cY = if (bbox != null) (bbox.top + bbox.bottom) / 2f else 0f

                                val points = if (isLassoed && bbox != null) {
                                    rawPoints.map { pt ->
                                        val tx = pt.x + lassoDragOffset.x
                                        val ty = pt.y + lassoDragOffset.y
                                        val fx = if (lassoScaleX != 1f) cX + (tx - cX) * lassoScaleX else tx
                                        val fy = if (lassoScaleY != 1f) cY + (ty - cY) * lassoScaleY else ty
                                        com.example.data.Point(fx, fy, pt.pressure)
                                    }
                                } else rawPoints

                                val baseAlpha = if (isLassoed) 0.95f else 1f
                                val rawC = stroke.color
                                val strokeColorInt = if (stroke.toolType != "highlighter" && stroke.toolType != "laser" && stroke.toolType != "tape") {
                                    val a = (rawC ushr 24) and 0xFF
                                    val r = ((rawC ushr 16) and 0xFF) / 255f
                                    val g = ((rawC ushr 8) and 0xFF) / 255f
                                    val b = (rawC and 0xFF) / 255f
                                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                                    if (isDarkPaperCanvas && lum < 0.35f && a > 200) {
                                        0xFFFFFFFF.toInt()
                                    } else if (!isDarkPaperCanvas && lum > 0.88f && a > 200) {
                                        0xFF1E1E1E.toInt()
                                    } else {
                                        rawC
                                    }
                                } else rawC
                                val color = androidx.compose.ui.graphics.Color(strokeColorInt).copy(alpha = baseAlpha * alphaMult)
                                val width = stroke.width
                                
                                // Cached path lookup for completed strokes to avoid re-constructing Path every frame
                                val path = if (!isLassoed) {
                                    strokePathCache.getOrPut(stroke) {
                                        val p = androidx.compose.ui.graphics.Path()
                                        val firstPt = points.first()
                                        val lx = fromNormalizedX(firstPt.x, strokePage)
                                        val ly = fromNormalizedY(firstPt.y, strokePage)
                                        p.moveTo(lx, ly)
                                        for (i in 1 until points.size) {
                                            val pt = points[i]
                                            val pX = fromNormalizedX(pt.x, strokePage)
                                            val pY = fromNormalizedY(pt.y, strokePage)
                                            p.lineTo(pX, pY)
                                        }
                                        p
                                    }
                                } else {
                                    val p = androidx.compose.ui.graphics.Path()
                                    val firstPt = points.first()
                                    val lx = fromNormalizedX(firstPt.x, strokePage)
                                    val ly = fromNormalizedY(firstPt.y, strokePage)
                                    p.moveTo(lx, ly)
                                    for (i in 1 until points.size) {
                                        val pt = points[i]
                                        val pX = fromNormalizedX(pt.x, strokePage)
                                        val pY = fromNormalizedY(pt.y, strokePage)
                                        p.lineTo(pX, pY)
                                    }
                                    p
                                }

                                if (stroke.fillShape && stroke.fillOpacity > 0f) {
                                    drawPath(
                                        path = path,
                                        color = androidx.compose.ui.graphics.Color(strokeColorInt).copy(alpha = stroke.fillOpacity * baseAlpha * alphaMult),
                                        style = androidx.compose.ui.graphics.drawscope.Fill
                                    )
                                }
                                val pathEffect = if (stroke.toolType == "lasso" && !lassoSolidLine) {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                } else if (stroke.toolType == "pencil") {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1f, 4f), 0f)
                                } else null

                                val rainbowBrush = if (stroke.isRainbow) {
                                    val bounds = path.getBounds()
                                    val brushEnd = if (bounds.width == 0f && bounds.height == 0f) {
                                        androidx.compose.ui.geometry.Offset(bounds.right + 1f, bounds.bottom + 1f)
                                    } else {
                                        androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom)
                                    }
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFFF0000),
                                            androidx.compose.ui.graphics.Color(0xFFFF7F00),
                                            androidx.compose.ui.graphics.Color(0xFFFFFF00),
                                            androidx.compose.ui.graphics.Color(0xFF00FF00),
                                            androidx.compose.ui.graphics.Color(0xFF0000FF),
                                            androidx.compose.ui.graphics.Color(0xFF4B0082),
                                            androidx.compose.ui.graphics.Color(0xFF8B00FF)
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.top),
                                        end = brushEnd
                                    )
                                } else null

                                if (stroke.toolType == "pencil") {
                                    // Pencil texture simulation with overlapping strokes
                                    val pencilAlpha = 0.5f * (color.alpha)
                                    val drawColor = color.copy(alpha = pencilAlpha)
                                    val baseStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Square,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                                        pathEffect = pathEffect
                                    )
                                    
                                    val drawAction: (androidx.compose.ui.graphics.drawscope.Stroke, androidx.compose.ui.graphics.Color?, androidx.compose.ui.graphics.Brush?) -> Unit = { style, col, br ->
                                        if (br != null) drawPath(path, brush = br, style = style)
                                        else drawPath(path, color = col!!, style = style)
                                    }
                                    
                                    // Base stroke
                                    drawAction(baseStyle, drawColor, rainbowBrush)
                                    
                                    // Overlapping shifted strokes to create grain
                                    val offset1 = 0.3f * width
                                    
                                    val stroke2 = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width * 0.7f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Square,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                                        pathEffect = pathEffect
                                    )
                                    val stroke3 = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width * 0.4f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Square,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                                        pathEffect = pathEffect
                                    )

                                    drawContext.transform.translate(offset1, offset1)
                                    drawAction(stroke2, drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    drawContext.transform.translate(-offset1, -offset1) // back to origin
                                    
                                    drawContext.transform.translate(-offset1, -offset1)
                                    drawAction(stroke2, drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    drawContext.transform.translate(offset1, offset1) // back to origin
                                    
                                    drawContext.transform.translate(offset1, -offset1)
                                    drawAction(stroke3, drawColor.copy(alpha = pencilAlpha * 0.5f), rainbowBrush)
                                    drawContext.transform.translate(-offset1, offset1) // back to origin
                                } else {
                                    if (rainbowBrush != null) {
                                        drawPath(
                                            path = path,
                                            brush = rainbowBrush,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = width,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                                pathEffect = pathEffect
                                            ),
                                            blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                                        )
                                    } else {
                                        drawPath(
                                            path = path,
                                            color = color,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = width,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                                pathEffect = pathEffect
                                            ),
                                            blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // 2. Draw Highlighter Layer (Renders BEHIND ink pens) using index loops without list allocation
                for (i in strokes.indices) {
                    val s = strokes[i]
                    if (s.toolType == "highlighter") {
                        drawSingleStroke(s, false, 1f)
                    }
                }

                // 3. Draw Ink Layer (Pens, Erasers paths, Lasso guides)
                for (i in strokes.indices) {
                    val s = strokes[i]
                    if (s.toolType != "highlighter") {
                        drawSingleStroke(s, false, 1f)
                    }
                }
            }
        } // End of first Canvas (background and completed strokes)

        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true }) {
            val isMultiPage = templateType == "pdf" || templateType == "docx" || pdfPageCount > 1
            val isNormalizedCoords = true
            val visibleTop = (-offset.y / scale) - 200f
            val visibleBottom = ((-offset.y + heightPx) / scale) + 200f

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset(size.width / 2f, 0f))
            }) {
                val drawSingleStroke: (com.example.data.Stroke, Boolean, Float) -> Unit = { stroke, isLassoed, alphaMult ->
                    if (!stroke.isHidden) {
                        val strokePage = stroke.page.coerceIn(1, pdfPageCount)
                        val pTop = getPageTop(strokePage)
                        val pH = getPageHeight(strokePage)
                        val pLeft = getPageLeft(strokePage)
                        val pW = getPageWidth(strokePage)
                        val isVisible = pTop + pH >= visibleTop && pTop <= visibleBottom

                        if (isVisible) {
                            clipRect(left = pLeft, top = pTop, right = pLeft + pW, bottom = pTop + pH) {
                                val rawPoints = stroke.points
                            if (rawPoints.isNotEmpty()) {
                                val bbox = lassoBoundingBox
                                val cX = if (bbox != null) (bbox.left + bbox.right) / 2f else 0f
                                val cY = if (bbox != null) (bbox.top + bbox.bottom) / 2f else 0f

                                val points = if (isLassoed && bbox != null) {
                                    rawPoints.map { pt ->
                                        val tx = pt.x + lassoDragOffset.x
                                        val ty = pt.y + lassoDragOffset.y
                                        val fx = if (lassoScaleX != 1f) cX + (tx - cX) * lassoScaleX else tx
                                        val fy = if (lassoScaleY != 1f) cY + (ty - cY) * lassoScaleY else ty
                                        com.example.data.Point(fx, fy, pt.pressure)
                                    }
                                } else rawPoints

                                val baseAlpha = if (isLassoed) 0.95f else 1f
                                val rawC = stroke.color
                                val strokeColorInt = if (stroke.toolType != "highlighter" && stroke.toolType != "laser" && stroke.toolType != "tape") {
                                    val a = (rawC ushr 24) and 0xFF
                                    val r = ((rawC ushr 16) and 0xFF) / 255f
                                    val g = ((rawC ushr 8) and 0xFF) / 255f
                                    val b = (rawC and 0xFF) / 255f
                                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                                    if (isDarkPaperCanvas && lum < 0.35f && a > 200) {
                                        0xFFFFFFFF.toInt()
                                    } else if (!isDarkPaperCanvas && lum > 0.88f && a > 200) {
                                        0xFF1E1E1E.toInt()
                                    } else {
                                        rawC
                                    }
                                } else rawC
                                val color = androidx.compose.ui.graphics.Color(strokeColorInt).copy(alpha = baseAlpha * alphaMult)
                                val width = stroke.width
                                
                                val path = androidx.compose.ui.graphics.Path()
                                val firstPt = points.first()
                                val lx = fromNormalizedX(firstPt.x, strokePage)
                                val ly = fromNormalizedY(firstPt.y, strokePage)
                                path.moveTo(lx, ly)

                                for (i in 1 until points.size) {
                                    val pt = points[i]
                                    val pX = fromNormalizedX(pt.x, strokePage)
                                    val pY = fromNormalizedY(pt.y, strokePage)
                                    path.lineTo(pX, pY)
                                }

                                if (stroke.fillShape && stroke.fillOpacity > 0f) {
                                    drawPath(
                                        path = path,
                                        color = androidx.compose.ui.graphics.Color(strokeColorInt).copy(alpha = stroke.fillOpacity * baseAlpha * alphaMult),
                                        style = androidx.compose.ui.graphics.drawscope.Fill
                                    )
                                }
                                val pathEffect = if (stroke.toolType == "lasso" && !lassoSolidLine) {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                } else if (stroke.toolType == "pencil") {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1f, 4f), 0f)
                                } else null

                                val rainbowBrush = if (stroke.isRainbow) {
                                    val bounds = path.getBounds()
                                    val brushEnd = if (bounds.width == 0f && bounds.height == 0f) {
                                        androidx.compose.ui.geometry.Offset(bounds.right + 1f, bounds.bottom + 1f)
                                    } else {
                                        androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom)
                                    }
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFFF0000),
                                            androidx.compose.ui.graphics.Color(0xFFFF7F00),
                                            androidx.compose.ui.graphics.Color(0xFFFFFF00),
                                            androidx.compose.ui.graphics.Color(0xFF00FF00),
                                            androidx.compose.ui.graphics.Color(0xFF0000FF),
                                            androidx.compose.ui.graphics.Color(0xFF4B0082),
                                            androidx.compose.ui.graphics.Color(0xFF8B00FF)
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(bounds.left, bounds.top),
                                        end = brushEnd
                                    )
                                } else null

                                if (stroke.toolType == "pencil") {
                                    val pencilAlpha = 0.5f * (color.alpha)
                                    val drawColor = color.copy(alpha = pencilAlpha)
                                    val baseStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width, cap = androidx.compose.ui.graphics.StrokeCap.Square, join = androidx.compose.ui.graphics.StrokeJoin.Bevel, pathEffect = pathEffect
                                    )
                                    val drawAction: (androidx.compose.ui.graphics.drawscope.Stroke, androidx.compose.ui.graphics.Color?, androidx.compose.ui.graphics.Brush?) -> Unit = { style, col, br ->
                                        if (br != null) drawPath(path, brush = br, style = style)
                                        else drawPath(path, color = col!!, style = style)
                                    }
                                    drawAction(baseStyle, drawColor, rainbowBrush)
                                    val offset1 = 0.3f * width
                                    val stroke2 = androidx.compose.ui.graphics.drawscope.Stroke(width = width * 0.7f, cap = androidx.compose.ui.graphics.StrokeCap.Square, join = androidx.compose.ui.graphics.StrokeJoin.Bevel, pathEffect = pathEffect)
                                    val stroke3 = androidx.compose.ui.graphics.drawscope.Stroke(width = width * 0.4f, cap = androidx.compose.ui.graphics.StrokeCap.Square, join = androidx.compose.ui.graphics.StrokeJoin.Bevel, pathEffect = pathEffect)

                                    drawContext.transform.translate(offset1, offset1)
                                    drawAction(stroke2, drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    drawContext.transform.translate(-offset1, -offset1) 
                                    drawContext.transform.translate(-offset1, -offset1)
                                    drawAction(stroke2, drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    drawContext.transform.translate(offset1, offset1) 
                                    drawContext.transform.translate(offset1, -offset1)
                                    drawAction(stroke3, drawColor.copy(alpha = pencilAlpha * 0.5f), rainbowBrush)
                                    drawContext.transform.translate(-offset1, offset1)
                                } else {
                                    if (rainbowBrush != null) {
                                        drawPath(path = path, brush = rainbowBrush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round, pathEffect = pathEffect), blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode)
                                    } else {
                                        drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round, pathEffect = pathEffect), blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode)
                                    }
                                }
                            }
                        }
                    }
                }
            } // End of drawSingleStroke in second Canvas

                // Draw active highlighters
                for (i in lassoSelectedStrokes.indices) {
                    val s = lassoSelectedStrokes[i]
                    if (s.toolType == "highlighter") drawSingleStroke(s, true, 1f)
                }
                currentStroke?.let { if (it.toolType == "highlighter") drawSingleStroke(it, false, 1f) }

                // Draw active inks and fading strokes
                val now = System.currentTimeMillis()
                for (i in fadingStrokes.indices) {
                    val fs = fadingStrokes[i]
                    val age = now - fs.createdAt
                    val alpha = if (fs.durationMs > 0) 1f - (age.toFloat() / fs.durationMs.toFloat()).coerceIn(0f, 1f) else 1f
                    drawSingleStroke(fs.stroke, false, alpha)
                }

                for (i in lassoSelectedStrokes.indices) {
                    val s = lassoSelectedStrokes[i]
                    if (s.toolType != "highlighter") drawSingleStroke(s, true, 1f)
                }
                currentStroke?.let { if (it.toolType != "highlighter") drawSingleStroke(it, false, 1f) }

                // 4. Draw Lasso Bounding Selector and Handles (if selection is active)
                lassoBoundingBox?.let { box ->
                    val selectColor = Color(0xFF2196F3)
                    val cX = (box.left + box.right) / 2f
                    val cY = (box.top + box.bottom) / 2f
                    val halfW = ((box.right - box.left) / 2f) * lassoScaleX
                    val halfH = ((box.bottom - box.top) / 2f) * lassoScaleY
                    val movedBox = Rect(
                        left = fromNormalizedX(cX - halfW + lassoDragOffset.x, pdfPage),
                        top = fromNormalizedY(cY - halfH + lassoDragOffset.y, pdfPage),
                        right = fromNormalizedX(cX + halfW + lassoDragOffset.x, pdfPage),
                        bottom = fromNormalizedY(cY + halfH + lassoDragOffset.y, pdfPage)
                    )

                    // Soft selection fill
                    drawRect(
                        color = selectColor.copy(alpha = 0.08f),
                        topLeft = Offset(movedBox.left, movedBox.top),
                        size = Size(movedBox.width, movedBox.height)
                    )

                    // Dashed bounding rectangle
                    drawRect(
                        color = selectColor,
                        topLeft = Offset(movedBox.left, movedBox.top),
                        size = Size(movedBox.width, movedBox.height),
                        style = DrawStroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    )

                    // Corner handles
                    val handleRadius = 6.dp.toPx()
                    val corners = listOf(
                        Offset(movedBox.left, movedBox.top),
                        Offset(movedBox.right, movedBox.top),
                        Offset(movedBox.left, movedBox.bottom),
                        Offset(movedBox.right, movedBox.bottom)
                    )
                    corners.forEach { corner ->
                        drawCircle(
                            color = selectColor,
                            radius = handleRadius,
                            center = corner
                        )
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius - 2.dp.toPx(),
                            center = corner
                        )
                    }
                }

                // 5. Draw Beautiful Rotatable & Draggable Ruler Overlay
                if (isRulerActive) {
                    val angleRad = Math.toRadians(rulerAngle.toDouble())
                    withTransform({
                        translate(rulerOffset.x, rulerOffset.y)
                        rotate(rulerAngle)
                    }) {
                        val rulerShadowColor = Color.Black.copy(alpha = 0.12f)
                        val rulerRectColor = Color(0xF2F8FAF9) // Soft ivory-gray translucent plastic
                        val rulerBorderColor = Color(0xFF94A3B8)
                        
                        // Ruler shadow for realistic glass/plastic lifting effect
                        drawRoundRect(
                            color = rulerShadowColor,
                            topLeft = Offset(-rulerW / 2 + 5f, -rulerH / 2 + 5f),
                            size = Size(rulerW, rulerH),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                        
                        // Translucent Ruler main body
                        drawRoundRect(
                            color = rulerRectColor,
                            topLeft = Offset(-rulerW / 2, -rulerH / 2),
                            size = Size(rulerW, rulerH),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                        
                        // Border
                        drawRoundRect(
                            color = rulerBorderColor,
                            topLeft = Offset(-rulerW / 2, -rulerH / 2),
                            size = Size(rulerW, rulerH),
                            cornerRadius = CornerRadius(14f, 14f),
                            style = DrawStroke(width = 1.5.dp.toPx())
                        )
                        
                        // Draw centimeter / millimeter markings along both edges
                        val tickColor = Color(0xFF475569)
                        val tickStep = 10f // pixels per division
                        val tickCount = (rulerH / tickStep).toInt()
                        
                        for (i in 0..tickCount) {
                            val yPos = -rulerH / 2 + i * tickStep
                            
                            // Left edge ticks (x = -rulerW/2)
                            val leftLen = when {
                                i % 10 == 0 -> 24f // 1cm
                                i % 5 == 0 -> 16f  // 0.5cm
                                else -> 8f         // 1mm
                            }
                            drawLine(
                                color = tickColor,
                                start = Offset(-rulerW / 2, yPos),
                                end = Offset(-rulerW / 2 + leftLen, yPos),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            // Right edge ticks (x = rulerW/2)
                            val rightLen = when {
                                i % 10 == 0 -> 24f
                                i % 5 == 0 -> 16f
                                else -> 8f
                            }
                            drawLine(
                                color = tickColor,
                                start = Offset(rulerW / 2, yPos),
                                end = Offset(rulerW / 2 - rightLen, yPos),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            // Center centimeter text label (e.g. "0 cm", "1 cm", ...)
                            if (i % 10 == 0 && yPos > -rulerH / 2 + 30f && yPos < rulerH / 2 - 30f) {
                                val cmValue = i / 10
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#475569")
                                        textSize = 10.dp.toPx()
                                        isAntiAlias = true
                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    canvas.nativeCanvas.drawText(
                                        "$cmValue cm",
                                        0f,
                                        yPos + 4.dp.toPx(),
                                        paint
                                    )
                                }
                            }
                        }
                        
                        // Center rotation dial (pivot circle)
                        val dialR = 36.dp.toPx()
                        drawCircle(
                            color = Color(0xFFE2E8F0),
                            radius = dialR,
                            center = Offset.Zero
                        )
                        drawCircle(
                            color = Color(0xFF3B82F6),
                            radius = dialR,
                            center = Offset.Zero,
                            style = DrawStroke(width = 2.dp.toPx())
                        )
                        
                        // Draw rotation angle text inside the dial
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#1D4ED8")
                                textSize = 11.dp.toPx()
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val displayAngle = rulerAngle.toInt().let { if (it < 0) it + 360 else it }
                            canvas.nativeCanvas.drawText(
                                "$displayAngle°",
                                0f,
                                4.dp.toPx(),
                                paint
                            )
                        }
                    }
                }
            }
        }
        } // Close the Box with pointerInteropFilter
        
        // 6. Floating Zoom Controls Overlay (aligned at bottom-start of the drawing canvas)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                AnimatedVisibility(
                    visible = showZoomIndicator || isZoomLocked,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.9f), // Sleek translucent slate-dark theme
                            contentColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Zoom Out [-]
                            IconButton(
                                onClick = { scale = (scale - 0.15f).coerceIn(0.5f, 3.5f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            // Zoom Percentage Indicator & Reset Button
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .clickable {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                    .padding(horizontal = 4.dp)
                            )
                            
                            // Zoom In [+]
                            IconButton(
                                onClick = { scale = (scale + 0.15f).coerceIn(0.5f, 3.5f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF475569))
                            )
                            
                            // Gesture Lock/Unlock Toggle Button
                            IconButton(
                                onClick = { isZoomLocked = !isZoomLocked },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isZoomLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Toggle gesture lock",
                                    tint = if (isZoomLocked) Color(0xFF3B82F6) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
