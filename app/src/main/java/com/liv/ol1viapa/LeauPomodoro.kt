package com.liv.ol1viapa

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Locale
import kotlin.math.max

object LeauPomodoro {
    private const val PREFS = "leau_pomodoro"
    private const val KEY_END = "end_at"
    private const val KEY_LABEL = "label"
    private const val REQUEST_CODE = 7712
    private const val CHANNEL_ID = "leau_pomodoro"
    private const val NOTIFICATION_ID = 7712
    const val ACTION_FINISH = "com.liv.ol1viapa.POMODORO_FINISH"

    fun start(context: Context, durationMillis: Long, label: String = "Focus") {
        val safeDuration = max(1_000L, durationMillis)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_END, System.currentTimeMillis() + safeDuration)
            .putString(KEY_LABEL, label)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, LeauPomodoroReceiver::class.java).setAction(ACTION_FINISH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + safeDuration, pendingIntent)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, LeauPomodoroReceiver::class.java).setAction(ACTION_FINISH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        clear(context)
    }

    fun isRunning(context: Context): Boolean = endAt(context) > System.currentTimeMillis()

    fun endAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_END, 0L)

    fun label(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LABEL, "Focus") ?: "Focus"

    fun formatRemaining(endAt: Long): String {
        val seconds = max(0L, (endAt - System.currentTimeMillis()) / 1000L)
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    private fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_END).remove(KEY_LABEL).apply()
    }

    private fun showFinishedNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(android.app.NotificationChannel(CHANNEL_ID, "LEAU Pomodoro", android.app.NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.liv.ol1viapa.R.mipmap.ic_launcher)
            .setContentTitle("LEAU")
            .setContentText("${label(context)} complete. Nice work.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        clear(context)
    }

    class LeauPomodoroReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != ACTION_FINISH) return
            showFinishedNotification(context)
            val serviceIntent = Intent(context, LeauOverlayService::class.java).setAction(LeauOverlayService.ACTION_SHOW)
            runCatching { context.startForegroundService(serviceIntent) }
        }
    }
}
