sed -i 's/androidx.compose.foundation.gestures.detectTapGestures/detectTapGestures/g' app/src/main/java/com/example/ui/components/NoteinApp.kt
sed -i 's/import androidx.compose.ui.Modifier/import androidx.compose.ui.Modifier\nimport androidx.compose.foundation.gestures.detectTapGestures/g' app/src/main/java/com/example/ui/components/NoteinApp.kt
