import re

with open("app/src/main/java/com/example/data/NoteEntity.kt", "r") as f:
    content = f.read()

new_fields = """    val coverType: String = "none",
    val pageColor: Long = 0xFFFFFFFF, // Default white
    val coverTitle: String = "",
    val coverSubtitle: String = "",
    val coverAuthor: String = "",
    val coverExtra: String = "",
    val pdfTitle: String? = null,"""

content = content.replace('    val coverType: String = "none",\n    val pageColor: Long = 0xFFFFFFFF, // Default white\n    val pdfTitle: String? = null,', new_fields)

with open("app/src/main/java/com/example/data/NoteEntity.kt", "w") as f:
    f.write(content)
