package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LipiPdfManager {

    private const val TAG = "LipiPdfManager"

    // Memory cache for rendered PDF page bitmaps: "filePath_pageIdx_width_height"
    private val pageBitmapCache = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()).coerceAtLeast(1024 * 4)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted && oldValue != null && oldValue != newValue && !oldValue.isRecycled) {
                try {
                    oldValue.recycle()
                } catch (_: Exception) {}
            }
        }
    }

    fun getPdfStorageDir(context: Context): File {
        val dir = File(context.filesDir, "attachments/pdf")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getPdfThumbnailDir(context: Context): File {
        val dir = File(context.filesDir, "attachments/pdf_thumbs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    data class PdfImportResult(
        val localFilePath: String,
        val originalFileName: String,
        val pageCount: Int,
        val fileSizeFormatted: String,
        val thumbnailPath: String?
    )

    suspend fun importPdfFile(context: Context, uri: Uri): PdfImportResult? = withContext(Dispatchers.IO) {
        try {
            var fileName = "document_${System.currentTimeMillis()}.pdf"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val targetFile = File(getPdfStorageDir(context), "pdf_${UUID.randomUUID().toString().take(8)}_$fileName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (fileSize == 0L) {
                fileSize = targetFile.length()
            }

            val pageCount = getPdfPageCount(targetFile)
            val thumbPath = generatePageThumbnail(context, targetFile, 0)

            PdfImportResult(
                localFilePath = targetFile.absolutePath,
                originalFileName = fileName,
                pageCount = pageCount,
                fileSizeFormatted = formatFileSize(fileSize),
                thumbnailPath = thumbPath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import PDF file: ${e.message}", e)
            null
        }
    }

    fun getPdfPageCount(file: File): Int {
        if (!file.exists()) return 1
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count", e)
            1
        }
    }

    fun renderPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        targetWidth: Int = 1200,
        targetHeight: Int = 1600
    ): Bitmap? {
        if (!pdfFile.exists()) return null

        val cacheKey = "${pdfFile.absolutePath}_${pageIndex}_${targetWidth}_${targetHeight}"
        pageBitmapCache.get(cacheKey)?.let {
            if (!it.isRecycled) return it
        }

        return try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return null
            }

            val page = renderer.openPage(pageIndex)
            val pw = page.width
            val ph = page.height

            val scaleX = targetWidth.toFloat() / pw
            val scaleY = targetHeight.toFloat() / ph
            val scale = kotlin.math.min(scaleX, scaleY).coerceAtLeast(1.0f)

            val renderW = (pw * scale).toInt().coerceAtLeast(1)
            val renderH = (ph * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()

            pageBitmapCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render PDF page $pageIndex: ${e.message}", e)
            null
        }
    }

    fun generatePageThumbnail(context: Context, pdfFile: File, pageIndex: Int): String? {
        val bitmap = renderPageToBitmap(pdfFile, pageIndex, 400, 550) ?: return null
        return try {
            val thumbFile = File(getPdfThumbnailDir(context), "thumb_${pdfFile.nameWithoutExtension}_p$pageIndex.jpg")
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write thumbnail", e)
            null
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }

    fun clearCache() {
        pageBitmapCache.evictAll()
    }
}
