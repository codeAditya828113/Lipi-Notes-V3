with open("app/src/main/java/com/example/ui/components/SmartInkEngine.kt", "r") as f:
    content = f.read()

new_func = """
    fun generateShape(stroke: Stroke, shapeType: String): Stroke {
        val points = stroke.points
        if (points.isEmpty()) return stroke
        val bounds = getBoundingBox(stroke)
        // If it's just a tap (small bounding box), make a default sized shape around it
        val (minX, minY, maxX, maxY) = if (bounds.right - bounds.left < 20f || bounds.bottom - bounds.top < 20f) {
            val cx = points.first().x
            val cy = points.first().y
            listOf(cx - 50f, cy - 50f, cx + 50f, cy + 50f)
        } else {
            listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)
        }
        
        val width = maxX - minX
        val height = maxY - minY
        
        val generatedPoints = mutableListOf<Point>()
        
        when (shapeType) {
            "rectangle", "square" -> {
                val finalW = if (shapeType == "square") minOf(width, height) else width
                val finalH = if (shapeType == "square") minOf(width, height) else height
                generatedPoints.add(Point(minX, minY))
                generatedPoints.add(Point(minX + finalW, minY))
                generatedPoints.add(Point(minX + finalW, minY + finalH))
                generatedPoints.add(Point(minX, minY + finalH))
                generatedPoints.add(Point(minX, minY))
            }
            "circle", "ellipse" -> {
                val cx = minX + width / 2f
                val cy = minY + height / 2f
                val rx = if (shapeType == "circle") minOf(width, height) / 2f else width / 2f
                val ry = if (shapeType == "circle") minOf(width, height) / 2f else height / 2f
                for (i in 0..48) {
                    val angle = (i / 48f) * 2 * Math.PI
                    generatedPoints.add(Point((cx + rx * cos(angle)).toFloat(), (cy + ry * sin(angle)).toFloat()))
                }
            }
            "cube" -> {
                // Draw a 3D cube isometric projection
                val size = minOf(width, height)
                val offset = size * 0.3f
                // Front face
                generatedPoints.add(Point(minX, minY + offset))
                generatedPoints.add(Point(minX + size, minY + offset))
                generatedPoints.add(Point(minX + size, minY + size + offset))
                generatedPoints.add(Point(minX, minY + size + offset))
                generatedPoints.add(Point(minX, minY + offset))
                // Top-left edge
                generatedPoints.add(Point(minX + offset, minY))
                // Top face
                generatedPoints.add(Point(minX + size + offset, minY))
                generatedPoints.add(Point(minX + size, minY + offset))
                generatedPoints.add(Point(minX + size + offset, minY)) // backtrack
                // Right face
                generatedPoints.add(Point(minX + size + offset, minY + size))
                generatedPoints.add(Point(minX + size, minY + size + offset))
                generatedPoints.add(Point(minX + size + offset, minY + size)) // backtrack
                // Back to Top-right
                generatedPoints.add(Point(minX + size + offset, minY))
            }
            "keyboard" -> {
                // Draw a simple keyboard rectangle with some key lines
                generatedPoints.add(Point(minX, minY))
                generatedPoints.add(Point(minX + width, minY))
                generatedPoints.add(Point(minX + width, minY + height))
                generatedPoints.add(Point(minX, minY + height))
                generatedPoints.add(Point(minX, minY))
                
                // Keys
                val cols = 5
                val rows = 3
                for (r in 1 until rows) {
                    val y = minY + (height / rows) * r
                    generatedPoints.add(Point(minX, y))
                    generatedPoints.add(Point(minX + width, y))
                    generatedPoints.add(Point(minX, y)) // return
                }
                for (c in 1 until cols) {
                    val x = minX + (width / cols) * c
                    generatedPoints.add(Point(x, minY))
                    generatedPoints.add(Point(x, minY + height))
                    generatedPoints.add(Point(x, minY)) // return
                }
            }
            else -> return detectAndCorrectShape(stroke)
        }
        
        return stroke.copy(points = generatedPoints, toolType = "pen") // Make it act like a regular pen stroke once generated
    }
"""

content = content.replace("fun detectAndCorrectShape", new_func + "\n    fun detectAndCorrectShape")

with open("app/src/main/java/com/example/ui/components/SmartInkEngine.kt", "w") as f:
    f.write(content)
