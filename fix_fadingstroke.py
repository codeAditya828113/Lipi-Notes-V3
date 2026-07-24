import re

# Remove from NoteViewModel
with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("""data class FadingStroke(
    val stroke: com.example.data.Stroke,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 3000L
)""", "")
content = content.replace("""data class FadingStroke(
    val stroke: Stroke,
    val createdAt: Long,
    val durationMs: Long
)""", "")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

# Add to DrawingModels.kt
with open("app/src/main/java/com/example/data/DrawingModels.kt", "r") as f:
    content = f.read()

new_class = """data class FadingStroke(
    val stroke: Stroke,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 3000L
)

data class Point("""

content = content.replace("data class Point(", new_class)

with open("app/src/main/java/com/example/data/DrawingModels.kt", "w") as f:
    f.write(content)
