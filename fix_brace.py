import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

content = content.replace(
"""            Text(
                text = "Last active: $dateString",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable""",
"""            Text(
                text = "Last active: $dateString",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        }
    }
}

@Composable""")

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
