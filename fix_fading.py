import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Add FadingStroke definition if it doesn't exist
if "data class FadingStroke" not in content:
    content = content.replace(
        "import kotlinx.coroutines.withContext",
        "import kotlinx.coroutines.withContext\n\n"
        "data class FadingStroke(\n"
        "    val stroke: com.example.data.Stroke,\n"
        "    val createdAt: Long = System.currentTimeMillis(),\n"
        "    val durationMs: Long = 3000L\n"
        ")\n"
    )

# Make sure fadingStrokes is defined
if "val fadingStrokes = mutableListOf<FadingStroke>()" not in content:
    content = content.replace(
        "    var currentStrokes by mutableStateOf<List<Stroke>>(emptyList())",
        "    var currentStrokes by mutableStateOf<List<Stroke>>(emptyList())\n"
        "    val fadingStrokes = mutableListOf<FadingStroke>()\n"
        "    var fadingTicker by mutableStateOf(0L)\n"
    )

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content_canvas = f.read()

content_canvas = content_canvas.replace(
    "import com.example.ui.components.FadingStroke",
    "import com.example.ui.components.NoteViewModel\n"
)
content_canvas = content_canvas.replace(
    "fadingStrokes: List<FadingStroke> = emptyList(),",
    "fadingStrokes: List<com.example.ui.components.FadingStroke> = emptyList(),"
)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content_canvas)

