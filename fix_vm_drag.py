import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_drag = """        } else {
            activeStroke?.let { stroke ->
                var currentPoints = stroke.points
                points.forEach { point ->
                    val lastPoint = currentPoints.lastOrNull()
                    val smoothedPoint = if (lastPoint != null) {"""

new_drag = """        } else {
            activeStroke?.let { stroke ->
                if (drawStraightLines && stroke.toolType != "shapes" && stroke.toolType != "lasso") {
                    val firstPoint = stroke.points.first()
                    val lastPoint = points.last()
                    activeStroke = stroke.copy(points = listOf(firstPoint, lastPoint))
                } else {
                    var currentPoints = stroke.points
                    points.forEach { point ->
                        val lastPoint = currentPoints.lastOrNull()
                        val smoothedPoint = if (lastPoint != null) {"""

content = content.replace(old_drag, new_drag)

# We need to add one more closing brace for the `if (drawStraightLines)`
old_drag_end = """                    }
                    currentPoints = currentPoints + smoothedPoint
                }
                activeStroke = stroke.copy(points = currentPoints)
            }
        }
    }

    fun handleStrokeEnded() {"""

new_drag_end = """                    }
                        currentPoints = currentPoints + smoothedPoint
                    }
                    activeStroke = stroke.copy(points = currentPoints)
                }
            }
        }
    }

    fun handleStrokeEnded() {"""

content = content.replace(old_drag_end, new_drag_end)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

