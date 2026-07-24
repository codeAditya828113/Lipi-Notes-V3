import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

old_draw = """                                val pathEffect = if (stroke.toolType == "lasso" && !lassoSolidLine) {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                } else null

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
                                )"""

new_draw = """                                val pathEffect = if (stroke.toolType == "lasso" && !lassoSolidLine) {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                } else if (stroke.toolType == "pencil") {
                                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1f, 4f), 0f)
                                } else null

                                val rainbowBrush = if (stroke.isRainbow) {
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
                                } else null

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
                                }"""

content = content.replace(old_draw, new_draw)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
