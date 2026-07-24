import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_settings = """    // Pencil Settings
    var pencilRainbowEnabled by mutableStateOf(false)"""

new_settings = """    // Pencil Settings
    var pencilRainbowEnabled by mutableStateOf(false)
    
    // Pen Settings
    var inkFlow by mutableStateOf(100f)
    var pressureSensitivity by mutableStateOf(100f)"""

content = content.replace(old_settings, new_settings)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
