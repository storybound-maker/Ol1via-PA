package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object LeauSettings {
    private const val PREFS = "leau_preferences"
    private const val THEME = "theme"
    private const val FONT = "font_scale"
    private const val REDUCE_MOTION = "reduce_motion"
    private const val HAPTICS = "haptics"
    private const val SOUNDS = "sounds"
    private const val NOTIFICATIONS = "notifications"
    private const val VOICE_RESPONSES = "voice_responses"
    private const val FLOATING_PILL = "floating_pill"
    private const val AUTO_PILL = "auto_pill"
    private const val MEMORY = "memory"
    private const val CHAT_HISTORY = "chat_history"
    private const val ONBOARDING = "onboarding_complete"
    private const val GUEST_MODE = "guest_mode"
    private const val ACCOUNT_NAME = "account_name"
    private const val ACCOUNT_EMAIL = "account_email"
    private const val ACCOUNT_PROVIDER = "account_provider"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun theme(context: Context): String = prefs(context).getString(THEME, "dark") ?: "dark"
    fun setTheme(context: Context, value: String) = prefs(context).edit().putString(THEME, value).apply()
    fun fontScale(context: Context): Float = prefs(context).getFloat(FONT, 1f)
    fun setFontScale(context: Context, value: Float) = prefs(context).edit().putFloat(FONT, value.coerceIn(.85f, 1.35f)).apply()
    fun reduceMotion(context: Context) = prefs(context).getBoolean(REDUCE_MOTION, false)
    fun setReduceMotion(context: Context, value: Boolean) = prefs(context).edit().putBoolean(REDUCE_MOTION, value).apply()
    fun haptics(context: Context) = prefs(context).getBoolean(HAPTICS, true)
    fun setHaptics(context: Context, value: Boolean) = prefs(context).edit().putBoolean(HAPTICS, value).apply()
    fun sounds(context: Context) = prefs(context).getBoolean(SOUNDS, true)
    fun setSounds(context: Context, value: Boolean) = prefs(context).edit().putBoolean(SOUNDS, value).apply()
    fun notifications(context: Context) = prefs(context).getBoolean(NOTIFICATIONS, true)
    fun setNotifications(context: Context, value: Boolean) = prefs(context).edit().putBoolean(NOTIFICATIONS, value).apply()
    fun voiceResponses(context: Context) = prefs(context).getBoolean(VOICE_RESPONSES, true)
    fun setVoiceResponses(context: Context, value: Boolean) = prefs(context).edit().putBoolean(VOICE_RESPONSES, value).apply()
    fun floatingPill(context: Context) = prefs(context).getBoolean(FLOATING_PILL, true)
    fun setFloatingPill(context: Context, value: Boolean) = prefs(context).edit().putBoolean(FLOATING_PILL, value).apply()
    fun autoPill(context: Context) = prefs(context).getBoolean(AUTO_PILL, true)
    fun setAutoPill(context: Context, value: Boolean) = prefs(context).edit().putBoolean(AUTO_PILL, value).apply()
    fun memory(context: Context) = prefs(context).getBoolean(MEMORY, true)
    fun setMemory(context: Context, value: Boolean) = prefs(context).edit().putBoolean(MEMORY, value).apply()
    fun chatHistory(context: Context) = prefs(context).getBoolean(CHAT_HISTORY, true)
    fun setChatHistory(context: Context, value: Boolean) = prefs(context).edit().putBoolean(CHAT_HISTORY, value).apply()
    fun onboardingComplete(context: Context) = prefs(context).getBoolean(ONBOARDING, false)
    fun setOnboardingComplete(context: Context, value: Boolean) = prefs(context).edit().putBoolean(ONBOARDING, value).apply()
    fun guestMode(context: Context) = prefs(context).getBoolean(GUEST_MODE, false)
    fun setGuestMode(context: Context, value: Boolean) = prefs(context).edit().putBoolean(GUEST_MODE, value).apply()
    fun accountName(context: Context) = prefs(context).getString(ACCOUNT_NAME, "") ?: ""
    fun accountEmail(context: Context) = prefs(context).getString(ACCOUNT_EMAIL, "") ?: ""
    fun accountProvider(context: Context) = prefs(context).getString(ACCOUNT_PROVIDER, "") ?: ""
    fun saveAccount(context: Context, name: String, email: String, provider: String) = prefs(context).edit().putString(ACCOUNT_NAME, name).putString(ACCOUNT_EMAIL, email).putString(ACCOUNT_PROVIDER, provider).putBoolean(GUEST_MODE, false).apply()
    fun clearAccount(context: Context) = prefs(context).edit().remove(ACCOUNT_NAME).remove(ACCOUNT_EMAIL).remove(ACCOUNT_PROVIDER).putBoolean(GUEST_MODE, false).putBoolean(ONBOARDING, false).apply()

    fun openAppSettings(context: Context) = context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        context.startActivity(intent)
    }
    fun openOverlaySettings(context: Context) = context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
}
