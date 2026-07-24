import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Replace updatedAt with lastModifiedTime
content = content.replace("note.updatedAt", "note.lastModifiedTime")

# Remove the strokes block from NoteCardPreview.
# It starts around: if (note.strokes.isNotEmpty()) {
# and ends after drawing the path
strokes_block = """        // Add some scribbles to preview handwriting
        if (note.strokes.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                note.strokes.take(5).forEach { stroke ->
                    val path = Path()
                    if (stroke.points.isNotEmpty()) {
                        val first = stroke.points.first()
                        path.moveTo(first.x * size.width / 600f, first.y * size.height / 800f)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            path.lineTo(pt.x * size.width / 600f, pt.y * size.height / 800f)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF0061A4).copy(alpha = 0.4f),
                        style = DrawStroke(
                            width = 2.5f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }"""
content = content.replace(strokes_block, "")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
