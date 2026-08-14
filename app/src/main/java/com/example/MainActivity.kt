package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.ui.components.NoteViewModel
import com.example.ui.components.NoteViewModelFactory
import com.example.ui.components.NoteinApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: NoteViewModel by viewModels {
    val database = AppDatabase.getDatabase(this)
    val repository = NoteRepository(database.noteDao())
    NoteViewModelFactory(application, repository)
  }

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
    
    handleIntent(intent)
    com.example.sync.AutoSyncWorker.schedulePeriodicAutoSync(this, 1)

    setContent {
      val isDark = when (viewModel.themeMode) {
        "dark", "oled" -> true
        "light" -> false
        else -> false
      }
      val isOled = viewModel.themeMode == "oled"
      MyApplicationTheme(
        darkTheme = isDark,
        isOled = isOled,
        dynamicColor = viewModel.dynamicColorEnabled
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          NoteinApp(viewModel = viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent) {
    if (intent.action == android.content.Intent.ACTION_VIEW || intent.action == android.content.Intent.ACTION_SEND) {
      if (intent.type == "application/pdf" || intent.type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") {
        val uri: android.net.Uri? = if (intent.action == android.content.Intent.ACTION_SEND) {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
          }
        } else {
          intent.data
        }

        if (uri != null) {
          var title = if (intent.type == "application/pdf") "Imported PDF" else "Imported DOCX"
          if (uri.scheme == "content") {
            try {
              contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                  val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                  if (displayNameIndex != -1) {
                    title = cursor.getString(displayNameIndex) ?: title
                  }
                }
              }
            } catch (e: Exception) {
              e.printStackTrace()
            }
          } else if (uri.scheme == "file") {
            title = uri.lastPathSegment ?: title
          }
          
          if (intent.type == "application/pdf") {
            viewModel.importPdfToNote(uri, title)
          } else {
            viewModel.importDocxToNote(uri, title)
          }
        }
      }
    }
  }
}
