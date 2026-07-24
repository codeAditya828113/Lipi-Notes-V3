import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Fix conflicting Stroke imports
content = content.replace("import com.example.data.Stroke\nimport com.example.data.FadingStroke", "import com.example.data.FadingStroke")
content = content.replace("import com.example.data.Stroke\nimport com.example.data.Stroke", "import com.example.data.Stroke")
content = content.replace("import com.example.data.FadingStrokeSerializer", "")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.components.FadingStroke", "import com.example.data.FadingStroke")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)

