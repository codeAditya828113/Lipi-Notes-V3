import re

with open("app/src/main/java/com/example/ui/components/PdfHelper.kt", "r") as f:
    content = f.read()

content = content.replace("strokePaint.strokeWidth = stroke.width * 0.3f", "strokePaint.strokeWidth = stroke.width * 0.25f")

with open("app/src/main/java/com/example/ui/components/PdfHelper.kt", "w") as f:
    f.write(content)
