with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

import re
# Replace NoteListHeader
old_header_pattern = r"fun NoteListHeader\(.*?\}\n\}\n"
# wait, better to use edit_file or simple replacement if possible. Let's find the exact string.
