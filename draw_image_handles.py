import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

handles_code = """
                        if (templateType == "pdf" || templateType == "docx" || pdfPageCount > 1) {
                            val pdfHVal = (pdfBitmaps[1]?.height?.toFloat() ?: size.height) / PdfHelper.PDF_QUALITY_FACTOR
                            val yOffset = (img.page - 1) * pdfHVal + img.y
                            drawImage(image = bmp, dstOffset = androidx.compose.ui.unit.IntOffset((img.x + pdfOffset.x).toInt(), (yOffset + pdfOffset.y).toInt()), dstSize = androidx.compose.ui.unit.IntSize(img.width.toInt(), img.height.toInt()))
                            if (selectedImageIndex == images.indexOf(img)) {
                                drawRect(color = Color.Blue, topLeft = Offset(img.x + pdfOffset.x, yOffset + pdfOffset.y), size = Size(img.width, img.height), style = DrawStroke(2f))
                                drawCircle(color = Color.Blue, radius = 15f, center = Offset(img.x + pdfOffset.x + img.width, yOffset + pdfOffset.y + img.height))
                            }
                        } else {
                            drawImage(image = bmp, dstOffset = androidx.compose.ui.unit.IntOffset(img.x.toInt(), img.y.toInt()), dstSize = androidx.compose.ui.unit.IntSize(img.width.toInt(), img.height.toInt()))
                            if (selectedImageIndex == images.indexOf(img)) {
                                drawRect(color = Color.Blue, topLeft = Offset(img.x, img.y), size = Size(img.width, img.height), style = DrawStroke(2f))
                                drawCircle(color = Color.Blue, radius = 15f, center = Offset(img.x + img.width, img.y + img.height))
                            }
                        }
"""

# Replace the block that starts with:
#                 // 1.5 Draw Images
#                 images.forEach { img ->
#                     imageBitmaps[img.uri]?.let { bmp ->
#                         if (templateType == "pdf" || templateType == "docx") {
#                             val pdfHVal = (pdfBitmaps[1]?.height?.toFloat() ?: size.height) / PdfHelper.PDF_QUALITY_FACTOR
#                             val yOffset = (img.page - 1) * pdfHVal + img.y
#                             drawImage(image = bmp, topLeft = Offset(img.x + pdfOffset.x, yOffset + pdfOffset.y))
#                         } else {
#                             drawImage(image = bmp, topLeft = Offset(img.x, img.y))
#                         }
#                     }
#                 }

old_images_block = """                // 1.5 Draw Images
                images.forEach { img ->
                    imageBitmaps[img.uri]?.let { bmp ->
                        if (templateType == "pdf" || templateType == "docx") {
                            val pdfHVal = (pdfBitmaps[1]?.height?.toFloat() ?: size.height) / PdfHelper.PDF_QUALITY_FACTOR
                            val yOffset = (img.page - 1) * pdfHVal + img.y
                            drawImage(image = bmp, topLeft = Offset(img.x + pdfOffset.x, yOffset + pdfOffset.y))
                        } else {
                            drawImage(image = bmp, topLeft = Offset(img.x, img.y))
                        }
                    }
                }"""

new_images_block = """                // 1.5 Draw Images
                images.forEach { img ->
                    imageBitmaps[img.uri]?.let { bmp ->
""" + handles_code + """                    }
                }"""

content = content.replace(old_images_block, new_images_block)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
