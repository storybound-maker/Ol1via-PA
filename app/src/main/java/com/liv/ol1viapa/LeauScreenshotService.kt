package com.liv.ol1viapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class LeauScreenshotService : Service() {
    companion object {
        const val ACTION_CAPTURE = "com.liv.ol1viapa.CAPTURE_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL_ID = "leau_screenshot"
        private const val NOTIFICATION_ID = 4211
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: android.hardware.display.VirtualDisplay? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "LEAU Screenshot", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CAPTURE) capture(intent)
        return START_NOT_STICKY
    }

    private fun capture(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            finish(false)
            return
        }

        startForegroundNotification()

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = runCatching { manager.getMediaProjection(resultCode, data) }.getOrNull()
            ?: run {
                finish(false)
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
                cleanup()
                stopSelf()
            }
        }, null)

        display = mediaProjection.createVirtualDisplay(
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
            val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes.firstOrNull() ?: run {
                    finish(false)
                    return@setOnImageAvailableListener
                }
                val width = image.width
                val height = image.height
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val bitmapWidth = width + rowPadding / plane.pixelStride
                val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                bitmap.recycle()

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Leau_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Leau")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                val saved = uri != null && runCatching {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        cropped.compress(Bitmap.CompressFormat.PNG, 100, output)
                    } ?: false
                }.getOrDefault(false)

                if (saved && uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.update(uri, ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }, null, null)
                } else if (!saved && uri != null) {
                    contentResolver.delete(uri, null, null)
                }

                cropped.recycle()
                finish(saved)
            } catch (_: Exception) {
                finish(false)
            } finally {
                image.close()
            }
        }, null)
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LEAU is capturing")
            .setContentText("Taking screenshot…")
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

    private fun finish(saved: Boolean) {
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        reader?.close()
        reader = null
        display?.release()
        display = null
        projection?.stop()
        projection = null
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
