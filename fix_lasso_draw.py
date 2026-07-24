import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

draw_path_code = """                                drawPath(
                                    path = path,
                                    color = color,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    ),
                                    blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                                )"""

new_draw_path_code = """                                val pathEffect = if (stroke.toolType == "lasso" && !lassoSolidLine) {
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

content = content.replace(draw_path_code, new_draw_path_code)

# Now we need to pass lassoSolidLine to DrawingCanvas
content = content.replace("    lassoBoundingBox: Rect?,", "    lassoBoundingBox: Rect?,\n    lassoSolidLine: Boolean = false,")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content2 = f.read()

content2 = content2.replace("lassoBoundingBox = viewModel.lassoBoundingBox,", "lassoBoundingBox = viewModel.lassoBoundingBox,\n                    lassoSolidLine = viewModel.lassoSolidLine,")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content2)

