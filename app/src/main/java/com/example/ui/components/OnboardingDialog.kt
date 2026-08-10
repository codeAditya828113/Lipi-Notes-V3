package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val illustrationType: String // "canvas", "ai", "sync"
)

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            title = "Smart Stylus & Handwriting",
            subtitle = "Draw, paint, and write fluidly with ultra-low latency stylus input, custom background templates, and infinite canvas.",
            icon = Icons.Default.Edit,
            primaryColor = Color(0xFF2196F3),
            illustrationType = "canvas"
        ),
        OnboardingPageData(
            title = "Gemini AI Summaries & Transcription",
            subtitle = "Instantly transcribe handwritten notes or recorded voice memos with Gemini 3.5 Flash AI model.",
            icon = Icons.Default.AutoAwesome,
            primaryColor = Color(0xFF9C27B0),
            illustrationType = "ai"
        ),
        OnboardingPageData(
            title = "Document Annotation (PDF/DOCX)",
            subtitle = "Import PDFs and Word documents, draw directly on them, extract handwritten text, and export them back with your annotations seamlessly.",
            icon = Icons.Default.PictureAsPdf,
            primaryColor = Color(0xFFE53935), // Red
            illustrationType = "docs"
        ),
        OnboardingPageData(
            title = "Tabs & Folder Organization",
            subtitle = "Keep everything organized using custom folders. Switch between them instantly via the tab bar at the top of your workspace.",
            icon = Icons.Default.Folder,
            primaryColor = Color(0xFFFF9800), // Orange
            illustrationType = "folder"
        ),
        OnboardingPageData(
            title = "Interactive Hyperlinks",
            subtitle = "Embed clickable hyperlinks into your text notes. Just tap them to instantly navigate to external web pages without leaving your context.",
            icon = Icons.Default.Link,
            primaryColor = Color(0xFF3F51B5), // Indigo
            illustrationType = "link"
        ),
        OnboardingPageData(
            title = "Cloud Sync & Native Sharing",
            subtitle = "Sync seamlessly with Google Drive, export encrypted backups, and share PNG drawings to any app.",
            icon = Icons.Default.CloudSync,
            primaryColor = Color(0xFF009688),
            illustrationType = "sync"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LipiBrandHeader(
                        iconSize = 32.dp,
                        showTagline = true
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) { page ->
                    val pageData = pages[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Vector Illustration Canvas
                        OnboardingIllustration(
                            type = pageData.illustrationType,
                            color = pageData.primaryColor,
                            icon = pageData.icon
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = pageData.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = pageData.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) pages[index].primaryColor
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // Action Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[pagerState.currentPage].primaryColor
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingIllustration(
    type: String,
    color: Color,
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (type) {
                "canvas" -> {
                    // Draw mini tablet frame and stylus curve
                    drawRoundRect(
                        color = color.copy(alpha = 0.3f),
                        topLeft = Offset(w * 0.15f, h * 0.15f),
                        size = Size(w * 0.7f, h * 0.7f),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = Stroke(width = 4f)
                    )
                    val p = Path().apply {
                        moveTo(w * 0.25f, h * 0.5f)
                        cubicTo(w * 0.4f, h * 0.2f, w * 0.6f, h * 0.8f, w * 0.75f, h * 0.4f)
                    }
                    drawPath(path = p, color = color, style = Stroke(width = 6f))
                }
                "ai" -> {
                    // Draw floating sparkles around center
                    drawCircle(color = color.copy(alpha = 0.2f), radius = w * 0.4f, center = Offset(w / 2, h / 2))
                    drawCircle(color = color.copy(alpha = 0.4f), radius = w * 0.25f, center = Offset(w / 2, h / 2))
                }
                "docs" -> {
                    // Draw document stack
                    drawRoundRect(
                        color = color.copy(alpha = 0.15f),
                        topLeft = Offset(w * 0.25f, h * 0.15f),
                        size = Size(w * 0.5f, h * 0.7f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = color.copy(alpha = 0.3f),
                        topLeft = Offset(w * 0.2f, h * 0.2f),
                        size = Size(w * 0.6f, h * 0.6f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
                "folder" -> {
                    // Draw a folder shape
                    val folderPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.3f)
                        lineTo(w * 0.35f, h * 0.3f)
                        lineTo(w * 0.45f, h * 0.4f)
                        lineTo(w * 0.85f, h * 0.4f)
                        lineTo(w * 0.85f, h * 0.75f)
                        lineTo(w * 0.15f, h * 0.75f)
                        close()
                    }
                    drawPath(path = folderPath, color = color.copy(alpha = 0.3f))
                }
                "link" -> {
                    // Draw a connected chain link abstract
                    drawCircle(color = color.copy(alpha = 0.2f), radius = w * 0.2f, center = Offset(w * 0.35f, h * 0.5f))
                    drawCircle(color = color.copy(alpha = 0.3f), radius = w * 0.2f, center = Offset(w * 0.65f, h * 0.5f))
                }
                else -> {
                    // Sync illustration
                    drawRoundRect(
                        color = color.copy(alpha = 0.25f),
                        topLeft = Offset(w * 0.2f, h * 0.2f),
                        size = Size(w * 0.6f, h * 0.6f),
                        cornerRadius = CornerRadius(20f, 20f)
                    )
                }
            }
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(56.dp)
        )
    }
}
