with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "r") as f:
    content = f.read()

# Add states for image selection
states_code = """
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var imageDragOffset by remember { mutableStateOf(Offset.Zero) }
    var imageResizeScale by remember { mutableStateOf(1f) }
    var activeImageInteraction by remember { mutableStateOf<String?>(null) } // "drag", "resize", null
"""

content = content.replace("    var isZooming by remember { mutableStateOf(false) }", states_code + "\n    var isZooming by remember { mutableStateOf(false) }")

with open("app/src/main/java/com/example/ui/components/DrawingCanvas.kt", "w") as f:
    f.write(content)
