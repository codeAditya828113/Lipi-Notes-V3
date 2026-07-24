import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Add to NoteListHeader
content = content.replace("onMenuClick: () -> Unit = {},", "onMenuClick: () -> Unit = {},\n    onHomeClick: () -> Unit = {},")
content = content.replace(
"""            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp)
            )""",
"""            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp).clickable { onHomeClick() }
            )""")

# Add to NoteWorkspace signature
content = content.replace("onToggleNoteList: () -> Unit,", "onToggleNoteList: () -> Unit,\n    onHomeClick: () -> Unit,")

# Add when NoteWorkspace is called in NoteinApp
content = content.replace("onToggleNoteList = { isNoteListExpanded = !isNoteListExpanded },", "onToggleNoteList = { isNoteListExpanded = !isNoteListExpanded },\n                                onHomeClick = { activeTab = \"home\" },")

# Add when NoteListHeader is called inside NoteWorkspace
content = content.replace("onMenuClick = onOpenMenu,", "onMenuClick = onOpenMenu,\n                        onHomeClick = onHomeClick,")


with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
