import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# remove my FadingStroke class
content = content.replace("data class FadingStroke(\n    val stroke: com.example.data.Stroke,\n    val createdAt: Long = System.currentTimeMillis(),\n    val durationMs: Long = 3000L\n)", "")

# add the import back
content = content.replace("import com.example.data.StrokeSerializer\n", "import com.example.data.StrokeSerializer\nimport com.example.data.FadingStroke\n")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content2 = f.read()

# add import in DrawingCanvas.kt
if "import com.example.data.FadingStroke" not in content2:
    content2 = content2.replace("import com.example.data.Stroke\n", "import com.example.data.Stroke\nimport com.example.data.FadingStroke\n")

content2 = content2.replace("fadingStrokes: List<com.example.ui.components.FadingStroke> = emptyList(),", "fadingStrokes: List<FadingStroke> = emptyList(),")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content2)

