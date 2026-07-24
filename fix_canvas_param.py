import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

content = content.replace("    lassoBoundingBox: Rect? = null,", "    lassoBoundingBox: Rect? = null,\n    lassoSolidLine: Boolean = false,")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)

