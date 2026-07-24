import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# We need to replace the entire `if (showToolSettings != null)` block.
# Let's find the start and end of it.
