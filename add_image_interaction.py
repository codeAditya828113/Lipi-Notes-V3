import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

image_interaction_code = """

                    // Image Interaction (Select, Move, Resize, Delete via LongPress / Corner drag)
                    if (action == MotionEvent.ACTION_DOWN) {
                        // Find if an image is touched
                        var touchedImageIndex: Int? = null
                        var isResize = false
                        for (i in images.indices.reversed()) {
                            val img = images[i]
                            val pdfHVal = if (isScrollablePdf) (pdfBitmaps[1]?.height?.toFloat() ?: heightPx.toFloat()) / PdfHelper.PDF_QUALITY_FACTOR else heightPx.toFloat()
                            val pdfOffsetVal = if (isScrollablePdf) {
                                val activePageBitmap = pdfBitmaps[img.page] ?: firstPageBitmap
                                val qFactor = if (activePageBitmap != null) PdfHelper.PDF_QUALITY_FACTOR else 1f
                                val activeW = (activePageBitmap?.width?.toFloat() ?: widthPx.toFloat()) / qFactor
                                val activeH = (activePageBitmap?.height?.toFloat() ?: heightPx.toFloat()) / qFactor
                                Offset((widthPx - activeW) / 2f, (img.page - 1) * activeH)
                            } else Offset.Zero
                            
                            val imgLocalX = img.x + pdfOffsetVal.x
                            val imgLocalY = img.y + pdfOffsetVal.y
                            // apply canvas offset and scale to touch point to get world coordinates
                            val worldX = (x - widthPx / 2f) / scale + widthPx / 2f
                            val worldY = (y - offset.y) / scale
                            
                            // Check resize handle (bottom right corner 40x40 area)
                            val handleSize = 40f
                            if (worldX >= imgLocalX + img.width - handleSize && worldX <= imgLocalX + img.width + handleSize &&
                                worldY >= imgLocalY + img.height - handleSize && worldY <= imgLocalY + img.height + handleSize) {
                                touchedImageIndex = i
                                isResize = true
                                break
                            } else if (worldX >= imgLocalX && worldX <= imgLocalX + img.width &&
                                worldY >= imgLocalY && worldY <= imgLocalY + img.height) {
                                touchedImageIndex = i
                                isResize = false
                                break
                            }
                        }
                        
                        if (touchedImageIndex != null) {
                            selectedImageIndex = touchedImageIndex
                            activeImageInteraction = if (isResize) "resize" else "drag"
                            lastFingerDragPoint = Offset(x, y)
                            return@pointerInteropFilter true
                        } else {
                            selectedImageIndex = null
                        }
                    }
                    
                    if (selectedImageIndex != null && activeImageInteraction != null) {
                        val i = selectedImageIndex!!
                        if (action == MotionEvent.ACTION_MOVE) {
                            val lastPoint = lastFingerDragPoint ?: Offset(x, y)
                            val dx = (x - lastPoint.x) / scale
                            val dy = (y - lastPoint.y) / scale
                            val img = images[i]
                            if (activeImageInteraction == "drag") {
                                onImageUpdated(i, img.copy(x = img.x + dx, y = img.y + dy))
                            } else if (activeImageInteraction == "resize") {
                                onImageUpdated(i, img.copy(width = maxOf(50f, img.width + dx), height = maxOf(50f, img.height + dy)))
                            }
                            lastFingerDragPoint = Offset(x, y)
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            activeImageInteraction = null
                            lastFingerDragPoint = null
                        }
                        return@pointerInteropFilter true
                    }
"""

content = content.replace("                    // Multi-touch gesture processing for ALL templates to allow zooming & vertical scrolling", image_interaction_code.strip('\n') + "\n\n                    // Multi-touch gesture processing for ALL templates to allow zooming & vertical scrolling")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)

