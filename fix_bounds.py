import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

old_bounds = """                                val rainbowBrush = if (stroke.isRainbow) {
                                    val bounds = path.getBounds()
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
                                        end = androidx.compose.ui.geometry.Offset(bounds.right, bounds.bottom)
                                    )
                                } else null"""

new_bounds = """                                val rainbowBrush = if (stroke.isRainbow) {
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
                                } else null"""

content = content.replace(old_bounds, new_bounds)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)

