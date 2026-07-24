import re

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

# Fix worldX
old_worldX = "val worldX = (x - widthPx / 2f) / scale + widthPx / 2f"
new_worldX = "val worldX = (x - widthPx / 2f - offset.x) / scale + widthPx / 2f"
content = content.replace(old_worldX, new_worldX)

# Fix mappedX
old_mappedX = "val mappedX = (x - pivotX) / scale + pivotX"
new_mappedX = "val mappedX = (x - pivotX - offset.x) / scale + pivotX"
content = content.replace(old_mappedX, new_mappedX)

# Fix mHx (historical points)
old_mHx = "val mHx = (hx - pivotX) / scale + pivotX"
new_mHx = "val mHx = (hx - pivotX - offset.x) / scale + pivotX"
content = content.replace(old_mHx, new_mHx)

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
