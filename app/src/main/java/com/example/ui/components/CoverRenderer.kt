package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RenderCover(
    coverType: String,
    title: String,
    subtitle: String,
    author: String,
    extra: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        when (coverType) {
            "3d_academic" -> Cover3DAcademic(title, subtitle, author, extra)
            "3d_journal" -> Cover3DJournal(title, subtitle, author)
            "3d_tech" -> Cover3DTech(title, subtitle, extra)
            "3d_creative" -> Cover3DCreative(title, author)
            "3d_luxury" -> Cover3DLuxury(title, subtitle, author, extra)
            "3d_glass" -> Cover3DGlass(title, subtitle, author)
            "3d_nature" -> Cover3DNature(title, subtitle, author)
            "3d_minimal" -> Cover3DMinimal(title, subtitle, author)
            // Subject Specific Graphic Covers
            "subject_math", "math" -> SubjectMathCover(title, subtitle, author, extra)
            "subject_gk_gs" -> SubjectGkGsCover(title, subtitle, author, extra)
            "subject_current_affairs" -> SubjectCurrentAffairsCover(title, subtitle, author, extra)
            "subject_reasoning" -> SubjectReasoningCover(title, subtitle, author, extra)
            "subject_hindi" -> SubjectHindiCover(title, subtitle, author, extra)
            "subject_english", "english" -> SubjectEnglishCover(title, subtitle, author, extra)
            "subject_science", "science" -> SubjectScienceCover(title, subtitle, author, extra)
            "subject_sst" -> SubjectSstCover(title, subtitle, author, extra)
            "subject_sanskrit" -> SubjectSanskritCover(title, subtitle, author, extra)
            "subject_computer" -> SubjectComputerCover(title, subtitle, author, extra)
            "subject_physics" -> SubjectPhysicsCover(title, subtitle, author, extra)
            "subject_chemistry" -> SubjectChemistryCover(title, subtitle, author, extra)
            "subject_biology" -> SubjectBiologyCover(title, subtitle, author, extra)
            "subject_history" -> SubjectHistoryCover(title, subtitle, author, extra)
            "earth" -> EarthCover(title, subtitle, extra)
            "journal" -> JournalCover(title)
            "treehouse" -> TreehouseCover(title, author)
            "daily" -> DailyCover(title, subtitle)
            "language" -> LanguageCover(title, author)
            "english" -> EnglishCover(title, author, extra)
            "math" -> MathCover(title, subtitle, author, extra)
            "dark" -> Cover3DLuxury(title, "Dark Edition", author, extra)
            "light" -> Cover3DMinimal(title, subtitle, author)
            "tiger" -> Cover3DTiger(title, author)
            "reader" -> Cover3DReader(title, author)
            "sketch" -> Cover3DSketch(title, author)
            "wash" -> Cover3DWash(title, author)
            "ink" -> Cover3DInk(title, author)
            "car" -> Cover3DCar(title, author)
            "geo1" -> Cover3DGeo1(title, subtitle, author)
            "geo2" -> Cover3DGeo2(title, subtitle, author)
            "geo3" -> Cover3DGeo3(title, subtitle, author)
            else -> Cover3DAcademic(if (title.isBlank()) "My Notebook" else title, subtitle, author, extra)
        }

        // 3D Book Spine Fold & Shadow Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spineWidth = size.width * 0.05f
            // Left spine shadow gradient
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = spineWidth * 1.5f
                ),
                size = Size(spineWidth * 1.5f, size.height)
            )
            // Left spine highlight
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(spineWidth * 0.6f, 0f),
                end = Offset(spineWidth * 0.6f, size.height),
                strokeWidth = 2f
            )
            // Top and Right subtle bevel highlight
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2f
            )
        }
    }
}

// ==================== 3D COVER TEMPLATES ====================

