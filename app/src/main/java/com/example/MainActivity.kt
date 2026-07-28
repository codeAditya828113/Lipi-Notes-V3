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
