import re

with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "r") as f:
    content = f.read()

target_block = """            // Right Panel (Tablet / Wide screens)
            if (isTablet) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .padding(top = 16.dp, end = 20.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = LipiCardWhite,
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp
                ) {
                    AllNotesRightPanel(
                        notes = notes,
                        viewModel = viewModel,
                        onSelectNote = onSelectNote,
                        onCreateNoteClick = onCreateNoteClick
                    )
                }
            }"""

if target_block in content:
    content = content.replace(target_block, "")
    with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "w") as f:
        f.write(content)
    print("Successfully removed Right Panel block")
else:
    print("Right Panel block not found")

