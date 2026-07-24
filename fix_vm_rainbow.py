import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "var drawStraightLines by mutableStateOf(false)",
    "var drawStraightLines by mutableStateOf(false)\n    var pencilRainbowEnabled by mutableStateOf(false)"
)

old_creation = """            activeStroke = Stroke(
                points = listOf(point),
                color = activeColor,
                width = activeWidth,
                toolType = activeToolType,
                page = if (selectedNote?.templateType == "pdf" || selectedNote?.templateType == "docx") pdfPage else 1,
                fillShape = fillShapeEnabled,
                fillOpacity = fillShapeOpacity
            )"""

new_creation = """            activeStroke = Stroke(
                points = listOf(point),
                color = activeColor,
                width = activeWidth,
                toolType = activeToolType,
                page = if (selectedNote?.templateType == "pdf" || selectedNote?.templateType == "docx") pdfPage else 1,
                fillShape = fillShapeEnabled,
                fillOpacity = fillShapeOpacity,
                isRainbow = if (activeToolType == "pencil") pencilRainbowEnabled else false
            )"""

content = content.replace(old_creation, new_creation)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
