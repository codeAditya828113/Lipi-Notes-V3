import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_creation = """            activeStroke = Stroke(
                points = listOf(point),
                color = activeColor,
                width = activeWidth,
                toolType = activeToolType,
                page = if (selectedNote?.templateType == "pdf" || selectedNote?.templateType == "docx") pdfPage else 1,
                fillShape = if (activeToolType == "shapes") fillShapeEnabled else false,
                fillOpacity = if (activeToolType == "shapes") fillShapeOpacity else 0f
            )"""

new_creation = """            activeStroke = Stroke(
                points = listOf(point),
                color = activeColor,
                width = activeWidth,
                toolType = activeToolType,
                page = if (selectedNote?.templateType == "pdf" || selectedNote?.templateType == "docx") pdfPage else 1,
                fillShape = fillShapeEnabled,
                fillOpacity = fillShapeOpacity
            )"""

content = content.replace(old_creation, new_creation)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

