import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_tap1 = """                        RealisticPenItem(
                            onDoubleTap = { showToolSettings = toolId },"""

new_tap1 = """                        RealisticPenItem(
                            onDoubleTap = { showToolSettings = if (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint") "ballpoint" else toolId },"""

content = content.replace(old_tap1, new_tap1)

old_tap2 = """                    RealisticPenItem(
                        onDoubleTap = { onToolDoubleTap(toolId) },"""

new_tap2 = """                    RealisticPenItem(
                        onDoubleTap = { onToolDoubleTap(if (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint") "ballpoint" else toolId) },"""

content = content.replace(old_tap2, new_tap2)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