// 1. 3D Academic (Science & Math)
@Composable
fun Cover3DAcademic(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Floating 3D Orbs & Spheres with gradient depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.2f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.85f, h * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC084FC), Color(0xFF9333EA), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.75f),
                    radius = w * 0.4f
                ),
                radius = w * 0.4f,
                center = Offset(w * 0.15f, h * 0.75f)
            )
            // 3D Ring / Torus
            drawOval(
                color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                topLeft = Offset(w * 0.25f, h * 0.15f),
                size = Size(w * 0.5f, h * 0.12f),
                style = Stroke(width = 12f)
            )
        }

        // 3D Embossed Card Badge
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF4F46E5),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (extra.isNotBlank()) extra.uppercase() else "ACADEMIC 3D",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = if (title.isNotBlank()) title else "SCIENCE & MATH",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
                Divider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Text(
                    text = if (author.isNotBlank()) author else "Student Notebook",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

// 2. 3D Journal (Warm Soft Pastel Glass)
@Composable
fun Cover3DJournal(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDE68A), Color(0xFFFCA5A5), Color(0xFFF472B6))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Floating 3D Bubbles
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = w * 0.35f,
                center = Offset(w * 0.3f, h * 0.25f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = w * 0.28f,
                center = Offset(w * 0.75f, h * 0.7f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.85f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "JOURNAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (title.isNotBlank()) title else "Daily Reflections",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF881337),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = if (author.isNotBlank()) author else "Personal Memories",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9F1239),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// 3. 3D Tech & Cyberpunk Code
@Composable
fun Cover3DTech(title: String, subtitle: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF030712), Color(0xFF0B0F19), Color(0xFF111827))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 3D Neon Mesh Grid
            val gridStep = 32f
            for (i in 0..(w / gridStep).toInt()) {
                drawLine(
                    color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                    start = Offset(i * gridStep, 0f),
                    end = Offset(i * gridStep, h),
                    strokeWidth = 1f
                )
            }
            for (j in 0..(h / gridStep).toInt()) {
                drawLine(
                    color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                    start = Offset(0f, j * gridStep),
                    end = Offset(w, j * gridStep),
                    strokeWidth = 1f
                )
            }
            // Glowing 3D Tech Orbs
            drawCircle(
                color = Color(0xFF06B6D4).copy(alpha = 0.25f),
                radius = w * 0.4f,
                center = Offset(w * 0.5f, h * 0.4f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, Color(0xFF06B6D4)),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "// 3D TECH PAD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (title.isNotBlank()) title else "DEVELOPER NOTES",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// 4. 3D Creative Clay
@Composable
fun Cover3DCreative(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF818CF8), Color(0xFF6366F1), Color(0xFF4338CA))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Floating 3D clay shapes
            drawCircle(Color(0xFFF43F5E), radius = w * 0.25f, center = Offset(w * 0.2f, h * 0.2f))
            drawCircle(Color(0xFF10B981), radius = w * 0.2f, center = Offset(w * 0.8f, h * 0.35f))
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(w * 0.25f, h * 0.7f),
                size = Size(w * 0.5f, h * 0.15f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (title.isNotBlank()) title else "CREATIVE STUDIO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E1B4B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (author.isNotBlank()) author else "Design Edition",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }
        }
    }
}

// 5. 3D Luxury Obsidian & Gold
@Composable
fun Cover3DLuxury(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF111827), Color(0xFF1F2937), Color(0xFF000000))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 3D Metallic Gold Ribbons
            val path = Path().apply {
                moveTo(0f, h * 0.3f)
                cubicTo(w * 0.4f, h * 0.1f, w * 0.6f, h * 0.5f, w, h * 0.35f)
                lineTo(w, h * 0.42f)
                cubicTo(w * 0.6f, h * 0.57f, w * 0.4f, h * 0.17f, 0f, h * 0.37f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04))
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF18181B),
                border = BorderStroke(1.5.dp, Color(0xFFEAB308)),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LUXURY 3D EDITION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (title.isNotBlank()) title.uppercase() else "EXECUTIVE NOTE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                    Divider(color = Color(0xFFEAB308).copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = if (author.isNotBlank()) author else "Confidential",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 6. 3D Glassmorphism
@Composable
fun Cover3DGlass(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color.White.copy(alpha = 0.3f), radius = w * 0.4f, center = Offset(w * 0.2f, h * 0.3f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.4f), radius = w * 0.35f, center = Offset(w * 0.8f, h * 0.7f))
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (title.isNotBlank()) title else "GLASSMORPHIC 3D",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

