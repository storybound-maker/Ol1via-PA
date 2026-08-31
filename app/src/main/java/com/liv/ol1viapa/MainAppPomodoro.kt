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
import androidx.core.app.NotificationManagerCompat
import java.util.Locale
import kotlin.math.max

/** Main-app-only timer. It intentionally does not touch the floating pill timer. */
object MainAppPomodoro {
    private const val PREFS = "leau_main_timer"
    private const val KEY_END = "end_at"
    private const val KEY_LABEL = "label"
    private const val REQUEST_CODE = 8814
    private const val NOTIFICATION_ID = 8814
    private const val CHANNEL_ID = "leau_main_timer"
    const val ACTION_FINISH = "com.liv.ol1viapa.MAIN_TIMER_FINISH"

    fun start(context: Context, durationMillis: Long, label: String = "Timer") {
        val duration = max(1_000L, durationMillis)
        val end = System.currentTimeMillis() + duration
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_END, end)
            .putString(KEY_LABEL, label)
            .apply()

        createChannel(context)
        postRunningNotification(context)

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            end,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context))
        clear(context)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun isRunning(context: Context): Boolean = endAt(context) > System.currentTimeMillis()

    fun endAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_END, 0L)

    fun label(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_LABEL, "Timer") ?: "Timer"

    fun remaining(context: Context): Long = (endAt(context) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun formatRemaining(context: Context): String {
        val seconds = remaining(context) / 1000L
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    fun add(context: Context, extraMillis: Long): Boolean {
        if (!isRunning(context)) return false
        start(context, remaining(context) + max(0L, extraMillis), label(context))
        return true
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MainAppPomodoroReceiver::class.java).setAction(ACTION_FINISH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "LEAU Main App Timer", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun postRunningNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LEAU timer running")
            .setContentText("${label(context)} • ${formatRemaining(context)} remaining")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun postFinishedNotification(context: Context, timerLabel: String) {
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LEAU — Time up")
            .setContentText("$timerLabel is complete.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_END)
            .remove(KEY_LABEL)
            .apply()
    }

    class MainAppPomodoroReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != ACTION_FINISH) return
            val label = label(context)
            clear(context)
            postFinishedNotification(context, label)
        }
    }
}
