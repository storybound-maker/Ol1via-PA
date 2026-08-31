package com.liv.ol1viapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class LeauScreenshotService : Service() {
    companion object {
        const val ACTION_CAPTURE = "com.liv.ol1viapa.action.CAPTURE_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "com.liv.ol1viapa.extra.RESULT_CODE"
        const val EXTRA_DATA = "com.liv.ol1viapa.extra.DATA"

        private const val CHANNEL_ID = "leau_screenshot"
        private const val NOTIFICATION_ID = 4202
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var finished = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CAPTURE) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = intent.intentExtra(EXTRA_DATA) ?: run {
            finishWithMessage("Screenshot permission data was missing.", startId)
            return START_NOT_STICKY
        }

        startForegroundCompat()
        startCapture(resultCode, data, startId)
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent, startId: Int) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = runCatching { manager.getMediaProjection(resultCode, data) }.getOrNull()

        if (mediaProjection == null) {
            finishWithMessage("Screenshot permission was not granted.", startId)
            return
        }

        projection = mediaProjection

        val metrics = resources.displayMetrics
        val imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )
        reader = imageReader

        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                if (!finished) {
                    finished = true
                    releaseCapture()
                    stopSelf(startId)
                }
            }
        }, null)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "LeauScreenshot",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            0,
            imageReader.surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ source ->
            if (finished) return@setOnImageAvailableListener

            val image = runCatching { source.acquireLatestImage() }.getOrNull()
                ?: return@setOnImageAvailableListener

            try {
                val plane = image.planes.firstOrNull()
                    ?: throw IllegalStateException("Screenshot image has no plane")

                val width = image.width
                val height = image.height
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val bitmapWidth = width + rowPadding / plane.pixelStride

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(plane.buffer)

                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                bitmap.recycle()

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Leau_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Leau")
                }

                val saved = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { output ->
                        cropped.compress(Bitmap.CompressFormat.PNG, 100, output)
                    } ?: false
                } ?: false

                cropped.recycle()
                finished = true
                releaseCapture()
                stopForegroundCompat()
                stopSelf(startId)

                Toast.makeText(
                    applicationContext,
                    if (saved) "Screenshot saved" else "Couldn't save screenshot",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                finished = true
                releaseCapture()
                stopForegroundCompat()
                stopSelf(startId)
                Toast.makeText(applicationContext, "Couldn't capture screenshot", Toast.LENGTH_SHORT).show()
            } finally {
                image.close()
            }
        }, null)
    }

    private fun releaseCapture() {
        reader?.close()
        reader = null
        virtualDisplay?.release()
        virtualDisplay = null
        projection?.stop()
        projection = null
    }

    private fun finishWithMessage(message: String, startId: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        stopForegroundCompat()
        stopSelf(startId)
    }

    private fun startForegroundCompat() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            4202,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Leau")
            .setContentText("Capturing your screen…")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Leau screenshots",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.intentExtra(key: String): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, Intent::class.java)
        } else {
            getParcelableExtra(key)
        }

    override fun onDestroy() {
        if (!finished) {
            finished = true
            releaseCapture()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
