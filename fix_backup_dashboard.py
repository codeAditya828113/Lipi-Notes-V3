import re

file_path = "app/src/main/java/com/example/ui/components/NoteinApp.kt"

with open(file_path, "r") as f:
    text = f.read()

# 1. Ensure formatStorageSize is placed after imports
if "private fun formatStorageSize" in text:
    text = text.replace("private fun formatStorageSize(bytes: Long): String {\n    return when {\n        bytes <= 0 -> \"0 B\"\n        bytes < 1024 -> \"$bytes B\"\n        bytes < 1024 * 1024 -> String.format(java.util.Locale.US, \"%.1f KB\", bytes / 1024f)\n        else -> String.format(java.util.Locale.US, \"%.2f MB\", bytes / (1024f * 1024f))\n    }\n}\n", "")
    text = text.replace("private fun formatStorageSize(bytes: Long): String {\n    return when {\n        bytes <= 0 -> \"0 B\"\n        bytes < 1024 -> \"$bytes B\"\n        bytes < 1024 * 1024 -> String.format(java.util.Locale.US, \"%.1f KB\", bytes / 1024f)\n        else -> String.format(java.util.Locale.US, \"%.2f MB\", bytes / (1024f * 1024f))\n    }\n}", "")

# Add formatStorageSize after package and main imports block
package_idx = text.find("package com.example.ui.components")
if package_idx != -1:
    first_fun_idx = text.find("fun ", package_idx)
    if first_fun_idx != -1:
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
        text = text[:first_fun_idx] + helper + text[first_fun_idx:]

# 2. Add realtime metrics calculation after localBackupList
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

if "val totalStorageBytes =" not in text:
    target_local_backup = "val localBackupList by androidx.compose.runtime.remember {\n        androidx.compose.runtime.mutableStateOf(viewModel.listLocalBackupFiles())\n    }"
    text = text.replace(target_local_backup, target_local_backup + "\n" + calc_block)

# 3. Replace all BackupStatusSectionCard calls
status_call_pattern = re.compile(
    r"BackupStatusSectionCard\(\s*lastSyncTime\s*=\s*viewModel\.lastSyncTime,\s*isSyncing\s*=\s*viewModel\.isSyncing,.*?\)",
    re.DOTALL
)

new_status_call = """BackupStatusSectionCard(
                            lastSyncTime = viewModel.lastSyncTime,
                            isSyncing = viewModel.isSyncing,
                            notesCount = notes.size,
                            localBackupCount = localBackupList.size,
                            totalStorageBytes = totalStorageBytes,
                            autoBackupEnabled = viewModel.autoBackupEnabled,
                            isSignedIn = isSignedIn,
                            savedProvider = savedProvider,
                            encryptBackup = encryptBackup,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            successColor = successColor
                        )"""

text = status_call_pattern.sub(new_status_call, text)

# 4. Replace all StorageAnalyticsSectionCard calls
analytics_call_pattern = re.compile(
    r"StorageAnalyticsSectionCard\(\s*notes\s*=\s*notes,\s*cardBg\s*=\s*cardBg,.*?\)",
    re.DOTALL
)

new_analytics_call = """StorageAnalyticsSectionCard(
                            notes = notes,
                            textBytes = textBytes,
                            drawingBytes = drawingBytes,
                            voiceBytes = voiceBytes,
                            pdfBytes = pdfBytes,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            accentColor = accentColor,
                            successColor = successColor
                        )"""

text = analytics_call_pattern.sub(new_analytics_call, text)

# 5. Replace all BackupHistorySectionCard calls
history_call_pattern = re.compile(
    r"BackupHistorySectionCard\(\s*lastSyncTime\s*=\s*viewModel\.lastSyncTime,\s*cardBg\s*=\s*cardBg,.*?\)",
    re.DOTALL
)

new_history_call = """BackupHistorySectionCard(
                            logs = logs,
                            lastSyncTime = viewModel.lastSyncTime,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            surfaceBorder = surfaceBorder,
                            successColor = successColor,
                            warningColor = warningColor,
                            primaryColor = primaryColor
                        )"""

text = history_call_pattern.sub(new_history_call, text)

with open(file_path, "w") as f:
    f.write(text)

print("Updated NoteinApp.kt fix script completed.")
