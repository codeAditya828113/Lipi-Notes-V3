import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

content = content.replace("        }\n        }\n    }\n}\n\ndata class TaskItem", "        }\n    }\n}\n\ndata class TaskItem")
content = content.replace("        }\n        }\n    }\n}\n\ndata class SuggestionData", "        }\n    }\n}\n\ndata class SuggestionData")

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
