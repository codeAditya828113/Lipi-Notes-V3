import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

old_draw = """                                    // Overlapping shifted strokes to create grain
                                    val offset1 = 0.3f * width
                                    androidx.compose.ui.graphics.drawscope.withTransform({ translate(offset1, offset1) }) {
                                        drawAction(baseStyle.copy(width = width * 0.7f), drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    }
                                    androidx.compose.ui.graphics.drawscope.withTransform({ translate(-offset1, -offset1) }) {
                                        drawAction(baseStyle.copy(width = width * 0.7f), drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    }
                                    androidx.compose.ui.graphics.drawscope.withTransform({ translate(offset1, -offset1) }) {
                                        drawAction(baseStyle.copy(width = width * 0.4f), drawColor.copy(alpha = pencilAlpha * 0.5f), rainbowBrush)
                                    }"""

new_draw = """                                    // Overlapping shifted strokes to create grain
                                    val offset1 = 0.3f * width
                                    
                                    val stroke2 = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width * 0.7f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Square,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                                        pathEffect = pathEffect
                                    )
                                    val stroke3 = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width * 0.4f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Square,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                                        pathEffect = pathEffect
                                    )

                                    drawContext.transform.translate(offset1, offset1)
                                    drawAction(stroke2, drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    drawContext.transform.translate(-offset1, -offset1) // back to origin
                                    
                                    drawContext.transform.translate(-offset1, -offset1)
                                    drawAction(stroke2, drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    drawContext.transform.translate(offset1, offset1) // back to origin
                                    
                                    drawContext.transform.translate(offset1, -offset1)
                                    drawAction(stroke3, drawColor.copy(alpha = pencilAlpha * 0.5f), rainbowBrush)
                                    drawContext.transform.translate(-offset1, offset1) // back to origin"""

content = content.replace(old_draw, new_draw)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
