import re
with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

content = content.replace('tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp)', 'tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp)')

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
