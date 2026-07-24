import re

with open("app/src/main/java/com/example/data/DrawingModels.kt", "r") as f:
    content = f.read()

stroke_old = """data class Stroke(
    val points: List<Point>,
    val color: Int,
    val width: Float,
    val toolType: String = "pen", // "pen", "highlighter", "eraser"
    val page: Int = 1,
    val isHidden: Boolean = false
) {
    // Utility to serialize this single stroke
    fun serialize(): String {
        val pointsStr = points.joinToString(";") { "${it.x},${it.y},${it.pressure}" }
        return "$color|$width|$toolType|$page|${if(isHidden) 1 else 0}|$pointsStr"
    }
}"""

stroke_new = """data class Stroke(
    val points: List<Point>,
    val color: Int,
    val width: Float,
    val toolType: String = "pen", // "pen", "highlighter", "eraser"
    val page: Int = 1,
    val isHidden: Boolean = false,
    val fillShape: Boolean = false,
    val fillOpacity: Float = 0f
) {
    // Utility to serialize this single stroke
    fun serialize(): String {
        val pointsStr = points.joinToString(";") { "${it.x},${it.y},${it.pressure}" }
        return "$color|$width|$toolType|$page|${if(isHidden) 1 else 0}|${if(fillShape) 1 else 0}|$fillOpacity|$pointsStr"
    }
}"""

content = content.replace(stroke_old, stroke_new)

ser_old = """                val (page, isHidden, pointsStr) = if (parts.size >= 6) {
                    Triple(parts[3].toIntOrNull() ?: 1, parts[4] == "1", parts[5])
                } else if (parts.size == 5) {
                    Triple(parts[3].toIntOrNull() ?: 1, false, parts[4])
                } else {
                    Triple(1, false, parts[3])
                }"""

ser_new = """                var fillShape = false
                var fillOpacity = 0f
                var pointsStr = ""
                var page = 1
                var isHidden = false

                if (parts.size >= 8) {
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
                }"""

content = content.replace(ser_old, ser_new)
content = content.replace("Stroke(points, color, width, toolType, page, isHidden)", "Stroke(points, color, width, toolType, page, isHidden, fillShape, fillOpacity)")

with open("app/src/main/java/com/example/data/DrawingModels.kt", "w") as f:
    f.write(content)

