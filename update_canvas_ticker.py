import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

sig_old = """    strokes: List<Stroke>,
    fadingStrokes: List<FadingStroke> = emptyList(),
    images: List<com.example.data.ImageElement> = emptyList(),"""

sig_new = """    strokes: List<Stroke>,
    fadingStrokes: List<FadingStroke> = emptyList(),
    fadingTicker: Long = 0L,
    images: List<com.example.data.ImageElement> = emptyList(),"""

content = content.replace(sig_old, sig_new)

# Use fadingTicker in onDraw to force recomposition
draw_old = """        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter {"""

draw_new = """        val currentTicker = fadingTicker // read to force recompose
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter {"""

content = content.replace(draw_old, draw_new)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
