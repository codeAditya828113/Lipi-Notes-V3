import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Add laser properties
laser_props = """    var activeShapeType by mutableStateOf("rectangle") // "pen", "highlighter", "eraser", "lasso"

    // Laser Tool Settings
    var laserMode by mutableStateOf("line") // "line" or "spot"
    var laserDisappearEnabled by mutableStateOf(true)
    var laserDisappearDelay by mutableStateOf(3000L) // milliseconds
"""

content = content.replace('    var activeShapeType by mutableStateOf("rectangle") // "pen", "highlighter", "eraser", "lasso"\n', laser_props)

# Modify handleStrokeDragged for spot laser
old_dragged = """    fun handleStrokeDragged(points: List<Point>) {
        if (points.isEmpty()) return
        if (activeToolType == "eraser") {"""

new_dragged = """    fun handleStrokeDragged(points: List<Point>) {
        if (points.isEmpty()) return
        if (activeToolType == "laser" && laserMode == "spot") {
            val lastPoint = points.last()
            activeStroke = activeStroke?.copy(points = listOf(lastPoint))
            return
        }
        if (activeToolType == "eraser") {"""

content = content.replace(old_dragged, new_dragged)

# Modify handleStrokeEnded for laser line fading
old_ended_laser = """                    if (stroke.toolType == "laser") {
                        activeStroke = null
                        return
                    }"""

new_ended_laser = """                    if (stroke.toolType == "laser") {
                        if (laserMode == "line") {
                            val capturedStroke = activeStroke
                            if (capturedStroke != null) {
                                currentStrokes = currentStrokes + capturedStroke
                                if (laserDisappearEnabled) {
                                    viewModelScope.launch(Dispatchers.Main) {
                                        kotlinx.coroutines.delay(laserDisappearDelay)
                                        currentStrokes = currentStrokes - capturedStroke
                                    }
                                }
                            }
                        }
                        activeStroke = null
                        return
                    }"""

content = content.replace(old_ended_laser, new_ended_laser)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
