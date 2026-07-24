import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

content = content.replace("translate(0f, topOffset) {", "withTransform({ translate(0f, topOffset) }) {")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
