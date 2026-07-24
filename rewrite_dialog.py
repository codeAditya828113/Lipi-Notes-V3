import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_dialog = re.search(r'    if \(showToolSettings != null\) \{.*?(?=^    }$)', content, re.DOTALL | re.MULTILINE)
if old_dialog:
    print("Found dialog block")
else:
    print("Could not find dialog block")

