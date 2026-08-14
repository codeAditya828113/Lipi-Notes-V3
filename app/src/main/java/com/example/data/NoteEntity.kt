package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String = "",
    val createdTime: Long = System.currentTimeMillis(),
    val lastModifiedTime: Long = System.currentTimeMillis(),
    val templateType: String = "blank", // "blank", "grid", "ruled", "cornell", "meeting", "pdf"
    val coverType: String = "none",
    val pageColor: Long = 0xFFFFFFFF, // Default white
    val coverTitle: String = "",
    val coverSubtitle: String = "",
    val coverAuthor: String = "",
    val coverExtra: String = "",
    val pdfTitle: String? = null, // Store mock annotated PDF file name
    val audioPath: String? = null, // Local storage audio path
    val audioTranscription: String? = null, // Transcribed text
    val summary: String? = null, // AI summary / categorization tag
    val drawingData: String = "[]", // Serialized drawing strokes
    val imagesData: String = "[]", // Serialized images
    val contentBlocksData: String = "[]", // Unified Lipi Content Blocks (Audio, Links, PDF pages, etc.)
    val isSynced: Boolean = false, // Sync status
    val tags: String = "", // Comma-separated tags e.g. "work,personal"
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val pinCode: String = "",
    val cardColor: Long = 0x00000000L // 0 means default theme card color
)
