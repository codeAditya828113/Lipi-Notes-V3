import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_loop = """    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timerIsRunning && timerRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                if (timerRemainingSeconds > 0) {
                    timerRemainingSeconds--
                    saveTimerState()
                } else {
                    timerIsRunning = false
                    saveTimerState()
                }
            }
        }
    }"""

new_loop = """    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timerIsRunning && timerRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                if (timerRemainingSeconds > 0) {
                    timerRemainingSeconds--
                    addStudySecond()
                    if (timerRemainingSeconds % 10 == 0) {
                        saveTimerState()
                    }
                } else {
                    timerIsRunning = false
                    saveTimerState()
                }
            }
        }
    }"""

content = content.replace(old_loop, new_loop)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
