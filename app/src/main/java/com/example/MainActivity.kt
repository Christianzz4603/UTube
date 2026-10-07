package com.example

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UTubeCloudRepository
import com.example.data.YouTubeDatabase
import com.example.data.YouTubeRepository
import com.example.ui.YouTubeApp
import com.example.ui.YouTubeViewModel
import com.example.ui.auth.UTubeSignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = YouTubeDatabase.getDatabase(applicationContext)
        val repository = YouTubeRepository(database.dao())
        val cloudRepository = runCatching { UTubeCloudRepository(applicationContext) }.getOrNull()
        val viewModelFactory = YouTubeViewModel.Factory(repository, cloudRepository)

        setContent {
            val viewModel: YouTubeViewModel = viewModel(factory = viewModelFactory)
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            var isSignedIn by remember {
                mutableStateOf(runCatching { Firebase.auth.currentUser != null }.getOrDefault(false))
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (!isSignedIn) {
                    UTubeSignInScreen(
                        onAuthSuccess = {
                            isSignedIn = true
                            viewModel.showSnackbar("Signed in to Google Account • Synced Watch History & SponsorBlock")
                        }
                    )
                } else {
                    YouTubeApp(
                        viewModel = viewModel,
                        onEnterPipMode = { enterWatchPipMode() },
                        onSignOut = {
                            isSignedIn = false
                        }
                    )
                }
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
