import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# I will find the bounds for CategoryFilterRow, NoteListHeader, NoteCardPreview, NoteList
start_idx = content.find("fun CategoryFilterRow")
if start_idx != -1:
    # Find the start of the @Composable before it
    start_idx = content.rfind("@Composable", 0, start_idx)

end_idx = content.find("fun AISummaryCenter")
if end_idx != -1:
    # Find the end of the previous @Composable
    end_idx = content.rfind("@Composable", 0, end_idx)

print(f"Start: {start_idx}, End: {end_idx}")
