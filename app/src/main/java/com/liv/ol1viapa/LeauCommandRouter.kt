package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

object LeauCommandRouter {
    fun openRequestedApp(context: Context, raw: String): Boolean {
        val text = raw.lowercase(Locale.US).trim()
        val match = Regex("^(?:please\\s+)?(?:open|launch|start)\\s+(?:the\\s+)?(.+?)(?:[.!?])?$").find(text)
            ?: return false
        val requested = normalize(match.groupValues[1])

        // Android system destinations that are not ordinary launcher apps.
        when (requested) {
            "settings", "android settings", "phone settings", "system settings" ->
                return launchIntent(context, Intent(Settings.ACTION_SETTINGS))
            "wifi", "wi fi", "wifi settings" ->
                return launchIntent(context, Intent(Settings.ACTION_WIFI_SETTINGS))
            "bluetooth", "bluetooth settings" ->
                return launchIntent(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            "display", "display settings" ->
                return launchIntent(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
            "sound", "sound settings" ->
                return launchIntent(context, Intent(Settings.ACTION_SOUND_SETTINGS))
            "apps", "app settings", "applications" ->
                return launchIntent(context, Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }

        val aliases = mapOf(
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "google chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "gmail" to "com.google.android.gm",
            "google maps" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps",
            "play store" to "com.android.vending",
            "google play" to "com.android.vending",
            "playstore" to "com.android.vending"
        )

        val packageName = aliases.entries.firstOrNull { requested == it.key || requested.startsWith("${it.key} ") }?.value
            ?: findInstalledLauncherPackage(context, requested)
            ?: return false

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
        }

        // Play Store can sometimes expose no normal launcher intent; use its store URL.
        if (packageName == "com.android.vending") {
            return launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://home"))) ||
                launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
        }

        if (packageName == "com.google.android.youtube") {
            return launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
        }

        return false
    }

    private fun findInstalledLauncherPackage(context: Context, requested: String): String? {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val candidates = pm.queryIntentActivities(launcherIntent, 0)

        return candidates
            .map { info ->
                val label = normalize(info.loadLabel(pm).toString())
                Triple(label, info.activityInfo.packageName, similarity(label, requested))
            }
            .filter { (label, _, score) -> label == requested || label.startsWith(requested) || score >= 0.86 }
            .maxByOrNull { it.third }
            ?.second
    }

    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.95
        val aWords = a.split(' ').filter(String::isNotBlank).toSet()
        val bWords = b.split(' ').filter(String::isNotBlank).toSet()
        if (aWords.isEmpty() || bWords.isEmpty()) return 0.0
        val intersection = aWords.intersect(bWords).size.toDouble()
        return (2.0 * intersection) / (aWords.size + bWords.size)
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.US)
        .replace("’", "'")
        .replace(Regex("\\bapp\\b"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun launchIntent(context: Context, intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
