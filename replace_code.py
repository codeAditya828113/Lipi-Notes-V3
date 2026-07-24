import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    lines = f.readlines()

new_code = """@Composable
fun CategoryFilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Replaced by tabs in NoteListHeader
}

@Composable
fun NoteListHeader(
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onImportDocxClick: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onMenuClick: () -> Unit = {},
    isTablet: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        // App Title Bar and Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(24.dp).clickable { onMenuClick() }
                )
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = "Cloud",
                    modifier = Modifier.size(24.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "Select",
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(24.dp).clickable { /* search */ }
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.align(Alignment.Center)) {
                val filters = listOf("All", "PDF", "Note", "Folder")
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter || (filter == "All" && selectedFilter == "All Notes")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onFilterSelected(if(filter=="All") "All Notes" else filter) }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 18.sp,
                            fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if(isSelected) Color(0xFF1976D2) else Color.Black
                        )
                        if (isSelected) {
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.width(40.dp).height(3.dp).background(Color(0xFF1976D2)))
                        } else {
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterEnd)) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val gradient = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue))
                    drawRoundRect(brush = gradient, cornerRadius = CornerRadius(4.dp.toPx()))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "View Mode",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun NoteCardPreview(note: NoteEntity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(note.pageColor), RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (note.coverType != "none") {
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                val primary = Color(0xFF3B82F6)
                val secondary = Color(0xFFF43F5E)
                when(note.coverType) {
                    "dark" -> drawRect(Color.DarkGray)
                    "light" -> drawRect(Color.LightGray)
                    "tiger", "reader", "sketch", "wash", "ink", "car" -> {
                        drawRect(Color(0xFFE2E8F0))
                        drawCircle(primary, size.height * 0.4f, center = Offset(size.width * 0.5f, size.height * 0.5f))
                    }
                    "geo1", "geo2", "geo3" -> {
                        drawRect(Color(0xFFF1F5F9))
                        drawRect(secondary, size = Size(size.width * 0.5f, size.height))
                    }
                    "watermelon", "pineapple", "lemon" -> {
                        drawRect(Color(0xFFFEF3C7))
                        drawCircle(Color(0xFF10B981), size.height * 0.3f)
                    }
                    else -> drawRect(Color(0xFFE2E8F0))
                }
            }
        } else {
            when (note.templateType) {
                "grid" -> {
                    Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                        val step = 10.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = Color(0xFFC4C7CF).copy(alpha = 0.3f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = Color(0xFFC4C7CF).copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }
                }
                "ruled" -> {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    val step = 12.dp.toPx()
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF93C5FD).copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f
                        )
                        y += step
                    }
                }
            }
            "cornell" -> {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    val cueX = size.width * 0.3f
                    drawLine(
                        color = Color(0xFFC4C7CF).copy(alpha = 0.7f),
                        start = Offset(cueX, 0f),
                        end = Offset(cueX, size.height * 0.75f),
                        strokeWidth = 1.5f
                    )
                    val summaryY = size.height * 0.75f
                    drawLine(
                        color = Color(0xFFC4C7CF).copy(alpha = 0.7f),
                        start = Offset(0f, summaryY),
                        end = Offset(size.width, summaryY),
                        strokeWidth = 1.5f
                    )
                }
            }
            "meeting" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF1F5F9)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF1F5F9)))
                    }
                }
            }
            "pdf" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Document",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            else -> {
                // Blank page preview
            }
            }
        }
        
        // Add some scribbles to preview handwriting
        if (note.strokes.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                note.strokes.take(5).forEach { stroke ->
                    val path = Path()
                    if (stroke.points.isNotEmpty()) {
                        val first = stroke.points.first()
                        path.moveTo(first.x * size.width / 600f, first.y * size.height / 800f)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            path.lineTo(pt.x * size.width / 600f, pt.y * size.height / 800f)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF0061A4).copy(alpha = 0.4f),
                        style = DrawStroke(
                            width = 2.5f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NoteList(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    onSelect: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ListAlt,
                    contentDescription = "No Notes",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No notes found",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                val isSelected = selectedNote?.id == note.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_card_${note.id}")
                        .clickable { onSelect(note) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f/4f)) {
                         NoteCardPreview(note = note, modifier = Modifier.fillMaxSize())
                         Icon(
                             imageVector = Icons.Default.FavoriteBorder,
                             contentDescription = "Favorite",
                             modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                             tint = Color.Gray.copy(alpha = 0.7f)
                         )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = note.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = "${(note.id % 20) + 1}P",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(note.updatedAt)),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.writelines(lines[:461])
    f.write(new_code)
    f.writelines(lines[962:])
