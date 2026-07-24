with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

draw_single_stroke_code = """
                val drawSingleStroke: (com.example.data.Stroke, Boolean) -> Unit = { stroke, isLassoed ->
                    if (!stroke.isHidden) {
                        val strokePage = if (isScrollablePdf) stroke.page.coerceIn(1, pdfPageCount) else 1
                        val isVisible = if (isScrollablePdf) {
                            val visibleStartPage = ((-offset.y) / pdfH).toInt() - 1
                            val visibleEndPage = ((-offset.y + heightPx) / pdfH).toInt() + 1
                            strokePage in visibleStartPage..visibleEndPage
                        } else true

                        if (isVisible) {
                            val topOffset = if (isScrollablePdf) (strokePage - 1) * pdfH else 0f
                            val points = stroke.points
                            if (points.isNotEmpty()) {
                                val color = if (isLassoed) androidx.compose.ui.graphics.Color(stroke.color).copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color(stroke.color)
                                val width = stroke.width
                                
                                val path = androidx.compose.ui.graphics.Path()
                                val firstPt = points.first()
                                val lx = if (isScrollablePdf) (firstPt.x / 600f) * pdfW else firstPt.x
                                val ly = if (isScrollablePdf) (firstPt.y / 800f) * pdfH else firstPt.y
                                path.moveTo(lx + pdfOffset.x, ly + pdfOffset.y + topOffset)

                                for (i in 1 until points.size) {
                                    val pt = points[i]
                                    val pX = if (isScrollablePdf) (pt.x / 600f) * pdfW else pt.x
                                    val pY = if (isScrollablePdf) (pt.y / 800f) * pdfH else pt.y
                                    path.lineTo(pX + pdfOffset.x, pY + pdfOffset.y + topOffset)
                                }

                                drawPath(
                                    path = path,
                                    color = color,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = width,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    ),
                                    blendMode = if (stroke.toolType == "highlighter") androidx.compose.ui.graphics.BlendMode.Multiply else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                                )
                            }
                        }
                    }
                }
"""

content = content.replace("// 2. Draw Highlighter Layer", draw_single_stroke_code.lstrip('\n') + "\n                // 2. Draw Highlighter Layer")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)

