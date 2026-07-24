import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_canvas = """        if (note.coverType != "none") {
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                val primary = Color(0xFF3B82F6)
                val secondary = Color(0xFFF43F5E)
                when(note.coverType) {
                    "dark" -> drawRect(Color.DarkGray)
                    "light" -> drawRect(Color.LightGray)
                    "tiger", "reader", "sketch", "wash", "ink", "car" -> {
                        drawRect(Color(0xFFE2E8F0))
                        drawCircle(primary, size.height * 0.4f, center = Offset(size.width * 0.5f, size.height * 0.5f))
                    }
                    "geo1", "geo2", "geo3" -> {
                        drawRect(Color(0xFFF1F5F9))
                        drawRect(secondary, size = Size(size.width * 0.5f, size.height))
                    }
                    "watermelon", "pineapple", "lemon" -> {
                        drawRect(Color(0xFFFEF3C7))
                        drawCircle(Color(0xFF10B981), size.height * 0.3f)
                    }
                    else -> drawRect(Color(0xFFE2E8F0))
                }
            }
        }"""

new_canvas = """        if (note.coverType != "none") {
            RenderCover(
                coverType = note.coverType,
                title = note.coverTitle,
                subtitle = note.coverSubtitle,
                author = note.coverAuthor,
                extra = note.coverExtra,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
            )
        }"""

content = content.replace(old_canvas, new_canvas)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
