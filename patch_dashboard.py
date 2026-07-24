import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

old_recent = """    Card(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {"""

new_recent = """    Card(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            if (note.coverType != "none") {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    RenderCover(
                        coverType = note.coverType,
                        title = note.coverTitle,
                        subtitle = note.coverSubtitle,
                        author = note.coverAuthor,
                        extra = note.coverExtra,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {"""

# Replace old_recent with new_recent
content = content.replace(old_recent, new_recent)

# We also need to add a closing brace to the inner Column
# The original code has:
#         Column(
#             modifier = Modifier.padding(14.dp)
#         ) {
#             Row(...) { ... }
#             Spacer(...)
#             Text(...) // Title
#             Text(...) // Snippet
#             Spacer(...)
#             Row(...) { ... } // Date
#         }
#     }

content = content.replace("            }\n        }\n    }\n}", "            }\n        }\n        }\n    }\n}")

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
