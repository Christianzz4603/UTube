package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Initializes Firebase manually. The project has no google-services.json, so the
 * Google Services plugin cannot auto-initialize FirebaseApp, and any call to
 * Firebase.auth / Firestore would otherwise crash the app on launch.
 */
class UTubeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:99548435019:web:8804d8835b76461359b3ce")
                    .setApiKey("AIzaSyBbtYMjrz1YbLo2kspiIqaof3K-s0DC7GA")
                    .setProjectId("gen-lang-client-0835312353")
                    .setStorageBucket("gen-lang-client-0835312353.firebasestorage.app")
                    .setGcmSenderId("99548435019")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        }.onFailure { Log.e("UTubeApplication", "Firebase init failed", it) }
    }
}
