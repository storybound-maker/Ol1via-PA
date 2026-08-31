package com.liv.ol1viapa

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object LeauPomodoro {
    const val ACTION_FINISH = "com.liv.ol1viapa.POMODORO_FINISH"
    private const val PREFS = "leau_pomodoro"
    private const val END = "end_at"
    private const val LABEL = "label"
    private const val CHANNEL = "leau_pomodoro"
    private const val NOTIFICATION = 7301

    fun start(context: Context, minutes: Int, label: String = "Focus") {
        val safeMinutes = minutes.coerceIn(1, 180)
        val endAt = System.currentTimeMillis() + safeMinutes * 60_000L
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(END, endAt).putString(LABEL, label).apply()
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi) else alarm.setExact(AlarmManager.RTC_WAKEUP, endAt, pi)
        showNotification(context, "${formatRemaining(endAt)} remaining", label, false)
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION)
    }

    fun endAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(END, 0L)
    fun label(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LABEL, "Focus") ?: "Focus"
    fun isRunning(context: Context): Boolean = endAt(context) > System.currentTimeMillis()

    fun remaining(context: Context): Long = (endAt(context) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun formatRemaining(endAt: Long): String {
        val total = ((endAt - System.currentTimeMillis()).coerceAtLeast(0L) / 1000L)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(context, 7301, Intent(context, LeauPomodoroReceiver::class.java).setAction(ACTION_FINISH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun showNotification(context: Context, text: String, title: String, finished: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "LEAU Pomodoro", NotificationManager.IMPORTANCE_HIGH))
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(if (finished) "✓ $title complete" else "LEAU · $title").setContentText(text).setOngoing(!finished).setAutoCancel(finished).setCategory(NotificationCompat.CATEGORY_ALARM).build()
        manager.notify(NOTIFICATION, notification)
    }

    class LeauPomodoroReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != ACTION_FINISH) return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val endAt = prefs.getLong(END, 0L)
            if (endAt == 0L) return
            val label = prefs.getString(LABEL, "Focus") ?: "Focus"
            prefs.edit().clear().apply()
            showNotification(context, "Time to take a break.", label, true)
            context.sendBroadcast(Intent(LeauOverlayService.ACTION_SHOW).setClass(context, LeauOverlayService::class.java))
        }
    }
}
