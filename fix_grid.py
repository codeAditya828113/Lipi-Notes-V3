import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Fix alignment in NoteList
content = content.replace(
    """                            textAlign = TextAlign.Center""",
    """                            textAlign = TextAlign.Start"""
)

content = content.replace(
    """Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center)""",
    """Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start)"""
)

# Add shadow to NoteCardPreview wrapper
content = content.replace(
    """                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f/4f)) {""",
    """                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f/4f).shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp)).background(Color.White)) {"""
)

# Fix NoteCardPreview border (remove it if there's a shadow, or keep it light)
# NoteCardPreview has Box(modifier = modifier.background(Color.White).border(BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(4.dp)))
content = content.replace(
    """.border(BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(4.dp))""",
    """"""
)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
