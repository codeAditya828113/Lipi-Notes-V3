import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

call_old = """                DrawingCanvas(
                    strokes = viewModel.currentStrokes,
                    images = viewModel.currentImages,"""

call_new = """                DrawingCanvas(
                    strokes = viewModel.currentStrokes,
                    fadingStrokes = viewModel.fadingStrokes,
                    fadingTicker = viewModel.fadingTicker,
                    images = viewModel.currentImages,"""

content = content.replace(call_old, call_new)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
