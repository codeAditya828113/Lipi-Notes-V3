package com.example.data

data class FadingStroke(
    val stroke: Stroke,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 3000L
)

data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

data class Stroke(
    val points: List<Point>,
    val color: Int,
    val width: Float,
    val toolType: String = "pen", // "pen", "highlighter", "eraser"
    val page: Int = 1,
    val isHidden: Boolean = false,
    val fillShape: Boolean = false,
    val fillOpacity: Float = 0f,
    val isRainbow: Boolean = false
) {
    // Utility to serialize this single stroke
    fun serialize(): String {
        val pointsStr = points.joinToString(";") { "${it.x},${it.y},${it.pressure}" }
        return "$color|$width|$toolType|$page|${if(isHidden) 1 else 0}|${if(fillShape) 1 else 0}|$fillOpacity|${if(isRainbow) 1 else 0}|$pointsStr"
    }
}

object StrokeSerializer {
    fun serializeStrokes(strokes: List<Stroke>): String {
        if (strokes.isEmpty()) return "[]"
        return strokes.joinToString("\n") { it.serialize() }
    }

    fun deserializeStrokes(data: String): List<Stroke> {
        if (data.isBlank() || data == "[]") return emptyList()
        return data.split("\n").mapNotNull { line ->
            try {
                val parts = line.split("|")
                if (parts.size < 4) return@mapNotNull null
                val color = parts[0].toInt()
                val width = parts[1].toFloat()
                val toolType = parts[2]
                var fillShape = false
                var fillOpacity = 0f
                var pointsStr = ""
                var page = 1
                var isHidden = false

                var isRainbow = false
                if (parts.size >= 9) {
                    page = parts[3].toIntOrNull() ?: 1
                    isHidden = parts[4] == "1"
                    fillShape = parts[5] == "1"
                    fillOpacity = parts[6].toFloatOrNull() ?: 0f
                    isRainbow = parts[7] == "1"
                    pointsStr = parts[8]
                } else if (parts.size >= 8) {
                    page = parts[3].toIntOrNull() ?: 1
                    isHidden = parts[4] == "1"
                    fillShape = parts[5] == "1"
                    fillOpacity = parts[6].toFloatOrNull() ?: 0f
                    pointsStr = parts[7]
                } else if (parts.size >= 6) {
                    page = parts[3].toIntOrNull() ?: 1
                    isHidden = parts[4] == "1"
                    pointsStr = parts[5]
                } else if (parts.size == 5) {
                    page = parts[3].toIntOrNull() ?: 1
                    pointsStr = parts[4]
                } else {
                    pointsStr = parts[3]
                }
                val points = pointsStr.split(";").mapNotNull { ptStr ->
                    val coords = ptStr.split(",")
                    if (coords.size >= 2) {
                        Point(
                            x = coords[0].toFloat(),
                            y = coords[1].toFloat(),
                            pressure = coords.getOrNull(2)?.toFloat() ?: 1.0f
                        )
                    } else null
                }
                Stroke(points, color, width, toolType, page, isHidden, fillShape, fillOpacity)
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ImageElement(
    val uri: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val page: Int = 1,
    val isHidden: Boolean = false,
    val filter: String = "none",
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 0f,
    val cropBottom: Float = 0f
) {
    fun serialize(): String {
        return "$uri|$x|$y|$width|$height|$page|$filter|$cropLeft|$cropTop|$cropRight|$cropBottom"
    }
}

object ImageElementSerializer {
    fun serializeImages(images: List<ImageElement>): String {
        if (images.isEmpty()) return "[]"
        return images.joinToString("\n") { it.serialize() }
    }

    fun deserializeImages(data: String): List<ImageElement> {
        if (data.isBlank() || data == "[]") return emptyList()
        return data.split("\n").mapNotNull { line ->
            try {
                val parts = line.split("|")
                if (parts.size < 6) return@mapNotNull null
                ImageElement(
                    uri = parts[0],
                    x = parts[1].toFloat(),
                    y = parts[2].toFloat(),
                    width = parts[3].toFloat(),
                    height = parts[4].toFloat(),
                    page = parts[5].toInt(),
                    filter = if (parts.size > 6) parts[6] else "none",
                    cropLeft = if (parts.size > 7) parts[7].toFloatOrNull() ?: 0f else 0f,
                    cropTop = if (parts.size > 8) parts[8].toFloatOrNull() ?: 0f else 0f,
                    cropRight = if (parts.size > 9) parts[9].toFloatOrNull() ?: 0f else 0f,
                    cropBottom = if (parts.size > 10) parts[10].toFloatOrNull() ?: 0f else 0f
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
