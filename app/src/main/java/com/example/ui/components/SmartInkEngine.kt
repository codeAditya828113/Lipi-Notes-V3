package com.example.ui.components

import androidx.compose.ui.geometry.Rect
import com.example.data.Point
import com.example.data.Stroke
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object SmartInkEngine {

    /**
     * Detects if a stroke represents a scribble/scratch gesture meant to erase content.
     * Criteria: High density of points, multiple horizontal reversals in a small area.
     */
    fun detectScratchToErase(stroke: Stroke): Boolean {
        val points = stroke.points
        if (points.size < 12) return false

        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var totalLength = 0f
        var prevPt = points.first()

        var xReversals = 0
        var lastDx = 0f

        for (i in 1 until points.size) {
            val pt = points[i]
            minX = minOf(minX, pt.x)
            maxX = maxOf(maxX, pt.x)
            minY = minOf(minY, pt.y)
            maxY = maxOf(maxY, pt.y)

            val dx = pt.x - prevPt.x
            val dy = pt.y - prevPt.y
            totalLength += hypot(dx, dy)

            if (i > 1) {
                if (dx * lastDx < 0f && abs(dx) > 3f) {
                    xReversals++
                }
            }
            if (abs(dx) > 1f) {
                lastDx = dx
            }
            prevPt = pt
        }

        val width = maxX - minX
        val height = maxY - minY

        // Compact but dense and wiggly horizontal motions
        return xReversals >= 5 && totalLength > 160f && width < 220f && height < 180f
    }

    /**
     * Determines the bounding box of a stroke.
     */
    fun getBoundingBox(stroke: Stroke): Rect {
        if (stroke.points.isEmpty()) return Rect.Zero
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        stroke.points.forEach { pt ->
            minX = minOf(minX, pt.x)
            maxX = maxOf(maxX, pt.x)
            minY = minOf(minY, pt.y)
            maxY = maxOf(maxY, pt.y)
        }
        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Analyzes and corrects a sloppy hand-drawn stroke into a clean geometric primitive.
     * Supports:
     * 1. Straight lines
     * 2. Circles / Ellipses
     * 3. Perfect Rectangles
     */
    
    fun generateShape(
        stroke: Stroke,
        shapeType: String,
        depth3D: Float = 0.35f,
        rotationAngle: Float = 0f
    ): Stroke {
        val points = stroke.points
        if (points.isEmpty()) return stroke
        val bounds = getBoundingBox(stroke)
        // If it's just a tap (small bounding box), make a default sized shape around it
        val (minX, minY, maxX, maxY) = if (bounds.right - bounds.left < 20f || bounds.bottom - bounds.top < 20f) {
            val cx = if (points.isNotEmpty()) points.first().x else 300f
            val cy = if (points.isNotEmpty()) points.first().y else 400f
            listOf(cx - 70f, cy - 70f, cx + 70f, cy + 70f)
        } else {
            listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)
        }
        
        val width = maxX - minX
        val height = maxY - minY
        val cx = minX + width / 2f
        val cy = minY + height / 2f
        val generatedPoints = mutableListOf<Point>()
        
        when (shapeType.lowercase()) {
            "rectangle", "square" -> {
                val finalW = if (shapeType.lowercase() == "square") minOf(width, height) else width
                val finalH = if (shapeType.lowercase() == "square") minOf(width, height) else height
                val x0 = cx - finalW / 2f
                val y0 = cy - finalH / 2f
                generatedPoints.add(Point(x0, y0))
                generatedPoints.add(Point(x0 + finalW, y0))
                generatedPoints.add(Point(x0 + finalW, y0 + finalH))
                generatedPoints.add(Point(x0, y0 + finalH))
                generatedPoints.add(Point(x0, y0))
            }
            "circle", "ellipse" -> {
                val rx = if (shapeType.lowercase() == "circle") minOf(width, height) / 2f else width / 2f
                val ry = if (shapeType.lowercase() == "circle") minOf(width, height) / 2f else height / 2f
                for (i in 0..48) {
                    val angle = (i / 48f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(angle)).toFloat(), (cy + ry * sin(angle)).toFloat()))
                }
            }
            "triangle" -> {
                generatedPoints.add(Point(cx, minY))
                generatedPoints.add(Point(maxX, maxY))
                generatedPoints.add(Point(minX, maxY))
                generatedPoints.add(Point(cx, minY))
            }
            "right_triangle" -> {
                generatedPoints.add(Point(minX, minY))
                generatedPoints.add(Point(maxX, maxY))
                generatedPoints.add(Point(minX, maxY))
                generatedPoints.add(Point(minX, minY))
            }
            "star" -> {
                val rx = width / 2f
                val ry = height / 2f
                for (i in 0..10) {
                    val angle = i * Math.PI / 5 - Math.PI / 2
                    val rFactor = if (i % 2 == 0) 1.0f else 0.4f
                    generatedPoints.add(Point((cx + rx * rFactor * cos(angle)).toFloat(), (cy + ry * rFactor * sin(angle)).toFloat()))
                }
            }
            "pentagon", "hexagon", "octagon" -> {
                val sides = if (shapeType.lowercase() == "pentagon") 5 else if (shapeType.lowercase() == "hexagon") 6 else 8
                val rx = width / 2f
                val ry = height / 2f
                for (i in 0..sides) {
                    val angle = i * 2 * Math.PI / sides - Math.PI / 2
                    generatedPoints.add(Point((cx + rx * cos(angle)).toFloat(), (cy + ry * sin(angle)).toFloat()))
                }
            }
            "rhombus", "diamond" -> {
                generatedPoints.add(Point(cx, minY))
                generatedPoints.add(Point(maxX, cy))
                generatedPoints.add(Point(cx, maxY))
                generatedPoints.add(Point(minX, cy))
                generatedPoints.add(Point(cx, minY))
            }
            "parallelogram" -> {
                val offset = width * 0.2f
                generatedPoints.add(Point(minX + offset, minY))
                generatedPoints.add(Point(maxX, minY))
                generatedPoints.add(Point(maxX - offset, maxY))
                generatedPoints.add(Point(minX, maxY))
                generatedPoints.add(Point(minX + offset, minY))
            }
            "trapezoid" -> {
                val inset = width * 0.2f
                generatedPoints.add(Point(minX + inset, minY))
                generatedPoints.add(Point(maxX - inset, minY))
                generatedPoints.add(Point(maxX, maxY))
                generatedPoints.add(Point(minX, maxY))
                generatedPoints.add(Point(minX + inset, minY))
            }
            "heart" -> {
                for (i in 0..60) {
                    val t = (i / 60f) * 2 * Math.PI
                    val hx = 16 * sin(t) * sin(t) * sin(t)
                    val hy = -(13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t))
                    val normX = (hx + 16) / 32f
                    val normY = (hy + 17) / 30f
                    generatedPoints.add(Point((minX + normX * width).toFloat(), (minY + normY * height).toFloat()))
                }
            }
            "arrow" -> {
                val sw = width * 0.5f
                val sh = height * 0.3f
                val xHeadStart = minX + sw
                val yTop = cy - sh / 2f
                val yBot = cy + sh / 2f
                generatedPoints.add(Point(minX, yTop))
                generatedPoints.add(Point(xHeadStart, yTop))
                generatedPoints.add(Point(xHeadStart, minY))
                generatedPoints.add(Point(maxX, cy))
                generatedPoints.add(Point(xHeadStart, maxY))
                generatedPoints.add(Point(xHeadStart, yBot))
                generatedPoints.add(Point(minX, yBot))
                generatedPoints.add(Point(minX, yTop))
            }
            "double_arrow" -> {
                val hW = width * 0.25f
                val hH = height * 0.3f
                generatedPoints.add(Point(minX + hW, cy - hH/2f))
                generatedPoints.add(Point(maxX - hW, cy - hH/2f))
                generatedPoints.add(Point(maxX - hW, minY))
                generatedPoints.add(Point(maxX, cy))
                generatedPoints.add(Point(maxX - hW, maxY))
                generatedPoints.add(Point(maxX - hW, cy + hH/2f))
                generatedPoints.add(Point(minX + hW, cy + hH/2f))
                generatedPoints.add(Point(minX + hW, maxY))
                generatedPoints.add(Point(minX, cy))
                generatedPoints.add(Point(minX + hW, minY))
                generatedPoints.add(Point(minX + hW, cy - hH/2f))
            }
            "speech_bubble" -> {
                val bodyH = height * 0.75f
                generatedPoints.add(Point(minX, minY))
                generatedPoints.add(Point(maxX, minY))
                generatedPoints.add(Point(maxX, minY + bodyH))
                generatedPoints.add(Point(minX + width * 0.4f, minY + bodyH))
                generatedPoints.add(Point(minX + width * 0.2f, maxY))
                generatedPoints.add(Point(minX + width * 0.25f, minY + bodyH))
                generatedPoints.add(Point(minX, minY + bodyH))
                generatedPoints.add(Point(minX, minY))
            }
            "cloud" -> {
                val centers = listOf(
                    Pair(minX + width * 0.25f, cy + height * 0.1f),
                    Pair(minX + width * 0.4f, minY + height * 0.3f),
                    Pair(minX + width * 0.65f, minY + height * 0.3f),
                    Pair(minX + width * 0.8f, cy + height * 0.1f),
                    Pair(cx, maxY - height * 0.15f)
                )
                val radii = listOf(width * 0.22f, width * 0.25f, width * 0.25f, width * 0.22f, width * 0.4f)
                centers.forEachIndexed { idx, center ->
                    val r = radii[idx]
                    val startA = idx * 2 * Math.PI / centers.size
                    for (step in 0..12) {
                        val a = startA + (step / 12f) * Math.PI
                        generatedPoints.add(Point((center.first + r * cos(a)).toFloat(), (center.second + r * sin(a)).toFloat()))
                    }
                }
            }
            "lightning" -> {
                generatedPoints.add(Point(minX + width * 0.55f, minY))
                generatedPoints.add(Point(minX + width * 0.2f, cy + height * 0.1f))
                generatedPoints.add(Point(minX + width * 0.45f, cy + height * 0.1f))
                generatedPoints.add(Point(minX + width * 0.35f, maxY))
                generatedPoints.add(Point(maxX, cy - height * 0.1f))
                generatedPoints.add(Point(minX + width * 0.6f, cy - height * 0.1f))
                generatedPoints.add(Point(minX + width * 0.55f, minY))
            }
            "plus" -> {
                val w3 = width / 3f
                val h3 = height / 3f
                generatedPoints.add(Point(minX + w3, minY))
                generatedPoints.add(Point(minX + 2 * w3, minY))
                generatedPoints.add(Point(minX + 2 * w3, minY + h3))
                generatedPoints.add(Point(maxX, minY + h3))
                generatedPoints.add(Point(maxX, minY + 2 * h3))
                generatedPoints.add(Point(minX + 2 * w3, minY + 2 * h3))
                generatedPoints.add(Point(minX + 2 * w3, maxY))
                generatedPoints.add(Point(minX + w3, maxY))
                generatedPoints.add(Point(minX + w3, minY + 2 * h3))
                generatedPoints.add(Point(minX, minY + 2 * h3))
                generatedPoints.add(Point(minX, minY + h3))
                generatedPoints.add(Point(minX + w3, minY + h3))
                generatedPoints.add(Point(minX + w3, minY))
            }
            // 3D FIGURES
            "cube" -> {
                val d = minOf(width, height) * depth3D
                val s = minOf(width, height) * (1f - depth3D * 0.5f)
                val x0 = minX
                val y0 = minY + d
                generatedPoints.add(Point(x0, y0))
                generatedPoints.add(Point(x0 + s, y0))
                generatedPoints.add(Point(x0 + s, y0 + s))
                generatedPoints.add(Point(x0, y0 + s))
                generatedPoints.add(Point(x0, y0))
                generatedPoints.add(Point(x0 + d, y0 - d))
                generatedPoints.add(Point(x0 + s + d, y0 - d))
                generatedPoints.add(Point(x0 + s, y0))
                generatedPoints.add(Point(x0 + s + d, y0 - d))
                generatedPoints.add(Point(x0 + s + d, y0 + s - d))
                generatedPoints.add(Point(x0 + s, y0 + s))
                generatedPoints.add(Point(x0 + s + d, y0 + s - d))
                generatedPoints.add(Point(x0 + d, y0 - d))
            }
            "cuboid" -> {
                val dX = width * depth3D * 0.5f
                val dY = height * depth3D * 0.5f
                val fW = width - dX
                val fH = height - dY
                generatedPoints.add(Point(minX, minY + dY))
                generatedPoints.add(Point(minX + fW, minY + dY))
                generatedPoints.add(Point(minX + fW, maxY))
                generatedPoints.add(Point(minX, maxY))
                generatedPoints.add(Point(minX, minY + dY))
                generatedPoints.add(Point(minX + dX, minY))
                generatedPoints.add(Point(maxX, minY))
                generatedPoints.add(Point(minX + fW, minY + dY))
                generatedPoints.add(Point(maxX, minY))
                generatedPoints.add(Point(maxX, maxY - dY))
                generatedPoints.add(Point(minX + fW, maxY))
                generatedPoints.add(Point(maxX, maxY - dY))
                generatedPoints.add(Point(minX + dX, minY))
            }
            "sphere" -> {
                val rx = width / 2f
                val ry = height / 2f
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(a)).toFloat(), (cy + ry * sin(a)).toFloat()))
                }
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(a)).toFloat(), (cy + (ry * 0.35f) * sin(a)).toFloat()))
                }
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + (rx * 0.35f) * cos(a)).toFloat(), (cy + ry * sin(a)).toFloat()))
                }
            }
            "cylinder" -> {
                val ry = height * 0.15f
                val rx = width / 2f
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(a)).toFloat(), (minY + ry + ry * sin(a)).toFloat()))
                }
                generatedPoints.add(Point(minX, minY + ry))
                generatedPoints.add(Point(minX, maxY - ry))
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(a)).toFloat(), (maxY - ry + ry * sin(a)).toFloat()))
                }
                generatedPoints.add(Point(maxX, maxY - ry))
                generatedPoints.add(Point(maxX, minY + ry))
            }
            "cone" -> {
                val ry = height * 0.18f
                val rx = width / 2f
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(a)).toFloat(), (maxY - ry + ry * sin(a)).toFloat()))
                }
                generatedPoints.add(Point(minX, maxY - ry))
                generatedPoints.add(Point(cx, minY))
                generatedPoints.add(Point(maxX, maxY - ry))
            }
            "pyramid" -> {
                val bY = maxY - height * 0.25f
                val apex = Point(cx, minY)
                val pBottom = Point(cx, maxY)
                val pLeft = Point(minX, bY)
                val pRight = Point(maxX, bY)
                val pBack = Point(cx, bY - height * 0.15f)

                generatedPoints.add(pLeft)
                generatedPoints.add(pBottom)
                generatedPoints.add(pRight)
                generatedPoints.add(pBack)
                generatedPoints.add(pLeft)
                generatedPoints.add(apex)
                generatedPoints.add(pBottom)
                generatedPoints.add(apex)
                generatedPoints.add(pRight)
                generatedPoints.add(apex)
                generatedPoints.add(pBack)
            }
            "triangular_prism" -> {
                val dX = width * depth3D * 0.6f
                val dY = height * depth3D * 0.6f
                val fW = width - dX
                val p1 = Point(minX + fW/2f, minY + dY)
                val p2 = Point(minX + fW, maxY)
                val p3 = Point(minX, maxY)

                generatedPoints.add(p1)
                generatedPoints.add(p2)
                generatedPoints.add(p3)
                generatedPoints.add(p1)

                val bp1 = Point(p1.x + dX, p1.y - dY)
                val bp2 = Point(p2.x + dX, p2.y - dY)
                val bp3 = Point(p3.x + dX, p3.y - dY)

                generatedPoints.add(bp1)
                generatedPoints.add(bp2)
                generatedPoints.add(p2)
                generatedPoints.add(bp2)
                generatedPoints.add(bp3)
                generatedPoints.add(p3)
                generatedPoints.add(bp3)
                generatedPoints.add(bp1)
            }
            "torus" -> {
                val rxOuter = width / 2f
                val ryOuter = height / 2f
                val rxInner = rxOuter * 0.5f
                val ryInner = ryOuter * 0.5f
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rxOuter * cos(a)).toFloat(), (cy + ryOuter * sin(a)).toFloat()))
                }
                for (i in 0..36) {
                    val a = (i / 36f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rxInner * cos(a)).toFloat(), (cy + ryInner * sin(a)).toFloat()))
                }
            }
            "capsule" -> {
                val capR = width / 2f
                for (i in 0..18) {
                    val a = Math.PI + (i / 18f) * Math.PI
                    generatedPoints.add(Point((cx + capR * cos(a)).toFloat(), (minY + capR + capR * sin(a)).toFloat()))
                }
                generatedPoints.add(Point(maxX, maxY - capR))
                for (i in 0..18) {
                    val a = (i / 18f) * Math.PI
                    generatedPoints.add(Point((cx + capR * cos(a)).toFloat(), (maxY - capR + capR * sin(a)).toFloat()))
                }
                generatedPoints.add(Point(minX, minY + capR))
            }
            "axis_3d" -> {
                val origin = Point(minX + width * 0.25f, maxY - height * 0.25f)
                val xAxis = Point(maxX, origin.y)
                val yAxis = Point(origin.x, minY)
                val zAxis = Point(minX, maxY)

                generatedPoints.add(origin)
                generatedPoints.add(xAxis)
                generatedPoints.add(Point(xAxis.x - 12f, xAxis.y - 6f))
                generatedPoints.add(xAxis)
                generatedPoints.add(Point(xAxis.x - 12f, xAxis.y + 6f))
                generatedPoints.add(xAxis)

                generatedPoints.add(origin)
                generatedPoints.add(yAxis)
                generatedPoints.add(Point(yAxis.x - 6f, yAxis.y + 12f))
                generatedPoints.add(yAxis)
                generatedPoints.add(Point(yAxis.x + 6f, yAxis.y + 12f))
                generatedPoints.add(yAxis)

                generatedPoints.add(origin)
                generatedPoints.add(zAxis)
                generatedPoints.add(Point(zAxis.x + 12f, zAxis.y - 4f))
                generatedPoints.add(zAxis)
                generatedPoints.add(Point(zAxis.x + 4f, zAxis.y - 12f))
                generatedPoints.add(zAxis)
            }
            "keyboard" -> {
                generatedPoints.add(Point(minX, minY))
                generatedPoints.add(Point(minX + width, minY))
                generatedPoints.add(Point(minX + width, minY + height))
                generatedPoints.add(Point(minX, minY + height))
                generatedPoints.add(Point(minX, minY))
                val cols = 5
                val rows = 3
                for (r in 1 until rows) {
                    val y = minY + (height / rows) * r
                    generatedPoints.add(Point(minX, y))
                    generatedPoints.add(Point(minX + width, y))
                    generatedPoints.add(Point(minX, y))
                }
                for (c in 1 until cols) {
                    val x = minX + (width / cols) * c
                    generatedPoints.add(Point(x, minY))
                    generatedPoints.add(Point(x, minY + height))
                    generatedPoints.add(Point(x, minY))
                }
            }
            else -> return detectAndCorrectShape(stroke)
        }
        
        val finalPoints = if (rotationAngle != 0f) {
            val rad = Math.toRadians(rotationAngle.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            generatedPoints.map { pt ->
                val dx = pt.x - cx
                val dy = pt.y - cy
                val rx = cx + (dx * cosA - dy * sinA)
                val ry = cy + (dx * sinA + dy * cosA)
                Point(rx, ry, pt.pressure)
            }
        } else generatedPoints

        return stroke.copy(points = finalPoints, toolType = "shapes")
    }

    fun detectAndCorrectShape(stroke: Stroke): Stroke {
        val points = stroke.points
        if (points.size < 8) return stroke

        val first = points.first()
        val last = points.last()
        val startToEndDist = hypot(last.x - first.x, last.y - first.y)

        // 1. Calculate total hand-drawn path length
        var totalLength = 0f
        for (i in 0 until points.size - 1) {
            totalLength += hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
        }
        if (totalLength <= 0f) return stroke

        // 2. Line Snapping: High directness ratio (start-to-end vs total length)
        if (startToEndDist > 60f && (startToEndDist / totalLength) > 0.91f) {
            val count = 20
            val snappedPoints = (0..count).map { step ->
                val t = step.toFloat() / count
                val x = first.x + t * (last.x - first.x)
                val y = first.y + t * (last.y - first.y)
                val p = first.pressure + t * (last.pressure - first.pressure)
                Point(x, y, p)
            }
            return stroke.copy(points = snappedPoints)
        }

        // Calculate bounding box variables
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        points.forEach { pt ->
            minX = minOf(minX, pt.x)
            maxX = maxOf(maxX, pt.x)
            minY = minOf(minY, pt.y)
            maxY = maxOf(maxY, pt.y)
        }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val width = maxX - minX
        val height = maxY - minY

        // 3. Circle / Ellipse / Rectangle Snapping (Closed loops)
        val isClosedLoop = startToEndDist < 100f && totalLength > 150f
        if (isClosedLoop) {
            val radii = points.map { pt -> hypot(pt.x - centerX, pt.y - centerY) }
            val avgRadius = radii.average().toFloat()
            val variance = radii.map { r -> (r - avgRadius) * (r - avgRadius) }.average()
            val stdDev = kotlin.math.sqrt(variance).toFloat()

            // If radius variations are low, it's a circle/ellipse
            if (avgRadius > 25f && (stdDev / avgRadius) < 0.23f) {
                val semiMajor = width / 2f
                val semiMinor = height / 2f
                val snappedPoints = (0..48).map { step ->
                    val angle = (step.toFloat() / 48f) * 2f * Math.PI.toFloat()
                    val x = centerX + semiMajor * cos(angle)
                    val y = centerY + semiMinor * sin(angle)
                    Point(x, y, 1.0f)
                }
                return stroke.copy(points = snappedPoints)
            } else if (avgRadius > 25f) {
                // Otherwise snap to a clean rectangle
                val snappedPoints = listOf(
                    Point(minX, minY, 1.0f),
                    Point(maxX, minY, 1.0f),
                    Point(maxX, maxY, 1.0f),
                    Point(minX, maxY, 1.0f),
                    Point(minX, minY, 1.0f) // Close the loop
                )
                return stroke.copy(points = snappedPoints)
            }
        }

        return stroke
    }

    /**
     * Checks if a stroke is contained within a closed lasso loop polygon.
     * For high performance, we check if the stroke's centroid is inside the lasso's bounding box.
     */
    fun isStrokeInsideLasso(stroke: Stroke, lassoPoints: List<Point>): Boolean {
        if (stroke.points.isEmpty() || lassoPoints.size < 3) return false

        // Lasso Bounding Box check
        var lMinX = Float.MAX_VALUE
        var lMaxX = Float.MIN_VALUE
        var lMinY = Float.MAX_VALUE
        var lMaxY = Float.MIN_VALUE
        lassoPoints.forEach { pt ->
            lMinX = minOf(lMinX, pt.x)
            lMaxX = maxOf(lMaxX, pt.x)
            lMinY = minOf(lMinY, pt.y)
            lMaxY = maxOf(lMaxY, pt.y)
        }

        // Calculate stroke centroid
        var sumX = 0f
        var sumY = 0f
        stroke.points.forEach { pt ->
            sumX += pt.x
            sumY += pt.y
        }
        val centroidX = sumX / stroke.points.size
        val centroidY = sumY / stroke.points.size

        // Simple bounding box containment check
        return centroidX in lMinX..lMaxX && centroidY in lMinY..lMaxY
    }
}
