import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

old_stats = """                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Goal",
                    value = "85%",
                    icon = Icons.Default.TrendingUp,
                    tint = Color(0xFF00B0FF)
                )
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title = "Study Streak",
                    value = "7 Days 🔥",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = Color(0xFFFF5722)
                )"""

new_stats = """                val dailyGoalPercent = (viewModel.dailyStudySeconds * 100 / 3600).coerceAtMost(100)
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Goal",
                    value = "${dailyGoalPercent}%",
                    icon = Icons.Default.TrendingUp,
                    tint = Color(0xFF00B0FF)
                )
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title = "Study Streak",
                    value = "${viewModel.studyStreakDays} Days 🔥",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = Color(0xFFFF5722)
                )"""

content = content.replace(old_stats, new_stats)

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)

