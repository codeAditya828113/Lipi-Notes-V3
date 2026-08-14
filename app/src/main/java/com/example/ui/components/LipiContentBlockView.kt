package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LipiAudioManager
import com.example.data.*
import com.example.pdf.LipiPdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Interactive renderer for Lipi Content Blocks placed on the note canvas.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LipiContentBlockItem(
    block: LipiContentBlock,
    isSelected: Boolean,
    audioManager: LipiAudioManager,
    scale: Float,
    renderX: Float,
    renderY: Float,
    renderWidth: Float,
    renderHeight: Float,
    onSelect: () -> Unit,
    onNavigateToNotePage: (noteId: Int, page: Int) -> Unit,
    onOpenPdf: (filePath: String, page: Int) -> Unit,
    onEditBlock: (LipiContentBlock) -> Unit,
    onDeleteBlock: (LipiContentBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthDp = with(density) { renderWidth.toDp() }
    val heightDp = with(density) { renderHeight.toDp() }

    Box(
        modifier = modifier
            .offset { IntOffset(renderX.toInt(), renderY.toInt()) }
            .size(widthDp, heightDp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = { onSelect() },
                onLongClick = { onEditBlock(block) }
            )
    ) {
        when (block) {
            is AudioContentBlock -> {
                AudioBlockView(
                    block = block,
                    audioManager = audioManager,
                    onEdit = { onEditBlock(block) },
                    onDelete = { onDeleteBlock(block) }
                )
            }
            is WebLinkContentBlock -> {
                WebLinkBlockView(
                    block = block,
                    onClick = {
                        try {
                            var targetUrl = block.url.trim()
                            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                                targetUrl = "https://$targetUrl"
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(block.url))
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onEdit = { onEditBlock(block) },
                    onDelete = { onDeleteBlock(block) }
                )
            }
            is InternalLinkContentBlock -> {
                InternalLinkBlockView(
                    block = block,
                    onClick = {
                        if (block.targetNoteId != -1) {
                            onNavigateToNotePage(block.targetNoteId, block.targetPage)
                        } else {
                            Toast.makeText(context, "Link destination not set", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEdit = { onEditBlock(block) },
                    onDelete = { onDeleteBlock(block) }
                )
            }
            is PdfAttachmentContentBlock -> {
                PdfAttachmentBlockView(
                    block = block,
                    onOpen = {
                        if (block.pdfFilePath.isNotBlank()) {
                            onOpenPdf(block.pdfFilePath, 1)
                        } else {
                            Toast.makeText(context, "PDF file missing", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEdit = { onEditBlock(block) },
                    onDelete = { onDeleteBlock(block) }
                )
            }
            is PdfPageContentBlock -> {
                PdfPageBlockView(
                    block = block,
                    onOpenSource = {
                        if (block.pdfFilePath.isNotBlank()) {
                            onOpenPdf(block.pdfFilePath, block.pdfPageIndex + 1)
                        }
                    },
                    onDelete = { onDeleteBlock(block) }
                )
            }
            is TextContentBlock -> {
                TextBlockView(
                    block = block,
                    onEdit = { onEditBlock(block) },
                    onDelete = { onDeleteBlock(block) }
                )
            }
        }

        // Selection Corner Indicators
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/**
 * Audio Content Block Card UI
 */
@Composable
fun AudioBlockView(
    block: AudioContentBlock,
    audioManager: LipiAudioManager,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCurrentPlaying = audioManager.currentPlayingBlockId == block.id && audioManager.isPlaying
    val position = if (audioManager.currentPlayingBlockId == block.id) audioManager.playbackPositionMs else 0L
    val totalDuration = if (block.durationMs > 0) block.durationMs else audioManager.playbackDurationMs

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF3B82F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = block.title.ifBlank { block.originalFileName },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Playback speed pill
                    if (isCurrentPlaying) {
                        Surface(
                            onClick = {
                                val nextSpeed = when (audioManager.playbackSpeed) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                audioManager.setSpeed(nextSpeed)
                            },
                            shape = CircleShape,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${audioManager.playbackSpeed}x",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Controls & Waveform Scrubber
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause Button
                IconButton(
                    onClick = {
                        if (isCurrentPlaying) {
                            audioManager.pausePlayback()
                        } else {
                            audioManager.playAudio(block.id, block.audioFilePath)
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
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

                // Slider / Scrubber
                Column(modifier = Modifier.weight(1f)) {
                    val progress = if (totalDuration > 0) (position.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                    Slider(
                        value = progress,
                        onValueChange = { newProgress ->
                            val targetMs = (newProgress * totalDuration).toLong()
                            audioManager.seekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF60A5FA),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.height(20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = audioManager.formatDuration(position),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = audioManager.formatDuration(totalDuration),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Web Hyperlink Block Card UI
 */
@Composable
fun WebLinkBlockView(
    block: WebLinkContentBlock,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(3.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC),
            contentColor = Color(0xFF0F172A)
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFEFF6FF), CircleShape)
                    .border(1.dp, Color(0xFFBFDBFE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = block.title.ifBlank { block.url },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = block.url,
                    fontSize = 10.sp,
                    color = Color(0xFF2563EB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Link",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Internal Note / Page Link Block Card UI
 */
@Composable
fun InternalLinkBlockView(
    block: InternalLinkContentBlock,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(3.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAF5FF), // Soft purple container
            contentColor = Color(0xFF581C87)
        ),
        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF8B5CF6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = block.label.ifBlank { block.targetNoteTitle.ifBlank { "Lipi Note" } },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4C1D95),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Jump to Page ${block.targetPage}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6D28D9)
                    )
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Jump",
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * PDF Attachment Block Card UI
 */
@Composable
fun PdfAttachmentBlockView(
    block: PdfAttachmentContentBlock,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF1F2), // Soft rose/red container
            contentColor = Color(0xFF881337)
        ),
        border = BorderStroke(1.dp, Color(0xFFFECDD3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFE11D48), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = block.originalFileName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF881337),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${block.pageCount} ${if (block.pageCount == 1) "page" else "pages"} • ${block.fileSizeFormatted}",
                    fontSize = 10.sp,
                    color = Color(0xFF9F1239)
                )
            }

            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE11D48),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Embedded PDF Page Block View (Renders the PDF page bitmap onto note canvas)
 */
@Composable
fun PdfPageBlockView(
    block: PdfPageContentBlock,
    onOpenSource: () -> Unit,
    onDelete: () -> Unit
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(block.pdfFilePath, block.pdfPageIndex) {
        if (block.pdfFilePath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val file = File(block.pdfFilePath)
                if (file.exists()) {
                    val bmp = LipiPdfManager.renderPageToBitmap(file, block.pdfPageIndex, 1200, 1600)
                    pageBitmap = bmp
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
    ) {
        if (pageBitmap != null && !pageBitmap!!.isRecycled) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${block.pdfPageIndex + 1}",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Header Tag Chip
        Surface(
            shape = RoundedCornerShape(bottomEnd = 8.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${block.sourcePdfTitle} • Page ${block.pdfPageIndex + 1}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Text / Sticky Note Block View
 */
@Composable
fun TextBlockView(
    block: TextContentBlock,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(block.backgroundColor),
            contentColor = Color(block.textColor)
        ),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = block.text.ifBlank { "Tap to write note..." },
                fontSize = block.fontSizeSp.sp,
                color = Color(block.textColor),
                modifier = Modifier.fillMaxSize()
            )

            // Pin / Sticky header icon
            if (block.isStickyNote) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}
