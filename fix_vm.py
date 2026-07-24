import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_vm = """    var activeToolType by mutableStateOf("pen")
    var activeShapeType by mutableStateOf("rectangle") // "pen", "highlighter", "eraser", "lasso\""""

new_vm = """    var activeToolType by mutableStateOf("pen")
    var activeShapeType by mutableStateOf("rectangle") // "pen", "highlighter", "eraser", "lasso"
    
    // Shape Fill Settings
    var fillShapeEnabled by mutableStateOf(false)
    var fillShapeOpacity by mutableStateOf(0.2f)"""

content = content.replace(old_vm, new_vm)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

