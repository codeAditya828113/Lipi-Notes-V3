import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

props_to_move = """    var dailyStudySeconds by mutableStateOf(0)
    var studyStreakDays by mutableStateOf(0)
    private var lastStudyDateString = ""
"""
content = content.replace(props_to_move, "")

init_loc = """    init {"""
new_init_loc = props_to_move + "\n" + init_loc

content = content.replace(init_loc, new_init_loc)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
