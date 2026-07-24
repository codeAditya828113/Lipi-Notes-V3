import re

with open("app/src/main/java/com/example/ui/components/TemplateDialog.kt", "r") as f:
    content = f.read()

# Replace the categories and covers
old_covers = """    var selectedCategory by remember { mutableStateOf("Basic") }
    val categories = listOf("Basic", "Illustration", "Graphic", "Fruits")
    
    val covers = mapOf(
        "Basic" to listOf("none", "dark", "light"),
        "Illustration" to listOf("tiger", "reader", "sketch", "wash", "ink", "car"),
        "Graphic" to listOf("geo1", "geo2", "geo3"),
        "Fruits" to listOf("watermelon", "pineapple", "lemon")
    )"""

new_covers = """    var selectedCategory by remember { mutableStateOf("Academic") }
    val categories = listOf("Academic", "Journals", "Creative", "Basic", "Illustration")
    
    val covers = mapOf(
        "Academic" to listOf("science", "earth", "language", "english", "math"),
        "Journals" to listOf("journal", "daily"),
        "Creative" to listOf("treehouse"),
        "Basic" to listOf("none", "dark", "light"),
        "Illustration" to listOf("tiger", "reader", "sketch", "wash", "ink", "car")
    )"""

content = content.replace(old_covers, new_covers)

old_grid_item = """                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .background(Color.White)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                    ) {
                        Text(cover, modifier = Modifier.align(Alignment.Center))
                    }"""

new_grid_item = """                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        RenderCover(
                            coverType = cover,
                            title = "TITLE",
                            subtitle = "SUBTITLE",
                            author = "AUTHOR",
                            extra = "EXTRA",
                            modifier = Modifier.fillMaxSize().padding(if (isSelected) 3.dp else 1.dp)
                        )
                    }"""

content = content.replace(old_grid_item, new_grid_item)

with open("app/src/main/java/com/example/ui/components/TemplateDialog.kt", "w") as f:
    f.write(content)
