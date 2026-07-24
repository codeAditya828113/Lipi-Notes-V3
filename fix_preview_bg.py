with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace("background(Color.White)", "background(MaterialTheme.colorScheme.surface)")
content = content.replace("color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground", "color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
