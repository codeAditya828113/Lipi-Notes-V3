import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

sig_old = """    strokes: List<Stroke>,
    images: List<com.example.data.ImageElement> = emptyList(),
    currentStroke: Stroke?,"""

sig_new = """    strokes: List<Stroke>,
    fadingStrokes: List<FadingStroke> = emptyList(),
    images: List<com.example.data.ImageElement> = emptyList(),
    currentStroke: Stroke?,"""

content = content.replace(sig_old, sig_new)

if "import com.example.ui.components.FadingStroke" not in content:
    content = content.replace("import com.example.data.Stroke", "import com.example.data.Stroke\nimport com.example.ui.components.FadingStroke")

# Now we need to render fadingStrokes!
# In DrawingCanvas.kt, we find where strokes are rendered.
#     // Render historical strokes
#     strokes.forEach { stroke ->
#         ...

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
