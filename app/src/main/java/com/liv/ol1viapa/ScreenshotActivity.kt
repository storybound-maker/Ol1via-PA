package com.liv.ol1viapa

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast

class ScreenshotActivity : Activity() {
    companion object { private const val REQUEST_CAPTURE = 7001 }

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Android activity result API is not used here because this is a one-shot system capture flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            finish()
            return
        }

        val projection = projectionManager.getMediaProjection(resultCode, data)
        if (projection == null) {
            Toast.makeText(this, "Screenshot permission was not granted.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        captureScreen(projection)
    }

    private fun captureScreen(projection: MediaProjection) {
        val metrics = resources.displayMetrics
        val reader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        var display: android.hardware.display.VirtualDisplay? = null
        var finished = false

        fun cleanup() {
            reader.close()
            display?.release()
            projection.stop()
        }

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                if (!finished) {
                    finished = true
                    reader.close()
                    display?.release()
                    finish()
                }
            }
        }, null)

        display = projection.createVirtualDisplay(
            "LeauScreenshot",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            0,
            reader.surface,
            null,
            null
        )

        reader.setOnImageAvailableListener({ imageReader ->
            if (finished) return@setOnImageAvailableListener

            val image = runCatching { imageReader.acquireLatestImage() }.getOrNull()
                ?: return@setOnImageAvailableListener

            try {
                val plane = image.planes.firstOrNull() ?: return@setOnImageAvailableListener
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
                cleanup()

                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (saved) "Screenshot saved" else "Couldn't save screenshot",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (_: Exception) {
                finished = true
                cleanup()
                runOnUiThread {
                    Toast.makeText(this, "Couldn't capture screenshot", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } finally {
                image.close()
            }
        }, null)
    }
}