// 7. 3D Emerald Nature
@Composable
fun Cover3DNature(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFF34D399).copy(alpha = 0.3f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.2f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.84f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "NATURE 3D", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "Botanical Field Notes", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF064E3B), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = if (author.isNotBlank()) author else "Green Edition", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 8. 3D Minimal Paper
@Composable
fun Cover3DMinimal(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                color = Color(0xFF38BDF8),
                topLeft = Offset(0f, 0f),
                size = Size(w, h * 0.08f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (title.isNotBlank()) title else "Minimalist Notebook", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if (author.isNotBlank()) author else "Clean Edition", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 1. Science Notebook
@Composable
fun ScienceCover(title: String, author: String, year: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE0EAF5))) {
        // Decorative doodles via canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Molecules, planets, test tubes, microscopes... simplified
            drawCircle(Color(0xFFF28B82), radius = w*0.08f, center = Offset(w*0.8f, h*0.2f))
            drawCircle(Color(0xFF81C995), radius = w*0.04f, center = Offset(w*0.75f, h*0.15f))
            drawCircle(Color(0xFFAECBFA), radius = w*0.03f, center = Offset(w*0.85f, h*0.25f))
            // Rocket
            drawOval(Color(0xFFFAD2CF), topLeft = Offset(w*0.1f, h*0.8f), size = Size(w*0.1f, h*0.15f))
            // Atom
            drawOval(Color.DarkGray, topLeft = Offset(w*0.4f, h*0.8f), size = Size(w*0.2f, h*0.08f), style = Stroke(width = 4f))
            drawOval(Color.DarkGray, topLeft = Offset(w*0.45f, h*0.76f), size = Size(w*0.08f, h*0.16f), style = Stroke(width = 4f))
            // Magnet
            drawArc(Color(0xFFE67C73), 180f, 180f, useCenter = false, topLeft = Offset(w*0.6f, h*0.5f), size = Size(w*0.2f, w*0.2f), style = Stroke(width = w*0.05f))
        }

        // Center card
        Card(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.8f).aspectRatio(1.2f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color.Gray.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp).border(2.dp, Color.Gray, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(title.uppercase(), fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(author, fontSize = 14.sp, color = Color.DarkGray)
                        Text(year, fontSize = 14.sp, color = Color.DarkGray)
                    }
                    Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

// 2. Earth Science
@Composable
fun EarthCover(title: String, teacher: String, time: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0F3624))) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.8f)
                    quadraticTo(size.width * 0.15f, size.height * 0.7f, size.width * 0.2f, size.height * 0.8f)
                }
                drawPath(path, Color(0xFFFFFDE7))
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFFC107))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(teacher.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3624))
                Text(time.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3624))
            }
        }
    }
    // Overlay text and globe
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title.uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFFDE7),
                 style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = Offset(4f, 4f), blurRadius = 0f)))
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(Color(0xFF64B5F6))
                // continents
                drawCircle(Color(0xFF81C995), radius = size.width * 0.2f, center = Offset(size.width * 0.3f, size.height * 0.3f))
                drawCircle(Color(0xFF81C995), radius = size.width * 0.15f, center = Offset(size.width * 0.7f, size.height * 0.6f))
                drawCircle(Color(0xFF81C995), radius = size.width * 0.1f, center = Offset(size.width * 0.5f, size.height * 0.8f))
            }
        }
    }
}

// 3. My Journal
@Composable
fun JournalCover(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Top flowers
            drawCircle(Color(0xFFF28B82), radius = w*0.2f, center = Offset(w*0.5f, h*0.1f))
            drawCircle(Color(0xFFFCE8E6), radius = w*0.1f, center = Offset(w*0.7f, h*0.15f))
            drawCircle(Color(0xFF81C995), radius = w*0.05f, center = Offset(w*0.4f, h*0.2f))
            // Bottom flowers
            drawCircle(Color(0xFFF28B82), radius = w*0.25f, center = Offset(w*0.4f, h*0.8f))
            drawCircle(Color(0xFFFCE8E6), radius = w*0.15f, center = Offset(w*0.6f, h*0.85f))
        }
        Text(title.uppercase(), fontSize = 28.sp, color = Color.DarkGray, letterSpacing = 4.sp, modifier = Modifier.align(Alignment.Center))
    }
}

// 4. Notebook Treehouse
@Composable
fun TreehouseCover(title: String, author: String) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFAD2CF), Color(0xFFFFFDE7))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Tree
            drawRoundRect(Color(0xFF8D6E63), topLeft = Offset(w*0.4f, h*0.5f), size = Size(w*0.2f, h*0.4f))
            // Leaves
            drawCircle(Color(0xFFAED581), radius = w*0.3f, center = Offset(w*0.5f, h*0.4f))
            drawCircle(Color(0xFF81C995), radius = w*0.25f, center = Offset(w*0.3f, h*0.5f))
            drawCircle(Color(0xFF9CCC65), radius = w*0.25f, center = Offset(w*0.7f, h*0.5f))
            // House
            drawRect(Color(0xFFFFCC80), topLeft = Offset(w*0.3f, h*0.55f), size = Size(w*0.4f, h*0.2f))
            // Roof
            val roofPath = Path().apply {
                moveTo(w*0.25f, h*0.55f)
                lineTo(w*0.5f, h*0.45f)
                lineTo(w*0.75f, h*0.55f)
                close()
            }
            drawPath(roofPath, Color(0xFF8D6E63))
        }
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            Spacer(modifier = Modifier.height(8.dp))
            Text(author, fontSize = 20.sp, color = Color(0xFF3E2723))
        }
    }
}

