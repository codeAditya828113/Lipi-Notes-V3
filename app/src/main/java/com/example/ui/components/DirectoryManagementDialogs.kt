package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DirectoryItem
import com.example.data.NoteEntity
import com.example.data.TagItem

val COLOR_PRESETS = listOf(
    0xFF1976D2 to "Blue",
    0xFF0288D1 to "Cyan",
    0xFF388E3C to "Green",
    0xFF4CAF50 to "Light Green",
    0xFF7B1FA2 to "Purple",
    0xFFE91E63 to "Pink",
    0xFFD32F2F to "Red",
    0xFFF57C00 to "Orange",
    0xFFFBC02D to "Yellow",
    0xFF009688 to "Teal",
    0xFF607D8B to "Slate"
)

@Composable
fun DirectoryEditDialog(
    initialDirectory: DirectoryItem? = null,
    defaultParentId: String? = null,
    allDirectories: List<DirectoryItem>,
    onDismiss: () -> Unit,
    onSave: (name: String, parentId: String?, colorHex: Long) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialDirectory?.name ?: "") }
    var parentId by remember { mutableStateOf(initialDirectory?.parentId ?: defaultParentId) }
    var selectedColorHex by remember { mutableStateOf(initialDirectory?.colorHex ?: 0xFF1976D2) }
    var showParentDropdown by remember { mutableStateOf(false) }

    // Filter out valid parent candidates (cannot be self or child of self)
    val validParentCandidates = remember(allDirectories, initialDirectory) {
        if (initialDirectory == null) allDirectories
        else allDirectories.filter { it.id != initialDirectory.id && it.parentId != initialDirectory.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (initialDirectory == null) Icons.Default.CreateNewFolder else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(selectedColorHex)
            )
        },
        title = {
            Text(
                text = if (initialDirectory == null) "Add New Directory" else "Edit Directory",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Directory Name") },
                    placeholder = { Text("e.g. Work Projects, Math Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Parent Directory Selector (For Nesting)
                Column {
                    Text(
                        text = "NESTING LEVEL / PARENT DIRECTORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val parentName = validParentCandidates.find { it.id == parentId }?.name ?: "Root Directory (Top-Level)"
                        OutlinedButton(
                            onClick = { showParentDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (parentId == null) Icons.Default.FolderSpecial else Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = parentName,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📁 Root Directory (Top-Level)") },
                                onClick = {
                                    parentId = null
                                    showParentDropdown = false
                                }
                            )
                            validParentCandidates.forEach { dir ->
                                DropdownMenuItem(
                                    text = { Text("↳ 📁 ${dir.name}") },
                                    onClick = {
                                        parentId = dir.id
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Color Text & Icon Swatch Picker
                Column {
                    Text(
                        text = "DIRECTORY COLOR & TEXT ACCENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        COLOR_PRESETS.take(6).forEach { (hex, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        COLOR_PRESETS.drop(6).forEach { (hex, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), parentId, selectedColorHex)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Directory")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialDirectory != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(initialDirectory.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun TagEditDialog(
    initialTag: TagItem? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: Long, textColorHex: Long) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialTag?.colorHex ?: 0xFF6200EE) }
    var selectedTextColorHex by remember { mutableStateOf(initialTag?.textColorHex ?: 0xFFFFFFFF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Label,
                contentDescription = null,
                tint = Color(selectedColorHex)
            )
        },
        title = {
            Text(
                text = if (initialTag == null) "Add Custom Tag" else "Edit Tag",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g. urgent, exam, meeting") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Live Preview Tag Badge with Colored Text!
                Column {
                    Text(
                        text = "TAG PREVIEW (COLORED TEXT)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(selectedColorHex))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#${name.ifBlank { "preview" }}",
                            color = Color(selectedTextColorHex),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Background Color Picker
                Column {
                    Text(
                        text = "TAG BADGE COLOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        COLOR_PRESETS.take(6).forEach { (hex, _) ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }

                // Text Color Picker
                Column {
                    Text(
                        text = "TAG TEXT COLOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            0xFFFFFFFF to "White",
                            0xFF000000 to "Black",
                            0xFFFFEB3B to "Yellow",
                            0xFFE0E0E0 to "Light Gray",
                            0xFF80D8FF to "Light Blue",
                            0xFFFF80AB to "Pink"
                        ).forEach { (hex, _) ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selectedTextColorHex == hex) 2.dp else 1.dp,
                                        color = if (selectedTextColorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedTextColorHex = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedColorHex, selectedTextColorHex)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Tag")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialTag != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(initialTag.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun AssignDirectoryAndTagsDialog(
    note: NoteEntity,
    directories: List<DirectoryItem>,
    tags: List<TagItem>,
    onDismiss: () -> Unit,
    onUpdateTags: (newTags: String) -> Unit,
    onAddNewDirectory: () -> Unit,
    onAddNewTag: () -> Unit
) {
    var noteTagsString by remember { mutableStateOf(note.tags) }

    val currentTagsList = remember(noteTagsString) {
        noteTagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(
                    text = "Assign Directory & Tags",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = note.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: DIRECTORIES
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NESTED DIRECTORIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = onAddNewDirectory,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add Directory",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                items(directories) { dir ->
                    val dirTag = "dir:${dir.id}"
                    val isChecked = currentTagsList.contains(dirTag) || currentTagsList.contains(dir.name)
                    val parentDir = directories.find { it.id == dir.parentId }
                    val indent = if (parentDir != null) 20.dp else 0.dp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val updatedSet = if (isChecked) {
                                    currentTagsList - dirTag - dir.name
                                } else {
                                    currentTagsList + dirTag
                                }
                                noteTagsString = updatedSet.joinToString(", ")
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val updatedSet = if (!checked) {
                                    currentTagsList - dirTag - dir.name
                                } else {
                                    currentTagsList + dirTag
                                }
                                noteTagsString = updatedSet.joinToString(", ")
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (parentDir != null) Icons.Default.SubdirectoryArrowRight else Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(dir.colorHex),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = dir.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(dir.colorHex)
                            )
                            if (parentDir != null) {
                                Text(
                                    text = "inside ${parentDir.name}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Section 2: COLORED TAGS
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLORED TAGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = onAddNewTag,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Tag",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                items(tags) { tag ->
                    val tagKey = "tag:${tag.name}"
                    val isChecked = currentTagsList.contains(tagKey) || currentTagsList.contains(tag.name)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val updatedSet = if (isChecked) {
                                    currentTagsList - tagKey - tag.name
                                } else {
                                    currentTagsList + tagKey
                                }
                                noteTagsString = updatedSet.joinToString(", ")
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val updatedSet = if (!checked) {
                                    currentTagsList - tagKey - tag.name
                                } else {
                                    currentTagsList + tagKey
                                }
                                noteTagsString = updatedSet.joinToString(", ")
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(tag.colorHex))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#${tag.name}",
                                color = Color(tag.textColorHex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateTags(noteTagsString)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
