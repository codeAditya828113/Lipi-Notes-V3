import re
with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

# Replace hardcoded Color.White and Color.Black with theme colors
content = content.replace("Color.White", "MaterialTheme.colorScheme.surface")
content = content.replace("Color.Black", "MaterialTheme.colorScheme.onSurface")

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
