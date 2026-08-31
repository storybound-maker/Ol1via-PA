package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

object LeauCommandRouter {
    fun handleCommand(context: Context, raw: String): Boolean {
        return startTimerIfRequested(context, raw) || callIfRequested(context, raw) || openRequestedApp(context, raw)
    }

    fun timerResponse(context: Context, raw: String): String? {
        val text = normalize(raw)
        if (text.isBlank()) return null

        if (text.matches(Regex("^(how much time is left|how much time is remaining|time left|timer status)$"))) {
            return if (LeauPomodoro.isRunning(context)) {
                "You have ${LeauPomodoro.formatRemaining(LeauPomodoro.endAt(context))} left on your timer."
            } else "There isn't an active timer right now."
        }

        if (text.matches(Regex("^(cancel|stop|delete|clear)( my)? timer$")) || text == "cancel timer") {
            if (!LeauPomodoro.isRunning(context)) return "There isn't an active timer to cancel."
            LeauPomodoro.cancel(context)
            return "Timer cancelled."
        }

        val add = Regex("^(?:add|increase|extend)\\s+(\\d+)\\s*(second|seconds|minute|minutes|hour|hours)(?:\\s+to\\s+(?:my\\s+)?timer)?$").find(text)
        if (add != null) {
            val amount = add.groupValues[1].toLongOrNull() ?: return null
            val unit = add.groupValues[2]
            if (amount <= 0L || !LeauPomodoro.isRunning(context)) return if (LeauPomodoro.isRunning(context)) null else "There isn't an active timer to extend."
            val extra = toMillis(amount, unit)
            val remaining = (LeauPomodoro.endAt(context) - System.currentTimeMillis()).coerceAtLeast(0L)
            LeauPomodoro.start(context, remaining + extra, LeauPomodoro.label(context))
            return "Added $amount $unit to your timer."
        }

        val change = Regex("^(?:actually|change|make) (?:that|it|the timer) (?:to )?(\\d+)\\s*(second|seconds|minute|minutes|hour|hours)$").find(text)
        if (change != null && LeauPomodoro.isRunning(context)) {
            val amount = change.groupValues[1].toLongOrNull() ?: return null
            val unit = change.groupValues[2]
            if (amount <= 0L) return null
            LeauPomodoro.start(context, toMillis(amount, unit), LeauPomodoro.label(context))
            return "Okay, I changed the timer to $amount $unit."
        }

        val start = Regex("^(?:set|start) (?:a )?timer(?: for)?\\s*(\\d+)\\s*(second|seconds|minute|minutes|hour|hours)$").find(text)
        if (start != null) {
            val amount = start.groupValues[1].toLongOrNull() ?: return null
            val unit = start.groupValues[2]
            if (amount <= 0L) return null
            LeauPomodoro.start(context, toMillis(amount, unit), "Timer")
            return "Timer set for $amount $unit."
        }
        return null
    }

    fun startTimerIfRequested(context: Context, raw: String): Boolean = timerResponse(context, raw) != null

    fun callIfRequested(context: Context, raw: String): Boolean {
        val match = Regex("^(?:please\\s+)?(?:call|phone|ring|dial)\\s+(.+?)(?:[.!?])?$", RegexOption.IGNORE_CASE).find(raw.trim()) ?: return false
        val target = match.groupValues[1].trim()
        if (target.isBlank()) return false
        return runCatching {
            context.startActivity(Intent(context, LeauCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(LeauCallActivity.EXTRA_TARGET, target)
            })
            true
        }.getOrDefault(false)
    }

    fun openRequestedApp(context: Context, raw: String): Boolean {
        val text = normalize(raw)
        val match = Regex("^(?:please )?(?:open|launch|start|run) (?:the )?(.+)$").find(text) ?: return false
        val requested = normalize(match.groupValues[1]).removeSuffix(" application").removeSuffix(" app").trim()
        if (requested.isBlank()) return false

        when (requested) {
            "settings", "android settings", "phone settings", "system settings" -> return launchIntent(context, Intent(Settings.ACTION_SETTINGS))
            "wifi", "wi fi", "wifi settings" -> return launchIntent(context, Intent(Settings.ACTION_WIFI_SETTINGS))
            "bluetooth", "bluetooth settings" -> return launchIntent(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            "display", "display settings" -> return launchIntent(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
            "sound", "sound settings" -> return launchIntent(context, Intent(Settings.ACTION_SOUND_SETTINGS))
            "apps", "app settings", "applications" -> return launchIntent(context, Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }

        val aliases = mapOf(
            "youtube" to "com.google.android.youtube", "yt" to "com.google.android.youtube",
            "chrome" to "com.android.chrome", "google chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music", "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android", "snapchat" to "com.snapchat.android",
            "facebook" to "com.facebook.katana", "messenger" to "com.facebook.orca",
            "gmail" to "com.google.android.gm", "google maps" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps", "play store" to "com.android.vending",
            "google play" to "com.android.vending", "playstore" to "com.android.vending",
            "twitch" to "tv.twitch.android.app", "discord" to "com.discord",
            "telegram" to "org.telegram.messenger"
        )

        val packageName = aliases[requested] ?: findInstalledLauncherPackage(context, requested) ?: return false
        if (launchPackage(context, packageName)) return true
        if (packageName == "com.android.vending") return launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://home"))) || launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
        if (packageName == "com.google.android.youtube") return launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
        return false
    }

    private fun findInstalledLauncherPackage(context: Context, requested: String): String? {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(launcherIntent, 0).asSequence().map { info ->
            val label = normalize(info.loadLabel(pm).toString())
            val packageName = info.activityInfo.packageName
            Triple(label, packageName, appMatchScore(label, requested))
        }.filter { (label, _, score) -> label == requested || label.startsWith(requested) || requested.startsWith(label) || score >= 0.62 }
            .maxByOrNull { it.third }?.second
    }

    private fun appMatchScore(label: String, requested: String): Double {
        if (label == requested) return 1.0
        if (label.startsWith(requested) || requested.startsWith(label)) return 0.94
        val labelWords = label.split(' ').filter(String::isNotBlank).toSet()
        val requestedWords = requested.split(' ').filter(String::isNotBlank).toSet()
        if (labelWords.isEmpty() || requestedWords.isEmpty()) return 0.0
        val intersection = labelWords.intersect(requestedWords).size.toDouble()
        val union = labelWords.union(requestedWords).size.toDouble()
        val jaccard = intersection / union
        return maxOf(jaccard, characterSimilarity(label, requested))
    }

    private fun characterSimilarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val distance = levenshteinDistance(a, b)
        return 1.0 - distance.toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            val swap = previous; previous = current; current = swap
        }
        return previous[b.length]
    }

    private fun toMillis(amount: Long, unit: String): Long = when {
        unit.startsWith("second") -> amount * 1_000L
        unit.startsWith("minute") -> amount * 60_000L
        else -> amount * 3_600_000L
    }

    private fun launchPackage(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    private fun launchIntent(context: Context, intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun normalize(value: String): String = value.lowercase(Locale.US)
        .replace("’", "'")
        .replace(Regex("\\b(please|could you|can you)\\b"), "")
        .replace(Regex("\\bapp\\b"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ").trim()
}