// 5. Daily Journal
@Composable
fun DailyCover(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFAECBFA), Color(0xFFFCE8E6))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Clouds
            drawCircle(Color.White.copy(alpha = 0.8f), radius = w*0.25f, center = Offset(w*0.2f, h*0.3f))
            drawCircle(Color.White.copy(alpha = 0.8f), radius = w*0.3f, center = Offset(w*0.6f, h*0.2f))
            drawCircle(Color.White.copy(alpha = 0.8f), radius = w*0.2f, center = Offset(w*0.8f, h*0.35f))
            
            drawCircle(Color(0xFFF8BBD0).copy(alpha=0.5f), radius = w*0.3f, center = Offset(w*0.3f, h*0.7f))
            drawCircle(Color(0xFFB39DDB).copy(alpha=0.5f), radius = w*0.4f, center = Offset(w*0.7f, h*0.8f))
        }
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MY DAILY", fontSize = 16.sp, color = Color.White, letterSpacing = 2.sp)
            Text("Journal", fontSize = 56.sp, color = Color.White, fontFamily = FontFamily.Cursive)
        }
        Text(subtitle.uppercase(), fontSize = 16.sp, color = Color.White, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
    }
}

// 6. Language Notebook
@Composable
fun LanguageCover(title: String, author: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFFDE7))) {
        // Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40f
            for (i in 0..(size.width / step).toInt()) {
                drawLine(Color(0xFFFFECB3), Offset(i * step, 0f), Offset(i * step, size.height), 2f)
            }
            for (i in 0..(size.height / step).toInt()) {
                drawLine(Color(0xFFFFECB3), Offset(0f, i * step), Offset(size.width, i * step), 2f)
            }
            
            // Draw some pencils (ABC shapes)
            drawRoundRect(Color(0xFFFFCA28), topLeft = Offset(size.width*0.1f, size.height*0.1f), size = Size(size.width*0.2f, size.height*0.05f))
            drawRoundRect(Color(0xFFF44336), topLeft = Offset(size.width*0.3f, size.height*0.1f), size = Size(size.width*0.05f, size.height*0.05f))
        }
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("My Language", fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Cursive, color = Color.Black)
            Text("Notebook", fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Cursive, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Text(author, fontSize = 24.sp, fontFamily = FontFamily.Cursive, color = Color.DarkGray)
        }
    }
}

// 7. English Notebook
@Composable
fun EnglishCover(title: String, author: String, year: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Pink floral petals
            drawCircle(Color(0xFFFCE8E6), radius = w*0.3f, center = Offset(0f, 0f))
            drawCircle(Color(0xFFFCE8E6), radius = w*0.3f, center = Offset(w, h*0.2f))
            drawCircle(Color(0xFFFCE8E6), radius = w*0.4f, center = Offset(w*0.3f, h))
            // Center dots
            drawCircle(Color(0xFFE64A19), radius = w*0.05f, center = Offset(0f, 0f))
            drawCircle(Color(0xFFE64A19), radius = w*0.05f, center = Offset(w, h*0.2f))
            drawCircle(Color(0xFFE64A19), radius = w*0.05f, center = Offset(w*0.3f, h))
        }
        
        Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.8f).background(Color.White).border(4.dp, Color.Black).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(title.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(author, fontSize = 20.sp, fontFamily = FontFamily.Cursive)
                Divider(color = Color.Black, modifier = Modifier.padding(vertical = 12.dp))
                Text(year, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 8. Math Cover
@Composable
fun MathCover(title: String, subtitle: String, studentInfo: String, classInfo: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F5))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title.uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF263238), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.background(Color(0xFF37474F)).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFF2E4D3E)).border(4.dp, Color(0xFF8D6E63))) {
            // Chalkboard content
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawLine(Color.White.copy(alpha=0.5f), Offset(w*0.1f, h*0.2f), Offset(w*0.4f, h*0.2f), 2f)
                drawCircle(Color.White.copy(alpha=0.5f), radius = 20f, center = Offset(w*0.2f, h*0.4f), style = Stroke(width=2f))
                drawLine(Color.White.copy(alpha=0.5f), Offset(w*0.6f, h*0.1f), Offset(w*0.8f, h*0.3f), 2f)
            }
            Text("PRACTICE MAKES PERFECT!", color = Color.White, fontFamily = FontFamily.Cursive, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }
        
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(studentInfo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(classInfo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 9. 3D Tiger Wild Fauna
@Composable
fun Cover3DTiger(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF78350F))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 3D Tiger Stripe Paths
            val stripe1 = Path().apply {
                moveTo(0f, h * 0.15f)
                lineTo(w * 0.45f, h * 0.22f)
                lineTo(0f, h * 0.28f)
                close()
            }
            val stripe2 = Path().apply {
                moveTo(w, h * 0.35f)
                lineTo(w * 0.5f, h * 0.42f)
                lineTo(w, h * 0.5f)
                close()
            }
            val stripe3 = Path().apply {
                moveTo(0f, h * 0.7f)
                lineTo(w * 0.4f, h * 0.78f)
                lineTo(0f, h * 0.85f)
                close()
            }
            drawPath(stripe1, Color(0xFF18181B))
            drawPath(stripe2, Color(0xFF18181B))
            drawPath(stripe3, Color(0xFF18181B))

            // Golden Glow Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE047).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.4f
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.4f
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TIGER 3D FAUNA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (title.isNotBlank()) title.uppercase() else "WILD NOTEBOOK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (author.isNotBlank()) author else "Aditya Kumar",
                    fontSize = 11.sp,
                    color = Color(0xFFFCD34D),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 10. 3D Reader Library
@Composable
fun Cover3DReader(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF451A03), Color(0xFF292524), Color(0xFF1C1917))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Floating 3D Book Page Fan
            for (i in 0..4) {
                val offsetY = h * (0.2f + i * 0.12f)
                drawRoundRect(
                    color = Color(0xFFFEF3C7).copy(alpha = 0.25f - i * 0.04f),
                    topLeft = Offset(w * (0.15f + i * 0.03f), offsetY),
                    size = Size(w * (0.7f - i * 0.06f), h * 0.08f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )
            }
            // Gold Bookmark Line
            drawRect(
                color = Color(0xFFD97706),
                topLeft = Offset(w * 0.8f, 0f),
                size = Size(w * 0.06f, h * 0.45f)
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(2.dp, Color(0xFFB45309)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("LITERARY CLASSICS 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (title.isNotBlank()) title else "Reader's Digest",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF451A03),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (author.isNotBlank()) author else "Aditya Kumar",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF78350F)
                )
            }
        }
    }
}

