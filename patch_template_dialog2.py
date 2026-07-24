import re

with open("app/src/main/java/com/example/ui/components/TemplateDialog.kt", "r") as f:
    content = f.read()

# Change onSave signature
content = content.replace(
    "onSave: (templateType: String, coverType: String, pageColor: Long) -> Unit",
    "onSave: (templateType: String, coverType: String, pageColor: Long, coverTitle: String, coverSubtitle: String, coverAuthor: String, coverExtra: String) -> Unit"
)

# In AdvancedTemplateDialog, add state for fields
state_vars = """    var currentPageColor by remember { mutableStateOf(note.pageColor) }
    
    var coverTitle by remember { mutableStateOf(note.coverTitle.takeIf { it.isNotBlank() } ?: "Subject / Title") }
    var coverSubtitle by remember { mutableStateOf(note.coverSubtitle) }
    var coverAuthor by remember { mutableStateOf(note.coverAuthor.takeIf { it.isNotBlank() } ?: "Name") }
    var coverExtra by remember { mutableStateOf(note.coverExtra) }"""

content = content.replace("    var currentPageColor by remember { mutableStateOf(note.pageColor) }", state_vars)

# Update onSave call
content = content.replace(
    "onSave(currentTemplateType, currentCoverType, currentPageColor)",
    "onSave(currentTemplateType, currentCoverType, currentPageColor, coverTitle, coverSubtitle, coverAuthor, coverExtra)"
)

# Update CoverSelectionPanel signature
content = content.replace(
    "fun CoverSelectionPanel(",
    "fun CoverSelectionPanel(\n    coverTitle: String,\n    coverSubtitle: String,\n    coverAuthor: String,\n    coverExtra: String,\n    onTitleChange: (String) -> Unit,\n    onSubtitleChange: (String) -> Unit,\n    onAuthorChange: (String) -> Unit,\n    onExtraChange: (String) -> Unit,"
)

# In AdvancedTemplateDialog, pass fields to CoverSelectionPanel
content = content.replace(
    "CoverSelectionPanel(\n                                selectedCover = currentCoverType,\n                                onCoverSelected = { currentCoverType = it }\n                            )",
    """CoverSelectionPanel(
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
                            )"""
)

# We need to add the TextFields to CoverSelectionPanel. Let's do that at the bottom of CoverSelectionPanel.
new_grid = """        // Cover Grid and Editor
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCoverSelected(cover) }
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
        }"""

# we need to replace the original Cover Grid with this new_grid.
old_grid_regex = re.compile(r"// Cover Grid.*?LazyVerticalGrid.*?}\n\s*}\n\s*}", re.DOTALL)
content = old_grid_regex.sub(new_grid, content)

with open("app/src/main/java/com/example/ui/components/TemplateDialog.kt", "w") as f:
    f.write(content)
