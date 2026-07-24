import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

start_marker = "                // Responsive Tablet Sidebar"
end_marker = "@Composable\nfun CategoryFilterRow("

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

repaired = """                // Responsive Tablet Sidebar
                if (isTablet && !viewModel.isFullscreen) {
                    AnimatedVisibility(
                        visible = isSidebarExpanded,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        ResponsiveSidebar(
                            notes = notes,
                            viewModel = viewModel,
                            activeTab = activeTab,
                            onTabChange = { activeTab = it },
                            selectedFilter = selectedFilter,
                            onFilterChange = { selectedFilter = it },
                            searchKeyword = searchKeyword,
                            onSearchChange = { searchKeyword = it }
                        )
                    }
                }
                // Main Contents
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeTab) {
                        "home" -> {
                            NovaDashboard(
                                notes = notes,
                                viewModel = viewModel,
                                onNavigateToNotes = { activeTab = "notes" },
                                onMenuClick = { scope.launch { drawerState.open() } },
                                isTablet = isTablet
                            )
                        }
                        "notes" -> {
                            NoteWorkspace(
                                notes = notes,
                                selectedNote = selectedNote,
                                viewModel = viewModel,
                                isTablet = isTablet,
                                searchKeyword = searchKeyword,
                                onSearchChange = { searchKeyword = it },
                                onCreateNoteClick = { showCreateDialog = true },
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it },
                                isSidebarExpanded = isSidebarExpanded,
                                onToggleSidebar = { isSidebarExpanded = !isSidebarExpanded },
                                isNoteListExpanded = isNoteListExpanded,
                                onToggleNoteList = { isNoteListExpanded = !isNoteListExpanded },
                                onOpenMenu = { scope.launch { drawerState.open() } }
                            )
                        }
                        "sync" -> {
                            SyncDashboard(viewModel = viewModel)
                        }
                        "ai" -> {
                            AISummaryCenter(notes = notes, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateNoteDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, templateType ->
                viewModel.createNewNote(title, templateType)
                showCreateDialog = false
                activeTab = "notes" // Switch back to editor
            }
        )
    }
}

@Composable
fun NoteWorkspace(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    viewModel: NoteViewModel,
    isTablet: Boolean,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    isNoteListExpanded: Boolean,
    onToggleNoteList: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var pdfName = "Imported Document"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        pdfName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.importPdfToNote(uri, pdfName)
        }
    }

    val docxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var docxName = "Imported Document"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        docxName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.importDocxToNote(uri, docxName)
        }
    }

    // Filter notes based on both search query and horizontal active filters
    val filteredNotes = remember(notes, searchKeyword, selectedFilter) {
        val baseFiltered = if (searchKeyword.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(searchKeyword, ignoreCase = true) ||
                        note.content.contains(searchKeyword, ignoreCase = true) ||
                        (note.summary ?: "").contains(searchKeyword, ignoreCase = true)
            }
        }
        
        when (selectedFilter) {
            "Handwritten" -> baseFiltered.filter { it.templateType in listOf("blank", "ruled", "grid") }
            "PDFs" -> baseFiltered.filter { it.templateType == "pdf" || it.templateType == "docx" }
            "Templates" -> baseFiltered.filter { it.templateType in listOf("cornell", "meeting") }
            else -> baseFiltered
        }
    }

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
}

"""

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + repaired + content[end_idx:]
    with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
        f.write(content)
else:
    print(f"Could not find markers: {start_idx}, {end_idx}")
