import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

# Add a ticker to force recomposition
ticker_def = """    val fadingStrokes = androidx.compose.runtime.mutableStateListOf<FadingStroke>()
    var fadingTicker by mutableStateOf(0L)"""

content = content.replace("    val fadingStrokes = androidx.compose.runtime.mutableStateListOf<FadingStroke>()", ticker_def)

# Update ticker in loop
loop_func = """    private fun startFadingLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(16L) // ~60fps
                if (fadingStrokes.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    fadingStrokes.removeAll { now - it.createdAt > it.durationMs }
                    fadingTicker = now
                }
            }
        }
    }"""

content = content.replace("""    private fun startFadingLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(16L) // ~60fps
                if (fadingStrokes.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    fadingStrokes.removeAll { now - it.createdAt > it.durationMs }
                }
            }
        }
    }""", loop_func)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
