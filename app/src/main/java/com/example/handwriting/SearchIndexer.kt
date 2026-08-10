package com.example.handwriting

import com.example.data.NoteEntity
import com.example.data.Stroke
import com.example.ui.components.NoteViewModel

object SearchIndexer {

    /**
     * Indexes recognized handwritten text into the note's metadata tags/summary so that
     * global search matches the handwritten words while leaving original visual strokes intact.
     */
    fun indexHandwritingText(
        viewModel: NoteViewModel,
        recognizedText: String,
        targetNote: NoteEntity? = viewModel.selectedNote
    ) {
        if (recognizedText.isBlank()) return

        val note = targetNote ?: viewModel.selectedNote ?: return

        // Extract key search terms from recognized handwriting
        val newTerms = recognizedText
            .split("\\s+".toRegex())
            .map { it.lowercase().trim() }
            .filter { it.length >= 2 }
            .distinct()

        val existingTags = note.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()

        newTerms.forEach { term ->
            if (!existingTags.contains(term)) {
                existingTags.add(term)
            }
        }

        val updatedTags = existingTags.take(25).joinToString(",")
        val updatedSummary = if (note.summary.isNullOrBlank()) {
            "Handwritten content: $recognizedText"
        } else if (!note.summary.contains(recognizedText)) {
            "${note.summary}\n[Handwriting Index]: $recognizedText"
        } else {
            note.summary
        }

        val updatedNote = note.copy(
            tags = updatedTags,
            summary = updatedSummary,
            lastModifiedTime = System.currentTimeMillis()
        )

        viewModel.updateNote(updatedNote)
    }
}
