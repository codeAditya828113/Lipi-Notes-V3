file_path = "app/src/main/java/com/example/ui/components/NoteinApp.kt"

with open(file_path, "r") as f:
    text = f.read()

# 1. Ensure formatStorageSize helper function exists at top level above SyncDashboard
if "private fun formatStorageSize" not in text:
    helper = """
private fun formatStorageSize(bytes: Long): String {
    return when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
        else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024f * 1024f))
    }
}
"""
    sync_dash_idx = text.find("@Composable\nfun SyncDashboard")
    if sync_dash_idx == -1:
        sync_dash_idx = text.find("fun SyncDashboard")
    if sync_dash_idx != -1:
        text = text[:sync_dash_idx] + helper + "\n" + text[sync_dash_idx:]

# 2. Insert calc_block right inside SyncDashboard after localBackupList
calc_block = """
    val textBytes = androidx.compose.runtime.remember(notes) {
        notes.sumOf { (it.title.length + it.content.length + it.coverTitle.length + it.coverSubtitle.length + it.tags.length).toLong() * 2L }
    }
    val drawingBytes = androidx.compose.runtime.remember(notes) {
        notes.sumOf { it.drawingData.length.toLong() }
    }
    val voiceBytes = androidx.compose.runtime.remember(notes) {
        notes.sumOf { note ->
            var b = note.audioTranscription.orEmpty().length.toLong() * 2L
            if (!note.audioPath.isNullOrBlank()) {
                try {
                    val f = java.io.File(note.audioPath!!)
                    if (f.exists()) b += f.length()
                } catch (_: Exception) {}
            }
            b
        }
    }
    val pdfBytes = androidx.compose.runtime.remember(notes, context) {
        notes.sumOf { note ->
            var b = note.pdfTitle.orEmpty().length.toLong() * 100L
            try {
                val fPdf = java.io.File(context.filesDir, "note_${note.id}.pdf")
                if (fPdf.exists()) b += fPdf.length()
                val fDocx = java.io.File(context.filesDir, "note_${note.id}.docx")
                if (fDocx.exists()) b += fDocx.length()
            } catch (_: Exception) {}
            b
        }
    }
    val totalStorageBytes = textBytes + drawingBytes + voiceBytes + pdfBytes
"""

idx_local = text.find("var localBackupList by androidx.compose.runtime.remember")
if idx_local != -1 and "val totalStorageBytes =" not in text:
    closing_brace = text.find("}", idx_local)
    if closing_brace != -1:
        text = text[:closing_brace+1] + "\n" + calc_block + text[closing_brace+1:]

with open(file_path, "w") as f:
    f.write(text)

print("Inserted realtime calculation block successfully.")
