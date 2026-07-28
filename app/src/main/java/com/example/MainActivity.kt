package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.ui.components.NoteViewModel
import com.example.ui.components.NoteViewModelFactory
import com.example.ui.components.NoteinApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Open in Full Display Mode: Hide status bar and navigation bar for whole screen experience
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    // Initialize local Room database and repository
    val database = AppDatabase.getDatabase(this)
    val repository = NoteRepository(database.noteDao())

    val viewModel: NoteViewModel by viewModels {
      NoteViewModelFactory(application, repository)
    }

    setContent {
      val isDark = when (viewModel.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
          NoteinApp(viewModel = viewModel)
        }
      }
    }
  }
}
