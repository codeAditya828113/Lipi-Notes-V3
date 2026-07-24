import re

with open("app/src/main/java/com/example/data/DrawingModels.kt", "r") as f:
    content = f.read()

content = content.replace(
    "val fillOpacity: Float = 0f\n) {",
    "val fillOpacity: Float = 0f,\n    val isRainbow: Boolean = false\n) {"
)

old_serialize = """    fun serialize(): String {
        val pointsStr = points.joinToString(";") { "${it.x},${it.y},${it.pressure}" }
        return "$color|$width|$toolType|$page|${if(isHidden) 1 else 0}|${if(fillShape) 1 else 0}|$fillOpacity|$pointsStr"
    }"""

new_serialize = """    fun serialize(): String {
        val pointsStr = points.joinToString(";") { "${it.x},${it.y},${it.pressure}" }
        return "$color|$width|$toolType|$page|${if(isHidden) 1 else 0}|${if(fillShape) 1 else 0}|$fillOpacity|${if(isRainbow) 1 else 0}|$pointsStr"
    }"""

content = content.replace(old_serialize, new_serialize)

old_deserialize = """                if (parts.size >= 8) {
                    page = parts[3].toIntOrNull() ?: 1
                    isHidden = parts[4] == "1"
                    fillShape = parts[5] == "1"
                    fillOpacity = parts[6].toFloatOrNull() ?: 0f
                    pointsStr = parts[7]
                } else if (parts.size >= 6) {"""

new_deserialize = """                var isRainbow = false
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
                } else if (parts.size >= 6) {"""

content = content.replace(old_deserialize, new_deserialize)

old_return = """                Stroke(
                    points = points,
                    color = color,
                    width = width,
                    toolType = toolType,
                    page = page,
                    isHidden = isHidden,
                    fillShape = fillShape,
                    fillOpacity = fillOpacity
                )"""

new_return = """                Stroke(
                    points = points,
                    color = color,
                    width = width,
                    toolType = toolType,
                    page = page,
                    isHidden = isHidden,
                    fillShape = fillShape,
                    fillOpacity = fillOpacity,
                    isRainbow = isRainbow
                )"""

content = content.replace(old_return, new_return)

with open("app/src/main/java/com/example/data/DrawingModels.kt", "w") as f:
    f.write(content)
