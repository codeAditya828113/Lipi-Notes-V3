import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# I already replaced one or all? The replace() replaces all occurrences.
# Let's check if the replacement actually worked for both.
print("Count of old_selection:", content.count('val isSelected = viewModel.activeToolType == toolId || (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint")'))
