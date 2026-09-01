package com.liv.ol1viapa

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Initializes Firebase from google-services.json when it is present in the app module.
 * The Gradle-property fallback keeps CI/local builds that provide explicit Firebase values working.
 */
object FirebaseConfig {
    fun initialize(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true

        // google-services.json is processed by the Google Services Gradle plugin.
        // Firebase can then initialize itself from the generated resources.
        if (runCatching { FirebaseApp.initializeApp(context) }.getOrNull() != null) return true

        val apiKey = BuildConfig.FIREBASE_API_KEY
        val appId = BuildConfig.FIREBASE_APP_ID
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        if (apiKey.isBlank() || appId.isBlank() || projectId.isBlank()) return false

        return runCatching {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
            )
            true
        }.getOrDefault(false)
    }
}
