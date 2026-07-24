import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace("listOf(4f, 10f, 22f)", "listOf(4f, 8f, 12f, 16f, 22f)")
content = content.replace("""                                val dotSize = when (width) {
                                    4f -> 4.dp
                                    10f -> 8.dp
                                    else -> 16.dp
                                }""", """                                val dotSize = when (width) {
                                    4f -> 4.dp
                                    8f -> 7.dp
                                    12f -> 10.dp
                                    16f -> 13.dp
                                    else -> 16.dp
                                }""")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
