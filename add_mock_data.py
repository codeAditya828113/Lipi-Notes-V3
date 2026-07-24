import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# We need to insert mock data when the ViewModel starts if notes are empty.
# But `allNotes` is a StateFlow that emits values.
# We can just do a check in init block.

mock_init = """    init {
        startAutoSaveLoop()
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getAllNotes().first().size
            if (count == 0) {
                // Insert mock notes to match the video
                val time = System.currentTimeMillis()
                repository.insertNote(NoteEntity(title = "Scratch paper", templateType = "ruled", lastModifiedTime = time))
                repository.insertNote(NoteEntity(title = "Scratch paper", templateType = "blank", lastModifiedTime = time - 1000))
                repository.insertNote(NoteEntity(title = "Deforestation Detection System", templateType = "blank", lastModifiedTime = time - 2000))
                repository.insertNote(NoteEntity(title = "Scratch paper", templateType = "blank", lastModifiedTime = time - 3000))
                repository.insertNote(NoteEntity(title = "Quick Start Guide", templateType = "blank", lastModifiedTime = time - 4000))
            }
        }
    }"""

content = content.replace(
    "    init {\n        startAutoSaveLoop()\n    }",
    mock_init
)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
