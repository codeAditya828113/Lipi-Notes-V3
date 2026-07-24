import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

lasso_creation = """            } else {
                clearLassoSelection()
                activeStroke = Stroke(
                    points = listOf(point),
                    color = 0xFF2196F3.toInt(), // Nice lasso blue
                    width = 3f,
                    toolType = "lasso","""

new_lasso_creation = """            } else {
                clearLassoSelection()
                activeStroke = Stroke(
                    points = listOf(point),
                    color = activeColor, // Uses the user-selected lasso color
                    width = 3f,
                    toolType = "lasso","""

content = content.replace(lasso_creation, new_lasso_creation)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

