import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Modify NoteCardPreview to show car icon if title is "Deforestation Detection System"
# and maybe a geometric icon if title is "Scratch paper" and templateType is blank?
# But wait, there are multiple "Scratch paper" notes.
# We can just match the title exactly for the car.

car_preview = """            "pdf" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Document",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }"""

replacement = """            "pdf" -> {
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
                if (note.title == "Deforestation Detection System") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car",
                            tint = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else if (note.title == "Quick Start Guide") {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFE0B2)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ImportContacts,
                            contentDescription = "Guide",
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else if (note.title == "Scratch paper" && note.templateType == "blank") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(size.width * 0.2f, size.height * 0.8f)
                            path.lineTo(size.width * 0.8f, size.height * 0.8f)
                            path.lineTo(size.width * 0.5f, size.height * 0.2f)
                            path.close()
                            drawPath(path, color = Color(0xFFFF5252).copy(alpha = 0.8f))
                        }
                    }
                } else {
                    // Blank page preview
                }
            }"""

# Remove the existing `else -> { // Blank page preview }`
content = content.replace("            else -> {\n                // Blank page preview\n            }", "")
content = content.replace(car_preview, replacement)

# ensure Icons.Default.DirectionsCar, ImportContacts are available
if "import androidx.compose.material.icons.filled.DirectionsCar" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Menu", "import androidx.compose.material.icons.filled.Menu\nimport androidx.compose.material.icons.filled.DirectionsCar\nimport androidx.compose.material.icons.filled.ImportContacts")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
