package com.liv.ol1viapa

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class LeauOverlayService : Service() {
    companion object {
        const val ACTION_SHOW = "com.liv.ol1viapa.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.liv.ol1viapa.HIDE_OVERLAY"
        private const val CHANNEL_ID = "leau_overlay"
        private const val NOTIFICATION_ID = 4201
        private const val TAP_WINDOW = 280L
        private const val LONG_PRESS_MS = 750L
    }

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var pill: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var pillParams: WindowManager.LayoutParams? = null
    private var lastTap = 0L
    private var singleTapRunnable: Runnable? = null
    private var longPressRunnable: Runnable? = null
    private var pomodoroRunnable: Runnable? = null
    private var pomodoroLabel: TextView? = null
    private var speechEyes: ImageView? = null
    private var speechStatus: TextView? = null
    private var speechListening = false
    private var speechThinking = false
    private var speechSpeaking = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LEAU is ready")
            .setContentText("LEAU can float above your apps.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else startForeground(NOTIFICATION_ID, notification)
        setupSpeechRecognizer()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.05f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = handler.post { speechSpeaking = true; speechThinking = false; updateSpeechVisual() }
                    override fun onDone(id: String?) = handler.post { speechSpeaking = false; updateSpeechVisual() }
                    override fun onError(id: String?) = handler.post { speechSpeaking = false; updateSpeechVisual() }
                })
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { speechListening = true; speechThinking = false; updateSpeechVisual() }
                override fun onBeginningOfSpeech() { speechListening = true; speechThinking = false; updateSpeechVisual() }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { speechListening = false; updateSpeechVisual() }
                override fun onPartialResults(results: Bundle?) {
                    val value = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (value.isNotBlank()) handler.post { speechStatus?.text = value.take(32); speechStatus?.setTextColor(0xFFB8FF5A.toInt()) }
                }
                override fun onResults(results: Bundle?) {
                    val value = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                    handler.post {
                        speechListening = false
                        if (value.isNotBlank()) { speechStatus?.text = value.take(32); dispatchOverlayCommand(value) } else updateSpeechVisual()
                    }
                }
                override fun onError(error: Int) {
                    handler.post {
                        speechListening = false; speechThinking = false; updateSpeechVisual()
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) setupSpeechRecognizer()
                        else if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) speechStatus?.text = "Try again"
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun beginOverlaySpeech() {
        if (speechSpeaking) {
            tts?.stop(); speechSpeaking = false; speechThinking = false; updateSpeechVisual(); return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP); putExtra("start_voice", true) })
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { speechStatus?.text = "Speech unavailable"; return }
        if (speechRecognizer == null) setupSpeechRecognizer()
        val recognizer = speechRecognizer ?: return
        speechListening = true; speechThinking = false; updateSpeechVisual(); recognizer.cancel()
        handler.postDelayed({
            runCatching {
                recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                })
            }.onFailure { speechListening = false; updateSpeechVisual(); setupSpeechRecognizer(); speechStatus?.text = "Try again" }
        }, 100L)
    }

    private fun stopOverlaySpeech() { speechRecognizer?.cancel(); speechListening = false; speechThinking = false; updateSpeechVisual() }

    private fun speakReply(text: String) {
        val spoken = text.replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Liu")
        speechThinking = false; speechSpeaking = true; updateSpeechVisual()
        tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "leau_overlay_reply")
    }

    private fun confirmationFor(text: String): String? {
        val lower = text.lowercase(Locale.US).trim()
        return when {
            lower.matches(Regex("^(please\\s+)?(call|phone|ring|dial)\\s+.+$")) -> "Calling now."
            lower.matches(Regex("^(please\\s+)?(open|launch|start)\\s+.+$")) -> "Opening it now."
            lower.startsWith("set timer") || lower.startsWith("set a timer") || lower.startsWith("start a timer") -> "Timer set."
            else -> null
        }
    }

    private fun dispatchOverlayCommand(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        stopOverlaySpeech()
        confirmationFor(trimmed)?.let { confirmation -> speechStatus?.text = confirmation; speakReply(confirmation) }
        if (LeauCommandRouter.openRequestedApp(this, trimmed)) {
            speechThinking = false
            speechStatus?.text = if (trimmed.lowercase(Locale.US).startsWith("set")) "Timer set" else "Opening…"
            return
        }
        sendRecognizedSpeech(trimmed)
    }

    private fun sendRecognizedSpeech(text: String) {
        val status = speechStatus ?: return
        speechThinking = true; speechSpeaking = false; status.text = "Thinking…"; updateSpeechVisual()
        val history = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(this)?.let(history::add)
        LeauApi.sendMessage(text, history) { result ->
            handler.post {
                result.onSuccess { reply -> status.text = reply.take(32); speakReply(reply) }
                    .onFailure { speechThinking = false; status.text = "Try again"; updateSpeechVisual() }
                updateLiveActivity()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "LEAU Floating Assistant", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) { ACTION_SHOW -> showBubble(); ACTION_HIDE -> hideAll() }
        if (intent?.action == ACTION_SHOW || intent?.action == null) updateLiveActivity()
        return START_STICKY
    }

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) return
        removePill(); if (bubble != null) return
        val view = ImageView(this).apply {
            setImageResource(R.drawable.leau_eyes); scaleType = ImageView.ScaleType.CENTER_INSIDE; setPadding(dp(7), dp(7), dp(7), dp(7))
            background = roundedBackground(0xFF101B19.toInt(), 32f); contentDescription = "Open LEAU"; elevation = dp(10).toFloat(); animateEyes(this)
        }
        val params = baseParams(dp(64), dp(64), false).apply { gravity = Gravity.TOP or Gravity.END; x = dp(14); y = dp(180) }
        installBubbleTouch(view, params); bubble = view; bubbleParams = params; runCatching { windowManager.addView(view, params) }
    }

    private fun installBubbleTouch(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false; var longPressed = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; longPressed = false
                    longPressRunnable?.let(handler::removeCallbacks)
                    longPressRunnable = Runnable { if (!moved) { longPressed = true; lastTap = 0L; singleTapRunnable?.let(handler::removeCallbacks); requestScreenshot() } }
                    handler.postDelayed(longPressRunnable!!, LONG_PRESS_MS); true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) longPressRunnable?.let(handler::removeCallbacks)
                    params.x = (startX + dx).coerceAtLeast(0); params.y = (startY + dy).coerceAtLeast(0); runCatching { windowManager.updateViewLayout(view, params) }; true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let(handler::removeCallbacks)
                    if (event.actionMasked == MotionEvent.ACTION_UP && !moved && !longPressed) {
                        val now = System.currentTimeMillis()
                        if (now - lastTap <= TAP_WINDOW) { singleTapRunnable?.let(handler::removeCallbacks); lastTap = 0L; startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
                        else { lastTap = now; singleTapRunnable = Runnable { showPill(); lastTap = 0L }; handler.postDelayed(singleTapRunnable!!, TAP_WINDOW) }
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun requestScreenshot() { startActivity(Intent(this, ScreenshotActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) }) }

    private fun showPill() {
        if (!Settings.canDrawOverlays(this)) return
        removeBubble(); removePill()
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(7), dp(8), dp(7)); background = roundedBackground(0xF20C1513.toInt(), 34f).apply { setStroke(dp(1), 0xFF3D8170.toInt()) }; elevation = dp(18).toFloat() }
        val eye = ImageView(this).apply { setImageResource(R.drawable.leau_eyes); scaleType = ImageView.ScaleType.CENTER_INSIDE; setPadding(dp(3), dp(3), dp(5), dp(3)); contentDescription = "LEAU microphone"; isClickable = true; isFocusable = true; animateEyes(this) }
        root.addView(eye, LinearLayout.LayoutParams(dp(48), dp(42)))
        val status = TextView(this).apply { text = "LEAU"; setTextColor(0xFFE7FFF7.toInt()); textSize = 12f; setPadding(dp(2), 0, dp(8), 0) }
        root.addView(status, LinearLayout.LayoutParams(dp(48), -1)); pomodoroLabel = status; speechEyes = eye; speechStatus = status
        val input = EditText(this).apply { hint = "Ask LEAU anything..."; setHintTextColor(0xFF7D9E94.toInt()); setTextColor(Color.WHITE); textSize = 14f; setSingleLine(true); setPadding(dp(4), 0, dp(4), 0); background = null; imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND }
        root.addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        val send = ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_send); setColorFilter(0xFFB8FF5A.toInt()); background = roundedBackground(0x331B4A40.toInt(), 22f); contentDescription = "Send"; isClickable = true; isFocusable = true }
        root.addView(send, LinearLayout.LayoutParams(dp(42), dp(42)))
        val params = baseParams(dp(360), dp(64), true).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(90); flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH }
        pill = root; pillParams = params; runCatching { windowManager.addView(root, params) }
        installPillTouch(root, eye, params); send.setOnClickListener { sendOverlayMessage(input) }; input.setOnEditorActionListener { _, _, _ -> sendOverlayMessage(input); true }; input.requestFocus()
        handler.postDelayed({ runCatching { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) } }, 150L); updateLiveActivity()
    }

    private fun installPillTouch(root: View, eye: ImageView, params: WindowManager.LayoutParams) {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_OUTSIDE -> { hidePillToBubble(); true }
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; false }
                MotionEvent.ACTION_MOVE -> { val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt(); if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true; if (moved) { params.x = startX + dx; params.y = (startY + dy).coerceAtLeast(0); runCatching { windowManager.updateViewLayout(root, params) }; true } else false }
                MotionEvent.ACTION_UP -> moved
                else -> false
            }
        }
        eye.setOnClickListener {
            when {
                speechSpeaking -> { tts?.stop(); speechSpeaking = false; speechThinking = false; updateSpeechVisual() }
                speechListening -> stopOverlaySpeech()
                else -> beginOverlaySpeech()
            }
        }
    }

    private fun sendOverlayMessage(input: EditText) { val text = input.text.toString().trim(); if (text.isEmpty()) return; input.setText(""); dispatchOverlayCommand(text) }

    private fun updateSpeechVisual() {
        val e = speechEyes ?: return; val l = speechStatus ?: return
        when {
            speechListening -> { e.background = roundedBackground(0x552B5C45.toInt(), 22f).apply { setStroke(dp(2), 0xFFB8FF5A.toInt()) }; if (l.text.isNullOrBlank() || l.text == "LEAU") l.text = "Listening…"; l.setTextColor(0xFFB8FF5A.toInt()); e.animate().scaleX(1.10f).scaleY(1.10f).setDuration(350L).start() }
            speechThinking -> { e.background = roundedBackground(0x44243F37.toInt(), 22f).apply { setStroke(dp(1), 0xFF6EA58F.toInt()) }; l.text = "Thinking…"; l.setTextColor(0xFF9CC8B5.toInt()); e.animate().scaleX(1.06f).scaleY(1.06f).setDuration(500L).start() }
            speechSpeaking -> { e.background = roundedBackground(0x552B5C45.toInt(), 22f).apply { setStroke(dp(2), 0xFFB8FF5A.toInt()) }; l.setTextColor(0xFFE7FFF7.toInt()); e.animate().scaleX(1.08f).scaleY(1.08f).setDuration(420L).start() }
            else -> { e.background = null; if (l.text.toString() == "Listening…" || l.text.toString() == "Thinking…") l.text = "LEAU"; l.setTextColor(0xFFE7FFF7.toInt()); e.animate().scaleX(1f).scaleY(1f).setDuration(250L).start() }
        }
    }

    private fun hidePillToBubble() { stopOverlaySpeech(); tts?.stop(); speechSpeaking = false; speechThinking = false; removePill(); showBubble() }
    private fun removeBubble() { bubble?.let { runCatching { windowManager.removeView(it) } }; bubble = null; bubbleParams = null }
    private fun removePill() { pill?.let { runCatching { windowManager.removeView(it) } }; pill = null; pillParams = null; pomodoroLabel = null; speechEyes = null; speechStatus = null }
    private fun hideAll() { stopOverlaySpeech(); tts?.stop(); speechSpeaking = false; speechThinking = false; removeBubble(); removePill() }

    private fun updateLiveActivity() {
        if (pill == null) return
        pomodoroRunnable?.let(handler::removeCallbacks)
        if (LeauPomodoro.isRunning(this)) {
            val tick = object : Runnable {
                override fun run() {
                    if (pill == null) return
                    if (LeauPomodoro.isRunning(this@LeauOverlayService)) { pomodoroLabel?.text = "${LeauPomodoro.label(this@LeauOverlayService)}  ${LeauPomodoro.formatRemaining(LeauPomodoro.endAt(this@LeauOverlayService))}"; handler.postDelayed(this, 1000L) }
                    else { pomodoroLabel?.text = "✓ Focus complete"; pomodoroRunnable = null }
                }
            }
            pomodoroRunnable = tick; handler.post(tick)
        } else if (!speechListening && !speechThinking && !speechSpeaking) pomodoroLabel?.text = "LEAU"
    }

    private fun animateEyes(view: View) {
        view.animate().translationY(dp(-2).toFloat()).scaleX(1.04f).scaleY(1.04f).setDuration(1400L).withEndAction { view.animate().translationY(dp(2).toFloat()).scaleX(.96f).scaleY(.96f).setDuration(1400L).withEndAction { if (view.windowToken != null) animateEyes(view) }.start() }.start()
    }

    private fun baseParams(width: Int, height: Int, focusable: Boolean): WindowManager.LayoutParams = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, if (focusable) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT)
    private fun roundedBackground(color: Int, radiusDp: Float) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        stopOverlaySpeech(); speechRecognizer?.destroy(); speechRecognizer = null; tts?.stop(); tts?.shutdown(); tts = null; handler.removeCallbacksAndMessages(null); hideAll(); super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
