import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

old_draw = """                                if (rainbowBrush != null) {
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

new_draw = """                                if (stroke.toolType == "pencil") {
                                    // Pencil texture simulation with overlapping strokes
                                    val pencilAlpha = 0.5f * (color.alpha)
                                    val drawColor = color.copy(alpha = pencilAlpha)
                                    val baseStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Square,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                                        pathEffect = pathEffect
                                    )
                                    
                                    val drawAction: (androidx.compose.ui.graphics.drawscope.Stroke, androidx.compose.ui.graphics.Color?, androidx.compose.ui.graphics.Brush?) -> Unit = { style, col, br ->
                                        if (br != null) drawPath(path, brush = br, style = style)
                                        else drawPath(path, color = col!!, style = style)
                                    }
                                    
                                    // Base stroke
                                    drawAction(baseStyle, drawColor, rainbowBrush)
                                    
                                    // Overlapping shifted strokes to create grain
                                    val offset1 = 0.3f * width
                                    androidx.compose.ui.graphics.drawscope.withTransform({ translate(offset1, offset1) }) {
                                        drawAction(baseStyle.copy(width = width * 0.7f), drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    }
                                    androidx.compose.ui.graphics.drawscope.withTransform({ translate(-offset1, -offset1) }) {
                                        drawAction(baseStyle.copy(width = width * 0.7f), drawColor.copy(alpha = pencilAlpha * 0.7f), rainbowBrush)
                                    }
                                    androidx.compose.ui.graphics.drawscope.withTransform({ translate(offset1, -offset1) }) {
                                        drawAction(baseStyle.copy(width = width * 0.4f), drawColor.copy(alpha = pencilAlpha * 0.5f), rainbowBrush)
                                    }
                                } else {
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
                                    }
                                }"""

content = content.replace(old_draw, new_draw)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
