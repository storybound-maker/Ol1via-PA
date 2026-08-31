package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object LeauCommandRouter {
    fun openRequestedApp(context: Context, raw: String): Boolean {
        val text = raw.lowercase(Locale.US).trim()
        val match = Regex("^(?:please\\s+)?(?:open|launch|start)\\s+(?:the\\s+)?(.+?)(?:[.!?])?$").find(text) ?: return false
        val target = match.groupValues[1].trim()
        val apps = mapOf(
            "youtube" to "com.google.android.youtube", "yt" to "com.google.android.youtube",
            "chrome" to "com.android.chrome", "google chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music", "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android", "facebook" to "com.facebook.katana",
            "gmail" to "com.google.android.gm", "google maps" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps", "play store" to "com.android.vending"
        )
        val packageName = apps.entries.firstOrNull { target == it.key || target.startsWith("${it.key} ") }?.value ?: return false
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(intent); true }.getOrDefault(false)
        }
        val url = when (packageName) {
            "com.google.android.youtube" -> "https://www.youtube.com"
            "com.android.chrome" -> "https://www.google.com"
            "com.google.android.apps.maps" -> "https://maps.google.com"
            else -> null
        }
        return url?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); true }.getOrDefault(false) } ?: false
    }
}
