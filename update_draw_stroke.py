import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

sig_old = """                                val drawSingleStroke: (com.example.data.Stroke, Boolean) -> Unit = { stroke, isLassoed ->
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
                                val color = if (isLassoed) androidx.compose.ui.graphics.Color(stroke.color).copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color(stroke.color)"""

sig_new = """                                val drawSingleStroke: (com.example.data.Stroke, Boolean, Float) -> Unit = { stroke, isLassoed, alphaMult ->
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
                                val baseAlpha = if (isLassoed) 0.3f else 1f
                                val color = androidx.compose.ui.graphics.Color(stroke.color).copy(alpha = baseAlpha * alphaMult)"""

content = content.replace(sig_old, sig_new)

# Update calls to drawSingleStroke
content = content.replace("drawSingleStroke(it, false)", "drawSingleStroke(it, false, 1f)")
content = content.replace("drawSingleStroke(it, true)", "drawSingleStroke(it, true, 1f)")

# Now inject fading strokes
fading_logic = """                // 3. Draw Ink Layer (Pens, Erasers paths, Lasso guides)
                strokes.filter { it.toolType != "highlighter" }.forEach { drawSingleStroke(it, false, 1f) }
                
                // Draw Fading Strokes (Laser)
                val now = System.currentTimeMillis()
                fadingStrokes.forEach { fs ->
                    val age = now - fs.createdAt
                    val alpha = if (fs.durationMs > 0) 1f - (age.toFloat() / fs.durationMs.toFloat()).coerceIn(0f, 1f) else 1f
                    drawSingleStroke(fs.stroke, false, alpha)
                }
"""

content = content.replace("""                // 3. Draw Ink Layer (Pens, Erasers paths, Lasso guides)
                strokes.filter { it.toolType != "highlighter" }.forEach { drawSingleStroke(it, false, 1f) }""", fading_logic)


with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
