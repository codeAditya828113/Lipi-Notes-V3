import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Remove sharedPrefs from current location
old_prefs = """    private val sharedPrefs by lazy {
        application.getSharedPreferences("note_timer_prefs", android.content.Context.MODE_PRIVATE)
    }"""
content = content.replace(old_prefs, "")

# Insert sharedPrefs before init
old_init = """    init {"""
new_init = """    private val sharedPrefs by lazy {
        application.getSharedPreferences("note_timer_prefs", android.content.Context.MODE_PRIVATE)
    }

    init {"""
content = content.replace(old_init, new_init)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
