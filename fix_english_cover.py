import re

with open("app/src/main/java/com/example/ui/components/CoverRenderer.kt", "r") as f:
    content = f.read()

content = content.replace("Modifier.align(Alignment.TopCenter).padding(top = h/4)", "Modifier.align(Alignment.Center)")

with open("app/src/main/java/com/example/ui/components/CoverRenderer.kt", "w") as f:
    f.write(content)
