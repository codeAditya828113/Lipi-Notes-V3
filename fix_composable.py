import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\n@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n@Composable\nfun NoteList", "@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n@Composable\nfun NoteList")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
