package com.liv.ol1viapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import android.content.Context
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class LeauOverlayService : Service() {
    companion object {
        const val ACTION_SHOW = "com.liv.ol1viapa.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.liv.ol1viapa.HIDE_OVERLAY"
        private const val CHANNEL_ID = "leau_overlay"
        private const val NOTIFICATION_ID = 4201
    }

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var chatPanel: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var chatParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Leau is ready")
            .setContentText("Leau can float above your apps.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        handler.postDelayed({
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermissionOnce()
            }
        }, 700)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showBubble()
            ACTION_HIDE -> hideOverlay()
        }
        return START_STICKY
    }

    private fun requestOverlayPermissionOnce() {
        val prefs = getSharedPreferences("leau_overlay", MODE_PRIVATE)
        if (prefs.getBoolean("permission_prompted", false)) return
        prefs.edit().putBoolean("permission_prompted", true).apply()
        runCatching {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun baseParams(width: Int, height: Int, focusable: Boolean): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            overlayType(),
            if (focusable) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) return
        removeChat()
        if (bubble != null) return

        val size = dp(68)
        val view = ImageView(this).apply {
            setImageResource(R.drawable.leau_eyes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(0xFFB8FF5A.toInt(), 34f)
            elevation = dp(10).toFloat()
            contentDescription = "Open Leau"
        }
        val params = baseParams(size, size, false).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(180)
        }
        installDragAndClick(view, params)
        bubble = view
        bubbleParams = params
        runCatching { windowManager.addView(view, params) }
    }

    private fun installDragAndClick(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6)) moved = true
                    params.x = (startX + dx).coerceAtLeast(0)
                    params.y = (startY + dy).coerceAtLeast(0)
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) showChat()
                    true
                }
                else -> true
            }
        }
    }

    private fun showChat() {
        if (!Settings.canDrawOverlays(this)) return
        removeBubble()
        if (chatPanel != null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground(0xFF0C1714.toInt(), 28f).apply { setStroke(dp(1), 0xFF69F0C4.toInt()) }
            elevation = dp(18).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val eyes = ImageView(this).apply {
            setImageResource(R.drawable.leau_eyes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        header.addView(eyes, LinearLayout.LayoutParams(dp(54), dp(38)))
        val title = TextView(this).apply {
            text = "Leau"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(6), 0, 0, 0)
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
        val close = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            background = roundedBackground(0xFF162722.toInt(), 18f)
            setOnClickListener { showBubble() }
            contentDescription = "Minimize Leau"
        }
        header.addView(close, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        val scroll = ScrollView(this).apply {
            fillViewport = true
            setPadding(0, dp(8), 0, dp(8))
        }
        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = "Ask Leau..."
            hintTextColor = 0xFF89A89D.toInt()
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(12), 0)
            background = roundedBackground(0xFF14251F.toInt(), 22f)
        }
        val send = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_send)
            setColorFilter(0xFFB8FF5A.toInt())
            background = roundedBackground(0xFF14251F.toInt(), 22f)
            contentDescription = "Send"
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(8) })
        inputRow.addView(send, LinearLayout.LayoutParams(dp(52), dp(52)))
        root.addView(inputRow)

        val params = baseParams(dp(340), dp(500), true).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(14)
            y = dp(18)
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }
        chatPanel = root
        chatParams = params
        runCatching { windowManager.addView(root, params) }

        send.setOnClickListener { sendOverlayMessage(input, messages, scroll) }
        input.setOnEditorActionListener { _, _, _ -> sendOverlayMessage(input, messages, scroll); true }
        input.requestFocus()
        handler.postDelayed({
            runCatching { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
        }, 180)
    }

    private fun sendOverlayMessage(input: EditText, messages: LinearLayout, scroll: ScrollView) {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        addMessage(messages, text, false)
        addMessage(messages, "Leau is thinking…", true)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        val history = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(this)?.let { history.add(it) }
        LeauApi.sendMessage(text, history) { result ->
            if (chatPanel == null) return@sendMessage
            val last = messages.getChildAt(messages.childCount - 1)
            if (last is TextView && last.text.toString() == "Leau is thinking…") messages.removeView(last)
            result.onSuccess { reply -> addMessage(messages, reply, true) }
                .onFailure { error -> addMessage(messages, "I couldn't reach my AI brain. ${error.message ?: "Try again."}", true) }
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun addMessage(container: LinearLayout, text: String, fromLeau: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(if (fromLeau) 0xFFE9FFF5.toInt() else 0xFF07120E.toInt())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = roundedBackground(if (fromLeau) 0xFF17332A.toInt() else 0xFFB8FF5A.toInt(), 18f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (fromLeau) Gravity.START else Gravity.END
        }
        row.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply {
            width = dp(270).coerceAtMost(dp(270))
            setMargins(0, dp(4), 0, dp(4))
        })
        container.addView(row)
    }

    private fun hideOverlay() {
        removeChat()
        removeBubble()
    }

    private fun removeBubble() {
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        bubbleParams = null
    }

    private fun removeChat() {
        chatPanel?.let { runCatching { windowManager.removeView(it) } }
        chatPanel = null
        chatParams = null
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius.toInt()).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeChat()
        removeBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
