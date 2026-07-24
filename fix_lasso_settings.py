import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

settings_code = """    // Lasso Selection States
    var lassoSelectedStrokes by mutableStateOf<List<Stroke>>(emptyList())
    var lassoDragOffset by mutableStateOf(Offset.Zero)
    var lassoBoundingBox by mutableStateOf<Rect?>(null)
    var isDraggingSelection by mutableStateOf(false)
    private var lastLassoDragPoint = Offset.Zero"""

new_settings_code = """    // Lasso Selection States
    var lassoSelectedStrokes by mutableStateOf<List<Stroke>>(emptyList())
    var lassoDragOffset by mutableStateOf(Offset.Zero)
    var lassoBoundingBox by mutableStateOf<Rect?>(null)
    var isDraggingSelection by mutableStateOf(false)
    private var lastLassoDragPoint = Offset.Zero
    
    // Lasso Filter Settings
    var lassoSelectPen by mutableStateOf(true)
    var lassoSelectShape by mutableStateOf(true)
    var lassoSelectHighlighter by mutableStateOf(true)
    var lassoSelectText by mutableStateOf(true)
    var lassoSelectImage by mutableStateOf(true)
    var lassoSolidLine by mutableStateOf(false)"""

content = content.replace(settings_code, new_settings_code)

old_lasso_filter = """                        val selected = pageStrokes.filter { s ->
                            SmartInkEngine.isStrokeInsideLasso(s, lassoPoints)
                        }"""

new_lasso_filter = """                        val selected = pageStrokes.filter { s ->
                            val toolAllowed = when (s.toolType) {
                                "pen" -> lassoSelectPen
                                "highlighter" -> lassoSelectHighlighter
                                "shapes" -> lassoSelectShape
                                else -> true // e.g. laser, eraser doesn't really matter
                            }
                            toolAllowed && SmartInkEngine.isStrokeInsideLasso(s, lassoPoints)
                        }"""

content = content.replace(old_lasso_filter, new_lasso_filter)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

