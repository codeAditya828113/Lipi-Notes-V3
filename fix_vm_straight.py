import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_settings = """    // Shape Fill Settings
    var fillShapeEnabled by mutableStateOf(false)
    var fillShapeOpacity by mutableStateOf(0.2f)"""

new_settings = """    // Shape Fill Settings
    var fillShapeEnabled by mutableStateOf(false)
    var fillShapeOpacity by mutableStateOf(0.2f)
    
    // Magic Settings
    var drawStraightLines by mutableStateOf(false)"""

content = content.replace(old_settings, new_settings)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

