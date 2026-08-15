package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.LipiAudioManager
import com.example.data.AudioBookmark
import com.example.data.AudioContentBlock
import kotlinx.coroutines.launch
import java.io.File

/**
 * Compact Non-Blocking Recording Dock Bar
 * Sits gracefully at the top/bottom of the Note Canvas during live recording.
 * Allows stylus drawing, writing, erasing, scrolling, and page turning simultaneously!
 */
@Composable
fun CompactRecordingDockBar(
    audioManager: LipiAudioManager,
    currentPage: Int,
    onAddBookmark: (title: String, pageId: Int) -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkTitleInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .padding(12.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .testTag("compact_recording_dock_bar"),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0F172A),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Pulsing Mic Indicator & Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            Color(0xFFEF4444).copy(alpha = if (audioManager.isRecordingPaused) 0.5f else alphaPulse),
                            CircleShape
                        )
                )

                Text(
                    text = audioManager.formatDuration(audioManager.recordingDurationMs),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Text(
                    text = if (audioManager.isRecordingPaused) "PAUSED" else "REC",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (audioManager.isRecordingPaused) Color(0xFFFBBF24) else Color(0xFFEF4444)
                )
            }

            // Live Waveform Visualizer Bar
            Row(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val amps = audioManager.liveAmplitudes.takeLast(16)
                val fillAmps = if (amps.size < 16) List(16 - amps.size) { 0.15f } + amps else amps
                fillAmps.forEach { amp ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(amp.coerceIn(0.15f, 1.0f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFF87171))
                                )
                            )
                    )
                }
            }

            // Quick Action: Add Bookmark ⭐
            IconButton(
                onClick = {
                    val bm = audioManager.addBookmarkDuringRecording("Page $currentPage Note", currentPage)
                    onAddBookmark("Page $currentPage Note", currentPage)
                    Toast.makeText(context, "⭐ Bookmark added at ${audioManager.formatDuration(bm.timestampMs)}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF334155), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Add Bookmark",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Quick Action: Pause / Resume
            IconButton(
                onClick = {
                    if (audioManager.isRecordingPaused) {
                        audioManager.resumeRecording()
                    } else {
                        audioManager.pauseRecording()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF334155), CircleShape)
            ) {
                Icon(
                    imageVector = if (audioManager.isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (audioManager.isRecordingPaused) "Resume" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Stop Button (Finalizes recording onto Note page)
            Button(
                onClick = onStopRecording,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Cancel Button
            IconButton(
                onClick = onCancelRecording,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Discard Recording",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Compact Persistent Mini Audio Player
 * Displays at the bottom/top of the screen when audio is playing across note navigation.
 */
@Composable
fun CompactAudioMiniPlayer(
    block: AudioContentBlock,
    audioManager: LipiAudioManager,
    onExpandFullPlayer: () -> Unit,
    onCloseMiniPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .testTag("compact_audio_mini_player"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column {
            // Mini Progress Bar Top Border
            val isPlayingThis = audioManager.currentPlayingBlockId == block.id && audioManager.isPlaying
            val currentPos = if (audioManager.currentPlayingBlockId == block.id) audioManager.playbackPositionMs else 0L
            val duration = if (block.durationMs > 0) block.durationMs else audioManager.playbackDurationMs
            val progress = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExpandFullPlayer() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = block.title.ifBlank { "Voice Note" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${audioManager.formatDuration(currentPos)} / ${audioManager.formatDuration(duration)}  •  Page ${block.page}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play / Pause
                    IconButton(
                        onClick = {
                            if (isPlayingThis) {
                                audioManager.pausePlayback()
                            } else {
                                audioManager.playAudio(block.id, block.audioFilePath)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingThis) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expand to Full Player
                    IconButton(
                        onClick = onExpandFullPlayer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand Full Player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close
                    IconButton(
                        onClick = {
                            audioManager.stopPlayback()
                            onCloseMiniPlayer()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Audio Player Modal Dialog / Sheet
 * Features:
 * - Interactive Waveform Seeking & Bookmark Markers
 * - Play / Pause, -10s, +10s, Previous Bookmark, Next Bookmark
 * - Speed Chips (0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x)
 * - Bookmarks list with "Play from here" jump buttons
 * - Audio Trimmer Sheet trigger
 */
@Composable
fun FullAudioPlayerDialog(
    block: AudioContentBlock,
    audioManager: LipiAudioManager,
    onNavigateToPage: (page: Int) -> Unit,
    onUpdateBlock: (AudioContentBlock) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isPlayingThis = audioManager.currentPlayingBlockId == block.id && audioManager.isPlaying
    val currentPos = if (audioManager.currentPlayingBlockId == block.id) audioManager.playbackPositionMs else 0L
    val duration = if (block.durationMs > 0) block.durationMs else audioManager.playbackDurationMs

    var bookmarksList by remember { mutableStateOf(block.bookmarks) }
    var waveformPoints by remember {
        mutableStateOf(
            if (block.waveformPoints.isNotEmpty()) block.waveformPoints
            else audioManager.generateWaveformPoints(block.audioFilePath, 60)
        )
    }

    var showTrimmerSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var newBookmarkTitleInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF3B82F6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = block.title.ifBlank { "Voice Note" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Page ${block.page}  •  ${block.fileFormat}  •  ${audioManager.formatDuration(duration)}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Waveform Box with Bookmark Pins
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        InteractiveWaveformCanvas(
                            waveformPoints = waveformPoints,
                            progress = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                            bookmarks = bookmarksList,
                            durationMs = duration,
                            onSeekRatio = { ratio ->
                                val targetMs = (ratio * duration).toLong()
                                audioManager.seekTo(targetMs)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrubber & Time Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = audioManager.formatDuration(currentPos),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA)
                    )

                    Slider(
                        value = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                        onValueChange = { ratio ->
                            val targetMs = (ratio * duration).toLong()
                            audioManager.seekTo(targetMs)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Text(
                        text = audioManager.formatDuration(duration),
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Bookmark
                    IconButton(
                        onClick = {
                            val prevBm = bookmarksList.filter { it.timestampMs < currentPos - 1000 }.maxByOrNull { it.timestampMs }
                            if (prevBm != null) {
                                audioManager.seekTo(prevBm.timestampMs)
                                onNavigateToPage(prevBm.pageId)
                            } else {
                                audioManager.seekTo(0L)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Bookmark", tint = Color.White)
                    }

                    // Seek -10s
                    IconButton(
                        onClick = {
                            val newPos = (currentPos - 10000L).coerceAtLeast(0L)
                            audioManager.seekTo(newPos)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "-10 seconds", tint = Color.White)
                    }

                    // Main Play / Pause Button
                    Surface(
                        onClick = {
                            if (isPlayingThis) {
                                audioManager.pausePlayback()
                            } else {
                                audioManager.playAudio(block.id, block.audioFilePath)
                            }
                        },
                        shape = CircleShape,
                        color = Color(0xFF3B82F6),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingThis) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Seek +10s
                    IconButton(
                        onClick = {
                            val newPos = (currentPos + 10000L).coerceAtMost(duration)
                            audioManager.seekTo(newPos)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "+10 seconds", tint = Color.White)
                    }

                    // Next Bookmark
                    IconButton(
                        onClick = {
                            val nextBm = bookmarksList.filter { it.timestampMs > currentPos + 1000 }.minByOrNull { it.timestampMs }
                            if (nextBm != null) {
                                audioManager.seekTo(nextBm.timestampMs)
                                onNavigateToPage(nextBm.pageId)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next Bookmark", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Speed Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        val isSelected = audioManager.playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = { audioManager.setSpeed(speed) },
                            label = { Text("${speed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Bar: Add Bookmark & Trim Audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bookmarks (${bookmarksList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddBookmarkDialog = true },
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Bookmark", fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { showTrimmerSheet = true },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trim", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bookmarks List
                if (bookmarksList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No bookmarks added yet. Tap 'Add Bookmark' or ⭐ during recording.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bookmarksList) { bm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                        Column {
                                            Text(
                                                text = bm.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${audioManager.formatDuration(bm.timestampMs)}  •  Page ${bm.pageId}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            audioManager.seekTo(bm.timestampMs)
                                            if (!isPlayingThis) {
                                                audioManager.playAudio(block.id, block.audioFilePath)
                                            }
                                            onNavigateToPage(bm.pageId)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Play From Here", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark Marker", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Timestamp: ${audioManager.formatDuration(currentPos)} (Page ${block.page})", fontSize = 12.sp)
                    OutlinedTextField(
                        value = newBookmarkTitleInput,
                        onValueChange = { newBookmarkTitleInput = it },
                        label = { Text("Bookmark Label") },
                        placeholder = { Text("e.g. Important Theorem") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newBookmarkTitleInput.ifBlank { "Bookmark @ ${audioManager.formatDuration(currentPos)}" }
                        val newBm = AudioBookmark(
                            audioId = block.id,
                            timestampMs = currentPos,
                            title = title,
                            pageId = block.page,
                            createdAt = System.currentTimeMillis()
                        )
                        val updatedList = bookmarksList + newBm
                        bookmarksList = updatedList
                        onUpdateBlock(block.copy(bookmarks = updatedList))
                        showAddBookmarkDialog = false
                        newBookmarkTitleInput = ""
                        Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Trimmer Sheet
    if (showTrimmerSheet) {
        AudioTrimmerDialog(
            sourceBlock = block,
            audioManager = audioManager,
            onDismiss = { showTrimmerSheet = false },
            onTrimComplete = { newPath, newDuration ->
                val updated = block.copy(
                    audioFilePath = newPath,
                    durationMs = newDuration,
                    title = "${block.title} (Trimmed)"
                )
                onUpdateBlock(updated)
                showTrimmerSheet = false
                Toast.makeText(context, "Audio trimmed successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Interactive Waveform Canvas Renderer
 */
@Composable
fun InteractiveWaveformCanvas(
    waveformPoints: List<Float>,
    progress: Float,
    bookmarks: List<AudioBookmark>,
    durationMs: Long,
    onSeekRatio: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeekRatio(ratio)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = waveformPoints.size.coerceAtLeast(10)
        val barWidth = width / barCount
        val middleY = height / 2f

        waveformPoints.forEachIndexed { index, amp ->
            val barX = index * barWidth + barWidth / 2f
            val barRatio = barX / width
            val isPlayed = barRatio <= progress
            val barHeight = (amp.coerceIn(0.1f, 1.0f) * (height * 0.8f)).coerceAtLeast(6f)

            drawLine(
                color = if (isPlayed) Color(0xFF3B82F6) else Color(0xFF475569),
                start = Offset(barX, middleY - barHeight / 2f),
                end = Offset(barX, middleY + barHeight / 2f),
                strokeWidth = (barWidth * 0.6f).coerceIn(2f, 10f),
                cap = StrokeCap.Round
            )
        }

        // Bookmark Pins
        bookmarks.forEach { bm ->
            if (durationMs > 0) {
                val pinRatio = (bm.timestampMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val pinX = pinRatio * width
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 8f,
                    center = Offset(pinX, 12f)
                )
            }
        }
    }
}

/**
 * Audio Trimmer Dialog (Non-destructive clip extraction)
 */
@Composable
fun AudioTrimmerDialog(
    sourceBlock: AudioContentBlock,
    audioManager: LipiAudioManager,
    onDismiss: () -> Unit,
    onTrimComplete: (newPath: String, newDuration: Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(sourceBlock.durationMs.coerceAtLeast(5000L)) }
    var isTrimming by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trim Audio Recording", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Source: ${sourceBlock.title} (${audioManager.formatDuration(sourceBlock.durationMs)})",
                    fontSize = 12.sp
                )

                Column {
                    Text("Start Time: ${audioManager.formatDuration(startMs)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = if (sourceBlock.durationMs > 0) (startMs.toFloat() / sourceBlock.durationMs).coerceIn(0f, 1f) else 0f,
                        onValueChange = { startMs = (it * sourceBlock.durationMs).toLong().coerceAtMost(endMs - 1000L) }
                    )
                }

                Column {
                    Text("End Time: ${audioManager.formatDuration(endMs)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = if (sourceBlock.durationMs > 0) (endMs.toFloat() / sourceBlock.durationMs).coerceIn(0f, 1f) else 1f,
                        onValueChange = { endMs = (it * sourceBlock.durationMs).toLong().coerceAtLeast(startMs + 1000L) }
                    )
                }

                Text(
                    text = "Trimmed Duration: ${audioManager.formatDuration(endMs - startMs)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isTrimming,
                onClick = {
                    isTrimming = true
                    coroutineScope.launch {
                        val trimmedPath = audioManager.trimAudioFile(sourceBlock.audioFilePath, startMs, endMs)
                        if (trimmedPath != null) {
                            onTrimComplete(trimmedPath, endMs - startMs)
                        }
                        isTrimming = false
                    }
                }
            ) {
                if (isTrimming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("Save Trimmed Clip")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Audio Storage Management & Analytics Dialog
 */
@Composable
fun AudioStorageManagementDialog(
    audioManager: LipiAudioManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(audioManager.getAudioStorageStats()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Audio Storage & Manage", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Audio Attachments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.fileCount} Audio Files", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = String.format("%.2f MB", stats.totalBytes / (1024f * 1024f)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text("Saved Audio Files", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                if (stats.audioFiles.isEmpty()) {
                    Text("No audio recordings stored.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(stats.audioFiles) { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                        Text("${file.length() / 1024} KB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = {
                                            try {
                                                file.delete()
                                                stats = audioManager.getAudioStorageStats()
                                                Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Page Audio Marker Badge
 * Rendered on Note Canvas page to show timestamp markers for instant "Play from here" jumps!
 */
@Composable
fun PageAudioMarkerBadge(
    bookmark: AudioBookmark,
    onPlayFromHere: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onPlayFromHere,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        contentColor = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFF3B82F6)),
        modifier = modifier.testTag("page_audio_marker_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
            Text(
                text = "${bookmark.title} • ${String.format("%02d:%02d", (bookmark.timestampMs / 1000) / 60, (bookmark.timestampMs / 1000) % 60)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
        }
    }
}
