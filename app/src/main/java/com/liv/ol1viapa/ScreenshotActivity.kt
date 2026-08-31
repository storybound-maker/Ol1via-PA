package com.liv.ol1viapa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class ScreenshotActivity : Activity() {
    companion object { private const val REQUEST_CAPTURE = 7001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("One-shot system screen-capture consent flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            finish()
            return
        }

        val serviceIntent = Intent(this, LeauScreenshotService::class.java).apply {
            action = LeauScreenshotService.ACTION_CAPTURE
            putExtra(LeauScreenshotService.EXTRA_RESULT_CODE, resultCode)
            putExtra(LeauScreenshotService.EXTRA_DATA, data)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            finish()
        } catch (_: Exception) {
            Toast.makeText(this, "Screenshot capture could not start", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
