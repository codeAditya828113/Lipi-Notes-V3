import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("noteDao.update(updated)", "repository.insertNote(updated)")
content = content.replace("loadNotes()", "")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
