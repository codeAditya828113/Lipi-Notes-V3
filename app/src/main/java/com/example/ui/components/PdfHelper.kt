package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
    const val PDF_QUALITY_FACTOR = 1.8f

    fun loadSoftwareBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            var bitmap: Bitmap? = null
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            }
            if (bitmap == null) {
                val file = File(uriString)
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                }
            }
            if (bitmap == null) {
                val request = ImageRequest.Builder(context)
                    .data(uriString)
                    .allowHardware(false)
                    .build()
                val result = kotlinx.coroutines.runBlocking {
                    context.imageLoader.execute(request)
                }
                if (result is SuccessResult && result.drawable is BitmapDrawable) {
                    bitmap = (result.drawable as BitmapDrawable).bitmap
                }
            }
            if (bitmap != null && bitmap!!.config == Bitmap.Config.HARDWARE) {
                val copy = bitmap!!.copy(Bitmap.Config.ARGB_8888, false)
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q && !bitmap!!.isRecycled) {
                    try { bitmap!!.recycle() } catch (_: Exception) {}
                }
                bitmap = copy
            }
            bitmap
        } catch (e: Exception) {
            Log.e("PdfHelper", "Failed to load software bitmap for $uriString", e)
            null
        }
    }

    fun createSamplePdf(file: File) {
        val document = PdfDocument()
        try {
            // Page 1
            var pageInfo = PdfDocument.PageInfo.Builder(600, 800, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var paint = Paint()

            // Background
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(0f, 0f, 600f, 800f, paint)

            paint.color = android.graphics.Color.rgb(0, 97, 164) // Blue Primary
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("QUANTUM COMPUTING FOUNDATIONS (1/3)", 40f, 80f, paint)

            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Subject: Qubit Superposition & Quantum Gates", 40f, 130f, paint)
            canvas.drawText("• Unlike classical bits (0 or 1), a qubit can exist in a superposition.", 40f, 180f, paint)
            canvas.drawText("• Represented mathematically as: |ψ⟩ = α|0⟩ + β|1⟩, where |α|² + |β|² = 1.", 40f, 220f, paint)
            canvas.drawText("• Hadamard Gate (H) maps the basis state to a superposition state.", 40f, 260f, paint)
            canvas.drawText("• [Sketch the superposition probability matrix below using the stylus]", 40f, 320f, paint)

            // Draw some decorative diagrams
            paint.color = android.graphics.Color.LTGRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(300f, 500f, 80f, paint)
            canvas.drawLine(300f, 400f, 300f, 600f, paint)
            canvas.drawLine(200f, 500f, 400f, 500f, paint)

            paint.color = android.graphics.Color.DKGRAY
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
            canvas.drawText("|0⟩", 290f, 395f, paint)
            canvas.drawText("|1⟩", 290f, 615f, paint)
            canvas.drawText("|ψ⟩", 360f, 440f, paint)

            document.finishPage(page)

            // Page 2
            pageInfo = PdfDocument.PageInfo.Builder(600, 800, 2).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            paint = Paint()

            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(0f, 0f, 600f, 800f, paint)

            paint.color = android.graphics.Color.rgb(180, 40, 40)
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("GRADIENT DESCENT & ERROR (2/3)", 40f, 80f, paint)

            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Subject: Machine Learning & Backpropagation", 40f, 130f, paint)
            canvas.drawText("• Backpropagation computes gradients of loss function with respect to weights.", 40f, 180f, paint)
            canvas.drawText("• Chain Rule: ∂L/∂w = (∂L/∂y) * (∂y/∂z) * (∂z/∂w).", 40f, 220f, paint)
            canvas.drawText("• Optimizer update step: w = w - η * (∂L/∂w).", 40f, 260f, paint)
            canvas.drawText("• [Sketch the layered neural network feedforward flow below]", 40f, 320f, paint)

            // Draw network node outline
            paint.color = android.graphics.Color.LTGRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(150f, 500f, 30f, paint)
            canvas.drawCircle(300f, 440f, 30f, paint)
            canvas.drawCircle(300f, 560f, 30f, paint)
            canvas.drawCircle(450f, 500f, 30f, paint)
            canvas.drawLine(180f, 500f, 270f, 440f, paint)
            canvas.drawLine(180f, 500f, 270f, 560f, paint)
            canvas.drawLine(330f, 440f, 420f, 500f, paint)
            canvas.drawLine(330f, 560f, 420f, 500f, paint)

            document.finishPage(page)

            // Page 3
            pageInfo = PdfDocument.PageInfo.Builder(600, 800, 3).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            paint = Paint()

            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(0f, 0f, 600f, 800f, paint)

            paint.color = android.graphics.Color.rgb(0, 120, 80)
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("MATERIAL DESIGN 3 STYLING (3/3)", 40f, 80f, paint)

            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Subject: Modern Mobile Product Scaling Specs", 40f, 130f, paint)
            canvas.drawText("• Spacing Grid: Built around an 8dp baseline grid (8dp, 16dp, 24dp).", 40f, 180f, paint)
            canvas.drawText("• Touch Targets: Standard size 48dp x 48dp (minimum accessible area).", 40f, 220f, paint)
            canvas.drawText("• Colour Hierarchy: Rely on primary, secondary, and tertiary containers.", 40f, 260f, paint)
            canvas.drawText("• [Use the highlighter tool to mark crucial sections here]", 40f, 320f, paint)

            document.finishPage(page)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e("PdfHelper", "Failed to create sample pdf", e)
        } finally {
            document.close()
        }
    }

    fun getPdfPageCount(pdfFile: File): Int {
        if (!pdfFile.exists()) return 1
        return try {
            val input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(input)
            val count = renderer.pageCount
            renderer.close()
            input.close()
            count
        } catch (e: Exception) {
            Log.e("PdfHelper", "Failed to get pdf page count", e)
            1
        }
    }

    fun renderPdfPageToBitmap(pdfFile: File, pageIndex: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (!pdfFile.exists() || maxWidth <= 0 || maxHeight <= 0) return null
        var input: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(input)
            
            if (pageIndex >= renderer.pageCount) {
                return null
            }
            
            page = renderer.openPage(pageIndex)
            val pageWidth = page.width
            val pageHeight = page.height
            
            // Calculate scale to fit within maxWidth and maxHeight
            val scaleX = maxWidth.toFloat() / pageWidth
            val scaleY = maxHeight.toFloat() / pageHeight
            val scale = kotlin.math.min(scaleX, scaleY)
            
            var destWidth = (pageWidth * scale * PDF_QUALITY_FACTOR).toInt().coerceAtLeast(1)
            var destHeight = (pageHeight * scale * PDF_QUALITY_FACTOR).toInt().coerceAtLeast(1)

            // Cap dimensions to avoid huge memory allocations (max 2048px on longest side)
            val maxDimension = 2048
            if (destWidth > maxDimension || destHeight > maxDimension) {
                val capScale = maxDimension.toFloat() / maxOf(destWidth, destHeight)
                destWidth = (destWidth * capScale).toInt().coerceAtLeast(1)
                destHeight = (destHeight * capScale).toInt().coerceAtLeast(1)
            }
            
            // Create a bitmap of exactly the scaled dimensions to preserve aspect ratio
            val bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // Render PDF page beautifully into the aspect-ratio-scaled bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (t: Throwable) {
            Log.e("PdfHelper", "Failed to render PDF page $pageIndex safely", t)
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { input?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Generates a beautiful paginated PDF from a list of paragraphs (imported DOCX)
     */
    fun createPdfFromText(file: File, title: String, paragraphs: List<String>) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(600, 800, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            val paint = Paint()

            // Setup styling
            val margin = 40f
            val pageWidth = 600f
            val pageHeight = 800f
            val contentWidth = pageWidth - (margin * 2)
            
            var currentY = margin + 40f

            fun drawHeader(canvas: Canvas) {
                val headerPaint = Paint().apply {
                    color = android.graphics.Color.rgb(0, 97, 164)
                    textSize = 18f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText(title.uppercase(), margin, margin + 20f, headerPaint)
                
                // Draw a nice thin border line below header
                val linePaint = Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    strokeWidth = 1f
                }
                canvas.drawLine(margin, margin + 30f, pageWidth - margin, margin + 30f, linePaint)
            }

            // Draw first page header
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(0f, 0f, pageWidth, pageHeight, paint)
            drawHeader(canvas)

            val textPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 13f
                isAntiAlias = true
            }

            // A helper to wrap lines beautifully
            fun wrapText(text: String, width: Float, paint: Paint): List<String> {
                val words = text.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val testWidth = paint.measureText(testLine)
                    if (testWidth <= width) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine)
                        }
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                return lines
            }

            for (p in paragraphs) {
                // If we're too close to the bottom, start a new page
                if (currentY > pageHeight - margin - 30f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(600, 800, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    
                    // Draw background
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawRect(0f, 0f, pageWidth, pageHeight, paint)
                    drawHeader(canvas)
                    currentY = margin + 50f
                }

                val lines = wrapText(p, contentWidth, textPaint)
                for (line in lines) {
                    if (currentY > pageHeight - margin - 20f) {
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(600, 800, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        
                        // Draw background
                        paint.color = android.graphics.Color.WHITE
                        canvas.drawRect(0f, 0f, pageWidth, pageHeight, paint)
                        drawHeader(canvas)
                        currentY = margin + 50f
                    }
                    canvas.drawText(line, margin, currentY, textPaint)
                    currentY += 20f // line height
                }
                currentY += 12f // paragraph spacing
            }

            document.finishPage(page)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e("PdfHelper", "Failed to create pdf from text", e)
        } finally {
            document.close()
        }
    }

    /**
     * Exports any note (with all its stylus annotations/strokes, front cover page, and underlying PDF pages/templates) to a flattened PDF file
     */
    fun exportNoteToPdf(
        context: Context? = null,
        pdfFile: File?, // If it's a PDF note, we have a base PDF file.
        outputFile: File,
        templateType: String,
        strokes: List<com.example.data.Stroke>,
        images: List<com.example.data.ImageElement> = emptyList(),
        pageCount: Int,
        title: String,
        coverType: String = "none",
        coverTitle: String = "",
        coverSubtitle: String = "",
        coverAuthor: String = "",
        coverExtra: String = ""
    ) {
        val document = PdfDocument()
        try {
            val paint = Paint()
            val strokePaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            var pdfPageIndex = 1

            // 0. Render Front Cover Page if cover details or title are present
            val shouldIncludeCover = coverType != "none" || coverTitle.isNotBlank() || title.isNotBlank()
            if (shouldIncludeCover) {
                val coverPageInfo = PdfDocument.PageInfo.Builder(600, 800, pdfPageIndex).create()
                val coverPage = document.startPage(coverPageInfo)
                drawFrontCoverPage(
                    canvas = coverPage.canvas,
                    title = title,
                    coverTitle = coverTitle,
                    coverSubtitle = coverSubtitle,
                    coverAuthor = coverAuthor,
                    coverExtra = coverExtra,
                    coverType = coverType,
                    pageCount = pageCount
                )

                // Also draw any user annotations on page 0 or page 1 if strokes exist for cover
                val coverStrokes = strokes.filter { it.page == 0 }
                for (stroke in coverStrokes) {
                    if (stroke.points.size > 1 && stroke.toolType != "eraser") {
                        strokePaint.color = stroke.color
                        strokePaint.strokeWidth = stroke.width * 0.25f
                        val path = android.graphics.Path()
                        stroke.points.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                path.moveTo(pt.x, pt.y)
                            } else {
                                path.lineTo(pt.x, pt.y)
                            }
                        }
                        coverPage.canvas.drawPath(path, strokePaint)
                    }
                }

                document.finishPage(coverPage)
                pdfPageIndex++
            }

            for (pageIndex in 1..pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(600, 800, pdfPageIndex).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw base page layout or original PDF page
                if ((templateType == "pdf" || templateType == "scanned_doc") && pdfFile != null && pdfFile.exists()) {
                    val originalBitmap = renderPdfPageToBitmap(pdfFile, pageIndex - 1, 600, 800)
                    if (originalBitmap != null) {
                        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
                    } else {
                        paint.color = android.graphics.Color.WHITE
                        canvas.drawRect(0f, 0f, 600f, 800f, paint)
                    }
                } else {
                    // Draw backgrounds matching note templates beautifully!
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawRect(0f, 0f, 600f, 800f, paint)

                    if (templateType == "ruled") {
                        paint.color = android.graphics.Color.rgb(230, 240, 255)
                        paint.strokeWidth = 1f
                        var y = 100f
                        while (y < 800f) {
                            canvas.drawLine(0f, y, 600f, y, paint)
                            y += 30f
                        }
                    } else if (templateType == "grid") {
                        paint.color = android.graphics.Color.rgb(240, 240, 240)
                        paint.strokeWidth = 1f
                        var x = 0f
                        while (x < 600f) {
                            canvas.drawLine(x, 0f, x, 800f, paint)
                            x += 30f
                        }
                        var y = 0f
                        while (y < 800f) {
                            canvas.drawLine(0f, y, 600f, y, paint)
                            y += 30f
                        }
                    }
                }

                // 2. Draw user inserted pictures/images for this page
                if (context != null && images.isNotEmpty()) {
                    val pageImages = images.filter { it.page == pageIndex && !it.isHidden }
                    for (imageElem in pageImages) {
                        try {
                            val bitmap = loadSoftwareBitmap(context, imageElem.uri)
                            if (bitmap != null) {
                                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                                val dstRect = RectF(
                                    imageElem.x,
                                    imageElem.y,
                                    imageElem.x + imageElem.width,
                                    imageElem.y + imageElem.height
                                )
                                canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q && !bitmap.isRecycled) {
                                    try { bitmap.recycle() } catch (_: Exception) {}
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PdfHelper", "Error drawing image in PDF page $pageIndex", e)
                        }
                    }
                }

                // 3. Draw user strokes on top
                val pageStrokes = strokes.filter { it.page == pageIndex }
                for (stroke in pageStrokes) {
                    if (stroke.points.size > 1 && stroke.toolType != "eraser") {
                        strokePaint.color = stroke.color
                        strokePaint.strokeWidth = stroke.width * 0.25f
                        val path = android.graphics.Path()
                        stroke.points.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                path.moveTo(pt.x, pt.y)
                            } else {
                                path.lineTo(pt.x, pt.y)
                            }
                        }
                        canvas.drawPath(path, strokePaint)
                    }
                }

                document.finishPage(page)
                pdfPageIndex++
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e("PdfHelper", "Failed to export note to PDF", e)
        } finally {
            document.close()
        }
    }

    private fun drawFrontCoverPage(
        canvas: Canvas,
        title: String,
        coverTitle: String,
        coverSubtitle: String,
        coverAuthor: String,
        coverExtra: String,
        coverType: String,
        pageCount: Int
    ) {
        val displayTitle = if (coverTitle.isNotBlank()) coverTitle else if (title.isNotBlank()) title else "Notebook"
        val isDarkTheme = coverType.contains("dark") || coverType.contains("luxury") || coverType.contains("tech") || coverType.contains("3d")

        val bgPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(15, 23, 42) else android.graphics.Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 600f, 800f, bgPaint)

        // Outer & Inner Accent Border
        val borderPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(59, 130, 246) else android.graphics.Color.rgb(37, 99, 235)
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(24f, 24f, 576f, 776f), 16f, 16f, borderPaint)

        val innerBorderPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.argb(100, 148, 163, 184) else android.graphics.Color.argb(100, 203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(32f, 32f, 568f, 768f), 12f, 12f, innerBorderPaint)

        // Decorative Top Header Ribbon
        val ribbonPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(30, 41, 59) else android.graphics.Color.rgb(226, 232, 240)
            style = Paint.Style.FILL
        }
        canvas.drawRect(33f, 33f, 567f, 120f, ribbonPaint)

        val headerTextPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(148, 163, 184) else android.graphics.Color.rgb(71, 85, 105)
            textSize = 14f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LIPI DIGITAL NOTEBOOK  •  FRONT COVER", 300f, 75f, headerTextPaint)

        // Title Text
        val titlePaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.rgb(15, 23, 42)
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        if (displayTitle.length > 25) {
            val mid = displayTitle.length / 2
            var splitIdx = displayTitle.lastIndexOf(' ', mid)
            if (splitIdx == -1) splitIdx = mid
            val line1 = displayTitle.substring(0, splitIdx)
            val line2 = displayTitle.substring(splitIdx).trim()
            canvas.drawText(line1, 300f, 280f, titlePaint)
            canvas.drawText(line2, 300f, 325f, titlePaint)
        } else {
            canvas.drawText(displayTitle, 300f, 300f, titlePaint)
        }

        // Decorative Accent Line under Title
        val linePaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(245, 158, 11) else android.graphics.Color.rgb(37, 99, 235)
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawLine(200f, 360f, 400f, 360f, linePaint)

        // Subtitle Text
        if (coverSubtitle.isNotBlank()) {
            val subtitlePaint = Paint().apply {
                color = if (isDarkTheme) android.graphics.Color.rgb(203, 213, 225) else android.graphics.Color.rgb(71, 85, 105)
                textSize = 20f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(coverSubtitle, 300f, 410f, subtitlePaint)
        }

        // Bottom Box for Author, Extra Details, Date, Page Count
        val footerBoxPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(30, 41, 59) else android.graphics.Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(60f, 540f, 540f, 720f), 12f, 12f, footerBoxPaint)

        val labelPaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.rgb(148, 163, 184) else android.graphics.Color.rgb(100, 116, 139)
            textSize = 14f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        val valuePaint = Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.rgb(15, 23, 42)
            textSize = 15f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }

        var yPos = 575f
        val authorText = if (coverAuthor.isNotBlank()) coverAuthor else "Default User"
        canvas.drawText("AUTHOR:", 80f, yPos, labelPaint)
        canvas.drawText(authorText, 180f, yPos, valuePaint)

        if (coverExtra.isNotBlank()) {
            yPos += 30f
            canvas.drawText("DETAILS:", 80f, yPos, labelPaint)
            canvas.drawText(coverExtra, 180f, yPos, valuePaint)
        }

        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        yPos += 30f
        canvas.drawText("DATE:", 80f, yPos, labelPaint)
        canvas.drawText(dateStr, 180f, yPos, valuePaint)

        yPos += 30f
        canvas.drawText("PAGES:", 80f, yPos, labelPaint)
        canvas.drawText("$pageCount Pages", 180f, yPos, valuePaint)
    }

    /**
     * Creates a multi-page PDF from a list of scanned page Bitmaps.
     */
    fun createPdfFromBitmaps(outputFile: File, bitmaps: List<Bitmap>) {
        val document = PdfDocument()
        try {
            for ((index, bitmap) in bitmaps.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width.coerceAtLeast(300),
                    bitmap.height.coerceAtLeast(400),
                    index + 1
                ).create()

                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint().apply { isFilterBitmap = true }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                document.finishPage(page)
            }
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e("PdfHelper", "Error creating PDF from bitmaps", e)
        } finally {
            try { document.close() } catch (_: Exception) {}
        }
    }

    /**
     * Applies document scan enhancement filters: Auto, Grayscale, Black & White, Color, Original
     */
    fun applyScanFilter(bitmap: Bitmap, filterName: String): Bitmap {
        if (filterName.equals("Original", ignoreCase = true)) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

        when (filterName.lowercase()) {
            "grayscale" -> {
                val cm = android.graphics.ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            "black & white", "bw" -> {
                // High contrast b&w filter matrix
                val cm = android.graphics.ColorMatrix(floatArrayOf(
                    1.5f, 1.5f, 1.5f, 0f, -160f,
                    1.5f, 1.5f, 1.5f, 0f, -160f,
                    1.5f, 1.5f, 1.5f, 0f, -160f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            "color" -> {
                val cm = android.graphics.ColorMatrix()
                cm.setSaturation(1.4f)
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            "auto", "enhanced" -> {
                // Auto document enhancement matrix: improves contrast, lightens paper background
                val cm = android.graphics.ColorMatrix(floatArrayOf(
                    1.25f, 0.1f, 0.1f, 0f, 10f,
                    0.1f, 1.25f, 0.1f, 0f, 10f,
                    0.1f, 0.1f, 1.25f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            else -> {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
        }
        return result
    }

    /**
     * Crops bitmap according to normalized corner points [TL, TR, BR, BL] (0.0 .. 1.0)
     */
    fun cropBitmapPerspective(bitmap: Bitmap, corners: List<androidx.compose.ui.geometry.Offset>): Bitmap {
        if (corners.size < 4) return bitmap
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        val minX = (corners.minOf { it.x } * width).coerceIn(0f, width - 10f)
        val maxX = (corners.maxOf { it.x } * width).coerceIn(minX + 10f, width)
        val minY = (corners.minOf { it.y } * height).coerceIn(0f, height - 10f)
        val maxY = (corners.maxOf { it.y } * height).coerceIn(minY + 10f, height)

        val cropW = (maxX - minX).toInt().coerceAtLeast(100)
        val cropH = (maxY - minY).toInt().coerceAtLeast(100)

        return try {
            Bitmap.createBitmap(bitmap, minX.toInt(), minY.toInt(), cropW, cropH)
        } catch (e: Exception) {
            Log.e("PdfHelper", "Error cropping bitmap", e)
            bitmap
        }
    }
}
