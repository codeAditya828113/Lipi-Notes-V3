with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    if line.startswith("fun ") or line.startswith("@Composable"):
        print(f"Line {i+1}: Brace count is {count} - {line.strip()}")
    count += line.count('{') - line.count('}')
