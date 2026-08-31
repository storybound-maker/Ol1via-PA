package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object LeauCommandRouter {
    fun openRequestedApp(context: Context, raw: String): Boolean {
        val text = raw.lowercase(Locale.US).trim()
        val target = when {
            Regex("\\b(open|launch|start)\\s+(youtube|yt)\\b").containsMatchIn(text) ->
                "com.google.android.youtube"
            Regex("\\b(open|launch|start)\\s+(chrome|google chrome)\\b").containsMatchIn(text) ->
                "com.android.chrome"
            Regex("\\b(open|launch|start)\\s+(spotify)\\b").containsMatchIn(text) ->
                "com.spotify.music"
            Regex("\\b(open|launch|start)\\s+(whatsapp)\\b").containsMatchIn(text) ->
                "com.whatsapp"
            Regex("\\b(open|launch|start)\\s+(instagram)\\b").containsMatchIn(text) ->
                "com.instagram.android"
            Regex("\\b(open|launch|start)\\s+(facebook)\\b").containsMatchIn(text) ->
                "com.facebook.katana"
            else -> null
        } ?: return false

        val launchIntent = context.packageManager.getLaunchIntentForPackage(target)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }

        if (target == "com.google.android.youtube") {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return true
            }
        }
        return false
    }
}
