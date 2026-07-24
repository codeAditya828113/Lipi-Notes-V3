import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

new_header = """@Composable
fun NoteListHeader(
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onImportDocxClick: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onMenuClick: () -> Unit = {},
    isTablet: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        // App Title Bar and Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(24.dp).clickable { onMenuClick() }
                )
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = "Cloud",
                    modifier = Modifier.size(24.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "Select",
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(24.dp).clickable { /* toggle search bar later if needed */ }
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.align(Alignment.Center)) {
                val filters = listOf("All", "PDF", "Note", "Folder")
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter || (filter == "All" && selectedFilter == "All Notes")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onFilterSelected(if(filter=="All") "All Notes" else filter) }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 18.sp,
                            fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if(isSelected) Color(0xFF1976D2) else Color.Black
                        )
                        if (isSelected) {
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.width(40.dp).height(3.dp).background(Color(0xFF1976D2)))
                        } else {
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterEnd)) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val gradient = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue))
                    drawRoundRect(brush = gradient, cornerRadius = CornerRadius(4.dp.toPx()))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "View Mode",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort", fontSize = 14.sp)
                }
            }
        }
    }
}
"""

old_header_pattern = r"@Composable\nfun NoteListHeader\(.*?\n\}\n\}\n"
# Actually, I'll use text replacement by string matching lines 498-653
