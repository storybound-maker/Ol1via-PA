package com.liv.ol1viapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale
import kotlin.math.max

class MainAppTimerService : Service() {
    companion object {
        const val ACTION_START = "com.liv.ol1viapa.MAIN_TIMER_START"
        const val ACTION_ADD = "com.liv.ol1viapa.MAIN_TIMER_ADD"
        const val ACTION_CANCEL = "com.liv.ol1viapa.MAIN_TIMER_CANCEL"
        const val ACTION_START_POMODORO = "com.liv.ol1viapa.MAIN_TIMER_START_POMODORO"
        const val EXTRA_DURATION = "duration_ms"
        const val EXTRA_LABEL = "label"
        const val EXTRA_POMODORO = "pomodoro"

        private const val CHANNEL_ID = "leau_main_timer"
        private const val NOTIFICATION_ID = 8801
        private const val PREFS = "leau_main_timer"
        private const val KEY_END = "end_at"
        private const val KEY_LABEL = "label"
        private const val KEY_POMODORO = "pomodoro"
        private const val KEY_PHASE = "phase"
        private const val KEY_CYCLE = "cycle"
        private const val WORK_MS = 25 * 60 * 1000L
        private const val BREAK_MS = 5 * 60 * 1000L
        private const val LONG_BREAK_MS = 15 * 60 * 1000L

        fun startTimer(context: android.content.Context, durationMs: Long, label: String = "Timer") {
            val intent = Intent(context, MainAppTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION, durationMs)
                putExtra(EXTRA_LABEL, label)
            }
            start(context, intent)
        }

        fun addTime(context: android.content.Context, durationMs: Long) {
            val intent = Intent(context, MainAppTimerService::class.java).apply {
                action = ACTION_ADD
                putExtra(EXTRA_DURATION, durationMs)
            }
            start(context, intent)
        }

        fun cancel(context: android.content.Context) {
            context.startService(Intent(context, MainAppTimerService::class.java).setAction(ACTION_CANCEL))
        }

        fun startPomodoro(context: android.content.Context) {
            context.startService(Intent(context, MainAppTimerService::class.java).setAction(ACTION_START_POMODORO))
        }

        fun endAt(context: android.content.Context): Long = context.getSharedPreferences(PREFS, 0).getLong(KEY_END, 0L)

        fun isRunning(context: android.content.Context): Boolean = endAt(context) > System.currentTimeMillis()

        fun remainingMillis(context: android.content.Context): Long = max(0L, endAt(context) - System.currentTimeMillis())

        fun formatRemaining(context: android.content.Context): String {
            val totalSeconds = remainingMillis(context) / 1000L
            return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
        }

        private fun start(context: android.content.Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ticking = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("LEAU Timer", "Starting…", false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = max(1000L, intent.getLongExtra(EXTRA_DURATION, 0L))
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Timer" }
                save(duration, label, false, "timer", 0)
                beginTicker()
            }
            ACTION_ADD -> {
                val currentEnd = prefs().getLong(KEY_END, 0L)
                if (currentEnd > System.currentTimeMillis()) {
                    prefs().edit().putLong(KEY_END, currentEnd + max(1000L, intent.getLongExtra(EXTRA_DURATION, 0L))).apply()
                    beginTicker()
                }
            }
            ACTION_CANCEL -> {
                clear()
                handler.removeCallbacksAndMessages(null)
                stopSelf()
            }
            ACTION_START_POMODORO -> {
                save(WORK_MS, "Pomodoro", true, "work", 1)
                beginTicker()
            }
        }
        return START_STICKY
    }

    private fun beginTicker() {
        if (ticking) return
        ticking = true
        handler.post(object : Runnable {
            override fun run() {
                val end = prefs().getLong(KEY_END, 0L)
                if (end <= 0L) {
                    ticking = false
                    return
                }
                val remaining = end - System.currentTimeMillis()
                if (remaining <= 0L) {
                    handleFinished()
                } else {
                    val label = prefs().getString(KEY_LABEL, "Timer") ?: "Timer"
                    val phase = prefs().getString(KEY_PHASE, "timer") ?: "timer"
                    val text = if (phase == "work") "Focus ${format(remaining)}" else if (phase == "break") "Break ${format(remaining)}" else "${format(remaining)} remaining"
                    updateNotification(label, text)
                    handler.postDelayed(this, 1000L)
                }
            }
        })
    }

    private fun handleFinished() {
        val pomodoro = prefs().getBoolean(KEY_POMODORO, false)
        if (pomodoro) {
            val phase = prefs().getString(KEY_PHASE, "work") ?: "work"
            val cycle = prefs().getInt(KEY_CYCLE, 1)
            vibrate()
            if (phase == "work") {
                val nextPhase = "break"
                val duration = if (cycle >= 4) LONG_BREAK_MS else BREAK_MS
                prefs().edit().putLong(KEY_END, System.currentTimeMillis() + duration).putString(KEY_PHASE, nextPhase).apply()
                speak(if (cycle >= 4) "Focus complete. Time for a long break." else "Focus complete. Time for a five minute break.")
                updateNotification("Pomodoro", if (cycle >= 4) "Long break ${format(duration)}" else "Break ${format(duration)}")
                handler.postDelayed({ ticking = false; beginTicker() }, 50L)
            } else {
                val nextCycle = cycle + 1
                prefs().edit().putLong(KEY_END, System.currentTimeMillis() + WORK_MS).putString(KEY_PHASE, "work").putInt(KEY_CYCLE, nextCycle).apply()
                speak("Break complete. Starting focus cycle $nextCycle.")
                updateNotification("Pomodoro", "Focus ${format(WORK_MS)}")
                handler.postDelayed({ ticking = false; beginTicker() }, 50L)
            }
            return
        }

        val label = prefs().getString(KEY_LABEL, "Timer") ?: "Timer"
        clear()
        vibrate()
        updateNotification("LEAU", "$label — TIME UP", true)
        speak("Time up. $label is complete.")
        handler.postDelayed({ stopSelf() }, 3500L)
        ticking = false
    }

    private fun save(durationMs: Long, label: String, pomodoro: Boolean, phase: String, cycle: Int) {
        prefs().edit()
            .putLong(KEY_END, System.currentTimeMillis() + durationMs)
            .putString(KEY_LABEL, label)
            .putBoolean(KEY_POMODORO, pomodoro)
            .putString(KEY_PHASE, phase)
            .putInt(KEY_CYCLE, cycle)
            .apply()
    }

    private fun clear() {
        prefs().edit().clear().apply()
    }

    private fun prefs() = getSharedPreferences(PREFS, 0)

    private fun format(ms: Long): String {
        val totalSeconds = max(0L, ms / 1000L)
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun buildNotification(title: String, text: String, finished: Boolean): android.app.Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(!finished)
            .setOnlyAlertOnce(!finished)
            .setPriority(if (finished) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setCategory(if (finished) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_PROGRESS)
            .build()

    private fun updateNotification(title: String, text: String, finished: Boolean = false) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(title, text, finished))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "LEAU Main Timer", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 280), -1)) else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 180, 100, 280), -1)
    }

    private fun speak(text: String) {
        tts?.shutdown()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(.95f)
                tts?.setPitch(1.05f)
                tts?.speak(text.replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Liu"), TextToSpeech.QUEUE_FLUSH, null, "main_timer")
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
