import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Fix 1: onDragEnd color
content = content.replace(
    "color = MaterialTheme.colorScheme.onBackground.toArgb(),",
    "color = android.graphics.Color.BLACK,"
)

# Fix 2 & 3: drawPath color
content = content.replace(
    "color = MaterialTheme.colorScheme.onBackground,\n                                    style = DrawStroke",
    "color = Color.Black,\n                                    style = DrawStroke"
)
content = content.replace(
    "color = MaterialTheme.colorScheme.onBackground,\n                                style = DrawStroke",
    "color = Color.Black,\n                                style = DrawStroke"
)


with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
