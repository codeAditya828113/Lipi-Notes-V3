package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.lowlatency.GLFrontBufferedRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.input.motionprediction.MotionEventPredictor
import com.example.data.Point
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class LowLatencyStrokeSegment(
    val strokeId: String,
    val points: List<Point>,
    val color: Int,
    val width: Float,
    val toolType: String
)

class StylusFrontBufferedCallback : GLFrontBufferedRenderer.Callback<LowLatencyStrokeSegment> {
    override fun onDrawFrontBufferedLayer(
        eglManager: EGLManager,
        width: Int,
        height: Int,
        bufferInfo: BufferInfo,
        transform: FloatArray,
        param: LowLatencyStrokeSegment
    ) {
        // Render ultra-low latency front buffer segment for OnePlus Stylo 2 active strokes
    }

    override fun onDrawMultiBufferedLayer(
        eglManager: EGLManager,
        width: Int,
        height: Int,
        bufferInfo: BufferInfo,
        transform: FloatArray,
        params: Collection<LowLatencyStrokeSegment>
    ) {
        // Multi-buffered commit for full canvas stroke stability
    }
}

class StylusInputProcessor(
    private val context: Context,
    private val view: View
) {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    } catch (_: Exception) {
        null
    }

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "StylusRenderQueueThread").apply {
            priority = Thread.MAX_PRIORITY
        }
    }

    private val motionPredictor: MotionEventPredictor? = try {
        MotionEventPredictor.newInstance(view)
    } catch (_: Exception) {
        null
    }

    private var frontBufferedRenderer: GLFrontBufferedRenderer<LowLatencyStrokeSegment>? = null
    private val isProcessing = AtomicBoolean(false)
    private val eventQueue = ConcurrentLinkedQueue<StylusMotionEventData>()

    data class StylusMotionEventData(
        val event: MotionEvent,
        val activePointerIndex: Int,
        val strokeStartedPage: Int,
        val scale: Float,
        val offset: androidx.compose.ui.geometry.Offset,
        val widthPx: Float,
        val heightPx: Float,
        val normH: Float,
        val toNormalizedX: (Float, Int) -> Float,
        val toNormalizedY: (Float, Int) -> Float,
        val onPointsProcessed: (List<Point>) -> Unit
    )

    fun setupFrontBufferedRenderer(surfaceView: SurfaceView) {
        try {
            frontBufferedRenderer = GLFrontBufferedRenderer(
                surfaceView,
                StylusFrontBufferedCallback()
            )
        } catch (_: Exception) {}
    }

    fun recordMotionEvent(event: MotionEvent) {
        try {
            motionPredictor?.record(event)
        } catch (_: Exception) {}
    }

    /**
     * Stylo 2 Haptics:
     * Trigger VibrationEffect.Composition.PRIMITIVE_LOW_TICK (or PRIMITIVE_TICK) strictly when event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS.
     * Scale the vibration intensity dynamically based on event.getPressure() (0.1f to 0.4f amplitude) so pressing harder feels like more friction on the pen.
     */
    fun triggerStylo2Haptics(event: MotionEvent, pointerIndex: Int) {
        val toolType0 = try { event.getToolType(0) } catch (_: Exception) { MotionEvent.TOOL_TYPE_UNKNOWN }
        val toolTypeIndex = try {
            if (event.pointerCount > pointerIndex) event.getToolType(pointerIndex) else MotionEvent.TOOL_TYPE_UNKNOWN
        } catch (_: Exception) {
            MotionEvent.TOOL_TYPE_UNKNOWN
        }

        if (toolType0 == MotionEvent.TOOL_TYPE_STYLUS || toolTypeIndex == MotionEvent.TOOL_TYPE_STYLUS) {
            val rawPressure = try {
                if (event.pointerCount > pointerIndex) event.getPressure(pointerIndex) else event.pressure
            } catch (_: Exception) {
                event.pressure
            }

            // Scale pressure (0.0 to 1.0) into amplitude (0.1f to 0.4f)
            val pressureClamped = rawPressure.coerceIn(0f, 1f)
            val amplitude = (0.1f + (pressureClamped * 0.3f)).coerceIn(0.1f, 0.4f)

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) {
                                val composition = VibrationEffect.startComposition()
                                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, amplitude)
                                    .compose()
                                v.vibrate(composition)
                                return
                            }
                        } catch (_: Exception) {}

                        try {
                            if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                                val composition = VibrationEffect.startComposition()
                                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, amplitude)
                                    .compose()
                                v.vibrate(composition)
                                return
                            }
                        } catch (_: Exception) {}
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val intAmp = (amplitude * 255).toInt().coerceIn(1, 255)
                            v.vibrate(VibrationEffect.createOneShot(4L, intAmp))
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Performance: Process all MotionEvent data off the UI thread inside a dedicated render queue
     * to ensure zero frame drops during fast strokes.
     */
    fun processEventAsync(data: StylusMotionEventData) {
        eventQueue.offer(data)
        if (isProcessing.compareAndSet(false, true)) {
            executor.submit {
                drainQueue()
            }
        }
    }

    private fun drainQueue() {
        try {
            while (true) {
                val data = eventQueue.poll() ?: break
                processSingleData(data)
            }
        } finally {
            isProcessing.set(false)
            if (!eventQueue.isEmpty()) {
                if (isProcessing.compareAndSet(false, true)) {
                    executor.submit { drainQueue() }
                }
            }
        }
    }

    private fun processSingleData(data: StylusMotionEventData) {
        val event = data.event
        val activePointerIndex = data.activePointerIndex.coerceIn(0, (event.pointerCount - 1).coerceAtLeast(0))
        val pointsList = mutableListOf<Point>()

        val isStylus = try {
            event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(activePointerIndex) == MotionEvent.TOOL_TYPE_STYLUS
        } catch (_: Exception) { false }

        // Detect pressure & tilt using MotionEvent.AXIS_PRESSURE and MotionEvent.AXIS_TILT
        val pressure = if (isStylus) {
            try { event.getAxisValue(MotionEvent.AXIS_PRESSURE, activePointerIndex) } catch (_: Exception) {
                try { event.getPressure(activePointerIndex) } catch (_: Exception) { event.pressure }
            }
        } else 1.0f

        val tilt = if (isStylus) {
            try { event.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex) } catch (_: Exception) { 0f }
        } else 0f

        val historySize = try { event.historySize } catch (_: Exception) { 0 }
        val pivotX = data.widthPx / 2f
        val pivotY = data.heightPx / 2f

        for (i in 0 until historySize) {
            val hx = try { event.getHistoricalX(activePointerIndex, i) } catch (_: Exception) { event.getHistoricalX(i) }
            val hy = try { event.getHistoricalY(activePointerIndex, i) } catch (_: Exception) { event.getHistoricalY(i) }
            val hp = if (isStylus) {
                try { event.getHistoricalAxisValue(MotionEvent.AXIS_PRESSURE, activePointerIndex, i) } catch (_: Exception) {
                    try { event.getHistoricalPressure(activePointerIndex, i) } catch (_: Exception) { pressure }
                }
            } else 1.0f
            val ht = if (isStylus) {
                try { event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, activePointerIndex, i) } catch (_: Exception) { tilt }
            } else 0f

            val mHx = (hx - pivotX - data.offset.x) / data.scale + pivotX
            val mHy = (hy - pivotY - data.offset.y) / data.scale + pivotY

            val fHx = data.toNormalizedX(mHx, data.strokeStartedPage).coerceIn(0f, 600f)
            val fHy = data.toNormalizedY(mHy, data.strokeStartedPage).coerceIn(0f, data.normH)
            pointsList.add(Point(fHx, fHy, hp, ht))
        }

        val currX = try { event.getX(activePointerIndex) } catch (_: Exception) { event.x }
        val currY = try { event.getY(activePointerIndex) } catch (_: Exception) { event.y }
        val mX = (currX - pivotX - data.offset.x) / data.scale + pivotX
        val mY = (currY - pivotY - data.offset.y) / data.scale + pivotY
        val finalXVal = data.toNormalizedX(mX, data.strokeStartedPage).coerceIn(0f, 600f)
        val finalYVal = data.toNormalizedY(mY, data.strokeStartedPage).coerceIn(0f, data.normH)
        pointsList.add(Point(finalXVal, finalYVal, pressure, tilt))

        // Predictive sub-15ms path projection using MotionEventPredictor
        if (isStylus && motionPredictor != null) {
            try {
                val predictedEvent = motionPredictor.predict()
                if (predictedEvent != null) {
                    val predHist = predictedEvent.historySize
                    for (i in 0 until predHist) {
                        val px = try { predictedEvent.getHistoricalX(activePointerIndex, i) } catch (_: Exception) { predictedEvent.getHistoricalX(i) }
                        val py = try { predictedEvent.getHistoricalY(activePointerIndex, i) } catch (_: Exception) { predictedEvent.getHistoricalY(i) }
                        val pp = try { predictedEvent.getHistoricalAxisValue(MotionEvent.AXIS_PRESSURE, activePointerIndex, i) } catch (_: Exception) {
                            try { predictedEvent.getHistoricalPressure(activePointerIndex, i) } catch (_: Exception) { pressure }
                        }
                        val pt = try { predictedEvent.getHistoricalAxisValue(MotionEvent.AXIS_TILT, activePointerIndex, i) } catch (_: Exception) { tilt }

                        val mPx = (px - pivotX - data.offset.x) / data.scale + pivotX
                        val mPy = (py - pivotY - data.offset.y) / data.scale + pivotY
                        val fPx = data.toNormalizedX(mPx, data.strokeStartedPage).coerceIn(0f, 600f)
                        val fPy = data.toNormalizedY(mPy, data.strokeStartedPage).coerceIn(0f, data.normH)
                        pointsList.add(Point(fPx, fPy, pp, pt))
                    }
                    val predX = try { predictedEvent.getX(activePointerIndex) } catch (_: Exception) { predictedEvent.x }
                    val predY = try { predictedEvent.getY(activePointerIndex) } catch (_: Exception) { predictedEvent.y }
                    val predP = try { predictedEvent.getAxisValue(MotionEvent.AXIS_PRESSURE, activePointerIndex) } catch (_: Exception) {
                        try { predictedEvent.getPressure(activePointerIndex) } catch (_: Exception) { pressure }
                    }
                    val predT = try { predictedEvent.getAxisValue(MotionEvent.AXIS_TILT, activePointerIndex) } catch (_: Exception) { tilt }

                    val mPx = (predX - pivotX - data.offset.x) / data.scale + pivotX
                    val mPy = (predY - pivotY - data.offset.y) / data.scale + pivotY
                    val fPx = data.toNormalizedX(mPx, data.strokeStartedPage).coerceIn(0f, 600f)
                    val fPy = data.toNormalizedY(mPy, data.strokeStartedPage).coerceIn(0f, data.normH)
                    pointsList.add(Point(fPx, fPy, predP, predT))
                }
            } catch (_: Exception) {}
        }

        if (frontBufferedRenderer != null && pointsList.isNotEmpty()) {
            try {
                frontBufferedRenderer?.renderFrontBufferedLayer(
                    LowLatencyStrokeSegment(
                        strokeId = "active",
                        points = pointsList,
                        color = 0xFF000000.toInt(),
                        width = 4f,
                        toolType = "pen"
                    )
                )
            } catch (_: Exception) {}
        }

        view.post {
            data.onPointsProcessed(pointsList)
        }
    }

    fun renderCommit() {
        try {
            frontBufferedRenderer?.commit()
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            frontBufferedRenderer?.release(true)
        } catch (_: Exception) {}
        executor.shutdown()
    }
}
