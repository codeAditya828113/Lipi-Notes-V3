import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Replace saveTimerState and loadTimerStateForActiveNote with global versions
# and track study time
old_timer = """    fun saveTimerState() {
        val note = selectedNote ?: return
        val noteId = note.id
        sharedPrefs.edit()
            .putInt("timer_remaining_$noteId", timerRemainingSeconds)
            .putInt("timer_total_$noteId", timerTotalSeconds)
            .putBoolean("timer_is_running_$noteId", timerIsRunning)
            .putLong("timer_last_active_$noteId", System.currentTimeMillis())
            .apply()
    }

    fun loadTimerStateForActiveNote() {
        val note = selectedNote
        if (note == null) {
            timerRemainingSeconds = 1500
            timerTotalSeconds = 1500
            timerIsRunning = false
            timerJob?.cancel()
            return
        }
        val noteId = note.id
        val total = sharedPrefs.getInt("timer_total_$noteId", 1500)
        val remaining = sharedPrefs.getInt("timer_remaining_$noteId", total)
        val isRunning = sharedPrefs.getBoolean("timer_is_running_$noteId", false)
        val lastActive = sharedPrefs.getLong("timer_last_active_$noteId", 0L)

        timerTotalSeconds = total
        
        if (isRunning && lastActive > 0L) {
            val elapsedSeconds = ((System.currentTimeMillis() - lastActive) / 1000).toInt()
            val newRemaining = remaining - elapsedSeconds
            if (newRemaining > 0) {
                timerRemainingSeconds = newRemaining
                timerIsRunning = true
                runTimerLoop()
            } else {
                timerRemainingSeconds = 0
                timerIsRunning = false
                saveTimerState()
            }
        } else {
            timerRemainingSeconds = remaining
            timerIsRunning = false
            timerJob?.cancel()
        }
    }"""

new_timer = """    var dailyStudySeconds by mutableStateOf(0)
    var studyStreakDays by mutableStateOf(0)
    private var lastStudyDateString = ""

    private fun getCurrentDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    private fun loadStudyStats() {
        val today = getCurrentDateString()
        lastStudyDateString = sharedPrefs.getString("last_study_date", "") ?: ""
        
        if (lastStudyDateString != today) {
            // New day, reset daily study seconds
            dailyStudySeconds = 0
            
            // Check if streak is broken (did not study yesterday)
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            
            studyStreakDays = sharedPrefs.getInt("study_streak_days", 0)
            if (lastStudyDateString != yesterday && lastStudyDateString.isNotEmpty()) {
                studyStreakDays = 0 // Streak broken
            }
        } else {
            dailyStudySeconds = sharedPrefs.getInt("daily_study_seconds", 0)
            studyStreakDays = sharedPrefs.getInt("study_streak_days", 0)
        }
    }

    private fun addStudySecond() {
        dailyStudySeconds++
        val today = getCurrentDateString()
        
        if (lastStudyDateString != today) {
            if (dailyStudySeconds == 1) {
                studyStreakDays++
            }
            lastStudyDateString = today
            sharedPrefs.edit()
                .putString("last_study_date", today)
                .putInt("study_streak_days", studyStreakDays)
                .apply()
        }
        
        if (dailyStudySeconds % 10 == 0) {
            // Save every 10 seconds to avoid too many writes
            sharedPrefs.edit().putInt("daily_study_seconds", dailyStudySeconds).apply()
        }
    }

    fun saveTimerState() {
        sharedPrefs.edit()
            .putInt("global_timer_remaining", timerRemainingSeconds)
            .putInt("global_timer_total", timerTotalSeconds)
            .putBoolean("global_timer_is_running", timerIsRunning)
            .putLong("global_timer_last_active", System.currentTimeMillis())
            .putInt("daily_study_seconds", dailyStudySeconds)
            .apply()
    }

    fun loadTimerStateForActiveNote() {
        // Loads global timer state
        loadStudyStats()
        
        val total = sharedPrefs.getInt("global_timer_total", 1500)
        val remaining = sharedPrefs.getInt("global_timer_remaining", total)
        val isRunning = sharedPrefs.getBoolean("global_timer_is_running", false)
        val lastActive = sharedPrefs.getLong("global_timer_last_active", 0L)

        timerTotalSeconds = total
        
        if (isRunning && lastActive > 0L) {
            val elapsedSeconds = ((System.currentTimeMillis() - lastActive) / 1000).toInt()
            val newRemaining = remaining - elapsedSeconds
            if (newRemaining > 0) {
                timerRemainingSeconds = newRemaining
                timerIsRunning = true
                runTimerLoop()
            } else {
                timerRemainingSeconds = 0
                timerIsRunning = false
                saveTimerState()
            }
        } else {
            timerRemainingSeconds = remaining
            timerIsRunning = false
            timerJob?.cancel()
        }
    }"""

content = content.replace(old_timer, new_timer)

# Add addStudySecond() call in runTimerLoop
old_loop = """    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timerIsRunning && timerRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
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
                kotlinx.coroutines.delay(1000)
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

