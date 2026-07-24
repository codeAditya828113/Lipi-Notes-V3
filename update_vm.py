import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Let's just append it after drawStraightLines
content = content.replace("var drawStraightLines by mutableStateOf(false)", "var drawStraightLines by mutableStateOf(false)\n    var inkFlow by mutableStateOf(100f)\n    var pressureSensitivity by mutableStateOf(100f)")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

