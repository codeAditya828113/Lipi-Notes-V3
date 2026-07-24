import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Remove data class FadingStroke from imports
bad_block = """import kotlinx.coroutines.withContext

data class FadingStroke(
    val stroke: com.example.data.Stroke,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 3000L
)
"""
content = content.replace(bad_block, "import kotlinx.coroutines.withContext\n")

# Add FadingStroke to the end of the file
if "data class FadingStroke(" not in content:
    content = content + "\n\ndata class FadingStroke(\n    val stroke: com.example.data.Stroke,\n    val createdAt: Long = System.currentTimeMillis(),\n    val durationMs: Long = 3000L\n)\n"

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
