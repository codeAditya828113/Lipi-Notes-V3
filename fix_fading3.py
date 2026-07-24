import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Clean up duplicate imports
content = content.replace("import com.example.data.Stroke\nimport com.example.data.FadingStroke\nimport com.example.data.Stroke\nimport com.example.data.FadingStrokeSerializer\n", "import com.example.data.Stroke\n")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content2 = f.read()

content2 = content2.replace("fadingStrokes: List<com.example.ui.components.FadingStroke> = emptyList(),", "fadingStrokes: List<com.example.ui.components.FadingStroke> = emptyList(),") # wait no, FadingStroke is in com.example.ui.components

# Actually let's change NoteViewModel.kt so FadingStroke is in com.example.ui.components
