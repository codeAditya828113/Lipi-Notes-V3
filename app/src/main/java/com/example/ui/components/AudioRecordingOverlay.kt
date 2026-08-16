package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

/**
 * Audio Recording Overlay with dynamic Waveform Visualizer, Start/Stop toggle button,
 * and background Gemini AI voice transcription.
 */
@Composable
fun AudioRecordingOverlay(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Check Microphone Permission state
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startAudioRecording()
        } else {
            Toast.makeText(context, "Microphone permission required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    // Recording duration timer (00:00)
    var recordingSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.isRecording) {
        if (viewModel.isRecording) {
            recordingSeconds = 0
            while (viewModel.isRecording) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    val formatTimer = remember(recordingSeconds) {
        val mins = recordingSeconds / 60
        val secs = recordingSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Dialog(
        onDismissRequest = {
            if (!viewModel.isRecording && !viewModel.isTranscribing) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !viewModel.isRecording,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("audio_recording_overlay_card"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (viewModel.isRecording) Color(0xFFEF4444).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (viewModel.isRecording) Icons.Default.Mic else Icons.Default.GraphicEq,
                                        contentDescription = "Audio Icon",
                                        tint = if (viewModel.isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Voice Memo & AI Transcription",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        viewModel.isRecording -> "Recording in progress..."
                                        viewModel.isTranscribing -> "Gemini transcribing..."
                                        viewModel.transcriptionResult?.isNotBlank() == true -> "Transcription completed"
                                        else -> "Tap microphone to record"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (viewModel.isRecording) {
                                    viewModel.stopAudioRecording()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close overlay",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!hasMicPermission) {
                        // Permission Request Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.MicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Microphone Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Allow microphone access to record voice memos and transcribe with Gemini.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    } else {
                        // 2. Waveform Visualizer Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (viewModel.isRecording) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WaveformVisualizer(
                                isRecording = viewModel.isRecording,
                                isTranscribing = viewModel.isTranscribing,
                                amplitude = viewModel.currentAudioAmplitude,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Status Badge Overlay
                            if (viewModel.isRecording) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PulsingRedDot()
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatTimer,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else if (viewModel.isTranscribing) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "TRANSCRIBING...",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Start/Stop Recording Toggle Button
                        Box(contentAlignment = Alignment.Center) {
                            if (viewModel.isRecording) {
                                PulsingRingBackground()
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (viewModel.isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (viewModel.isRecording) {
                                            viewModel.stopAudioRecording()
                                        } else {
                                            viewModel.startAudioRecording()
                                        }
                                    }
                                    .testTag("start_stop_audio_recording_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (viewModel.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (viewModel.isRecording) "Stop Recording" else "Start Recording",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (viewModel.isRecording) "Tap to Stop & Transcribe" else "Tap to Start Recording",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (viewModel.isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3.5. Recorded Audio File Preview & Direct Insertion Card
                        val recordedPath = viewModel.lastRecordedFilePath
                        if (!viewModel.isRecording && !recordedPath.isNullOrBlank()) {
                            val audioManager = viewModel.lipiAudioManager
                            val isOverlayPlaying = audioManager.currentPlayingBlockId == "overlay_preview" && audioManager.isPlaying
                            val overlayPos = if (audioManager.currentPlayingBlockId == "overlay_preview") audioManager.playbackPositionMs else 0L
                            val overlayDur = if (audioManager.currentPlayingBlockId == "overlay_preview") audioManager.playbackDurationMs else 0L

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Recorded Audio File Saved",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isOverlayPlaying) {
                                                        "${audioManager.formatDuration(overlayPos)} / ${audioManager.formatDuration(overlayDur)}"
                                                    } else {
                                                        "Play to listen or insert directly into note"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Play / Pause Preview Button
                                        IconButton(
                                            onClick = {
                                                if (isOverlayPlaying) {
                                                    audioManager.pausePlayback()
                                                } else {
                                                    audioManager.playAudio("overlay_preview", recordedPath)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isOverlayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isOverlayPlaying) "Pause" else "Play",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val inserted = viewModel.insertAudioBlockFromLastRecording()
                                                if (inserted) {
                                                    Toast.makeText(context, "Inserted audio player card into Notes page", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                    viewModel.openAudioPlayerLibrary()
                                                } else {
                                                    Toast.makeText(context, "No audio recording available", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Insert into Note", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                onDismiss()
                                                viewModel.openAudioPlayerLibrary()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Player Section", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 4. Realtime Speech Preview or Final Transcription Box
                        val currentText = when {
                            viewModel.isRecording && viewModel.liveSpeechText.isNotBlank() -> viewModel.liveSpeechText
                            viewModel.transcriptionResult?.isNotBlank() == true -> viewModel.transcriptionResult
                            else -> null
                        }

                        if (currentText != null || viewModel.isTranscribing) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (viewModel.isRecording) "LIVE SPEECH PREVIEW" else "GEMINI AUDIO TRANSCRIPTION",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        if (currentText != null) {
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(currentText))
                                                        Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy text",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (viewModel.isTranscribing) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Analyzing speech audio with Gemini 3.5 Flash...",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (currentText != null) {
                                        Text(
                                            text = currentText,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    val currentNote = viewModel.selectedNote
                                                    if (currentNote != null) {
                                                        viewModel.appendTextToSelectedNote("[Voice Transcription]: $currentText")
                                                        Toast.makeText(context, "Transcript appended to active note", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    } else {
                                                        viewModel.createNewNote("Voice Note: ${currentText.take(20)}...", "blank")
                                                        Toast.makeText(context, "Created new voice note with transcript", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Insert into Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dynamic Multi-Bar Waveform Visualizer Canvas Component
 */
@Composable
fun WaveformVisualizer(
    isRecording: Boolean,
    isTranscribing: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 30
) {
    val transition = rememberInfiniteTransition()
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val recordingColor = Color(0xFFEF4444)
    val transcribingColor = Color(0xFF06B6D4)
    val idleColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.25f
        val barWidth = (width - totalSpacing) / barCount
        val barGap = totalSpacing / (barCount - 1)

        val normalizedAmp = (amplitude / 3000f).coerceIn(0.1f, 1.0f)

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val sinWave = abs(sin(progress * Math.PI * 3 + animatedPhase)).toFloat()

            val targetHeightFactor = when {
                isRecording -> {
                    val centerFactor = 1f - abs(progress - 0.5f) * 1.5f
                    (sinWave * 0.6f + normalizedAmp * 0.4f) * centerFactor.coerceAtLeast(0.2f)
                }
                isTranscribing -> {
                    (sinWave * 0.5f + 0.2f)
                }
                else -> 0.08f
            }

            val currentBarHeight = (height * targetHeightFactor).coerceIn(4.dp.toPx(), height * 0.85f)
            val x = i * (barWidth + barGap)
            val y = (height - currentBarHeight) / 2f

            val barColor = when {
                isRecording -> {
                    if (i % 2 == 0) recordingColor else primaryColor
                }
                isTranscribing -> {
                    if (i % 2 == 0) transcribingColor else primaryColor
                }
                else -> idleColor
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Pulsing Red Dot indicator
 */
@Composable
private fun PulsingRedDot() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(Color.White.copy(alpha = alpha), CircleShape)
    )
}

/**
 * Pulsing Ring animation behind Mic Recording Button
 */
@Composable
private fun PulsingRingBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(scale)
            .background(Color(0xFFEF4444).copy(alpha = alpha), CircleShape)
    )
}
