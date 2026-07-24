import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_ended_laser = """                    if (stroke.toolType == "laser") {
                        if (laserMode == "line") {
                            val capturedStroke = activeStroke
                            if (capturedStroke != null) {
                                currentStrokes = currentStrokes + capturedStroke
                                if (laserDisappearEnabled) {
                                    viewModelScope.launch(Dispatchers.Main) {
                                        kotlinx.coroutines.delay(laserDisappearDelay)
                                        currentStrokes = currentStrokes - capturedStroke
                                    }
                                }
                            }
                        }
                        activeStroke = null
                        return
                    }"""

new_ended_laser = """                    if (stroke.toolType == "laser") {
                        if (laserMode == "line") {
                            val capturedStroke = activeStroke
                            if (capturedStroke != null) {
                                if (laserDisappearEnabled) {
                                    fadingStrokes.add(com.example.data.FadingStroke(capturedStroke, System.currentTimeMillis(), laserDisappearDelay))
                                } else {
                                    currentStrokes = currentStrokes + capturedStroke
                                }
                            }
                        }
                        activeStroke = null
                        return
                    }"""

content = content.replace(old_ended_laser, new_ended_laser)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

