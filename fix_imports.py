import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.components.FadingStroke", "import com.example.data.FadingStroke")
if "import com.example.data.FadingStroke" not in content:
    content = content.replace("import com.example.data.Stroke", "import com.example.data.Stroke\nimport com.example.data.FadingStroke")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

if "import com.example.data.FadingStroke" not in content:
    content = content.replace("import com.example.data.Stroke", "import com.example.data.Stroke\nimport com.example.data.FadingStroke")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
