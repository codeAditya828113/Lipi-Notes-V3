with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "r") as f:
    content = f.read()

idx = content.find("/**\n * Right Panel displayed on wide tablet screens")
if idx == -1:
    idx = content.find("/**\n * Right Panel")

if idx != -1:
    content = content[:idx]
    with open("app/src/main/java/com/example/ui/components/AllNotesView.kt", "w") as f:
        f.write(content)
    print("Removed AllNotesRightPanel function")
else:
    print("Function not found")

