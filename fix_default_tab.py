with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace('var activeTab by remember { mutableStateOf("home") }', 'var activeTab by remember { mutableStateOf("notes") }')

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
