import re
with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

start = content.find("fun NoteListHeader")
end = content.find("fun NoteEditorEmptyState")
if start != -1 and end != -1:
    print(content[start:end])
