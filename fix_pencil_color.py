with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace(
    'contentColor = Color(0xFF001E2F)',
    'contentColor = Color(0xFF1976D2)'
)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
