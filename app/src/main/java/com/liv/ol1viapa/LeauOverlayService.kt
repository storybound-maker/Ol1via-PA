package com.liv.ol1viapa

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
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
import androidx.core.app.ServiceCompat
import android.app.NotificationManager
import android.app.Service
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

class LeauOverlayService : Service() {
    companion object {
        const val ACTION_SHOW = "com.liv.ol1viapa.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.liv.ol1viapa.HIDE_OVERLAY"
        private const val CHANNEL_ID = "leau_overlay"
        private const val NOTIFICATION_ID = 4201
        private const val TAP_WINDOW = 280L
    }

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var pill: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var pillParams: WindowManager.LayoutParams? = null
    private var lastTap = 0L
    private var singleTapRunnable: Runnable? = null
    private var pomodoroRunnable: Runnable? = null
    private var pomodoroLabel: TextView? = null

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
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "LEAU Floating Assistant", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showBubble()
            ACTION_HIDE -> hideAll()
        }
        if (intent?.action == ACTION_SHOW || intent?.action == null) updateLiveActivity()
        return START_STICKY
    }

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) return
        removePill()
        if (bubble != null) return
        val size = dp(64)
        val view = ImageView(this).apply {
            setImageResource(R.drawable.leau_eyes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(7), dp(7), dp(7), dp(7))
            background = roundedBackground(0xFF101B19.toInt(), 32)
            contentDescription = "Open LEAU"
            elevation = dp(10).toFloat()
            animateEyes(this)
        }
        val params = baseParams(size, size, false).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(180)
        }
        installBubbleTouch(view, params)
        bubble = view
        bubbleParams = params
        runCatching { windowManager.addView(view, params) }
    }

    private fun installBubbleTouch(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    params.x = (startX + dx).coerceAtLeast(0); params.y = (startY + dy).coerceAtLeast(0)
                    runCatching { windowManager.updateViewLayout(view, params) }; true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTap <= TAP_WINDOW) {
                            singleTapRunnable?.let(handler::removeCallbacks)
                            lastTap = 0L
                            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        } else {
                            lastTap = now
                            singleTapRunnable = Runnable { showPill(); lastTap = 0L }
                            handler.postDelayed(singleTapRunnable!!, TAP_WINDOW)
                        }
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun showPill() {
        if (!Settings.canDrawOverlays(this)) return
        removeBubble()
        removePill()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(7), dp(8), dp(7))
            background = roundedBackground(0xF20C1513.toInt(), 34).apply { setStroke(dp(1), 0xFF3D8170.toInt()) }
            elevation = dp(18).toFloat()
        }
        val eyes = ImageView(this).apply {
            setImageResource(R.drawable.leau_eyes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(3), dp(3), dp(5), dp(3))
            contentDescription = "LEAU microphone"
            animateEyes(this)
        }
        root.addView(eyes, LinearLayout.LayoutParams(dp(48), dp(42)))
        val status = TextView(this).apply { text = "LEAU"; setTextColor(0xFFE7FFF7.toInt()); textSize = 12f; setPadding(dp(2), 0, dp(8), 0) }
        root.addView(status, LinearLayout.LayoutParams(dp(48), -1))
        pomodoroLabel = status
        val input = EditText(this).apply {
            hint = "Ask LEAU anything..."
            setHintTextColor(0xFF7D9E94.toInt())
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            setPadding(dp(4), 0, dp(4), 0)
            background = null
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        }
        root.addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        val send = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_send)
            setColorFilter(0xFFB8FF5A.toInt())
            background = roundedBackground(0x331B4A40.toInt(), 22)
            contentDescription = "Send"
        }
        root.addView(send, LinearLayout.LayoutParams(dp(42), dp(42)))
        val params = baseParams(dp(360), dp(64), true).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(90)
            flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }
        pill = root
        pillParams = params
        runCatching { windowManager.addView(root, params) }
        installPillDrag(root, eyes, params)
        root.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) { hideAll(); true } else false
        }
        eyes.setOnClickListener {
            input.clearFocus()
            startMainSpeechFromOverlay()
        }
        send.setOnClickListener { sendOverlayMessage(input) }
        input.setOnEditorActionListener { _, _, _ -> sendOverlayMessage(input); true }
        input.requestFocus()
        handler.postDelayed(Runnable {
            runCatching { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
        }, 150L)
        updateLiveActivity()
    }

    private fun installPillDrag(root: View, handle: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).toInt()
                    params.y = (startY + (event.rawY - downY).toInt()).coerceAtLeast(0)
                    runCatching { windowManager.updateViewLayout(root, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun animateEyes(view: View) {
        view.animate().translationY(dp(-2).toFloat()).scaleX(1.04f).scaleY(1.04f).setDuration(1400L).withEndAction {
            view.animate().translationY(dp(2).toFloat()).scaleX(.96f).scaleY(.96f).setDuration(1400L).withEndAction {
                if (view.windowToken != null) animateEyes(view)
            }.start()
        }.start()
    }

    private fun startMainSpeechFromOverlay() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("start_voice", true)
        })
    }

    private fun sendOverlayMessage(input: EditText) {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        val label = pomodoroLabel ?: return
        label.text = "Thinking…"
        val history = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(this)?.let(history::add)
        LeauApi.sendMessage(text, history) { result ->
            handler.post {
                result.onSuccess { reply -> label.text = reply.take(24) }.onFailure { label.text = "Try again" }
                updateLiveActivity()
            }
        }
    }

    private fun updateLiveActivity() {
        if (pill == null) return
        pomodoroRunnable?.let(handler::removeCallbacks)
        if (LeauPomodoro.isRunning(this)) {
            val tick = object : Runnable {
                override fun run() {
                    if (pill == null) return
                    if (LeauPomodoro.isRunning(this@LeauOverlayService)) {
                        pomodoroLabel?.text = "${LeauPomodoro.label(this@LeauOverlayService)}  ${LeauPomodoro.formatRemaining(LeauPomodoro.endAt(this@LeauOverlayService))}"
                        handler.postDelayed(this, 1000L)
                    } else {
                        pomodoroLabel?.text = "✓ Focus complete"
                        pomodoroRunnable = null
                    }
                }
            }
            pomodoroRunnable = tick
            handler.post(tick)
        } else {
            pomodoroLabel?.text = "LEAU"
        }
    }

    private fun baseParams(width: Int, height: Int, focusable: Boolean): WindowManager.LayoutParams = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, if (focusable) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT)
    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }
    private fun dp(value: Int): Int = (value.toFloat() * resources.displayMetrics.density).roundToInt()
    private fun removeBubble() { bubble?.let { runCatching { windowManager.removeView(it) } }; bubble = null; bubbleParams = null }
    private fun removePill() { pill?.let { runCatching { windowManager.removeView(it) } }; pill = null; pillParams = null; pomodoroLabel = null; pomodoroRunnable?.let(handler::removeCallbacks); pomodoroRunnable = null }
    private fun hideAll() { removePill(); removeBubble() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removePill(); removeBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
