import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

fading_stroke = """data class FadingStroke(
    val stroke: Stroke,
    val createdAt: Long,
    val durationMs: Long
)

class NoteViewModel(application: Application) : AndroidViewModel(application) {"""

content = content.replace("class NoteViewModel(application: Application) : AndroidViewModel(application) {", fading_stroke)

fading_list = """    var activeShapeType by mutableStateOf("rectangle") // "pen", "highlighter", "eraser", "lasso"
    var laserInvisibleAfter by mutableStateOf(1.5f) // Time in seconds
    var laserDisappearOnLift by mutableStateOf(false)
    
    val fadingStrokes = androidx.compose.runtime.mutableStateListOf<FadingStroke>()"""

content = content.replace("""    var activeShapeType by mutableStateOf("rectangle") // "pen", "highlighter", "eraser", "lasso"
    var laserInvisibleAfter by mutableStateOf(1.5f) // Time in seconds
    var laserDisappearOnLift by mutableStateOf(false)""", fading_list)

laser_logic = """                    if (stroke.toolType == "laser") {
                        if (!laserDisappearOnLift) {
                            fadingStrokes.add(FadingStroke(stroke, System.currentTimeMillis(), (laserInvisibleAfter * 1000).toLong()))
                        }
                        activeStroke = null
                        return
                    }"""

content = content.replace("""                    if (stroke.toolType == "laser") {
                        activeStroke = null
                        return
                    }""", laser_logic)

# Also need a way to remove old strokes. We can do that in a LaunchedEffect in NoteEditorCanvas, 
# or a coroutine loop in NoteViewModel.
loop = """    init {
        startAutoSaveLoop()
        startFadingLoop()
"""

content = content.replace("    init {\n        startAutoSaveLoop()", loop)

loop_func = """    private fun startFadingLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(16L) // ~60fps
                if (fadingStrokes.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    fadingStrokes.removeAll { now - it.createdAt > it.durationMs }
                }
            }
        }
    }

    private fun startAutoSaveLoop() {"""

content = content.replace("    private fun startAutoSaveLoop() {", loop_func)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
