package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Unified Lipi Content Block model representing embedded rich elements within notes:
 * - Audio Recordings & Audio Imports (MP3, M4A, WAV, etc.)
 * - Web Hyperlinks
 * - Internal Links to Lipi Notebooks & specific Pages
 * - PDF Document Attachments
 * - Embedded PDF Pages (annotatable with stylus)
 * - Rich Text / Sticky Notes
 * - Media Attachments
 */
sealed class LipiContentBlock(
    open val id: String,
    open val page: Int,
    open val x: Float,          // Normalized X (0..600)
    open val y: Float,          // Normalized Y (0..800+)
    open val width: Float,      // Normalized width
    open val height: Float,     // Normalized height
    open val zIndex: Int = 0,
    open val createdAt: Long = System.currentTimeMillis()
) {
    abstract val blockType: String
    abstract fun copyWith(
        x: Float = this.x,
        y: Float = this.y,
        width: Float = this.width,
        height: Float = this.height,
        page: Int = this.page
    ): LipiContentBlock
}

/**
 * Audio Bookmark / Timestamp Marker
 */
data class AudioBookmark(
    val bookmarkId: String = UUID.randomUUID().toString(),
    val audioId: String = "",
    val timestampMs: Long = 0L,
    val title: String = "Bookmark",
    val pageId: Int = 1,
    val colorHex: String = "#5B6DFF",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Recorded Audio Item stored in user's audio recordings history
 */
data class RecordedAudioItem(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val fileName: String,
    val title: String = "Recording",
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val transcription: String = ""
)

/**
 * Audio Recording or Imported Audio File (MP3, M4A, WAV, AAC, OGG, FLAC)
 */
data class AudioContentBlock(
    override val id: String = UUID.randomUUID().toString(),
    override val page: Int = 1,
    override val x: Float = 50f,
    override val y: Float = 50f,
    override val width: Float = 320f,
    override val height: Float = 110f,
    val audioFilePath: String = "",
    val originalFileName: String = "audio_recording.m4a",
    val title: String = "Voice Note",
    val durationMs: Long = 0L,
    val fileSize: Long = 0L,
    val fileFormat: String = "M4A",
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    val recordingSource: String = "Lipi Mic",
    val transcription: String = "",
    val isRecording: Boolean = false,
    val isLocked: Boolean = false,
    val bookmarks: List<AudioBookmark> = emptyList(),
    val waveformPoints: List<Float> = emptyList(),
    override val zIndex: Int = 1,
    override val createdAt: Long = System.currentTimeMillis()
) : LipiContentBlock(id, page, x, y, width, height, zIndex, createdAt) {
    override val blockType: String = "audio"
    override fun copyWith(x: Float, y: Float, width: Float, height: Float, page: Int): LipiContentBlock {
        return this.copy(x = x, y = y, width = width, height = height, page = page)
    }
}

/**
 * Web Hyperlink with smart preview and direct URL launch
 */
data class WebLinkContentBlock(
    override val id: String = UUID.randomUUID().toString(),
    override val page: Int = 1,
    override val x: Float = 50f,
    override val y: Float = 50f,
    override val width: Float = 260f,
    override val height: Float = 70f,
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val faviconUrl: String = "",
    override val zIndex: Int = 1,
    override val createdAt: Long = System.currentTimeMillis()
) : LipiContentBlock(id, page, x, y, width, height, zIndex, createdAt) {
    override val blockType: String = "web_link"
    override fun copyWith(x: Float, y: Float, width: Float, height: Float, page: Int): LipiContentBlock {
        return this.copy(x = x, y = y, width = width, height = height, page = page)
    }
}

/**
 * Internal Link to another Lipi Notebook and specific page
 */
data class InternalLinkContentBlock(
    override val id: String = UUID.randomUUID().toString(),
    override val page: Int = 1,
    override val x: Float = 50f,
    override val y: Float = 50f,
    override val width: Float = 240f,
    override val height: Float = 64f,
    val targetNoteId: Int = -1,
    val targetNoteTitle: String = "",
    val targetPage: Int = 1,
    val label: String = "",
    override val zIndex: Int = 1,
    override val createdAt: Long = System.currentTimeMillis()
) : LipiContentBlock(id, page, x, y, width, height, zIndex, createdAt) {
    override val blockType: String = "internal_link"
    override fun copyWith(x: Float, y: Float, width: Float, height: Float, page: Int): LipiContentBlock {
        return this.copy(x = x, y = y, width = width, height = height, page = page)
    }
}

/**
 * PDF Document Attachment (with thumbnail preview & multi-page reader launch)
 */
data class PdfAttachmentContentBlock(
    override val id: String = UUID.randomUUID().toString(),
    override val page: Int = 1,
    override val x: Float = 50f,
    override val y: Float = 50f,
    override val width: Float = 260f,
    override val height: Float = 90f,
    val pdfFilePath: String = "",
    val originalFileName: String = "document.pdf",
    val pageCount: Int = 1,
    val fileSizeFormatted: String = "0 KB",
    val previewThumbnailPath: String = "",
    override val zIndex: Int = 1,
    override val createdAt: Long = System.currentTimeMillis()
) : LipiContentBlock(id, page, x, y, width, height, zIndex, createdAt) {
    override val blockType: String = "pdf_attachment"
    override fun copyWith(x: Float, y: Float, width: Float, height: Float, page: Int): LipiContentBlock {
        return this.copy(x = x, y = y, width = width, height = height, page = page)
    }
}

/**
 * Embedded PDF Page rendered directly onto the note canvas (allows drawing over page)
 */
data class PdfPageContentBlock(
    override val id: String = UUID.randomUUID().toString(),
    override val page: Int = 1,
    override val x: Float = 30f,
    override val y: Float = 30f,
    override val width: Float = 540f,
    override val height: Float = 720f,
    val pdfFilePath: String = "",
    val pdfPageIndex: Int = 0,     // 0-indexed in the PDF file
    val sourcePdfTitle: String = "Document",
    val rotation: Float = 0f,
    override val zIndex: Int = 0,    // Renders below handwriting strokes
    override val createdAt: Long = System.currentTimeMillis()
) : LipiContentBlock(id, page, x, y, width, height, zIndex, createdAt) {
    override val blockType: String = "pdf_page"
    override fun copyWith(x: Float, y: Float, width: Float, height: Float, page: Int): LipiContentBlock {
        return this.copy(x = x, y = y, width = width, height = height, page = page)
    }
}

/**
 * Rich Sticky Note or Text Block
 */
data class TextContentBlock(
    override val id: String = UUID.randomUUID().toString(),
    override val page: Int = 1,
    override val x: Float = 50f,
    override val y: Float = 50f,
    override val width: Float = 220f,
    override val height: Float = 140f,
    val text: String = "",
    val fontSizeSp: Float = 14f,
    val textColor: Long = 0xFF1E293BL,
    val backgroundColor: Long = 0xFFFEF3C7L, // Default warm yellow sticky note
    val isStickyNote: Boolean = true,
    override val zIndex: Int = 1,
    override val createdAt: Long = System.currentTimeMillis()
) : LipiContentBlock(id, page, x, y, width, height, zIndex, createdAt) {
    override val blockType: String = "text"
    override fun copyWith(x: Float, y: Float, width: Float, height: Float, page: Int): LipiContentBlock {
        return this.copy(x = x, y = y, width = width, height = height, page = page)
    }
}

/**
 * Robust JSON Serialization & Deserialization for Content Blocks
 */
object LipiContentBlockSerializer {

    fun serializeBlocks(blocks: List<LipiContentBlock>): String {
        if (blocks.isEmpty()) return "[]"
        val array = JSONArray()
        for (block in blocks) {
            val obj = JSONObject().apply {
                put("type", block.blockType)
                put("id", block.id)
                put("page", block.page)
                put("x", block.x.toDouble())
                put("y", block.y.toDouble())
                put("width", block.width.toDouble())
                put("height", block.height.toDouble())
                put("zIndex", block.zIndex)
                put("createdAt", block.createdAt)

                when (block) {
                    is AudioContentBlock -> {
                        put("audioFilePath", block.audioFilePath)
                        put("originalFileName", block.originalFileName)
                        put("title", block.title)
                        put("durationMs", block.durationMs)
                        put("fileSize", block.fileSize)
                        put("fileFormat", block.fileFormat)
                        put("sampleRate", block.sampleRate)
                        put("channelCount", block.channelCount)
                        put("recordingSource", block.recordingSource)
                        put("transcription", block.transcription)
                        put("isLocked", block.isLocked)

                        val bmArr = JSONArray()
                        for (bm in block.bookmarks) {
                            val bmObj = JSONObject().apply {
                                put("bookmarkId", bm.bookmarkId)
                                put("audioId", bm.audioId)
                                put("timestampMs", bm.timestampMs)
                                put("title", bm.title)
                                put("pageId", bm.pageId)
                                put("colorHex", bm.colorHex)
                                put("createdAt", bm.createdAt)
                            }
                            bmArr.put(bmObj)
                        }
                        put("bookmarks", bmArr)

                        val wfArr = JSONArray()
                        for (wf in block.waveformPoints) {
                            wfArr.put(wf.toDouble())
                        }
                        put("waveformPoints", wfArr)
                    }
                    is WebLinkContentBlock -> {
                        put("url", block.url)
                        put("title", block.title)
                        put("description", block.description)
                        put("faviconUrl", block.faviconUrl)
                    }
                    is InternalLinkContentBlock -> {
                        put("targetNoteId", block.targetNoteId)
                        put("targetNoteTitle", block.targetNoteTitle)
                        put("targetPage", block.targetPage)
                        put("label", block.label)
                    }
                    is PdfAttachmentContentBlock -> {
                        put("pdfFilePath", block.pdfFilePath)
                        put("originalFileName", block.originalFileName)
                        put("pageCount", block.pageCount)
                        put("fileSizeFormatted", block.fileSizeFormatted)
                        put("previewThumbnailPath", block.previewThumbnailPath)
                    }
                    is PdfPageContentBlock -> {
                        put("pdfFilePath", block.pdfFilePath)
                        put("pdfPageIndex", block.pdfPageIndex)
                        put("sourcePdfTitle", block.sourcePdfTitle)
                        put("rotation", block.rotation.toDouble())
                    }
                    is TextContentBlock -> {
                        put("text", block.text)
                        put("fontSizeSp", block.fontSizeSp.toDouble())
                        put("textColor", block.textColor)
                        put("backgroundColor", block.backgroundColor)
                        put("isStickyNote", block.isStickyNote)
                    }
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeBlocks(jsonString: String?): List<LipiContentBlock> {
        if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
        val list = mutableListOf<LipiContentBlock>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = obj.optString("type", "")
                val id = obj.optString("id", UUID.randomUUID().toString())
                val page = obj.optInt("page", 1)
                val x = obj.optDouble("x", 50.0).toFloat()
                val y = obj.optDouble("y", 50.0).toFloat()
                val width = obj.optDouble("width", 200.0).toFloat()
                val height = obj.optDouble("height", 80.0).toFloat()
                val zIndex = obj.optInt("zIndex", 0)
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                when (type) {
                    "audio" -> {
                        val bmList = mutableListOf<AudioBookmark>()
                        val bmArr = obj.optJSONArray("bookmarks")
                        if (bmArr != null) {
                            for (bIdx in 0 until bmArr.length()) {
                                val bObj = bmArr.optJSONObject(bIdx) ?: continue
                                bmList.add(
                                    AudioBookmark(
                                        bookmarkId = bObj.optString("bookmarkId", UUID.randomUUID().toString()),
                                        audioId = bObj.optString("audioId", id),
                                        timestampMs = bObj.optLong("timestampMs", 0L),
                                        title = bObj.optString("title", "Bookmark"),
                                        pageId = bObj.optInt("pageId", page),
                                        colorHex = bObj.optString("colorHex", "#5B6DFF"),
                                        createdAt = bObj.optLong("createdAt", System.currentTimeMillis())
                                    )
                                )
                            }
                        }

                        val wfList = mutableListOf<Float>()
                        val wfArr = obj.optJSONArray("waveformPoints")
                        if (wfArr != null) {
                            for (wIdx in 0 until wfArr.length()) {
                                wfList.add(wfArr.optDouble(wIdx, 0.0).toFloat())
                            }
                        }

                        list.add(
                            AudioContentBlock(
                                id = id,
                                page = page,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                audioFilePath = obj.optString("audioFilePath", ""),
                                originalFileName = obj.optString("originalFileName", "audio.m4a"),
                                title = obj.optString("title", "Voice Note"),
                                durationMs = obj.optLong("durationMs", 0L),
                                fileSize = obj.optLong("fileSize", 0L),
                                fileFormat = obj.optString("fileFormat", "M4A"),
                                sampleRate = obj.optInt("sampleRate", 44100),
                                channelCount = obj.optInt("channelCount", 1),
                                recordingSource = obj.optString("recordingSource", "Lipi Mic"),
                                transcription = obj.optString("transcription", ""),
                                isLocked = obj.optBoolean("isLocked", false),
                                bookmarks = bmList,
                                waveformPoints = wfList,
                                zIndex = zIndex,
                                createdAt = createdAt
                            )
                        )
                    }
                    "web_link" -> {
                        list.add(
                            WebLinkContentBlock(
                                id = id,
                                page = page,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                url = obj.optString("url", ""),
                                title = obj.optString("title", ""),
                                description = obj.optString("description", ""),
                                faviconUrl = obj.optString("faviconUrl", ""),
                                zIndex = zIndex,
                                createdAt = createdAt
                            )
                        )
                    }
                    "internal_link" -> {
                        list.add(
                            InternalLinkContentBlock(
                                id = id,
                                page = page,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                targetNoteId = obj.optInt("targetNoteId", -1),
                                targetNoteTitle = obj.optString("targetNoteTitle", ""),
                                targetPage = obj.optInt("targetPage", 1),
                                label = obj.optString("label", ""),
                                zIndex = zIndex,
                                createdAt = createdAt
                            )
                        )
                    }
                    "pdf_attachment" -> {
                        list.add(
                            PdfAttachmentContentBlock(
                                id = id,
                                page = page,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                pdfFilePath = obj.optString("pdfFilePath", ""),
                                originalFileName = obj.optString("originalFileName", "document.pdf"),
                                pageCount = obj.optInt("pageCount", 1),
                                fileSizeFormatted = obj.optString("fileSizeFormatted", "0 KB"),
                                previewThumbnailPath = obj.optString("previewThumbnailPath", ""),
                                zIndex = zIndex,
                                createdAt = createdAt
                            )
                        )
                    }
                    "pdf_page" -> {
                        list.add(
                            PdfPageContentBlock(
                                id = id,
                                page = page,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                pdfFilePath = obj.optString("pdfFilePath", ""),
                                pdfPageIndex = obj.optInt("pdfPageIndex", 0),
                                sourcePdfTitle = obj.optString("sourcePdfTitle", "Document"),
                                rotation = obj.optDouble("rotation", 0.0).toFloat(),
                                zIndex = zIndex,
                                createdAt = createdAt
                            )
                        )
                    }
                    "text" -> {
                        list.add(
                            TextContentBlock(
                                id = id,
                                page = page,
                                x = x,
                                y = y,
                                width = width,
                                height = height,
                                text = obj.optString("text", ""),
                                fontSizeSp = obj.optDouble("fontSizeSp", 14.0).toFloat(),
                                textColor = obj.optLong("textColor", 0xFF1E293BL),
                                backgroundColor = obj.optLong("backgroundColor", 0xFFFEF3C7L),
                                isStickyNote = obj.optBoolean("isStickyNote", true),
                                zIndex = zIndex,
                                createdAt = createdAt
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LipiContentBlock", "Failed to deserialize blocks: ${e.message}", e)
        }
        return list
    }
}

/**
 * Dynamic sizing calculation for Sticky Notes, Text Boxes, and Hyperlinks based on content length.
 */
fun calculateAutoBlockDimensions(
    text: String = "",
    url: String = "",
    title: String = "",
    isStickyNote: Boolean = false,
    isTextBox: Boolean = false,
    isWebLink: Boolean = false,
    isInternalLink: Boolean = false
): Pair<Float, Float> {
    if (isWebLink) {
        val display = title.ifBlank { url }.trim()
        val maxLen = maxOf(display.length, url.trim().length, 14)
        val w = (maxLen * 9.5f + 95f).coerceIn(200f, 420f)
        return Pair(w, 72f)
    }
    if (isInternalLink) {
        val display = title.ifBlank { "Note Link" }.trim()
        val w = (display.length * 10f + 90f).coerceIn(180f, 380f)
        return Pair(w, 64f)
    }
    if (isStickyNote) {
        val cleanText = text.trim().ifBlank { "Sticky Note" }
        val linesList = cleanText.split("\n")
        var estimatedLines = 0
        var maxLineLen = 10
        linesList.forEach { line ->
            val len = line.length
            if (len > maxLineLen) maxLineLen = len
            estimatedLines += maxOf(1, Math.ceil(len / 22.0).toInt())
        }
        val w = (maxLineLen * 9.5f + 50f).coerceIn(160f, 360f)
        val h = (estimatedLines * 24f + 48f).coerceIn(100f, 380f)
        return Pair(w, h)
    }
    if (isTextBox) {
        val cleanText = text.trim().ifBlank { "Text Box" }
        val linesList = cleanText.split("\n")
        var estimatedLines = 0
        var maxLineLen = 8
        linesList.forEach { line ->
            val len = line.length
            if (len > maxLineLen) maxLineLen = len
            estimatedLines += maxOf(1, Math.ceil(len / 24.0).toInt())
        }
        val w = (maxLineLen * 9.5f + 40f).coerceIn(140f, 380f)
        val h = (estimatedLines * 22f + 32f).coerceIn(50f, 360f)
        return Pair(w, h)
    }
    return Pair(220f, 120f)
}
