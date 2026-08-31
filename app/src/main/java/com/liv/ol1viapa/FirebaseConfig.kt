package com.liv.ol1viapa

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Firebase is configured from Gradle properties so secrets/configuration are not committed.
 * Add these to gradle.properties (or CI environment) before enabling account sign-in:
 * LEAU_FIREBASE_API_KEY, LEAU_FIREBASE_APP_ID, LEAU_FIREBASE_PROJECT_ID.
 */
object FirebaseConfig {
    fun initialize(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true

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
