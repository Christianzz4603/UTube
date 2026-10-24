package com.example

import android.accounts.Account
import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UTubeCloudRepository
import com.example.data.YouTubeApi
import com.example.data.YouTubeDatabase
import com.example.data.YouTubeRepository
import com.example.data.YouTubeSync
import com.example.ui.YouTubeApp
import com.example.ui.YouTubeViewModel
import com.example.ui.auth.UTubeSignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Gets an OAuth access token for the signed-in Google account so the app can call the
 * YouTube Data API on the user's behalf (subscriptions, liked videos, playlists, ...).
 */
class YouTubeAuthorizer(private val activity: ComponentActivity) {
    var lastError: String? = null
        private set

    private var pending: CompletableDeferred<String?>? = null
    private val mutex = Mutex()

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val deferred = pending
        pending = null
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val token = try {
                Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(result.data!!)
                    .accessToken
            } catch (e: Exception) {
                lastError = e.message
                null
            }
            deferred?.complete(token)
        } else {
            lastError = "Permission screen was closed"
            deferred?.complete(null)
        }
    }

    suspend fun getToken(): String? = mutex.withLock {
        lastError = null
        try {
            val builder = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(YOUTUBE_SCOPE)))
            val email = try {
                Firebase.auth.currentUser?.email
            } catch (e: Exception) {
                null
            }
            if (!email.isNullOrBlank()) builder.setAccount(Account(email, "com.google"))
            val result = Identity.getAuthorizationClient(activity).authorize(builder.build()).await()
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent == null) {
                    null
                } else {
                    val deferred = CompletableDeferred<String?>()
                    pending = deferred
                    withContext(Dispatchers.Main) {
                        launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    }
                    deferred.await()
                }
            } else {
                result.accessToken
            }
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            null
        }
    }

    companion object {
        const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube.force-ssl"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authorizer = YouTubeAuthorizer(this)

        val database = YouTubeDatabase.getDatabase(applicationContext)
        val repository = YouTubeRepository(database.dao())
        val cloudRepository = runCatching { UTubeCloudRepository(applicationContext) }.getOrNull()
        val apiKey = runCatching {
            getString(resources.getIdentifier("google_api_key", "string", packageName))
        }.getOrNull()
        val sync = YouTubeSync(database.dao(), YouTubeApi(apiKey, packageName))
        val viewModelFactory = YouTubeViewModel.Factory(repository, cloudRepository, sync)

        val prefs = getSharedPreferences("utube_prefs", MODE_PRIVATE)

        setContent {
            val viewModel: YouTubeViewModel = viewModel(factory = viewModelFactory)
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            var isSignedIn by remember {
                mutableStateOf(
                    runCatching { Firebase.auth.currentUser != null }.getOrDefault(false) ||
                        prefs.getBoolean("guest_mode", false)
                )
            }

            LaunchedEffect(isSignedIn) {
                if (isSignedIn) {
                    viewModel.tokenProvider = {
                        if (prefs.getBoolean("guest_mode", false)) {
                            null
                        } else {
                            val token = authorizer.getToken()
                            if (token == null) {
                                val reason = authorizer.lastError?.take(140)
                                viewModel.showSnackbar(
                                    if (reason != null) "YouTube access failed: $reason"
                                    else "YouTube access wasn't granted - showing public videos only"
                                )
                            }
                            token
                        }
                    }
                    viewModel.refreshAll()
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (!isSignedIn) {
                    UTubeSignInScreen(
                        onAuthSuccess = {
                            isSignedIn = true
                            viewModel.showSnackbar("Signed in with Google")
                        },
                        onSkip = {
                            prefs.edit().putBoolean("guest_mode", true).apply()
                            isSignedIn = true
                        }
                    )
                } else {
                    YouTubeApp(
                        viewModel = viewModel,
                        onEnterPipMode = { enterWatchPipMode() },
                        onSignOut = {
                            prefs.edit().putBoolean("guest_mode", false).apply()
                            viewModel.clearAccountData()
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
