package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.LipiAudioManager
import com.example.data.AudioContentBlock
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for audio item listed in the player library hub
 */
data class AudioLibraryItem(
    val id: String,
    val file: File,
    val title: String,
    val fileName: String,
    val durationMs: Long,
    val fileSizeFormatted: String,
    val dateFormatted: String,
    val isRecording: Boolean,
    val associatedBlock: AudioContentBlock? = null
)

@Composable
fun AudioPlayerLibraryDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = viewModel.lipiAudioManager
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Recordings, 2: Imported, 3: In Note
    var refreshKey by remember { mutableIntStateOf(0) }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val imported = audioManager.importAudioFile(uri)
                if (imported != null) {
                    val file = File(imported.localFilePath)
                    val newBlock = AudioContentBlock(
                        page = viewModel.pdfPage,
                        x = 60f,
                        y = 120f,
                        width = 240f,
                        height = 48f,
                        audioFilePath = imported.localFilePath,
                        originalFileName = imported.originalFileName,
                        title = imported.title,
                        durationMs = imported.durationMs,
                        fileSize = if (file.exists()) file.length() else 0L
                    )
                    viewModel.addContentBlock(newBlock)
                    Toast.makeText(context, "Imported and inserted: ${imported.title}", Toast.LENGTH_SHORT).show()
                    refreshKey++
                } else {
                    Toast.makeText(context, "Could not import selected audio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Scan audio storage directory & merge active note blocks
    val audioItems = remember(refreshKey, viewModel.currentContentBlocks, viewModel.isRecording, viewModel.savedAudioRecordings.size) {
        val itemsList = mutableListOf<AudioLibraryItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. Scan storage directory
        val stats = audioManager.getAudioStorageStats()
        stats.audioFiles.forEach { file ->
            seenPaths.add(file.absolutePath)
            val isRec = file.name.startsWith("rec_") || file.name.startsWith("note_audio_") || file.name.endsWith(".3gp")
            val duration = audioManager.getAudioDuration(file.absolutePath)
            val rawTitle = audioManager.getAudioTitle(file.absolutePath)
            val displayTitle = if (!rawTitle.isNullOrBlank()) rawTitle
            else if (isRec) "Voice Recording (${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))})"
            else file.nameWithoutExtension

            val sizeMb = String.format("%.1f MB", file.length() / (1024f * 1024f))
            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(file.lastModified()))

            // Find matching block in active note if any
            val matchingBlock = viewModel.currentContentBlocks
                .filterIsInstance<AudioContentBlock>()
                .firstOrNull { it.audioFilePath == file.absolutePath }

            itemsList.add(
                AudioLibraryItem(
                    id = file.absolutePath,
                    file = file,
                    title = matchingBlock?.title?.ifBlank { displayTitle } ?: displayTitle,
                    fileName = file.name,
                    durationMs = if (duration > 0) duration else (matchingBlock?.durationMs ?: 0L),
                    fileSizeFormatted = sizeMb,
                    dateFormatted = dateStr,
                    isRecording = isRec,
                    associatedBlock = matchingBlock
                )
            )
        }

        // 2. Scan active note content blocks for external or unlisted paths
        viewModel.currentContentBlocks.filterIsInstance<AudioContentBlock>().forEach { block ->
            if (block.audioFilePath.isNotBlank() && !seenPaths.contains(block.audioFilePath)) {
                val file = File(block.audioFilePath)
                if (file.exists()) {
                    seenPaths.add(file.absolutePath)
                    val sizeMb = String.format("%.1f MB", file.length() / (1024f * 1024f))
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(file.lastModified()))
                    itemsList.add(
                        AudioLibraryItem(
                            id = file.absolutePath,
                            file = file,
                            title = block.title.ifBlank { block.originalFileName },
                            fileName = block.originalFileName,
                            durationMs = block.durationMs,
                            fileSizeFormatted = sizeMb,
                            dateFormatted = dateStr,
                            isRecording = file.name.startsWith("rec_") || file.name.startsWith("note_audio_"),
                            associatedBlock = block
                        )
                    )
                }
            }
        }

        // 3. Scan viewModel.savedAudioRecordings list
        viewModel.savedAudioRecordings.forEach { savedRec ->
            if (savedRec.filePath.isNotBlank() && !seenPaths.contains(savedRec.filePath)) {
                val file = File(savedRec.filePath)
                if (file.exists()) {
                    seenPaths.add(file.absolutePath)
                    val sizeMb = String.format("%.1f MB", file.length() / (1024f * 1024f))
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(file.lastModified()))
                    val duration = audioManager.getAudioDuration(file.absolutePath)
                    itemsList.add(
                        AudioLibraryItem(
                            id = file.absolutePath,
                            file = file,
                            title = savedRec.title.ifBlank { savedRec.fileName },
                            fileName = savedRec.fileName,
                            durationMs = duration,
                            fileSizeFormatted = sizeMb,
                            dateFormatted = dateStr,
                            isRecording = true,
                            associatedBlock = null
                        )
                    )
                }
            }
        }

        itemsList.sortedByDescending { it.file.lastModified() }
    }

    // Filter audio items based on filter tabs & search query
    val filteredItems = remember(audioItems, searchQuery, selectedFilterIndex) {
        audioItems.filter { item ->
            val matchesFilter = when (selectedFilterIndex) {
                1 -> item.isRecording
                2 -> !item.isRecording
                3 -> item.associatedBlock != null
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.fileName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Audio Library & Player",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${audioItems.size} Audio ${if (audioItems.size == 1) "File" else "Files"} • Recorded & Imported",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Top Quick Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Record New Audio Button
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.openAudioOverlay()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record Voice Note", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Import Audio File Button
                    OutlinedButton(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Audio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recording or file title...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips Row
                val filters = listOf("All Audio (${audioItems.size})", "Recordings", "Imported", "In Current Note")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Audio Items List or Empty State
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "No audio files match '$searchQuery'" else "No audio recordings or imported files found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap 'Record Voice Note' or 'Import Audio' above to add audio to your library.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            AudioLibraryItemCard(
                                item = item,
                                viewModel = viewModel,
                                audioManager = audioManager,
                                onInsertToNote = {
                                    val newBlock = AudioContentBlock(
                                        page = viewModel.pdfPage,
                                        x = 60f,
                                        y = 120f,
                                        width = 240f,
                                        height = 48f,
                                        audioFilePath = item.file.absolutePath,
                                        originalFileName = item.fileName,
                                        title = item.title,
                                        durationMs = item.durationMs,
                                        fileSize = item.file.length()
                                    )
                                    viewModel.addContentBlock(newBlock)
                                    Toast.makeText(context, "Inserted '${item.title}' into Page ${viewModel.pdfPage}", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteFile = {
                                    try {
                                        if (item.file.exists()) {
                                            item.file.delete()
                                        }
                                        if (item.associatedBlock != null) {
                                            viewModel.deleteContentBlock(item.associatedBlock.id)
                                        }
                                        Toast.makeText(context, "Deleted audio file", Toast.LENGTH_SHORT).show()
                                        refreshKey++
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not delete file: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                // Sticky Playing Audio Bar at bottom if playing
                if (audioManager.isPlaying && audioManager.currentPlayingBlockId != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        contentColor = Color.White,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { audioManager.pausePlayback() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF3B82F6), CircleShape)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Now Playing Audio",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD)
                                )
                                Text(
                                    text = "${audioManager.formatDuration(audioManager.playbackPositionMs)} / ${audioManager.formatDuration(audioManager.playbackDurationMs)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            IconButton(
                                onClick = { audioManager.stopPlayback() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioLibraryItemCard(
    item: AudioLibraryItem,
    viewModel: NoteViewModel,
    audioManager: LipiAudioManager,
    onInsertToNote: () -> Unit,
    onDeleteFile: () -> Unit
) {
    val context = LocalContext.current
    val isCurrentPlaying = audioManager.currentPlayingBlockId == item.id && audioManager.isPlaying
    val position = if (audioManager.currentPlayingBlockId == item.id) audioManager.playbackPositionMs else 0L
    val totalDuration = if (item.durationMs > 0) item.durationMs else audioManager.playbackDurationMs

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.dp,
            if (isCurrentPlaying) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Audio Type Badge Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (item.isRecording) Color(0xFFFEF2F2) else Color(0xFFEFF6FF),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (item.isRecording) Color(0xFFFCA5A5) else Color(0xFFBFDBFE),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isRecording) Icons.Default.Mic else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (item.isRecording) Color(0xFFEF4444) else Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // File Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (item.isRecording) "Voice Recording" else "Imported File",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isRecording) Color(0xFFDC2626) else Color(0xFF2563EB)
                        )
                        Text(
                            text = "• ${audioManager.formatDuration(item.durationMs)} • ${item.fileSizeFormatted}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Inline Play / Pause Button
                IconButton(
                    onClick = {
                        if (isCurrentPlaying) {
                            audioManager.pausePlayback()
                        } else {
                            audioManager.playAudio(item.id, item.file.absolutePath)
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isCurrentPlaying) Color(0xFFEF4444) else Color(0xFF3B82F6),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Expand Options Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Live Playback Slider Bar when playing
            if (isCurrentPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    val progress = if (totalDuration > 0) (position.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                    Slider(
                        value = progress,
                        onValueChange = { frac ->
                            audioManager.seekTo((frac * totalDuration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = audioManager.formatDuration(position),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = audioManager.formatDuration(totalDuration),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Options Panel
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Insert into Note Button
                        FilledTonalButton(
                            onClick = onInsertToNote,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Insert into Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share Audio File
                        IconButton(
                            onClick = {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        item.file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Audio Note"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not share audio: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }

                        // Delete Audio File
                        IconButton(
                            onClick = onDeleteFile,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEF2F2), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
