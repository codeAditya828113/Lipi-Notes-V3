import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

bad_def = """@Composable
fun RealisticPenItem(
                            onDoubleTap = { showToolSettings = toolId },
    toolId: String,"""
good_def = """@Composable
fun RealisticPenItem(
    toolId: String,"""

content = content.replace(bad_def, good_def)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