// 11. 3D Sketchbook & Graphite
@Composable
fun Cover3DSketch(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF27272A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 3D Isometric Wireframe Cubes & Circles
            val cubePath = Path().apply {
                moveTo(w * 0.5f, h * 0.2f)
                lineTo(w * 0.8f, h * 0.35f)
                lineTo(w * 0.8f, h * 0.65f)
                lineTo(w * 0.5f, h * 0.8f)
                lineTo(w * 0.2f, h * 0.65f)
                lineTo(w * 0.2f, h * 0.35f)
                close()
            }
            drawPath(cubePath, Color(0xFFA1A1AA), style = Stroke(width = 3f))
            drawLine(Color(0xFFA1A1AA), Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 3f)
            drawLine(Color(0xFFA1A1AA), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.8f, h * 0.35f), strokeWidth = 3f)
            drawLine(Color(0xFFA1A1AA), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.2f, h * 0.35f), strokeWidth = 3f)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .background(Color(0xFF18181B), RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFA1A1AA), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("GRAPHITE SKETCH 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4D4D8), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (title.isNotBlank()) title else "STUDIO SKETCHBOOK",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }
        }
    }
}

// 12. 3D Watercolor Wash
@Composable
fun Cover3DWash(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE0F2FE), Color(0xFFF0FDFA), Color(0xFFFCE7F3))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Vibrant watercolor blobs
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.45f), radius = w * 0.45f, center = Offset(w * 0.2f, h * 0.25f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.45f), radius = w * 0.4f, center = Offset(w * 0.8f, h * 0.7f))
            drawCircle(Color(0xFF2DD4BF).copy(alpha = 0.35f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WATERCOLOR WASH 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "Fluid Art Notes", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 13. 3D Calligraphy Ink
@Composable
fun Cover3DInk(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0284C7), Color(0xFF0F172A), Color(0xFF020617))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Metallic Gold Ink Swirl Path
            val path = Path().apply {
                moveTo(0f, h * 0.2f)
                cubicTo(w * 0.6f, h * 0.1f, w * 0.2f, h * 0.9f, w, h * 0.8f)
            }
            drawPath(path, brush = Brush.horizontalGradient(colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308))), style = Stroke(width = 16f))
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            border = BorderStroke(1.5.dp, Color(0xFFFDE047))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CALLIGRAPHY INK 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (title.isNotBlank()) title else "Ink & Brush Journal", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFCBD5E1))
            }
        }
    }
}

