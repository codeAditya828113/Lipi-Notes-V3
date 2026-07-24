sed -i 's/val (page, pointsStr) = if (parts.size >= 5) {/val (page, isHidden, pointsStr) = if (parts.size >= 6) {\n                    Triple(parts[3].toIntOrNull() ?: 1, parts[4] == "1", parts[5])\n                } else if (parts.size == 5) {/g' app/src/main/java/com/example/data/DrawingModels.kt
sed -i 's/Pair(parts\[3\].toIntOrNull() ?: 1, parts\[4\])/Triple(parts[3].toIntOrNull() ?: 1, false, parts[4])/g' app/src/main/java/com/example/data/DrawingModels.kt
sed -i 's/Pair(1, parts\[3\])/Triple(1, false, parts[3])/g' app/src/main/java/com/example/data/DrawingModels.kt
sed -i 's/Stroke(points, color, width, toolType, page)/Stroke(points, color, width, toolType, page, isHidden)/g' app/src/main/java/com/example/data/DrawingModels.kt
