package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.NoteEntity

@Composable
fun PageTemplateCanvasPreview(
    templateType: String,
    pageColor: Long = 0xFFFFFFFF,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = pageColor == 0xFF1A1A1AL
    val bgColor = Color(pageColor)
    val gridLineColor = if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color(0xFF94A3B8).copy(alpha = 0.55f)
    val marginLineColor = if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.65f) else Color(0xFFFF8A80)
    val primaryLineColor = if (isDarkTheme) Color(0xFF60A5FA).copy(alpha = 0.75f) else Color(0xFF3B82F6).copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .background(bgColor)
            .clipToBounds()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (templateType.lowercase()) {
                "blank", "legal-blank", "2-col-blank" -> {
                    if (templateType.lowercase().contains("2-col")) {
                        drawLine(
                            color = marginLineColor,
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 1.5f
                        )
                    }
                    if (templateType.lowercase().contains("legal")) {
                        drawLine(
                            color = marginLineColor,
                            start = Offset(w * 0.22f, 0f),
                            end = Offset(w * 0.22f, h),
                            strokeWidth = 1.5f
                        )
                    }
                }
                "ruled", "legal-ruled", "2-col-ruled" -> {
                    val lineSpacing = h / 9f
                    for (i in 1..8) {
                        val y = i * lineSpacing
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }
                    val marginX = if (templateType.lowercase().contains("legal")) w * 0.24f else w * 0.16f
                    drawLine(
                        color = marginLineColor,
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, h),
                        strokeWidth = 1.5f
                    )
                    if (templateType.lowercase().contains("2-col")) {
                        drawLine(
                            color = marginLineColor,
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 1.5f
                        )
                    }
                }
                "grid", "square", "2-col-grid" -> {
                    val cols = 7
                    val rows = 9
                    val stepX = w / cols
                    val stepY = h / rows
                    for (i in 1 until cols) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(i * stepX, 0f),
                            end = Offset(i * stepX, h),
                            strokeWidth = 1f
                        )
                    }
                    for (j in 1 until rows) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, j * stepY),
                            end = Offset(w, j * stepY),
                            strokeWidth = 1f
                        )
                    }
                    if (templateType.lowercase().contains("2-col")) {
                        drawLine(
                            color = marginLineColor,
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 2f
                        )
                    }
                }
                "dotted" -> {
                    val cols = 7
                    val rows = 9
                    val stepX = w / cols
                    val stepY = h / rows
                    for (i in 1 until cols) {
                        for (j in 1 until rows) {
                            drawCircle(
                                color = gridLineColor,
                                radius = 1.8f,
                                center = Offset(i * stepX, j * stepY)
                            )
                        }
                    }
                }
                "cornell" -> {
                    val splitX = w * 0.32f
                    val summaryY = h * 0.78f
                    val lineSpacing = summaryY / 7f
                    
                    for (i in 1..6) {
                        val y = i * lineSpacing
                        drawLine(
                            color = gridLineColor,
                            start = Offset(splitX, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }
                    drawLine(
                        color = primaryLineColor,
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, summaryY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = primaryLineColor,
                        start = Offset(0f, summaryY),
                        end = Offset(w, summaryY),
                        strokeWidth = 2f
                    )
                }
                "meeting" -> {
                    val headerH = h * 0.2f
                    val agendaH = h * 0.58f
                    drawLine(primaryLineColor, Offset(0f, headerH), Offset(w, headerH), strokeWidth = 2f)
                    drawLine(primaryLineColor, Offset(0f, agendaH), Offset(w, agendaH), strokeWidth = 2f)
                    drawLine(gridLineColor, Offset(w * 0.5f, agendaH), Offset(w * 0.5f, h), strokeWidth = 1.5f)

                    val lineSpacing = (agendaH - headerH) / 5f
                    for (i in 1..4) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(w * 0.05f, y), Offset(w * 0.95f, y), strokeWidth = 1f)
                    }
                }
                "pdf" -> {
                    val headerH = h * 0.15f
                    drawRect(color = primaryLineColor.copy(alpha = 0.2f), topLeft = Offset(0f, 0f), size = Size(w, headerH))
                    val lineSpacing = (h - headerH) / 6f
                    for (i in 1..5) {
                        val y = headerH + i * lineSpacing
                        drawLine(gridLineColor, Offset(w * 0.1f, y), Offset(w * 0.9f, y), strokeWidth = 1f)
                    }
                }
                else -> {
                    val lineSpacing = h / 8f
                    for (i in 1..7) {
                        val y = i * lineSpacing
                        drawLine(
                            color = gridLineColor,
                            start = Offset(w * 0.08f, y),
                            end = Offset(w * 0.92f, y),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedTemplateDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSave: (templateType: String, coverType: String, pageColor: Long, coverTitle: String, coverSubtitle: String, coverAuthor: String, coverExtra: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Template Library, 1: My Templates
    var selectedMode by remember { mutableStateOf(0) } // 0: Cover, 1: Paper

    var currentCoverType by remember { mutableStateOf(note.coverType) }
    var currentTemplateType by remember { mutableStateOf(note.templateType) }
    var currentPageColor by remember { mutableStateOf(note.pageColor) }
    
    var coverTitle by remember { mutableStateOf(note.coverTitle.takeIf { it.isNotBlank() } ?: "Subject / Title") }
    var coverSubtitle by remember { mutableStateOf(note.coverSubtitle) }
    var coverAuthor by remember { mutableStateOf(note.coverAuthor.takeIf { it.isNotBlank() } ?: "Name") }
    var coverExtra by remember { mutableStateOf(note.coverExtra) }

    val pageColors = listOf(
        0xFFFFFFFF, // White
        0xFFFFF8E7, // Cream/Yellow
        0xFF1A1A1A, // Black
        0xFFF5F5F5, // Light Gray
        0xFFE3F2FD  // Light Blue
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(48.dp)) // Balance
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Template Library",
                            fontSize = 18.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.clickable { selectedTab = 0 }
                        )
                        Text(
                            text = "My Templates",
                            fontSize = 18.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.clickable { selectedTab = 1 }
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                Row(modifier = Modifier.weight(1f)) {
                    // Left Panel (Categories & Content)
                    Box(modifier = Modifier.weight(3f).fillMaxHeight()) {
                        if (selectedMode == 0) {
                            CoverSelectionPanel(
                                selectedCover = currentCoverType,
                                onCoverSelected = { currentCoverType = it },
                                coverTitle = coverTitle,
                                coverSubtitle = coverSubtitle,
                                coverAuthor = coverAuthor,
                                coverExtra = coverExtra,
                                onTitleChange = { coverTitle = it },
                                onSubtitleChange = { coverSubtitle = it },
                                onAuthorChange = { coverAuthor = it },
                                onExtraChange = { coverExtra = it }
                            )
                        } else {
                            PaperSelectionPanel(
                                selectedTemplate = currentTemplateType,
                                selectedColor = currentPageColor,
                                availableColors = pageColors,
                                onTemplateSelected = { currentTemplateType = it },
                                onColorSelected = { currentPageColor = it }
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

                    // Right Panel (Structure)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cover Item
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable { selectedMode = 0 },
                            border = BorderStroke(2.dp, if (selectedMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Cover", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 100.dp)
                                        .background(Color.White)
                                        .border(1.dp, Color.LightGray)
                                ) {
                                    // Mini preview of cover
                                    Text(currentCoverType, fontSize = 8.sp, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }

                        // Paper Item
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable { selectedMode = 1 },
                            border = BorderStroke(2.dp, if (selectedMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Paper", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 100.dp)
                                        .background(Color(currentPageColor))
                                        .border(1.dp, Color.LightGray)
                                        .clipToBounds()
                                ) {
                                    PageTemplateCanvasPreview(
                                        templateType = currentTemplateType,
                                        pageColor = currentPageColor,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = { onSave(currentTemplateType, currentCoverType, currentPageColor, coverTitle, coverSubtitle, coverAuthor, coverExtra) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoverSelectionPanel(
    coverTitle: String,
    coverSubtitle: String,
    coverAuthor: String,
    coverExtra: String,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onExtraChange: (String) -> Unit,
    selectedCover: String,
    onCoverSelected: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Subject Covers 📚") }
    val categories = listOf("Subject Covers 📚", "3D Covers 🔥", "Academic", "Journals", "Creative", "Basic", "Illustration")
    
    val covers = mapOf(
        "Subject Covers 📚" to listOf(
            "subject_math", "subject_gk_gs", "subject_current_affairs", "subject_reasoning",
            "subject_hindi", "subject_english", "subject_science", "subject_sst",
            "subject_sanskrit", "subject_computer", "subject_physics", "subject_chemistry",
            "subject_biology", "subject_history"
        ),
        "3D Covers 🔥" to listOf("3d_academic", "3d_journal", "3d_tech", "3d_creative", "3d_luxury", "3d_glass", "3d_nature", "3d_minimal"),
        "Academic" to listOf("science", "earth", "language", "english", "math"),
        "Journals" to listOf("journal", "daily"),
        "Creative" to listOf("treehouse"),
        "Basic" to listOf("none", "dark", "light"),
        "Illustration" to listOf("tiger", "reader", "sketch", "wash", "ink", "car")
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Categories
        LazyColumn(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(Color(0xFFF8F9FA))
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategory = cat }
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = cat,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                }
            }
        }

                // Cover Grid and Editor
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentCovers = covers[selectedCategory] ?: emptyList()
                items(currentCovers) { cover ->
                    val isSelected = selectedCover == cover
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "coverScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable { onCoverSelected(cover) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            RenderCover(
                                coverType = cover,
                                title = coverTitle,
                                subtitle = coverSubtitle,
                                author = coverAuthor,
                                extra = coverExtra,
                                modifier = Modifier.fillMaxSize().padding(if (isSelected) 3.dp else 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(cover.capitalize(), fontSize = 12.sp)
                    }
                }
            }
            
            // Editable Fields
            if (selectedCover != "none" && selectedCover != "dark" && selectedCover != "light" && selectedCategory != "Illustration") {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Customize Cover", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = coverTitle, onValueChange = onTitleChange, label = { Text("Title / Subject") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = coverAuthor, onValueChange = onAuthorChange, label = { Text("Author / Name") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = coverSubtitle, onValueChange = onSubtitleChange, label = { Text("Subtitle") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = coverExtra, onValueChange = onExtraChange, label = { Text("Extra (Year/Class)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }
    }
}

@Composable
fun PaperSelectionPanel(
    selectedTemplate: String,
    selectedColor: Long,
    availableColors: List<Long>,
    onTemplateSelected: (String) -> Unit,
    onColorSelected: (Long) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Standard") }
    val categories = listOf("Standard", "2-Column", "Legal")
    
    val templates = mapOf(
        "Standard" to listOf("blank", "grid", "ruled", "dotted", "square"),
        "2-Column" to listOf("2-col-blank", "2-col-ruled", "2-col-grid"),
        "Legal" to listOf("legal-ruled", "legal-blank")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Top options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rule:", fontWeight = FontWeight.Bold)
                availableColors.forEach { colorVal ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal))
                            .border(1.dp, Color.LightGray, CircleShape)
                            .clickable { onColorSelected(colorVal) }
                            .padding(2.dp)
                    ) {
                        if (selectedColor == colorVal) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
                IconButton(onClick = { /* Add custom color */ }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Color")
                }
                Text("Add More Colors", fontSize = 12.sp, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var changeAll by remember { mutableStateOf(false) }
                Checkbox(checked = changeAll, onCheckedChange = { changeAll = it })
                Text("Change All", fontSize = 14.sp)
                
                Button(onClick = { /* Replace current page action if needed */ }) {
                    Text("Replace Current Page")
                }
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            // Categories
            LazyColumn(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFF8F9FA))
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = cat }
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = cat,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                        )
                    }
                }
            }

            // Template Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentTemplates = templates[selectedCategory] ?: emptyList()
                items(currentTemplates) { template ->
                    val isSelected = selectedTemplate == template
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onTemplateSelected(template) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .background(Color(selectedColor))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            PageTemplateCanvasPreview(
                                templateType = template,
                                pageColor = selectedColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(template.capitalize(), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
