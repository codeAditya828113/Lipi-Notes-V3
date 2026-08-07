import re

with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "r") as f:
    content = f.read()

content = content.replace("note.directory == folder", "note.tags.contains(folder, ignoreCase = true)")

with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "w") as f:
    f.write(content)
