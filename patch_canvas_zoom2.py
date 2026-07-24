import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

old_drag = """                                    val pageCount = if (isScrollablePdf) pdfPageCount else 1
                                    val pageH = if (isScrollablePdf) pdfH else heightPx.toFloat()
                                    val maxScrollY = -((pageCount * pageH * scale - heightPx).coerceAtLeast(0f))
                                    
                                    offset = Offset(0f, rawOffset.y.coerceIn(maxScrollY, 0f))"""

new_drag = """                                    val pageCount = if (isScrollablePdf) pdfPageCount else 1
                                    val pageH = if (isScrollablePdf) pdfH else heightPx.toFloat()
                                    val maxScrollY = -((pageCount * pageH * scale - heightPx).coerceAtLeast(0f))
                                    
                                    val maxScrollX = ((scale - 1f) * widthPx / 2f).coerceAtLeast(0f)
                                    
                                    offset = Offset(
                                        rawOffset.x.coerceIn(-maxScrollX, maxScrollX),
                                        rawOffset.y.coerceIn(maxScrollY, 0f)
                                    )"""

content = content.replace(old_drag, new_drag)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
