import re

file_path = "app/src/main/java/com/example/ui/components/NoteinApp.kt"

with open(file_path, "r") as f:
    content = f.read()

# Helper function formatStorageSize
helper_func = """
private fun formatStorageSize(bytes: Long): String {
    return when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
        else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024f * 1024f))
    }
}
"""

if "private fun formatStorageSize" not in content:
    content = helper_func + "\n" + content

# 1. Update SyncDashboard calculations
dashboard_calc = """    // Realtime storage metrics calculation
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

# Inject dashboard_calc right after `val localBackupList = viewModel.listLocalBackupFiles()` inside SyncDashboard
content = content.replace(
    "val localBackupList = viewModel.listLocalBackupFiles()",
    "val localBackupList = viewModel.listLocalBackupFiles()\n" + dashboard_calc
)

# 2. Update calls inside SyncDashboard
old_card_calls = """                BackupStatusSectionCard(
                    lastSyncTime = viewModel.lastSyncTime,
                    isSyncing = viewModel.isSyncing,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    successColor = successColor
                )"""

new_card_calls = """                BackupStatusSectionCard(
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

content = content.replace(old_card_calls, new_card_calls)

old_analytics_call = """                StorageAnalyticsSectionCard(
                    notes = notes,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    successColor = successColor
                )"""

new_analytics_call = """                StorageAnalyticsSectionCard(
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

content = content.replace(old_analytics_call, new_analytics_call)

old_history_call = """                BackupHistorySectionCard(
                    lastSyncTime = viewModel.lastSyncTime,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceBorder = surfaceBorder,
                    successColor = successColor,
                    warningColor = warningColor,
                    primaryColor = primaryColor
                )"""

new_history_call = """                BackupHistorySectionCard(
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

content = content.replace(old_history_call, new_history_call)

# 3. Replace BackupStatusSectionCard implementation
old_status_impl_pattern = re.compile(
    r"@Composable\s+private\s+fun\s+BackupStatusSectionCard\(.*?\)\s*\{.*?\n\}",
    re.DOTALL
)

new_status_impl = """@Composable
private fun BackupStatusSectionCard(
    lastSyncTime: String,
    isSyncing: Boolean,
    notesCount: Int,
    localBackupCount: Int,
    totalStorageBytes: Long,
    autoBackupEnabled: Boolean,
    isSignedIn: Boolean,
    savedProvider: String,
    encryptBackup: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color,
    secondaryColor: Color,
    successColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Backup & Sync Health Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val formattedSize = formatStorageSize(totalStorageBytes)
            val metrics = listOf(
                Triple("Last Backup", if (lastSyncTime.isNotBlank()) lastSyncTime else "Local Saved (Realtime)", Icons.Default.History),
                Triple("Schedule Mode", if (autoBackupEnabled) "Auto-Sync Active" else "Manual Sync Only", Icons.Default.Event),
                Triple("Total Backups", "$notesCount Notes ($localBackupCount Files)", Icons.Default.FolderZip),
                Triple("Vault Storage Used", "$formattedSize Active", Icons.Default.CloudQueue),
                Triple("Sync Status", if (isSyncing) "Syncing active..." else if (isSignedIn) "Connected ($savedProvider)" else "Local Vault Active", Icons.Default.Sync),
                Triple("Encryption Status", if (encryptBackup) "AES-256 Encrypted 🔒" else "Standard JSON Format", Icons.Default.Lock)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0 until 2) {
                            val item = metrics[row * 2 + col]
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (col % 2 == 0) primaryColor.copy(alpha = 0.06f) else secondaryColor.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, surfaceBorder.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (col % 2 == 0) primaryColor.copy(alpha = 0.15f) else secondaryColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.third,
                                            contentDescription = null,
                                            tint = if (col % 2 == 0) primaryColor else secondaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                                        Text(item.second, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}"""

content = old_status_impl_pattern.sub(new_status_impl, content)

# 4. Replace StorageAnalyticsSectionCard implementation
old_analytics_impl_pattern = re.compile(
    r"@Composable\s+private\s+fun\s+StorageAnalyticsSectionCard\(.*?\)\s*\{.*?\n\}",
    re.DOTALL
)

new_analytics_impl = """@Composable
private fun StorageAnalyticsSectionCard(
    notes: List<NoteEntity>,
    textBytes: Long,
    drawingBytes: Long,
    voiceBytes: Long,
    pdfBytes: Long,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    successColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Storage Analytics & Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            val notebookCount = notes.count { it.templateType != "pdf" && it.templateType != "docx" }
            val pdfCount = notes.count { !it.pdfTitle.isNullOrBlank() || it.templateType == "pdf" || it.templateType == "docx" }
            val voiceCount = notes.count { !it.audioTranscription.isNullOrBlank() || !it.audioPath.isNullOrBlank() }
            val drawingCount = notes.count { !it.drawingData.isNullOrBlank() && it.drawingData != "[]" }

            val grandTotal = maxOf(1L, textBytes + drawingBytes + voiceBytes + pdfBytes)
            val wText = (textBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)
            val wPdf = (pdfBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)
            val wVoice = (voiceBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)
            val wDrawing = (drawingBytes.toFloat() / grandTotal.toFloat()).coerceAtLeast(0.05f)

            // Visual stacked distribution bar
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(textSecondary.copy(alpha = 0.15f))
                ) {
                    Box(modifier = Modifier.weight(wText).fillMaxHeight().background(primaryColor))
                    Box(modifier = Modifier.weight(wPdf).fillMaxHeight().background(secondaryColor))
                    Box(modifier = Modifier.weight(wVoice).fillMaxHeight().background(accentColor))
                    Box(modifier = Modifier.weight(wDrawing).fillMaxHeight().background(successColor))
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Legend Items Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnalyticsLegendItem("Notebooks ($notebookCount)", formatStorageSize(textBytes), primaryColor, textPrimary, textSecondary)
                    AnalyticsLegendItem("PDFs ($pdfCount)", formatStorageSize(pdfBytes), secondaryColor, textPrimary, textSecondary)
                    AnalyticsLegendItem("Voice ($voiceCount)", formatStorageSize(voiceBytes), accentColor, textPrimary, textSecondary)
                    AnalyticsLegendItem("Drawings ($drawingCount)", formatStorageSize(drawingBytes), successColor, textPrimary, textSecondary)
                }
            }
        }
    }
}"""

content = old_analytics_impl_pattern.sub(new_analytics_impl, content)

# 5. Replace BackupHistorySectionCard implementation
old_history_impl_pattern = re.compile(
    r"@Composable\s+private\s+fun\s+BackupHistorySectionCard\(.*?\)\s*\{.*?\n\}",
    re.DOTALL
)

new_history_impl = """@Composable
private fun BackupHistorySectionCard(
    logs: List<String>,
    lastSyncTime: String,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceBorder: Color,
    successColor: Color,
    warningColor: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup History Timeline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Text("Realtime Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val timelineItems = if (logs.isNotEmpty()) {
                logs.take(4).map { logStr ->
                    val parts = logStr.split("]", limit = 2)
                    val time = if (parts.size > 1) parts[0].removePrefix("[").trim() else "Just now"
                    val msg = if (parts.size > 1) parts[1].trim() else logStr
                    val isError = msg.contains("Error", ignoreCase = true) || msg.contains("Warning", ignoreCase = true) || msg.contains("Failed", ignoreCase = true)
                    val isSuccess = msg.contains("Restored", ignoreCase = true) || msg.contains("Exported", ignoreCase = true) || msg.contains("complete", ignoreCase = true) || msg.contains("Successfully", ignoreCase = true) || msg.contains("Saved", ignoreCase = true) || msg.contains("complete", ignoreCase = true)
                    
                    TimelineItemData(
                        title = msg,
                        timestamp = time,
                        badgeText = if (isError) "Warning" else if (isSuccess) "Success" else "Info",
                        iconColor = if (isError) warningColor else if (isSuccess) successColor else primaryColor,
                        icon = if (isError) Icons.Default.Warning else if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Sync
                    )
                }
            } else {
                listOf(
                    TimelineItemData("Realtime Database Engine Active", "Active", "Local Vault", successColor, Icons.Default.CheckCircle),
                    TimelineItemData("Cloud Sync Ready", if (lastSyncTime.isNotBlank()) lastSyncTime else "Standby", "Cloud", primaryColor, Icons.Default.CloudQueue)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                timelineItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(item.iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(item.timestamp, fontSize = 11.sp, color = textSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, surfaceBorder)
                        ) {
                            Text(item.badgeText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = item.iconColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}"""

content = old_history_impl_pattern.sub(new_history_impl, content)

with open(file_path, "w") as f:
    f.write(content)

print("Updated NoteinApp.kt successfully.")
