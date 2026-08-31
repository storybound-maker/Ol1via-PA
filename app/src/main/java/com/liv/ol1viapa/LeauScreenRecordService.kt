package com.liv.ol1viapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File

class LeauScreenRecordService : Service() {
    companion object {
        const val ACTION_START = "com.liv.ol1viapa.START_SCREEN_RECORDING"
        const val ACTION_STOP = "com.liv.ol1viapa.STOP_SCREEN_RECORDING"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL_ID = "leau_screen_recording"
        private const val NOTIFICATION_ID = 4210
        private const val PREFS = "leau_screen_recording"
        private const val KEY_RUNNING = "running"
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "LEAU Screen Recording", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> stopRecording(true)
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        if (recorder != null) return

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DATA)
        } ?: run {
            notifyOverlay(false, "Screen recording failed")
            stopSelf()
            return
        }

        if (resultCode != android.app.Activity.RESULT_OK) {
            notifyOverlay(false, "Screen recording cancelled")
            stopSelf()
            return
        }

        // Android 14+ requires the mediaProjection foreground service to be
        // promoted before creating the MediaProjection/VirtualDisplay.
        startForegroundNotification()

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = runCatching { manager.getMediaProjection(resultCode, data) }.getOrNull()
            ?: run {
                notifyOverlay(false, "Screen recording failed")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
        projection = mediaProjection

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        val file = File(getExternalFilesDir(null), "LeauScreen_${System.currentTimeMillis()}.mp4")
        outputFile = file

        try {
            recorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(8_000_000)
                setVideoFrameRate(30)
                setVideoSize(width, height)
                setOutputFile(file.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "LeauScreenRecording",
                width,
                height,
                density,
                0,
                recorder!!.surface,
                null,
                null
            )
            recorder!!.start()
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, true).apply()
            notifyOverlay(true, "Screen recording")
        } catch (_: Exception) {
            stopRecording(false)
        }
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LEAU is recording")
            .setContentText("Tap Stop in the LEAU recording bar when finished.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopRecording(save: Boolean) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply()

        val activeRecorder = recorder
        recorder = null
        runCatching { activeRecorder?.stop() }
        runCatching { activeRecorder?.reset() }
        runCatching { activeRecorder?.release() }

        virtualDisplay?.release()
        virtualDisplay = null
        projection?.stop()
        projection = null

        val file = outputFile
        outputFile = null
        val saved = save && file != null && file.exists() && file.length() > 0L && LeauMediaStore.saveVideo(this, file) != null

        notifyOverlay(false, if (saved) "Screen recording saved" else "Screen recording failed")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notifyOverlay(recording: Boolean, message: String) {
        startService(Intent(this, LeauOverlayService::class.java).apply {
            action = if (recording) LeauOverlayService.ACTION_RECORDING_STARTED else LeauOverlayService.ACTION_RECORDING_STOPPED
            putExtra("message", message)
        })
    }

    override fun onDestroy() {
        if (recorder != null || projection != null) stopRecording(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
