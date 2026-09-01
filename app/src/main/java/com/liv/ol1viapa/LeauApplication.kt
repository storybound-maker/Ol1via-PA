package com.liv.ol1viapa

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class LeauApplication : Application() {
    companion object {
        lateinit var instance: LeauApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                startLeauOverlayService()
                sendOverlayCommand(LeauOverlayService.ACTION_HIDE)
            }

            override fun onStop(owner: LifecycleOwner) {
                sendOverlayCommand(LeauOverlayService.ACTION_SHOW)
            }
        })
    }

    private fun startLeauOverlayService() {
        // Do not start the microphone foreground service until RECORD_AUDIO
        // has been granted. Android 14+ rejects microphone FGS startup
        // without the runtime microphone permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, LeauOverlayService::class.java))
        }.onFailure {
            runCatching { startService(Intent(this, LeauOverlayService::class.java)) }
        }
    }

    private fun sendOverlayCommand(action: String) {
        // Avoid implicitly starting the microphone foreground service before
        // its runtime permission is available.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        runCatching {
            startService(Intent(this, LeauOverlayService::class.java).setAction(action))
        }
    }
}
