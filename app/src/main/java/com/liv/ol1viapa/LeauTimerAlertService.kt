package com.liv.ol1viapa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

class LeauTimerAlertService : Service() {
    companion object {
        const val ACTION_TIME_UP = "com.liv.ol1viapa.TIMER_TIME_UP"
        const val EXTRA_LABEL = "label"
        private const val CHANNEL_ID = "leau_timer_alert"
        private const val NOTIFICATION_ID = 7713
    }

    private lateinit var windowManager: WindowManager
    private var alertView: View? = null
    private var tts: android.speech.tts.TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LEAU")
            .setContentText("Timer finished")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TIME_UP) {
            val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Timer" }
            showTimeUpPill(label)
            vibrate()
            speak("Time up. $label is complete.")
        }
        return START_NOT_STICKY
    }

    private fun showTimeUpPill(label: String) {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        alertView?.let { runCatching { windowManager.removeView(it) } }
        val view = TextView(this).apply {
            text = "TIME UP"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(15), dp(28), dp(15))
            background = GradientDrawable().apply {
                setColor(0xF20D1B17.toInt())
                cornerRadius = dp(32).toFloat()
                setStroke(dp(2), 0xFFB8FF5A.toInt())
            }
            elevation = dp(20).toFloat()
            contentDescription = "Leau timer finished: $label"
            setOnClickListener { dismiss() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(92)
        }
        alertView = view
        runCatching { windowManager.addView(view, params) }.onFailure { stopSelf() }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 280), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 180, 100, 280), -1)
        }
    }

    private fun speak(text: String) {
        tts?.shutdown()
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.05f)
                tts?.speak(text.replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Liu"), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "leau_timer_alert")
            }
        }
    }

    private fun dismiss() {
        alertView?.let { runCatching { windowManager.removeView(it) } }
        alertView = null
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "LEAU Timer Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        alertView?.let { runCatching { windowManager.removeView(it) } }
        alertView = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
