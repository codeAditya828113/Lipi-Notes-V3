import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Replace Color.Black in NoteList and NoteListHeader with MaterialTheme.colorScheme.onBackground
content = content.replace("color = Color.Black", "color = MaterialTheme.colorScheme.onBackground")
content = content.replace("tint = Color.Black", "tint = MaterialTheme.colorScheme.onBackground")

# Replace Color(0xFF1976D2) in NoteListHeader with MaterialTheme.colorScheme.primary
content = content.replace("Color(0xFF1976D2)", "MaterialTheme.colorScheme.primary")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
