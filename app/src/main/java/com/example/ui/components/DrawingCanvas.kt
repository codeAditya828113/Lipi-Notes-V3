package com.example.ui.components

import androidx.compose.animation.*
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
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
    onImageLongPressed: (Int, com.example.data.ImageElement) -> Unit = {_,_->},
    onImageDeleted: (Int) -> Unit = {},
    onLassoDrag: (Offset) -> Unit = {},
    onLassoScaleUpdated: (Float, Float) -> Unit = {_,_->},
    onScrollStateChanged: (Boolean) -> Unit = {}
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
    var activeLassoCorner by remember { mutableStateOf<String?>(null) } // "top_left", "top_right", "bottom_left", "bottom_right"
    var initialLassoTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var lastLassoTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var initialLassoScaleX by remember { mutableStateOf(1f) }
    var initialLassoScaleY by remember { mutableStateOf(1f) }
    var potentialShapeStroke by remember { mutableStateOf<Stroke?>(null) }
    var lastShapeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var imageDragOffset by remember { mutableStateOf(Offset.Zero) }
    var imageResizeScale by remember { mutableStateOf(1f) }
    var activeImageInteraction by remember { mutableStateOf<String?>(null) } // "drag", "resize", null
    var activeImageCorner by remember { mutableStateOf<String?>(null) } // "top_left", "top_right", "bottom_left", "bottom_right"

    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var downTouchX by remember { mutableStateOf(0f) }
    var downTouchY by remember { mutableStateOf(0f) }
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

    LaunchedEffect(scale, offset, isZooming) {
        if (isZooming) {
            showZoomIndicator = true
        } else {
            showZoomIndicator = true
            delay(2000)
            showZoomIndicator = false
        }
    }

    LaunchedEffect(showZoomIndicator) {
        onScrollStateChanged(showZoomIndicator)
    }

    // Stylus double-tap / gesture and Finger panning states
    var lastFingerDragPoint by remember { mutableStateOf<Offset?>(null) }
    var lastStylusDownTime by remember { mutableStateOf(0L) }
    var lastStylusDownX by remember { mutableStateOf(0f) }
    var lastStylusDownY by remember { mutableStateOf(0f) }
    var isWritingStartedOnPage by remember { mutableStateOf(false) }

    // Smooth inertia fling & animated page jump states
    var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var animatedPageScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastHandledPdfPage by remember { mutableStateOf(pdfPage) }
    val velocityTracker = remember { android.view.VelocityTracker.obtain() }

    // Reset translation if switched back to fixed page mode (unless it's a PDF note, which scrolls)
    LaunchedEffect(canvasMode) {
        if (canvasMode == "fixed" && templateType != "pdf" && templateType != "docx") {
            scale = 1f
            offset = Offset.Zero
        }
    }

    val actualBgColor = if (isDarkTheme && canvasBgColor == Color(0xFFFFFFFF)) Color(0xFF1E293B) else canvasBgColor
    val isDarkPaperCanvas = isDarkTheme || (0.299f * actualBgColor.red + 0.587f * actualBgColor.green + 0.114f * actualBgColor.blue) < 0.45f
    val deskBgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    BoxWithConstraints(modifier = modifier.background(deskBgColor)) {
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

        // Compute which pages are currently visible on screen (plus a generous padding buffer)
        val pageGap = with(density) { 20.dp.toPx() }
        val pageTopMargin = with(density) { 16.dp.toPx() }

        val visiblePages = remember(offset, scale, pdfPageCount, heightPx, pdfPageSizes, widthPx) {
            val visible = mutableSetOf<Int>()
            val visibleStart = -offset.y / scale
            val visibleEnd = (-offset.y + heightPx) / scale

            var currentTop = pageTopMargin
            for (p in 1..pdfPageCount) {
                val originalSize = pdfPageSizes.getOrNull(p - 1)
                val pH = if (originalSize != null && originalSize.width > 0f) {
                    val scaleX = widthPx.toFloat() / originalSize.width
                    val scaleY = heightPx.toFloat() / originalSize.height
                    val s = kotlin.math.min(scaleX, scaleY)
                    originalSize.height * s
                } else {
                    widthPx.toFloat() * (800f / 600f)
                }

                val buffer = 1500f // Pre-render buffer above & below screen for smooth lag-free scrolling
                if (currentTop + pH >= visibleStart - buffer && currentTop <= visibleEnd + buffer) {
                    visible.add(p)
                }
                currentTop += pH + pageGap
            }
            if (visible.isEmpty() && pdfPageCount >= 1) {
                visible.add(1)
            }
            visible
        }

        // Lazy render/load visible pages, retain rendered bitmaps in memory up to 25 pages
        LaunchedEffect(visiblePages, pdfFile, widthPx, heightPx) {
            if (pdfFile == null || !pdfFile.exists() || (templateType != "pdf" && templateType != "docx")) {
                pdfBitmaps = emptyMap()
                return@LaunchedEffect
            }

            withContext(Dispatchers.IO) {
                val updatedBitmaps = pdfBitmaps.toMutableMap()
                
                // 1. Only evict cached bitmaps if memory footprint grows beyond 25 pages
                var removedAny = false
                if (updatedBitmaps.size > 25) {
                    val iterator = updatedBitmaps.iterator()
                    while (iterator.hasNext() && updatedBitmaps.size > 20) {
                        val entry = iterator.next()
                        val p = entry.key
                        if (!visiblePages.contains(p) && kotlin.math.abs(p - pdfPage) > 4) {
                            iterator.remove()
                            removedAny = true
                        }
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

        val view = LocalView.current
        val motionEventPredictor = remember(view) {
            try {
                androidx.input.motionprediction.MotionEventPredictor.newInstance(view)
            } catch (e: Exception) {
                null
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
            var top = pageTopMargin
            for (i in 1 until p) {
                top += getPageHeight(i) + pageGap
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

        // Smooth animate scroll when pdfPage changes externally (e.g. Next/Prev button, jump dialog, or add page)
        LaunchedEffect(pdfPage, pdfPageCount, heightPx) {
            if (pdfPage != lastHandledPdfPage) {
                if (!isWritingStartedOnPage) {
                    val targetY = -getPageTop(pdfPage) + pageTopMargin
                    val startY = offset.y
                    val diffY = kotlin.math.abs(targetY - startY)
                    if (diffY > 4f) {
                        flingJob?.cancel()
                        animatedPageScrollJob?.cancel()
                        animatedPageScrollJob = coroutineScope.launch {
                            val anim = androidx.compose.animation.core.Animatable(startY)
                            anim.animateTo(
                                targetValue = targetY,
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 380,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            ) {
                                offset = Offset(offset.x, value)
                            }
                        }
                    }
                }
                lastHandledPdfPage = pdfPage
            }
        }

        // Sync visible page center to viewmodel selection during scrolling
        LaunchedEffect(offset.y, pdfPageCount) {
            val pageHVal = getPageHeight(1)
            if (pageHVal > 0f) {
                var pageIdx = 1
                var accumulatedHeight = pageTopMargin
                val centerY = -offset.y + heightPx / 2f
                for (p in 1..pdfPageCount) {
                    val pH = getPageHeight(p) + pageGap
                    if (centerY >= accumulatedHeight - pageGap / 2f && centerY < accumulatedHeight + pH - pageGap / 2f) {
                        pageIdx = p
                        break
                    }
                    accumulatedHeight += pH
                    if (p == pdfPageCount) pageIdx = pdfPageCount
                }
                val coercedPage = pageIdx.coerceIn(1, pdfPageCount)
                if (coercedPage != lastHandledPdfPage) {
                    lastHandledPdfPage = coercedPage
                    onPageSelected(coercedPage)
                }
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
                        try {
                            motionEventPredictor?.record(motionEvent)
                        } catch (_: Exception) {}
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

                    // Palm Rejection: If stylus is active, ignore finger touches when stylusOnlyDrawing is enabled
                    if (isStylus && isFinger && stylusOnlyDrawing) {
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

                    // Image Interaction (Select, Move, Resize via 4 Corner Handles)
                    if (action == MotionEvent.ACTION_DOWN) {
                        var touchedImageIndex: Int? = null
                        var isResize = false
                        var touchedCorner: String? = null

                        val pivotX = widthPx / 2f
                        val pivotY = heightPx / 2f
                        val worldX = (x - pivotX - offset.x) / scale + pivotX
                        val worldY = (y - pivotY - offset.y) / scale + pivotY

                        for (i in images.indices.reversed()) {
                            val img = images[i]
                            val imgPage = img.page.coerceIn(1, pdfPageCount)
                            val renderX = fromNormalizedX(img.x, imgPage)
                            val renderY = fromNormalizedY(img.y, imgPage)
                            val renderW = (img.width / 600f) * getPageWidth(imgPage)
                            val renderH = (img.height / getNormH(imgPage)) * getPageHeight(imgPage)

                            val handleRadius = 45f

                            val isTL = kotlin.math.hypot(worldX - renderX, worldY - renderY) <= handleRadius
                            val isTR = kotlin.math.hypot(worldX - (renderX + renderW), worldY - renderY) <= handleRadius
                            val isBL = kotlin.math.hypot(worldX - renderX, worldY - (renderY + renderH)) <= handleRadius
                            val isBR = kotlin.math.hypot(worldX - (renderX + renderW), worldY - (renderY + renderH)) <= handleRadius

                            if (isTL) {
                                touchedImageIndex = i; isResize = true; touchedCorner = "top_left"; break
                            } else if (isTR) {
                                touchedImageIndex = i; isResize = true; touchedCorner = "top_right"; break
                            } else if (isBL) {
                                touchedImageIndex = i; isResize = true; touchedCorner = "bottom_left"; break
                            } else if (isBR) {
                                touchedImageIndex = i; isResize = true; touchedCorner = "bottom_right"; break
                            } else if (worldX >= renderX && worldX <= renderX + renderW && worldY >= renderY && worldY <= renderY + renderH) {
                                touchedImageIndex = i; isResize = false; break
                            }
                        }

                        if (touchedImageIndex != null) {
                            if (selectedImageIndex == touchedImageIndex) {
                                if (isResize) {
                                    activeImageInteraction = "resize"
                                    activeImageCorner = touchedCorner
                                    lastFingerDragPoint = Offset(x, y)
                                    return@pointerInteropFilter true
                                } else {
                                    activeImageInteraction = "drag"
                                    activeImageCorner = null
                                    lastFingerDragPoint = Offset(x, y)
                                    return@pointerInteropFilter true
                                }
                            }
                        } else if (lassoSelectedStrokes.isEmpty()) {
                            selectedImageIndex = null
                        }
                    }

                    if (selectedImageIndex != null && activeImageInteraction != null) {
                        val i = selectedImageIndex!!
                        if (action == MotionEvent.ACTION_MOVE) {
                            val lastPoint = lastFingerDragPoint ?: Offset(x, y)
                            val dx = (x - lastPoint.x) / scale
                            val dy = (y - lastPoint.y) / scale
                            val img = images.getOrNull(i)
                            if (img != null) {
                                val imgPage = img.page.coerceIn(1, pdfPageCount)
                                val pW = getPageWidth(imgPage)
                                val pH = getPageHeight(imgPage)

                                val virtualDx = (dx / pW) * 600f
                                val virtualDy = (dy / pH) * getNormH(imgPage)

                                if (activeImageInteraction == "drag") {
                                    onImageUpdated(i, img.copy(x = img.x + virtualDx, y = img.y + virtualDy))
                                } else if (activeImageInteraction == "resize") {
                                    var newW = img.width
                                    var newH = img.height
                                    var newX = img.x
                                    var newY = img.y

                                    when (activeImageCorner) {
                                        "bottom_right" -> {
                                            newW = maxOf(50f, img.width + virtualDx)
                                            newH = maxOf(50f, img.height + virtualDy)
                                        }
                                        "bottom_left" -> {
                                            newW = maxOf(50f, img.width - virtualDx)
                                            newH = maxOf(50f, img.height + virtualDy)
                                            newX = img.x + (img.width - newW)
                                        }
                                        "top_right" -> {
                                            newW = maxOf(50f, img.width + virtualDx)
                                            newH = maxOf(50f, img.height - virtualDy)
                                            newY = img.y + (img.height - newH)
                                        }
                                        "top_left" -> {
                                            newW = maxOf(50f, img.width - virtualDx)
                                            newH = maxOf(50f, img.height - virtualDy)
                                            newX = img.x + (img.width - newW)
                                            newY = img.y + (img.height - newH)
                                        }
                                        else -> {
                                            newW = maxOf(50f, img.width + virtualDx)
                                            newH = maxOf(50f, img.height + virtualDy)
                                        }
                                    }
                                    onImageUpdated(i, img.copy(x = newX, y = newY, width = newW, height = newH))
                                }
                            }
                            lastFingerDragPoint = Offset(x, y)
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            activeImageInteraction = null
                            activeImageCorner = null
                            lastFingerDragPoint = null
                        }
                        return@pointerInteropFilter true
                    }

                    // Active Lasso Shape Interaction (Move or Resize via 4 Corner Handles)
                    if (lassoSelectedStrokes.isNotEmpty() && lassoBoundingBox != null) {
                        val lassoTargetPage = lassoSelectedStrokes.firstOrNull()?.page?.coerceIn(1, pdfPageCount) ?: pdfPage
                        val box = lassoBoundingBox!!
                        val cX = (box.left + box.right) / 2f
                        val cY = (box.top + box.bottom) / 2f
                        val halfW = ((box.right - box.left) / 2f) * lassoScaleX
                        val halfH = ((box.bottom - box.top) / 2f) * lassoScaleY

                        val leftPx = fromNormalizedX(cX - halfW + lassoDragOffset.x, lassoTargetPage)
                        val rightPx = fromNormalizedX(cX + halfW + lassoDragOffset.x, lassoTargetPage)
                        val topPx = fromNormalizedY(cY - halfH + lassoDragOffset.y, lassoTargetPage)
                        val bottomPx = fromNormalizedY(cY + halfH + lassoDragOffset.y, lassoTargetPage)

                        val touchPxX = x
                        val touchPxY = y

                        if (action == MotionEvent.ACTION_DOWN) {
                            val handleRadius = 45f
                            val isTL = kotlin.math.hypot(touchPxX - leftPx, touchPxY - topPx) <= handleRadius
                            val isTR = kotlin.math.hypot(touchPxX - rightPx, touchPxY - topPx) <= handleRadius
                            val isBL = kotlin.math.hypot(touchPxX - leftPx, touchPxY - bottomPx) <= handleRadius
                            val isBR = kotlin.math.hypot(touchPxX - rightPx, touchPxY - bottomPx) <= handleRadius

                            if (isTL || isTR || isBL || isBR) {
                                activeLassoInteraction = "resize"
                                activeLassoCorner = if (isTL) "top_left" else if (isTR) "top_right" else if (isBL) "bottom_left" else "bottom_right"
                                initialLassoTouchPoint = Offset(touchPxX, touchPxY)
                                lastLassoTouchPoint = Offset(touchPxX, touchPxY)
                                initialLassoScaleX = lassoScaleX
                                initialLassoScaleY = lassoScaleY
                                return@pointerInteropFilter true
                            } else if (touchPxX in (leftPx - 20f)..(rightPx + 20f) && touchPxY in (topPx - 20f)..(bottomPx + 20f)) {
                                activeLassoInteraction = "move"
                                lastLassoTouchPoint = Offset(touchPxX, touchPxY)
                                return@pointerInteropFilter true
                            }
                        } else if (action == MotionEvent.ACTION_MOVE && activeLassoInteraction != null) {
                            val lastPoint = lastLassoTouchPoint ?: Offset(x, y)
                            if (activeLassoInteraction == "move") {
                                val dxPx = x - lastPoint.x
                                val dyPx = y - lastPoint.y
                                val pageW = getPageWidth(lassoTargetPage)
                                val pageH = getPageHeight(lassoTargetPage)
                                val normDx = (dxPx / scale / pageW) * 600f
                                val normDy = (dyPx / scale / pageH) * getNormH(lassoTargetPage)
                                onLassoDrag(Offset(normDx, normDy))
                                lastLassoTouchPoint = Offset(x, y)
                            } else if (activeLassoInteraction == "resize") {
                                val initPt = initialLassoTouchPoint ?: Offset(x, y)
                                val pivotPxX = if (activeLassoCorner == "top_left" || activeLassoCorner == "bottom_left") rightPx else leftPx
                                val pivotPxY = if (activeLassoCorner == "top_left" || activeLassoCorner == "top_right") bottomPx else topPx

                                val initialDistX = kotlin.math.abs(initPt.x - pivotPxX).coerceAtLeast(10f)
                                val initialDistY = kotlin.math.abs(initPt.y - pivotPxY).coerceAtLeast(10f)
                                val currentDistX = kotlin.math.abs(x - pivotPxX)
                                val currentDistY = kotlin.math.abs(y - pivotPxY)

                                val scaleXFactor = currentDistX / initialDistX
                                val scaleYFactor = currentDistY / initialDistY

                                val newScaleX = (initialLassoScaleX * scaleXFactor).coerceIn(0.1f, 10f)
                                val newScaleY = (initialLassoScaleY * scaleYFactor).coerceIn(0.1f, 10f)
                                onLassoScaleUpdated(newScaleX, newScaleY)
                                lastLassoTouchPoint = Offset(x, y)
                            }
                            return@pointerInteropFilter true
                        } else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && activeLassoInteraction != null) {
                            activeLassoInteraction = null
                            activeLassoCorner = null
                            initialLassoTouchPoint = null
                            lastLassoTouchPoint = null
                            return@pointerInteropFilter true
                        }
                    }

                    // Multi-touch gesture processing for ALL templates to allow zooming & vertical scrolling
                    if (motionEvent.pointerCount >= 2) {
                        if (isWritingStartedOnPage) {
                            onStrokeEnded()
                            isWritingStartedOnPage = false
                        }
                        when (action) {
                            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                                flingJob?.cancel()
                                animatedPageScrollJob?.cancel()
                                try { velocityTracker.clear() } catch (e: Exception) {}
                                try { velocityTracker.addMovement(motionEvent) } catch (e: Exception) {}
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
                                try { velocityTracker.addMovement(motionEvent) } catch (e: Exception) {}
                                if (isZooming && motionEvent.pointerCount >= 2) {
                                    val x0 = motionEvent.getX(0)
                                    val y0 = motionEvent.getY(0)
                                    val x1 = motionEvent.getX(1)
                                    val y1 = motionEvent.getY(1)
                                    val currentSpacing = kotlin.math.hypot(x0 - x1, y0 - y1)
                                    val currentPivot = Offset((x0 + x1) / 2f, (y0 + y1) / 2f)
                                    
                                    val viewportCenterY = heightPx / 2f
                                    val viewportCenterX = widthPx / 2f

                                    if (initialSpacing > 0f && !isZoomLocked) {
                                        val newScale = ((currentSpacing / initialSpacing) * initialScale).coerceIn(0.5f, 3.5f)
                                        val scaleRatio = if (initialScale > 0f) newScale / initialScale else 1f

                                        // Vertical focal point zoom math
                                        val rawOffsetY = currentPivot.y - viewportCenterY + (initialOffset.y + viewportCenterY - initialPivot.y) * scaleRatio

                                        // Horizontal fixed-center zoom math:
                                        // Keep page centered horizontally so left and right margins zoom in/out symmetrically
                                        val rawOffsetX = if (newScale <= 1.05f) {
                                            0f
                                        } else {
                                            // Allow slight horizontal shift relative to center if pinching off-center when zoomed in
                                            (currentPivot.x - viewportCenterX) * 0.3f + initialOffset.x * scaleRatio
                                        }

                                        scale = newScale

                                        var totalCanvasHeight = 0f
                                        for (p in 1..pdfPageCount) {
                                            totalCanvasHeight += getPageHeight(p)
                                        }
                                        val maxPositiveY = ((scale - 1f) * heightPx / 2f).coerceAtLeast(0f)
                                        val minNegativeY = kotlin.math.min(maxPositiveY, -((totalCanvasHeight - heightPx / 2f) * scale - heightPx / 2f).coerceAtLeast(0f))
                                        val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                        
                                        offset = Offset(
                                            rawOffsetX.coerceIn(-maxScrollX, maxScrollX),
                                            rawOffsetY.coerceIn(minNegativeY, maxPositiveY)
                                        )
                                    }
                                }
                            }
                            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isZooming = false
                                try { velocityTracker.clear() } catch (_: Exception) {}
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    // Map screen coordinates back to canvas space using scale and centered pivot
                    val pivotX = widthPx / 2f
                    val pivotY = heightPx / 2f
                    val mappedX = (x - pivotX - offset.x) / scale + pivotX
                    val mappedY = (y - pivotY - offset.y) / scale + pivotY

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

                    // Hand/Finger scrolling when Stylus-Only Drawing / Palm Rejection mode is active
                    if (stylusOnlyDrawing && isFinger) {
                        when (action) {
                            MotionEvent.ACTION_DOWN -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                flingJob?.cancel()
                                animatedPageScrollJob?.cancel()
                                try { velocityTracker.clear() } catch (e: Exception) {}
                                try { velocityTracker.addMovement(motionEvent) } catch (e: Exception) {}
                                lastFingerDragPoint = Offset(x, y)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                try { velocityTracker.addMovement(motionEvent) } catch (e: Exception) {}
                                val lastPoint = lastFingerDragPoint
                                if (lastPoint != null) {
                                    val dx = x - lastPoint.x
                                    val dy = y - lastPoint.y
                                    val rawOffset = Offset(offset.x + dx, offset.y + dy)
                                    
                                    var totalHeight = 0f
                                    for (p in 1..pdfPageCount) {
                                        totalHeight += getPageHeight(p)
                                    }
                                    val maxPositiveY = ((scale - 1f) * heightPx / 2f).coerceAtLeast(0f)
                                    val minNegativeY = kotlin.math.min(maxPositiveY, -((totalHeight - heightPx / 2f) * scale - heightPx / 2f).coerceAtLeast(0f))
                                    val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                    
                                    offset = Offset(
                                        rawOffset.x.coerceIn(-maxScrollX, maxScrollX),
                                        rawOffset.y.coerceIn(minNegativeY, maxPositiveY)
                                    )
                                    lastFingerDragPoint = Offset(x, y)
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                try {
                                    velocityTracker.addMovement(motionEvent)
                                    velocityTracker.computeCurrentVelocity(1000)
                                    val vx = velocityTracker.xVelocity
                                    val vy = velocityTracker.yVelocity
                                    if (kotlin.math.hypot(vx, vy) > 120f) {
                                        flingJob?.cancel()
                                        flingJob = coroutineScope.launch {
                                            var curVx = vx
                                            var curVy = vy
                                            var lastTime = System.currentTimeMillis()
                                            while (kotlin.math.hypot(curVx, curVy) > 25f) {
                                                delay(16)
                                                val now = System.currentTimeMillis()
                                                val dt = ((now - lastTime) / 1000f).coerceIn(0.001f, 0.05f)
                                                lastTime = now

                                                var totalHeight = 0f
                                                for (p in 1..pdfPageCount) {
                                                    totalHeight += getPageHeight(p)
                                                }
                                                val maxPositiveY = ((scale - 1f) * heightPx / 2f).coerceAtLeast(0f)
                                                val minNegativeY = kotlin.math.min(maxPositiveY, -((totalHeight - heightPx / 2f) * scale - heightPx / 2f).coerceAtLeast(0f))
                                                val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)

                                                val nextX = (offset.x + curVx * dt).coerceIn(-maxScrollX, maxScrollX)
                                                val nextY = (offset.y + curVy * dt).coerceIn(minNegativeY, maxPositiveY)

                                                if (nextX == -maxScrollX || nextX == maxScrollX) curVx = 0f
                                                if (nextY == minNegativeY || nextY == maxPositiveY) curVy = 0f

                                                offset = Offset(nextX, nextY)
                                                curVx *= 0.91f
                                                curVy *= 0.91f
                                            }
                                        }
                                    }
                                } catch (e: Exception) {}
                                lastFingerDragPoint = null
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    // Map to local PDF coordinate space to support vertical scrolling & rotation/split-screen perfectly
                    val touchedPage = if (isMultiPage) {
                        var pageIdx = 1
                        var accumulatedHeight = pageTopMargin
                        for (p in 1..pdfPageCount) {
                            val pH = getPageHeight(p) + pageGap
                            if (snappedY >= accumulatedHeight - pageGap / 2f && snappedY < accumulatedHeight + pH - pageGap / 2f) {
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

                    // Detect stylus pressure & tilt if available
                    val pressure = if (isStylus) {
                        try { motionEvent.getPressure(activePointerIndex) } catch (e: Exception) { motionEvent.pressure }
                    } else {
                        1.0f
                    }
                    val tilt = if (isStylus) {
                        try { motionEvent.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex) } catch (e: Exception) { 0f }
                    } else {
                        0f
                    }

                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            isWritingStartedOnPage = true
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                            isZooming = false
                            strokeStartedPage = touchedPage
                            lastHandledPdfPage = touchedPage
                            onPageSelected(touchedPage)
                            
                            val startX = toNormalizedX(snappedX, touchedPage).coerceIn(0f, 600f)
                            val startY = toNormalizedY(snappedY, touchedPage).coerceIn(0f, getNormH(touchedPage))
                            onStrokeStarted(Point(startX, startY, pressure, tilt))

                            downTouchX = x
                            downTouchY = y
                            longPressJob?.cancel()
                            longPressJob = coroutineScope.launch {
                                delay(400) // 400ms long press threshold
                                val targetPage = touchedPage
                                val targetNormX = startX
                                val targetNormY = startY

                                // Check for target image ONLY on long press
                                var targetImgIdx: Int? = null
                                var targetImgElem: com.example.data.ImageElement? = null
                                for (i in images.indices.reversed()) {
                                    val img = images[i]
                                    val imgPage = img.page.coerceIn(1, pdfPageCount)
                                    if (imgPage == targetPage) {
                                        val renderX = img.x
                                        val renderY = img.y
                                        val renderW = img.width
                                        val renderH = img.height
                                        if (targetNormX >= renderX - 15f && targetNormX <= renderX + renderW + 15f &&
                                            targetNormY >= renderY - 15f && targetNormY <= renderY + renderH + 15f) {
                                            targetImgIdx = i
                                            targetImgElem = img
                                            break
                                        }
                                    }
                                }

                                if (targetImgIdx != null && targetImgElem != null) {
                                    selectedImageIndex = targetImgIdx
                                    activeImageInteraction = "drag"
                                    lastFingerDragPoint = Offset(downTouchX, downTouchY)
                                    try {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    } catch (e: Exception) {}
                                } else {
                                    // Check for shape or stroke on long press
                                    val pageStrokes = strokes.filter { it.page == targetPage || (pdfPageCount <= 1 && it.page <= 1) }
                                    var targetShape: com.example.data.Stroke? = null

                                    // Pass 1: Prioritize explicit shapes (toolType == "shapes" or fillShape == true)
                                    for (s in pageStrokes.reversed()) {
                                        val box = SmartInkEngine.getBoundingBox(s)
                                        val padding = 35f
                                        if (targetNormX >= box.left - padding && targetNormX <= box.right + padding &&
                                            targetNormY >= box.top - padding && targetNormY <= box.bottom + padding) {
                                            if (s.toolType == "shapes" || s.fillShape) {
                                                targetShape = s
                                                break
                                            }
                                        }
                                    }

                                    // Pass 2: Check any stroke whose bounding box or points match touch location
                                    if (targetShape == null) {
                                        for (s in pageStrokes.reversed()) {
                                            val box = SmartInkEngine.getBoundingBox(s)
                                            val padding = 35f
                                            if (targetNormX >= box.left - padding && targetNormX <= box.right + padding &&
                                                targetNormY >= box.top - padding && targetNormY <= box.bottom + padding) {
                                                val isNearPoint = s.points.any { pt ->
                                                    kotlin.math.hypot(pt.x - targetNormX, pt.y - targetNormY) <= 45f
                                                }
                                                if (isNearPoint || s.toolType == "pen" || s.toolType == "highlighter") {
                                                    targetShape = s
                                                    break
                                                }
                                            }
                                        }
                                    }

                                    if (targetShape != null) {
                                        onShapeLongPressed(targetShape)
                                        activeLassoInteraction = "move"
                                        lastLassoTouchPoint = Offset(downTouchX, downTouchY)
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (activeLassoInteraction != null) {
                                longPressJob?.cancel()
                                val lastPoint = lastLassoTouchPoint ?: Offset(x, y)
                                if (activeLassoInteraction == "move") {
                                    val dxPx = x - lastPoint.x
                                    val dyPx = y - lastPoint.y
                                    val pageW = getPageWidth(strokeStartedPage)
                                    val pageH = getPageHeight(strokeStartedPage)
                                    val normDx = (dxPx / scale / pageW) * 600f
                                    val normDy = (dyPx / scale / pageH) * getNormH(strokeStartedPage)
                                    onLassoDrag(Offset(normDx, normDy))
                                    lastLassoTouchPoint = Offset(x, y)
                                }
                                return@pointerInteropFilter true
                            }
                            if (kotlin.math.hypot(x - downTouchX, y - downTouchY) > 15f) {
                                longPressJob?.cancel()
                            }
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
                                    val ht = if (isStylus) {
                                        try { motionEvent.getHistoricalAxisValue(MotionEvent.AXIS_TILT, activePointerIndex, i) } catch (e: Exception) { tilt }
                                    } else 0f

                                    val pivotX = widthPx / 2f
                                    val pivotY = heightPx / 2f
                                    val mHx = (hx - pivotX - offset.x) / scale + pivotX
                                    val mHy = (hy - pivotY - offset.y) / scale + pivotY
                                    
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
                                    pointsList.add(Point(fHx, fHy, hp, ht))
                                }
                                val finalXVal = toNormalizedX(snappedX, strokeStartedPage).coerceIn(0f, 600f)
                                val finalYVal = toNormalizedY(snappedY, strokeStartedPage).coerceIn(0f, normH)
                                pointsList.add(Point(finalXVal, finalYVal, pressure, tilt))

                                // Low-latency predictive path processing using MotionEventPredictor
                                if (isStylus && motionEventPredictor != null) {
                                    try {
                                        val predictedEvent = motionEventPredictor.predict()
                                        if (predictedEvent != null) {
                                            val predHist = predictedEvent.historySize
                                            val pivotX = widthPx / 2f
                                            val pivotY = heightPx / 2f
                                            for (i in 0 until predHist) {
                                                val px = try { predictedEvent.getHistoricalX(activePointerIndex, i) } catch (e: Exception) { predictedEvent.getHistoricalX(i) }
                                                val py = try { predictedEvent.getHistoricalY(activePointerIndex, i) } catch (e: Exception) { predictedEvent.getHistoricalY(i) }
                                                val pp = try { predictedEvent.getHistoricalPressure(activePointerIndex, i) } catch (e: Exception) { pressure }
                                                val pt = try { predictedEvent.getHistoricalAxisValue(MotionEvent.AXIS_TILT, activePointerIndex, i) } catch (e: Exception) { tilt }
                                                val mPx = (px - pivotX - offset.x) / scale + pivotX
                                                val mPy = (py - pivotY - offset.y) / scale + pivotY
                                                val fPx = toNormalizedX(mPx, strokeStartedPage).coerceIn(0f, 600f)
                                                val fPy = toNormalizedY(mPy, strokeStartedPage).coerceIn(0f, normH)
                                                pointsList.add(Point(fPx, fPy, pp, pt))
                                            }
                                            val predX = try { predictedEvent.getX(activePointerIndex) } catch (e: Exception) { predictedEvent.x }
                                            val predY = try { predictedEvent.getY(activePointerIndex) } catch (e: Exception) { predictedEvent.y }
                                            val predP = try { predictedEvent.getPressure(activePointerIndex) } catch (e: Exception) { pressure }
                                            val predT = try { predictedEvent.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex) } catch (e: Exception) { tilt }
                                            val mPx = (predX - pivotX - offset.x) / scale + pivotX
                                            val mPy = (predY - pivotY - offset.y) / scale + pivotY
                                            val fPx = toNormalizedX(mPx, strokeStartedPage).coerceIn(0f, 600f)
                                            val fPy = toNormalizedY(mPy, strokeStartedPage).coerceIn(0f, normH)
                                            pointsList.add(Point(fPx, fPy, predP, predT))
                                        }
                                    } catch (_: Exception) {}
                                }

                                if (pointsList.isNotEmpty()) {
                                    onStrokeDragged(pointsList)
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressJob?.cancel()
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
                scale(scale, scale, pivot = Offset(size.width / 2f, size.height / 2f))
            }) {
                var pdfOffset = Offset.Zero

                // Compute visible range in page Y coordinates for frustum culling
                val pivotY = heightPx / 2f
                val visibleTop = pivotY + (-offset.y - pivotY) / scale - 300f
                val visibleBottom = pivotY + (heightPx - offset.y - pivotY) / scale + 300f

                // 1. Draw Template/PDF Background with 3D Real Paper Depth
                for (p in 1..pdfPageCount) {
                    val topOffset = getPageTop(p)
                    val pageH = getPageHeight(p)
                    if (topOffset + pageH >= visibleTop && topOffset <= visibleBottom) {
                        val pageW = getPageWidth(p)
                        val pageL = getPageLeft(p)

                        // 3D Drop Shadows on Desk Surface
                        drawIntoCanvas { canvas ->
                            val dropShadowPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    24f,
                                    0f,
                                    10f,
                                    if (isDarkTheme) android.graphics.Color.argb(160, 0, 0, 0)
                                    else android.graphics.Color.argb(55, 15, 23, 42)
                                )
                                isAntiAlias = true
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                pageL + 2f, topOffset + 2f, pageL + pageW - 2f, topOffset + pageH - 2f,
                                12f, 12f,
                                dropShadowPaint
                            )

                            val ambientShadowPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    8f, 0f, 2f,
                                    if (isDarkTheme) android.graphics.Color.argb(90, 0, 0, 0)
                                    else android.graphics.Color.argb(25, 30, 41, 59)
                                )
                                isAntiAlias = true
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                pageL, topOffset, pageL + pageW, topOffset + pageH,
                                12f, 12f,
                                ambientShadowPaint
                            )
                        }

                        // Paper Surface Background
                        val paperBg = if (templateType == "pdf" || templateType == "docx") Color.White else actualBgColor
                        drawRoundRect(
                            color = paperBg,
                            topLeft = Offset(pageL, topOffset),
                            size = Size(pageW, pageH),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )

                        // PDF/DOCX or Template grid content
                        if (templateType == "pdf" || templateType == "docx") {
                            val bitmap = pdfBitmaps[p]
                            if (bitmap != null && !bitmap.isRecycled) {
                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(pageL.toInt(), topOffset.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(pageW.toInt(), pageH.toInt()),
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
                            }
                        } else {
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

                        // Paper Edge Highlight & Border Definition
                        val paperEdgeColor = if (isDarkPaperCanvas) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                        drawRoundRect(
                            color = paperEdgeColor,
                            topLeft = Offset(pageL, topOffset),
                            size = Size(pageW, pageH),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = DrawStroke(width = 1.dp.toPx())
                        )

                        val topHighlightColor = if (isDarkPaperCanvas) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
                        drawLine(
                            color = topHighlightColor,
                            start = Offset(pageL + 8.dp.toPx(), topOffset + 1.5f),
                            end = Offset(pageL + pageW - 8.dp.toPx(), topOffset + 1.5f),
                            strokeWidth = 2f
                        )
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
                                val filter = getImageColorFilter(img.filter)
                                val srcX = (bmp.width * img.cropLeft).toInt().coerceIn(0, bmp.width - 1)
                                val srcY = (bmp.height * img.cropTop).toInt().coerceIn(0, bmp.height - 1)
                                val srcW = (bmp.width * (1f - img.cropLeft - img.cropRight)).toInt().coerceIn(1, bmp.width - srcX)
                                val srcH = (bmp.height * (1f - img.cropTop - img.cropBottom)).toInt().coerceIn(1, bmp.height - srcY)

                                drawImage(
                                    image = bmp,
                                    srcOffset = androidx.compose.ui.unit.IntOffset(srcX, srcY),
                                    srcSize = androidx.compose.ui.unit.IntSize(srcW, srcH),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(renderX.toInt(), renderY.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(renderW.toInt(), renderH.toInt()),
                                    colorFilter = filter
                                )
                                if (selectedImageIndex == images.indexOf(img)) {
                                    val selectColor = Color(0xFF2196F3)
                                    drawRect(
                                        color = selectColor,
                                        topLeft = Offset(renderX, renderY),
                                        size = Size(renderW, renderH),
                                        style = DrawStroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                        )
                                    )
                                    val handleRadius = 6.dp.toPx()
                                    val corners = listOf(
                                        Offset(renderX, renderY),
                                        Offset(renderX + renderW, renderY),
                                        Offset(renderX, renderY + renderH),
                                        Offset(renderX + renderW, renderY + renderH)
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

                                if (stroke.toolType == "laser") {
                                    val spotPt = points.last()
                                    val sx = fromNormalizedX(spotPt.x, strokePage)
                                    val sy = fromNormalizedY(spotPt.y, strokePage)
                                    val spotCenter = androidx.compose.ui.geometry.Offset(sx, sy)
                                    val baseRadius = (width * 1.5f).coerceAtLeast(14f)

                                    drawCircle(
                                        color = color.copy(alpha = 0.35f * color.alpha),
                                        radius = baseRadius * 2.2f,
                                        center = spotCenter
                                    )
                                    drawCircle(
                                        color = color.copy(alpha = 0.85f * color.alpha),
                                        radius = baseRadius * 1.1f,
                                        center = spotCenter
                                    )
                                    drawCircle(
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f * color.alpha),
                                        radius = baseRadius * 0.45f,
                                        center = spotCenter
                                    )

                                    if (points.size > 1) {
                                        drawPath(
                                            path = path,
                                            color = color,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = width,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                                            )
                                        )
                                    }
                                } else if (stroke.toolType == "pencil") {
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
            val pivotY = heightPx / 2f
            val visibleTop = pivotY + (-offset.y - pivotY) / scale - 300f
            val visibleBottom = pivotY + (heightPx - offset.y - pivotY) / scale + 300f

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset(size.width / 2f, size.height / 2f))
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

                                if (stroke.toolType == "laser") {
                                    val spotPt = points.last()
                                    val sx = fromNormalizedX(spotPt.x, strokePage)
                                    val sy = fromNormalizedY(spotPt.y, strokePage)
                                    val spotCenter = androidx.compose.ui.geometry.Offset(sx, sy)
                                    val baseRadius = (width * 1.5f).coerceAtLeast(14f)

                                    drawCircle(
                                        color = color.copy(alpha = 0.35f * color.alpha),
                                        radius = baseRadius * 2.2f,
                                        center = spotCenter
                                    )
                                    drawCircle(
                                        color = color.copy(alpha = 0.85f * color.alpha),
                                        radius = baseRadius * 1.1f,
                                        center = spotCenter
                                    )
                                    drawCircle(
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f * color.alpha),
                                        radius = baseRadius * 0.45f,
                                        center = spotCenter
                                    )

                                    if (points.size > 1) {
                                        drawPath(
                                            path = path,
                                            color = color,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = width,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                                            )
                                        )
                                    }
                                } else if (stroke.toolType == "pencil") {
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
                    val lassoTargetPage = lassoSelectedStrokes.firstOrNull()?.page?.coerceIn(1, pdfPageCount) ?: pdfPage
                    val cX = (box.left + box.right) / 2f
                    val cY = (box.top + box.bottom) / 2f
                    val halfW = ((box.right - box.left) / 2f) * lassoScaleX
                    val halfH = ((box.bottom - box.top) / 2f) * lassoScaleY
                    val movedBox = Rect(
                        left = fromNormalizedX(cX - halfW + lassoDragOffset.x, lassoTargetPage),
                        top = fromNormalizedY(cY - halfH + lassoDragOffset.y, lassoTargetPage),
                        right = fromNormalizedX(cX + halfW + lassoDragOffset.x, lassoTargetPage),
                        bottom = fromNormalizedY(cY + halfH + lassoDragOffset.y, lassoTargetPage)
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
        
        // Dynamic Interactive Vertical Scrollbar and Attached Page Indicator (No overlap with top pen palette!)
        var isDraggingScrollbar by remember { mutableStateOf(false) }

        val totalCanvasHeight = remember(pdfPageCount, pdfPageSizes, widthPx, heightPx) {
            var h = pageTopMargin
            for (p in 1..pdfPageCount) {
                val originalSize = pdfPageSizes.getOrNull(p - 1)
                val pH = if (originalSize != null && originalSize.width > 0f) {
                    val scaleX = widthPx.toFloat() / originalSize.width
                    val scaleY = heightPx.toFloat() / originalSize.height
                    val s = kotlin.math.min(scaleX, scaleY)
                    originalSize.height * s
                } else {
                    widthPx.toFloat() * (800f / 600f)
                }
                h += pH + pageGap
            }
            h
        }

        val maxPositiveYScroll = ((scale - 1f) * heightPx / 2f).coerceAtLeast(0f)
        val minNegativeYScroll = kotlin.math.min(maxPositiveYScroll, -((totalCanvasHeight - heightPx / 2f) * scale - heightPx / 2f).coerceAtLeast(0f))
        val scrollRangeYVal = (maxPositiveYScroll - minNegativeYScroll).coerceAtLeast(1f)
        if (scale > 1.01f || (totalCanvasHeight - heightPx) > 10f) {
            val trackHeightPx = (heightPx.toFloat() - with(density) { 80.dp.toPx() }).coerceAtLeast(100f)
            val thumbHeightPx = (trackHeightPx * (heightPx.toFloat() / ((totalCanvasHeight * scale).coerceAtLeast(heightPx.toFloat()))))
                .coerceIn(with(density) { 48.dp.toPx() }, trackHeightPx * 0.4f)
            val maxThumbOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
            val currentProgress = ((maxPositiveYScroll - offset.y) / scrollRangeYVal).coerceIn(0f, 1f)
            val thumbTopPx = currentProgress * maxThumbOffsetPx

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 6.dp)
            ) {
                AnimatedVisibility(
                    visible = showZoomIndicator || isDraggingScrollbar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.graphicsLayer {
                            translationY = thumbTopPx
                        }
                    ) {
                        // Dynamic Floating Page Indicator Attached to Scroll Thumb
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B).copy(alpha = 0.92f),
                            shadowElevation = 6.dp,
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text(
                                text = "Page $pdfPage of $pdfPageCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Scrollbar Thumb Handle
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .background(
                                    color = if (isDraggingScrollbar) Color(0xFF3B82F6) else Color(0xFF94A3B8).copy(alpha = 0.85f),
                                    shape = CircleShape
                                )
                                .pointerInput(scrollRangeYVal, maxThumbOffsetPx) {
                                    detectVerticalDragGestures(
                                        onDragStart = { isDraggingScrollbar = true },
                                        onDragEnd = { isDraggingScrollbar = false },
                                        onDragCancel = { isDraggingScrollbar = false },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            showZoomIndicator = true
                                            val newProgress = ((thumbTopPx + dragAmount) / maxThumbOffsetPx).coerceIn(0f, 1f)
                                            val targetY = maxPositiveYScroll - newProgress * scrollRangeYVal
                                            val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                            offset = Offset(offset.x.coerceIn(-maxScrollX, maxScrollX), targetY)
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }

        // Floating Zoom Controls & Lock State Overlay (Bottom-Start)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            if (isZoomLocked) {
                // When locked: ONLY show a small lock icon in the corner
                Surface(
                    onClick = { isZoomLocked = false },
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock Page",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // When unlocked: Zoom controls appear ONLY when scrolling/zooming and disappear after 2s inactivity
                AnimatedVisibility(
                    visible = showZoomIndicator,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.9f),
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
                            IconButton(
                                onClick = {
                                    scale = (scale - 0.15f).coerceIn(0.5f, 3.5f)
                                    showZoomIndicator = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Zoom percentage text pill with single-tap 100% reset button
                            Surface(
                                onClick = {
                                    scale = 1f
                                    offset = Offset(0f, offset.y)
                                    showZoomIndicator = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                                border = BorderStroke(0.5.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = {
                                            scale = 1f
                                            offset = Offset(0f, offset.y)
                                            showZoomIndicator = true
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = "Reset Zoom to 100%",
                                            tint = if (scale != 1f) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    scale = (scale + 0.15f).coerceIn(0.5f, 3.5f)
                                    showZoomIndicator = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF475569))
                            )

                            IconButton(
                                onClick = { isZoomLocked = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Lock Page",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

                // Floating Photo Action Bar when an image is selected
                AnimatedVisibility(
                    visible = selectedImageIndex != null && selectedImageIndex!! in images.indices,
                    enter = fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) +
                            slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = tween(180)) +
                            scaleOut(targetScale = 0.85f, animationSpec = tween(180)) +
                            slideOutVertically(targetOffsetY = { -it / 2 }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    if (selectedImageIndex != null && selectedImageIndex!! in images.indices) {
                        val selImg = images[selectedImageIndex!!]
                        val selIdx = selectedImageIndex!!
                        Card(
                            modifier = Modifier.padding(top = 16.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Photo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Button(
                                    onClick = {
                                        onImageLongPressed(selIdx, selImg)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val newRot = (selImg.rotation + 90f) % 360f
                                        onImageUpdated(selIdx, selImg.copy(rotation = newRot))
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp))
                                }

                                IconButton(
                                    onClick = {
                                        onImageDeleted(selIdx)
                                        selectedImageIndex = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { selectedImageIndex = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Deselect", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

fun getImageColorFilter(filter: String): androidx.compose.ui.graphics.ColorFilter? {
    return when (filter) {
        "grayscale" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
        "sepia" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
        "invert" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )))
        "vivid" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(1.8f) })
        "warm" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 10f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
        "cool" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 1.3f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )))
        "high_contrast" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(2.2f) })
        else -> null
    }
}
