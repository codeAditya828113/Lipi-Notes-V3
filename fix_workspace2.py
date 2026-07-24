import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

start_idx = content.find("    if (isTablet && !viewModel.isFullscreen) {")
end_idx = content.find("}\n\n@Composable\nfun CategoryFilterRow")

new_block = """    if (isTablet && !viewModel.isFullscreen && selectedNote != null) {
        // Tablet Side-by-side Layout
        Row(modifier = Modifier.fillMaxSize()) {
            // Left list panel (320dp width) - collapsible with animation
            AnimatedVisibility(
                visible = isNoteListExpanded,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(0.dp)
                        )
                ) {
                    NoteListHeader(
                        searchKeyword = searchKeyword,
                        onSearchChange = onSearchChange,
                        onCreateNoteClick = onCreateNoteClick,
                        onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                        onImportDocxClick = { docxPickerLauncher.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") },
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected,
                        onMenuClick = onOpenMenu,
                        isTablet = true
                    )
                    NoteList(
                        notes = filteredNotes,
                        selectedNote = selectedNote,
                        onSelect = { viewModel.selectNote(it) },
                        onDelete = { viewModel.deleteNote(it) }
                    )
                }
            }
            // Right Editor Canvas panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NoteEditorCanvas(
                    viewModel = viewModel,
                    selectedNote = selectedNote,
                    notes = notes,
                    onCreateNoteClick = onCreateNoteClick,
                    onBackClick = null,
                    isSidebarExpanded = isSidebarExpanded,
                    onToggleSidebar = onToggleSidebar,
                    isNoteListExpanded = isNoteListExpanded,
                    onToggleNoteList = onToggleNoteList
                )
            }
        }
    } else {
        // Phone view or Full Screen Tablet
        if (selectedNote != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                NoteEditorCanvas(
                    viewModel = viewModel,
                    selectedNote = selectedNote,
                    notes = notes,
                    onCreateNoteClick = onCreateNoteClick,
                    onBackClick = { viewModel.selectNote(null) },
                    isSidebarExpanded = false,
                    onToggleSidebar = {},
                    isNoteListExpanded = true,
                    onToggleNoteList = {}
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    NoteListHeader(
                        searchKeyword = searchKeyword,
                        onSearchChange = onSearchChange,
                        onCreateNoteClick = onCreateNoteClick,
                        onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                        onImportDocxClick = { docxPickerLauncher.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") },
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected,
                        onMenuClick = onOpenMenu,
                        isTablet = false
                    )
                    NoteList(
                        notes = filteredNotes,
                        selectedNote = null,
                        onSelect = { viewModel.selectNote(it) },
                        onDelete = { viewModel.deleteNote(it) }
                    )
                }
                
                // Floating Action Buttons on bottom right
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { /* edit */ },
                        containerColor = Color(0xFFD3E3FD),
                        contentColor = Color(0xFF001E2F),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    FloatingActionButton(
                        onClick = onCreateNoteClick,
                        containerColor = Color(0xFF4285F4),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }"""

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_block + content[end_idx:]
    with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
        f.write(content)
else:
    print("Could not find block boundaries")