// 14. 3D Cyber Car & Racing
@Composable
fun Cover3DCar(title: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09090B), Color(0xFF18181B), Color(0xFF000000))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Neon Speed Trails
            for (i in 0..5) {
                val startY = h * (0.3f + i * 0.08f)
                drawLine(
                    color = if (i % 2 == 0) Color(0xFFEF4444) else Color(0xFF06B6D4),
                    start = Offset(0f, startY),
                    end = Offset(w * (0.4f + i * 0.1f), startY),
                    strokeWidth = 6f
                )
            }
            // Speedometer Arc
            drawArc(
                color = Color(0xFFEF4444),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.25f, h * 0.12f),
                size = Size(w * 0.5f, h * 0.25f),
                style = Stroke(width = 8f)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF18181B),
            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CYBER AUTO 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (title.isNotBlank()) title.uppercase() else "RACING TELEMETRY", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }
        }
    }
}

// 15. 3D Geo 1 (Isometric Cubes)
@Composable
fun Cover3DGeo1(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4C1D95), Color(0xFF7C3AED), Color(0xFFC084FC))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Floating 3D Cubes
            drawCircle(Color.White.copy(alpha = 0.25f), radius = w * 0.35f, center = Offset(w * 0.3f, h * 0.3f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.35f), radius = w * 0.4f, center = Offset(w * 0.7f, h * 0.7f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFF7C3AED))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ISOMETRIC GEO 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "GEOMETRIC PAD", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E1B4B), textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF6B21A8))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 16. 3D Geo 2 (Pyramids & Spheres)
@Composable
fun Cover3DGeo2(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF134E4A))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Golden Pyramid Triangle
            val pPath = Path().apply {
                moveTo(w * 0.5f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.45f)
                lineTo(w * 0.15f, h * 0.45f)
                close()
            }
            drawPath(pPath, brush = Brush.horizontalGradient(colors = listOf(Color(0xFFFDE047), Color(0xFFCA8A04))))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFCCFBF1)),
            border = BorderStroke(1.5.dp, Color(0xFF0D9488))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PYRAMID GEO 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "STRUCTURED NOTES", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF134E4A), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFF0D9488), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 17. 3D Geo 3 (Crystalline Prism)
@Composable
fun Cover3DGeo3(title: String, subtitle: String, author: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFBE185D), Color(0xFF831843), Color(0xFF500724))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Crystalline Prism Beams
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.4f), radius = w * 0.45f, center = Offset(w * 0.8f, h * 0.2f))
            drawCircle(Color(0xFFFDE047).copy(alpha = 0.3f), radius = w * 0.35f, center = Offset(w * 0.2f, h * 0.8f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE7F3)),
            border = BorderStroke(1.5.dp, Color(0xFFDB2777))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CRYSTAL PRISM 3D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9D174D), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "PRISM NOTEBOOK", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF500724), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFBE185D), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== SUBJECT GRAPHICAL COVERS ====================

