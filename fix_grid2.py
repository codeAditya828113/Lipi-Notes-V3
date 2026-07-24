import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Increase minSize to get roughly 3 columns
content = content.replace("GridCells.Adaptive(minSize = 140.dp)", "GridCells.Adaptive(minSize = 200.dp)")

# Change padding to match the video
content = content.replace("contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp)", "contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp)")
content = content.replace("horizontalArrangement = Arrangement.spacedBy(48.dp)", "horizontalArrangement = Arrangement.spacedBy(32.dp)")
content = content.replace("verticalArrangement = Arrangement.spacedBy(48.dp)", "verticalArrangement = Arrangement.spacedBy(40.dp)")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
