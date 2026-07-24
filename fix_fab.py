with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace("containerColor = Color(0xFFD3E3FD)", "containerColor = MaterialTheme.colorScheme.secondaryContainer")
content = content.replace("contentColor = MaterialTheme.colorScheme.primary", "contentColor = MaterialTheme.colorScheme.onSecondaryContainer")
content = content.replace("containerColor = Color(0xFF4285F4)", "containerColor = MaterialTheme.colorScheme.primaryContainer")
content = content.replace("contentColor = Color.White,\n                    shape = CircleShape", "contentColor = MaterialTheme.colorScheme.onPrimaryContainer,\n                    shape = CircleShape")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
