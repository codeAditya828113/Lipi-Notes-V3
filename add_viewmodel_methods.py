import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

new_methods = """
    fun renameNote(note: NoteEntity, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(
                title = newTitle,
                lastModifiedTime = System.currentTimeMillis(),
                isSynced = false
            )
            repository.insertNote(updated)
            withContext(Dispatchers.Main) {
                if (selectedNote?.id == note.id) {
                    selectedNote = updated
                }
            }
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = note.copy(
                id = 0,
                title = note.title + " (Copy)",
                lastModifiedTime = System.currentTimeMillis(),
                isSynced = false
            )
            repository.insertNote(duplicate)
        }
    }

    fun deleteNote("""

content = content.replace("    fun deleteNote(", new_methods)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
