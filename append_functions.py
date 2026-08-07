with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "a") as f:
    f.write("""

@Composable
fun MoveToFolderDialog(
    note: com.example.data.NoteEntity,
    viewModel: com.example.ui.components.NoteViewModel,
    onDismissRequest: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    var showCreateNewFolderDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newFolderName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    if (showCreateNewFolderDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateNewFolderDialog = false },
            title = { androidx.compose.material3.Text("Create New Folder", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { androidx.compose.material3.Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.addDirectory(newFolderName)
                            onFolderSelected(newFolderName)
                            showCreateNewFolderDialog = false
                            onDismissRequest()
                        }
                    }
                ) { androidx.compose.material3.Text("Create & Move") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateNewFolderDialog = false }) { androidx.compose.material3.Text("Cancel") }
            }
        )
    }

    val availableFolders = androidx.compose.runtime.remember(viewModel.customDirectories) {
        val base = listOf("Projects", "School", "Personal", "Templates")
        val custom = viewModel.customDirectories.map { it.name }
        (base + custom).distinct()
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.DriveFileMove, contentDescription = null, tint = LipiPrimary)
                androidx.compose.material3.Text("Move \\"${note.title}\\"", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text("Select a folder destination:", color = LipiTextSecondary)
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = androidx.compose.ui.Modifier.heightIn(max = 240.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(availableFolders.size) { index ->
                        val folder = availableFolders[index]
                        androidx.compose.material3.Surface(
                            onClick = {
                                onFolderSelected(folder)
                                onDismissRequest()
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (note.directory == folder) LipiPrimary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                            ) {
                                androidx.compose.material3.Text(folder, fontWeight = if (note.directory == folder) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
                                if (note.directory == folder) {
                                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Current", tint = LipiPrimary, modifier = androidx.compose.ui.Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = { showCreateNewFolderDialog = true }) {
                androidx.compose.material3.Text("New Folder")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                androidx.compose.material3.Text("Cancel")
            }
        }
    )
}

@Composable
fun AllNotesEmptyState(onCreateNoteClick: () -> Unit, onImportPdfClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text("No notebooks found", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onCreateNoteClick) {
            androidx.compose.material3.Text("Create Note")
        }
    }
}
""")
