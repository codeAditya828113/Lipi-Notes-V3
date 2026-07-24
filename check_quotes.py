with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

count = 0
in_quote = False
escaped = False
for i, c in enumerate(content):
    if c == '\\':
        escaped = not escaped
    elif c == '"' and not escaped:
        in_quote = not in_quote
        escaped = False
    else:
        escaped = False

print(f"In quote at end: {in_quote}")
