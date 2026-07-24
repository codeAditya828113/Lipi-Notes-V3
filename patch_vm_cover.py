import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

new_func = """    fun updateCoverInfo(title: String, subtitle: String, author: String, extra: String) {
        val currentNote = selectedNote ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val updated = currentNote.copy(
                coverTitle = title,
                coverSubtitle = subtitle,
                coverAuthor = author,
                coverExtra = extra,
                lastModifiedTime = System.currentTimeMillis(),
                isSynced = false
            )
            noteDao.update(updated)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                selectedNote = updated
                loadNotes()
            }
        }
    }
"""

content = content.replace("fun updateNoteDesign", new_func + "\n    fun updateNoteDesign")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
