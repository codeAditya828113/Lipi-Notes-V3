import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

# We look for DashboardPomodoroWidget to find the end of RecentNoteCard
target = """            Text(
                text = "Last active: $dateString",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DashboardPomodoroWidget() {"""

replacement = """            Text(
                text = "Last active: $dateString",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        }
    }
}

@Composable
fun DashboardPomodoroWidget() {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)
