import re

with open('app/src/main/java/com/example/ui/components/DrawingCanvas.kt', 'r') as f:
    content = f.read()

# We want to replace the `when (templateType) { ... }` block inside `withTransform`
# The easiest way is to find the exact block and replace it.

replacement = """
                when (templateType) {
                    "pdf", "docx" -> {
                        for (p in 1..pdfPageCount) {
                            val bitmap = pdfBitmaps[p]
                            if (bitmap != null) {
                                val layoutW = bitmap.width.toFloat() / PdfHelper.PDF_QUALITY_FACTOR
                                val layoutH = bitmap.height.toFloat() / PdfHelper.PDF_QUALITY_FACTOR
                                val left = (size.width - layoutW) / 2f
                                val top = (p - 1) * layoutH
                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(layoutW.toInt(), layoutH.toInt()),
                                    colorFilter = if (isDarkTheme) {
                                        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                            androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                                                -1f, 0f, 0f, 0f, 255f,
                                                0f, -1f, 0f, 0f, 255f,
                                                0f, 0f, -1f, 0f, 255f,
                                                0f, 0f, 0f, 1f, 0f
                                            ))
                                        )
                                    } else null
                                )
                            } else {
                                val firstPageBmp = pdfBitmaps[1]
                                val pageH = if (firstPageBmp != null) firstPageBmp.height.toFloat() / PdfHelper.PDF_QUALITY_FACTOR else size.height
                                val top = (p - 1) * pageH
                                drawRect(color = actualBgColor, topLeft = Offset(0f, top), size = Size(size.width, pageH))
                            }
                        }
                    }
                    else -> {
                        for (p in 1..pdfPageCount) {
                            val topOffset = (p - 1) * size.height
                            translate(0f, topOffset) {
                                when (templateType) {
                                    "grid" -> {
                                        val gridSpacing = 30.dp.toPx()
                                        val gridColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.35f)
                                        for (gx in 0..size.width.toInt() step gridSpacing.toInt()) {
                                            drawLine(gridColor, start = Offset(gx.toFloat(), 0f), end = Offset(gx.toFloat(), size.height), strokeWidth = 1f)
                                        }
                                        for (gy in 0..size.height.toInt() step gridSpacing.toInt()) {
                                            drawLine(gridColor, start = Offset(0f, gy.toFloat()), end = Offset(size.width, gy.toFloat()), strokeWidth = 1f)
                                        }
                                    }
                                    "dotted" -> {
                                        val dotSpacing = 24.dp.toPx()
                                        val dotColor = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.45f)
                                        val dotRadius = 1.5.dp.toPx()
                                        for (gx in dotSpacing.toInt()..size.width.toInt() step dotSpacing.toInt()) {
                                            for (gy in dotSpacing.toInt()..size.height.toInt() step dotSpacing.toInt()) {
                                                drawCircle(color = dotColor, radius = dotRadius, center = Offset(gx.toFloat(), gy.toFloat()))
                                            }
                                        }
                                    }
                                    "ruled" -> {
                                        val lineSpacing = 40.dp.toPx()
                                        val ruledColor = if (isDarkTheme) Color(0xFF64748B).copy(alpha = 0.6f) else Color(0xFF94A3B8).copy(alpha = 0.75f)
                                        val marginColor = if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFFFFCDD2)
                                        val marginX = 60.dp.toPx()
                                        for (ry in lineSpacing.toInt()..size.height.toInt() step lineSpacing.toInt()) {
                                            drawLine(ruledColor, start = Offset(0f, ry.toFloat()), end = Offset(size.width, ry.toFloat()), strokeWidth = 1f)
                                        }
                                        drawLine(marginColor, start = Offset(marginX, 0f), end = Offset(marginX, size.height), strokeWidth = 2f)
                                    }
                                    "cornell" -> {
                                        val splitX = size.width * 0.28f
                                        val summaryY = size.height * 0.82f
                                        val lineColor = if (isDarkTheme) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFFBBDEFB).copy(alpha = 0.4f)
                                        val divisionColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF90A4AE)
                                        val lineSpacing = 28.dp.toPx()
                                        for (cy in lineSpacing.toInt()..summaryY.toInt() step lineSpacing.toInt()) {
                                            drawLine(lineColor, start = Offset(splitX, cy.toFloat()), end = Offset(size.width, cy.toFloat()), strokeWidth = 1f)
                                        }
                                        drawLine(divisionColor, start = Offset(splitX, 0f), end = Offset(splitX, summaryY), strokeWidth = 3f)
                                        drawLine(divisionColor, start = Offset(0f, summaryY), end = Offset(size.width, summaryY), strokeWidth = 3f)
                                        drawIntoCanvas { canvas ->
                                            val paint = android.graphics.Paint().apply {
                                                color = android.graphics.Color.GRAY
                                                textSize = 36f
                                                isAntiAlias = true
                                            }
                                            canvas.nativeCanvas.drawText("Cue / Keywords", 30f, 60f, paint)
                                            canvas.nativeCanvas.drawText("Notes Canvas", splitX + 30f, 60f, paint)
                                            canvas.nativeCanvas.drawText("Summary block", 30f, summaryY + 50f, paint)
                                        }
                                    }
                                    "meeting" -> {
                                        val cardBg = Color.White
                                        val borderColor = Color.LightGray.copy(alpha = 0.5f)
                                        drawRoundRect(color = cardBg, topLeft = Offset(20.dp.toPx(), 20.dp.toPx()), size = Size(size.width * 0.45f - 30.dp.toPx(), size.height * 0.35f), cornerRadius = CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
                                        drawRoundRect(color = borderColor, topLeft = Offset(20.dp.toPx(), 20.dp.toPx()), size = Size(size.width * 0.45f - 30.dp.toPx(), size.height * 0.35f), cornerRadius = CornerRadius(8.dp.toPx()), style = DrawStroke(width = 2f))
                                        drawRoundRect(color = cardBg, topLeft = Offset(20.dp.toPx(), size.height * 0.35f + 40.dp.toPx()), size = Size(size.width * 0.45f - 30.dp.toPx(), size.height * 0.55f - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
                                        drawRoundRect(color = borderColor, topLeft = Offset(20.dp.toPx(), size.height * 0.35f + 40.dp.toPx()), size = Size(size.width * 0.45f - 30.dp.toPx(), size.height * 0.55f - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = DrawStroke(width = 2f))
                                        drawRoundRect(color = cardBg, topLeft = Offset(size.width * 0.45f + 10.dp.toPx(), 20.dp.toPx()), size = Size(size.width * 0.55f - 30.dp.toPx(), size.height - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
                                        drawRoundRect(color = borderColor, topLeft = Offset(size.width * 0.45f + 10.dp.toPx(), 20.dp.toPx()), size = Size(size.width * 0.55f - 30.dp.toPx(), size.height - 40.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = DrawStroke(width = 2f))
                                        drawIntoCanvas { canvas ->
                                            val titlePaint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 32f; isFakeBoldText = true; isAntiAlias = true }
                                            canvas.nativeCanvas.drawText("Agenda", 20.dp.toPx() + 20f, 20.dp.toPx() + 40f, titlePaint)
                                            canvas.nativeCanvas.drawText("Action Items", 20.dp.toPx() + 20f, size.height * 0.35f + 40.dp.toPx() + 40f, titlePaint)
                                            canvas.nativeCanvas.drawText("Meeting Minutes", size.width * 0.45f + 10.dp.toPx() + 20f, 20.dp.toPx() + 40f, titlePaint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
"""

# Let's find the `when (templateType) {` block inside `DrawingCanvas.kt` and replace it using regex.
start_idx = content.find('when (templateType) {\n                    "pdf", "docx" -> {')
end_idx = content.find('// 1.5 Draw Images')

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + replacement + "                " + content[end_idx:]
    with open('app/src/main/java/com/example/ui/components/DrawingCanvas.kt', 'w') as f:
        f.write(content)
else:
    print("Could not find the block to replace")
