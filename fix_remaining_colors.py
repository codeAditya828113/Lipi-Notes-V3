import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Fix NoteListHeader filter text
content = content.replace(
    "color = if(isSelected) MaterialTheme.colorScheme.primary else Color.Black",
    "color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface"
)

# Fix NoteList gray text
content = content.replace(
    "color = Color.Gray",
    "color = MaterialTheme.colorScheme.onSurfaceVariant"
)

# Fix NoteList favorite icon
content = content.replace(
    "tint = Color.Gray.copy(alpha = 0.7f)",
    "tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)"
)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
