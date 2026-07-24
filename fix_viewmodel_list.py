import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "val fadingStrokes = mutableListOf<FadingStroke>()",
    "val fadingStrokes = androidx.compose.runtime.mutableStateListOf<com.example.data.FadingStroke>()"
)
content = content.replace("fadingStrokes.isNotEmpty()", "fadingStrokes.isNotEmpty()") # just to check

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
