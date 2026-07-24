import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

# Fix multi-touch gesture processing
old_calc = """                                        val pageCount = if (isScrollablePdf) pdfPageCount else 1
                                        val pageH = if (isScrollablePdf) pdfH else heightPx.toFloat()
                                        val maxScrollY = -((pageCount * pageH * scale - heightPx).coerceAtLeast(0f))
                                        
                                        offset = Offset(0f, rawOffset.y.coerceIn(maxScrollY, 0f))"""

new_calc = """                                        val pageCount = if (isScrollablePdf) pdfPageCount else 1
                                        val pageH = if (isScrollablePdf) pdfH else heightPx.toFloat()
                                        val maxScrollY = -((pageCount * pageH * scale - heightPx).coerceAtLeast(0f))
                                        
                                        val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                        
                                        offset = Offset(
                                            rawOffset.x.coerceIn(-maxScrollX, maxScrollX),
                                            rawOffset.y.coerceIn(maxScrollY, 0f)
                                        )"""

content = content.replace(old_calc, new_calc)

# Fix withTransform
old_transform = """            // Apply canvas panning and zooming transformations if infinite mode is active, hand scroll is enabled, or scrollable PDF is shown
            val isScrollablePdf = templateType == "pdf" || templateType == "docx" || pdfPageCount > 1
            withTransform({
                translate(0f, offset.y)
                scale(scale, scale, pivot = Offset(size.width / 2f, 0f))
            }) {"""

new_transform = """            // Apply canvas panning and zooming transformations if infinite mode is active, hand scroll is enabled, or scrollable PDF is shown
            val isScrollablePdf = templateType == "pdf" || templateType == "docx" || pdfPageCount > 1
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset(size.width / 2f, 0f))
            }) {"""

content = content.replace(old_transform, new_transform)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
