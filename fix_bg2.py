with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace(
    'Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {',
    'Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {'
)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
