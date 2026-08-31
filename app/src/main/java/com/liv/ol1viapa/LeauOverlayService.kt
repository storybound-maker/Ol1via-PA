package com.l1vo.ol1via.pa

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class LeauOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var root: FrameLayout? = null
    private var timerText: TextView? = null
    private var timer: CountDownTimer? = null
    private var x = 24
    private var y = 180
    private var expanded = false

    companion object {
        const val ACTION_START_TIMER = "com.l1vo.ol1via.pa.START_TIMER"
        const val EXTRA_SECONDS = "seconds"
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_TIMER) startTimer(intent.getIntExtra(EXTRA_SECONDS, 60))
        return START_STICKY
    }

    private fun showBubble() {
        removeOverlay(); expanded = false
        val r = FrameLayout(this); root = r
        val b = ImageView(this).apply {
            setImageResource(R.drawable.leau_eyes); setPadding(14,14,14,14)
            background = bg(Color.rgb(19,27,28),100f); contentDescription = "LEAU"
        }
        r.addView(b, FrameLayout.LayoutParams(64,64))
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { showPanel(); return true }
            override fun onDoubleTap(e: MotionEvent): Boolean { openApp(); return true }
        })
        b.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_MOVE) move(e.rawX,e.rawY)
            detector.onTouchEvent(e); true
        }
        wm.addView(r, overlayParams(76,76,false))
    }

    private fun showPanel() {
        if (expanded) return
        expanded = true
        val r = root ?: return
        r.removeAllViews()
        val full = overlayParams(-1,-1,true)
        wm.updateViewLayout(r,full)
        r.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) { hideKeyboard(); showBubble() }
            true
        }
        val card = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(16,12,16,12)
            background=panelBg(); elevation=20f
        }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.leau_eyes); setPadding(2,2,2,2) },LinearLayout.LayoutParams(40,40))
        header.addView(TextView(this).apply { text="  LEAU"; textSize=14f; setTextColor(Color.WHITE) },LinearLayout.LayoutParams(0,40,1f))
        header.addView(TextView(this).apply { text="×"; textSize=25f; setTextColor(Color.LTGRAY); setOnClickListener{showBubble()} },LinearLayout.LayoutParams(38,40))
        card.addView(header)
        timerText?.let { card.addView(it,0,LinearLayout.LayoutParams(-1,38)) }
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val input=EditText(this).apply{
            hint="Ask LEAU anything..."; textSize=15f; setSingleLine(true); setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(160,170,170)); setPadding(16,0,16,0); background=bg(Color.rgb(45,54,55),40f)
        }
        row.addView(input,LinearLayout.LayoutParams(0,52,1f))
        row.addView(TextView(this).apply{text="➤";textSize=20f;gravity=Gravity.CENTER;setTextColor(Color.rgb(185,255,99))},LinearLayout.LayoutParams(48,52))
        card.addView(row)
        r.addView(card,FrameLayout.LayoutParams(310,-2).apply{leftMargin=x;topMargin=y})
        input.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT)
    }

    private fun startTimer(seconds:Int){
        if(seconds<=0)return
        timer?.cancel()
        timerText=TextView(this).apply{textSize=18f;setTextColor(Color.rgb(185,255,99));setPadding(4,0,4,0)}
        timer=object:CountDownTimer(seconds*1000L,1000L){
            override fun onTick(ms:Long){timerText?.text="⏱  ${fmt(ms)}  •  Timer running"}
            override fun onFinish(){timerText?.text="✓  Timer finished"}
        }.start()
        if(!expanded)showPanel()
    }

    private fun fmt(ms:Long):String{val s=ms/1000;return String.format("%02d:%02d",s/60,s%60)}

    private fun move(rawX:Float,rawY:Float){
        x=(rawX-38).toInt().coerceAtLeast(0);y=(rawY-38).toInt().coerceAtLeast(0)
        root?.let{v->(v.layoutParams as WindowManager.LayoutParams).also{p->p.x=x;p.y=y;wm.updateViewLayout(v,p)}}
    }

    private fun overlayParams(w:Int,h:Int,focus:Boolean)=WindowManager.LayoutParams(
        w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        if(focus) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply{gravity=Gravity.TOP or Gravity.START;x=this@LeauOverlayService.x;y=this@LeauOverlayService.y}

    private fun openApp(){startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP));stopSelf()}
    private fun hideKeyboard(){root?.windowToken?.let{(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it,0)}}
    private fun removeOverlay(){root?.let{try{wm.removeView(it)}catch(_:Exception){}};root=null}
    private fun bg(c:Int,r:Float)=GradientDrawable().apply{setColor(c);cornerRadius=r}
    private fun panelBg()=GradientDrawable().apply{setColor(Color.rgb(18,24,25));cornerRadius=34f;setStroke(2,Color.argb(100,185,255,99))}
    override fun onDestroy(){timer?.cancel();removeOverlay();super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