// 1. Mathematics
@Composable
fun SubjectMathCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E3A8A))
                )
            )
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Coordinate axis grid
            val grid = 28f
            for (i in 0..(w / grid).toInt()) {
                drawLine(Color(0xFF38BDF8).copy(alpha = 0.12f), Offset(i * grid, 0f), Offset(i * grid, h))
            }
            for (j in 0..(h / grid).toInt()) {
                drawLine(Color(0xFF38BDF8).copy(alpha = 0.12f), Offset(0f, j * grid), Offset(w, j * grid))
            }
            // 3D Geometry Triangle & Sine Wave
            val wavePath = Path().apply {
                moveTo(0f, h * 0.3f)
                cubicTo(w * 0.25f, h * 0.1f, w * 0.75f, h * 0.5f, w, h * 0.3f)
            }
            drawPath(wavePath, Color(0xFF38BDF8).copy(alpha = 0.7f), style = Stroke(width = 4f))
            // Floating 3D Pi / Delta Orb Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF818CF8).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.75f),
                    radius = w * 0.35f
                ),
                center = Offset(w * 0.75f, h * 0.75f),
                radius = w * 0.35f
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.92f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0284C7),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = if (extra.isNotBlank()) extra.uppercase() else "MATHEMATICS • गणित",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Text(
                    text = if (title.isNotBlank()) title else "MATH & ALGEBRA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFBAE6FD), fontFamily = FontFamily.SansSerif)
                }
                Divider(color = Color(0xFF38BDF8).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = if (author.isNotBlank()) author else "Aditya Kumar",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

// 2. GK & GS (General Knowledge & General Studies)
@Composable
fun SubjectGkGsCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF022C22))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Globe Longitude/Latitude Latice
            drawOval(
                color = Color(0xFFFDE047).copy(alpha = 0.3f),
                topLeft = Offset(w * 0.15f, h * 0.15f),
                size = Size(w * 0.7f, h * 0.35f),
                style = Stroke(width = 3f)
            )
            drawOval(
                color = Color(0xFFFDE047).copy(alpha = 0.3f),
                topLeft = Offset(w * 0.32f, h * 0.15f),
                size = Size(w * 0.36f, h * 0.35f),
                style = Stroke(width = 3f)
            )
            // Glowing Wisdom Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF34D399).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.7f),
                    radius = w * 0.4f
                ),
                center = Offset(w * 0.5f, h * 0.7f),
                radius = w * 0.4f
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)),
            border = BorderStroke(1.5.dp, Color(0xFFFDE047)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GENERAL KNOWLEDGE & GS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (title.isNotBlank()) title else "GK & GENERAL STUDIES",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFA7F3D0))
                }
                Divider(color = Color(0xFFFDE047).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 3. Current Affairs
@Composable
fun SubjectCurrentAffairsCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF881337), Color(0xFF9F1239), Color(0xFF18181B))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Radar / Live Broadcast Rings
            drawCircle(Color(0xFFFB7185).copy(alpha = 0.3f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.25f), style = Stroke(width = 3f))
            drawCircle(Color(0xFFFB7185).copy(alpha = 0.2f), radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.25f), style = Stroke(width = 3f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.5.dp, Color(0xFFE11D48)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE11D48), modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("LIVE • CURRENT AFFAIRS 2026", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Text(
                    text = if (title.isNotBlank()) title.uppercase() else "DAILY CURRENT AFFAIRS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFFECDD3))
                }
                Divider(color = Color(0xFFE11D48).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 4. Reasoning
@Composable
fun SubjectReasoningCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4C1D95), Color(0xFF6B21A8), Color(0xFF0F172A))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Logic node network
            drawLine(Color(0xFFC084FC).copy(alpha = 0.4f), Offset(w * 0.2f, h * 0.2f), Offset(w * 0.5f, h * 0.35f), strokeWidth = 3f)
            drawLine(Color(0xFFC084FC).copy(alpha = 0.4f), Offset(w * 0.8f, h * 0.2f), Offset(w * 0.5f, h * 0.35f), strokeWidth = 3f)
            drawLine(Color(0xFFC084FC).copy(alpha = 0.4f), Offset(w * 0.5f, h * 0.35f), Offset(w * 0.5f, h * 0.75f), strokeWidth = 3f)
            drawCircle(Color(0xFFA855F7), radius = 12f, center = Offset(w * 0.2f, h * 0.2f))
            drawCircle(Color(0xFFA855F7), radius = 12f, center = Offset(w * 0.8f, h * 0.2f))
            drawCircle(Color(0xFF38BDF8), radius = 16f, center = Offset(w * 0.5f, h * 0.35f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFFA855F7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REASONING & LOGICAL APTITUDE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "LOGICAL REASONING", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFE9D5FF))
                }
                Divider(color = Color(0xFFA855F7).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 5. Hindi (हिंदी)
@Composable
fun SubjectHindiCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFC2410C), Color(0xFF9A3412), Color(0xFF451A03))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Sacred Lotus & Mandala Arcs
            drawCircle(Color(0xFFFDBA74).copy(alpha = 0.25f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.25f))
            drawCircle(Color(0xFFFDBA74).copy(alpha = 0.15f), radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.25f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(2.dp, Color(0xFFEA580C)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("हिंदी साहित्य एवं व्याकरण", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C), letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (title.isNotBlank()) title else "हिंदी (HINDI)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF7C2D12),
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF9A3412))
                }
                Divider(color = Color(0xFFEA580C).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 6. English
@Composable
fun SubjectEnglishCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Elegant Gold Double Border
            drawRect(Color(0xFFFDE047).copy(alpha = 0.5f), topLeft = Offset(w * 0.06f, h * 0.04f), size = Size(w * 0.88f, h * 0.92f), style = Stroke(width = 2f))
            drawRect(Color(0xFFFDE047).copy(alpha = 0.3f), topLeft = Offset(w * 0.08f, h * 0.05f), size = Size(w * 0.84f, h * 0.9f), style = Stroke(width = 1f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFFFDE047)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ENGLISH LITERATURE & GRAMMAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047), letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (title.isNotBlank()) title else "ENGLISH LANGUAGE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, fontFamily = FontFamily.Serif, color = Color(0xFF93C5FD))
                }
                Divider(color = Color(0xFFFDE047).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, fontFamily = FontFamily.Serif, color = Color(0xFFFDE047))
            }
        }
    }
}

