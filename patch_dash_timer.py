import re

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "r") as f:
    content = f.read()

# Replace local timer state in Dashboard with viewModel's timer
old_timer_state = """    var isTimerRunning by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableStateOf(1500) } // Default 25 minutes

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
        }
        if (timeLeftSeconds == 0) {
            isTimerRunning = false
            timeLeftSeconds = 1500
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)"""

new_timer_state = """    val isTimerRunning = viewModel.timerIsRunning
    val timeLeftSeconds = viewModel.timerRemainingSeconds

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)"""

content = content.replace(old_timer_state, new_timer_state)

# Replace toggle timer action
old_timer_toggle = """onClick = { isTimerRunning = !isTimerRunning }"""
new_timer_toggle = """onClick = { if (isTimerRunning) viewModel.pauseTimer() else viewModel.startTimer() }"""
content = content.replace(old_timer_toggle, new_timer_toggle)

# Replace reset timer action
old_timer_reset = """onClick = {
                        isTimerRunning = false
                        timeLeftSeconds = 1500
                    }"""
new_timer_reset = """onClick = {
                        viewModel.resetTimer(1500)
                    }"""
content = content.replace(old_timer_reset, new_timer_reset)

with open("app/src/main/java/com/example/ui/components/Dashboard.kt", "w") as f:
    f.write(content)

