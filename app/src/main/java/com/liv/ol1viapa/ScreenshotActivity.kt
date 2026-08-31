package com.liv.ol1viapa

import android.app.Activity
import android.content.ContentValues
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.PixelFormat

class ScreenshotActivity : Activity() {
    companion object { private const val REQUEST_CAPTURE = 7001 }
    private var projectionManager: MediaProjectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager!!.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Android activity result API is not used here because this is a one-shot system capture flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            finish(); return
        }
        val projection = projectionManager!!.getMediaProjection(resultCode, data)
        val metrics = resources.displayMetrics
        val reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        val display = projection.createVirtualDisplay("LeauScreenshot", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            0, reader.surface, null, null)
        reader.setOnImageAvailableListener({ r ->
            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val width = image.width
                val height = image.height
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val bitmap = Bitmap.createBitmap(width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Leau_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Leau")
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                bitmap.recycle()
                cropped.recycle()
                runOnUiThread { Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show(); finish() }
            } finally {
                image.close()
                reader.close()
                display.release()
                projection.stop()
            }
        }, null)
    }
}
