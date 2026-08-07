import re

with open("app/src/main/java/com/example/ui/components/PdfHelper.kt", "r") as f:
    content = f.read()

# Replace `strokePaint.strokeWidth = stroke.width` with `strokePaint.strokeWidth = stroke.width * 0.5f`
content = content.replace("strokePaint.strokeWidth = stroke.width", "strokePaint.strokeWidth = stroke.width * 0.5f")

with open("app/src/main/java/com/example/ui/components/PdfHelper.kt", "w") as f:
    f.write(content)

print("Replaced stroke.width in PdfHelper.kt")
