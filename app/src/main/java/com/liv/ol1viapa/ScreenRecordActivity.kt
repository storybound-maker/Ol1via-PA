package com.liv.ol1viapa

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ScreenRecordActivity : Activity() {
    companion object {
        const val ACTION_START = "com.liv.ol1viapa.START_SCREEN_RECORD"
        private const val REQUEST_CAPTURE = 7101
    }

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("One-shot system screen-capture consent flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            finish()
            return
        }

        startService(Intent(this, LeauScreenRecordService::class.java).apply {
            action = LeauScreenRecordService.ACTION_START
            putExtra(LeauScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(LeauScreenRecordService.EXTRA_DATA, data)
        })
        moveTaskToBack(true)
        finish()
    }
}
