import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

# I will replace exactly the occurrences of:
#         }
#         }
#     }
# }
# 
# @Composable
# With:
#         }
#     }
# }
# 
# @Composable

bad_pattern = "        }\n        }\n    }\n}\n\n@Composable"
good_pattern = "        }\n    }\n}\n\n@Composable"

content = content.replace(bad_pattern, good_pattern)

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
