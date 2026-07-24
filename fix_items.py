import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

items_old = """            items(notes, key = { it.id }) { note ->
                val isSelected = selectedNote?.id == note.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_card_${note.id}")
                        .clickable { onSelect(note) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {"""

items_new = """            items(notes, key = { it.id }) { note ->
                val isSelected = selectedNote?.id == note.id
                var showContextMenu by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_card_${note.id}")
                        .combinedClickable(
                            onClick = { onSelect(note) },
                            onLongClick = { showContextMenu = true }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {"""

content = content.replace(items_old, items_new)

# Now we need to append the DropdownMenu and AlertDialog at the end of the Column
# The column ends exactly here:
#                         Text(
#                             text = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(note.lastModifiedTime)),
#                             fontSize = 11.sp,
#                             color = Color.Gray
#                         )
#                     }
#                 }
#             }

# Find that block
end_block = """                        Text(
                            text = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(note.lastModifiedTime)),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }"""

end_replacement = end_block + """
                    
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { 
                                showContextMenu = false
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = { 
                                showContextMenu = false
                                // Move functionality placeholder
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { 
                                showContextMenu = false
                                onDuplicate(note)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { 
                                showContextMenu = false
                                onDelete(note)
                            }
                        )
                    }
                    
                    if (showRenameDialog) {
                        var newTitle by remember { mutableStateOf(note.title) }
                        AlertDialog(
                            onDismissRequest = { showRenameDialog = false },
                            title = { Text("Rename Notebook") },
                            text = {
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newTitle.isNotBlank()) {
                                            onRename(note, newTitle)
                                        }
                                        showRenameDialog = false
                                    }
                                ) {
                                    Text("Rename")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRenameDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
"""

content = content.replace(end_block, end_replacement)

# ensure combinedClickable and DropdownMenu are imported
if "androidx.compose.foundation.combinedClickable" not in content:
    content = content.replace("import androidx.compose.foundation.clickable", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\nimport androidx.compose.material3.DropdownMenu\nimport androidx.compose.material3.DropdownMenuItem\nimport androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.TextButton\nimport androidx.compose.material3.OutlinedTextField")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
