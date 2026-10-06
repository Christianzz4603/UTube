package com.example

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.YouTubeDatabase
import com.example.data.YouTubeRepository
import com.example.ui.YouTubeApp
import com.example.ui.YouTubeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = YouTubeDatabase.getDatabase(applicationContext)
        val repository = YouTubeRepository(database.dao())
        val viewModelFactory = YouTubeViewModel.Factory(repository)

        setContent {
            val viewModel: YouTubeViewModel = viewModel(factory = viewModelFactory)
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                YouTubeApp(
                    viewModel = viewModel,
                    onEnterPipMode = { enterWatchPipMode() }
                )
            }
        }
    }

    private fun enterWatchPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }
}
