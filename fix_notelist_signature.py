import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Replace the NoteList signature
old_sig = """fun NoteList(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    onSelect: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit
) {"""

new_sig = """@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteList(
    notes: List<NoteEntity>,
    selectedNote: NoteEntity?,
    onSelect: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onRename: (NoteEntity, String) -> Unit,
    onDuplicate: (NoteEntity) -> Unit
) {"""

content = content.replace(old_sig, new_sig)

# Also update the calls to NoteList
# In NoteWorkspace
content = content.replace(
"""                NoteList(
                    notes = filteredNotes,
                    selectedNote = null,
                    onSelect = { viewModel.selectNote(it) },
                    onDelete = { viewModel.deleteNote(it) }
                )""",
"""                NoteList(
                    notes = filteredNotes,
                    selectedNote = null,
                    onSelect = { viewModel.selectNote(it) },
                    onDelete = { viewModel.deleteNote(it) },
                    onRename = { note, newTitle -> viewModel.renameNote(note, newTitle) },
                    onDuplicate = { viewModel.duplicateNote(it) }
                )""")

# Are there other calls?
# Wait, check if there are other NoteList calls
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
