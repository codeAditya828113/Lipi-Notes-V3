sed -i 's/val page: Int = 1/val page: Int = 1,\n    val isHidden: Boolean = false/g' app/src/main/java/com/example/data/DrawingModels.kt
sed -i 's/return "$color|$width|$toolType|$page|$pointsStr"/return "$color|$width|$toolType|$page|${if(isHidden) 1 else 0}|$pointsStr"/g' app/src/main/java/com/example/data/DrawingModels.kt
