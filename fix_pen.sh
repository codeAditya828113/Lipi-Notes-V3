sed -i 's/onClick: () -> Unit/onClick: () -> Unit,\n    onDoubleTap: (() -> Unit)? = null/g' app/src/main/java/com/example/ui/components/NoteinApp.kt
sed -i 's/.clickable { onClick() }/.pointerInput(Unit) { androidx.compose.foundation.gestures.detectTapGestures(onTap = { onClick() }, onDoubleTap = { onDoubleTap?.invoke() }) }/g' app/src/main/java/com/example/ui/components/NoteinApp.kt