// 7. General Science
@Composable
fun SubjectScienceCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF022C22))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 3D Atomic Orbit Rings
            drawOval(Color(0xFF2DD4BF).copy(alpha = 0.5f), topLeft = Offset(w * 0.2f, h * 0.15f), size = Size(w * 0.6f, h * 0.2f), style = Stroke(width = 3f))
            drawOval(Color(0xFF2DD4BF).copy(alpha = 0.5f), topLeft = Offset(w * 0.35f, h * 0.1f), size = Size(w * 0.3f, h * 0.3f), style = Stroke(width = 3f))
            drawCircle(Color(0xFF2DD4BF), radius = 10f, center = Offset(w * 0.5f, h * 0.25f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)),
            border = BorderStroke(1.5.dp, Color(0xFF2DD4BF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GENERAL SCIENCE • विज्ञान", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2DD4BF), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "SCIENCE LAB NOTES", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF99F6E4))
                }
                Divider(color = Color(0xFF2DD4BF).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFF2DD4BF), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 8. SST (Social Studies)
@Composable
fun SubjectSstCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF78350F), Color(0xFF92400E), Color(0xFF451A03))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Greco-Roman Pillars & Sun
            drawCircle(Color(0xFFFDE047).copy(alpha = 0.25f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.2f))
            drawLine(Color(0xFFFDE047).copy(alpha = 0.4f), Offset(w * 0.25f, h * 0.1f), Offset(w * 0.25f, h * 0.35f), strokeWidth = 6f)
            drawLine(Color(0xFFFDE047).copy(alpha = 0.4f), Offset(w * 0.75f, h * 0.1f), Offset(w * 0.75f, h * 0.35f), strokeWidth = 6f)
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(2.dp, Color(0xFFB45309)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOCIAL STUDIES • सामाजिक विज्ञान", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "SST & GEOGRAPHY", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF451A03), textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF78350F))
                }
                Divider(color = Color(0xFFB45309).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 9. Sanskrit (संस्कृतम्)
@Composable
fun SubjectSanskritCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFB45309), Color(0xFFD97706), Color(0xFF78350F))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Sacred Temple Border & Halo
            drawCircle(Color(0xFFFEF3C7).copy(alpha = 0.3f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.25f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(2.dp, Color(0xFFD97706)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("संस्कृतम् • वैदिक साहित्यम्", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "संस्कृत भाषा (SANSKRIT)", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF78350F), textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF92400E))
                }
                Divider(color = Color(0xFFD97706).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 10. Computer Science
@Composable
fun SubjectComputerCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // CPU Chip silhouette & Circuit traces
            drawRoundRect(Color(0xFF38BDF8).copy(alpha = 0.3f), topLeft = Offset(w * 0.35f, h * 0.12f), size = Size(w * 0.3f, h * 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f), style = Stroke(width = 3f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("// COMPUTER & IT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "COMPUTER SCIENCE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFBAE6FD), fontFamily = FontFamily.Monospace)
                }
                Divider(color = Color(0xFF38BDF8).copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// 11. Physics
@Composable
fun SubjectPhysicsCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF312E81))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Quantum wave orbits
            drawCircle(Color(0xFF818CF8).copy(alpha = 0.35f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.25f), style = Stroke(width = 3f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
            border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PHYSICS • भौतिक विज्ञान", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "QUANTUM PHYSICS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 12. Chemistry
@Composable
fun SubjectChemistryCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF047857), Color(0xFF065F46), Color(0xFF064E3B))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Benzene Hexagon & Bubbles
            drawCircle(Color(0xFF34D399).copy(alpha = 0.3f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.22f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
            border = BorderStroke(1.5.dp, Color(0xFF34D399)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CHEMISTRY • रसायन विज्ञान", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "ORGANIC CHEMISTRY", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 13. Biology
@Composable
fun SubjectBiologyCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF15803D), Color(0xFF166534), Color(0xFF14532D))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Biological Leaf & Cell Nucleus
            drawCircle(Color(0xFF4ADE80).copy(alpha = 0.35f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.25f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14532D)),
            border = BorderStroke(1.5.dp, Color(0xFF4ADE80)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BIOLOGY • जीव विज्ञान", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "BIOLOGY & GENETICS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFBBF7D0), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 14. History & Geography
@Composable
fun SubjectHistoryCover(title: String, subtitle: String, author: String, extra: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF78350F), Color(0xFF451A03), Color(0xFF1C1917))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Vintage Compass
            drawCircle(Color(0xFFFDE047).copy(alpha = 0.3f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.22f), style = Stroke(width = 3f))
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(2.dp, Color(0xFFD97706)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HISTORY & GEOGRAPHY • इतिहास एवं भूगोल", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (title.isNotBlank()) title else "WORLD HISTORY", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF451A03), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (author.isNotBlank()) author else "Aditya Kumar", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
            }
        }
    }
}


