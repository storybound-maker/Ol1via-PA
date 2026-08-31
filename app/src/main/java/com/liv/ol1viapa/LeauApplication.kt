package com.liv.ol1viapa

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.content.ContextCompat

class LeauApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, LeauOverlayService::class.java))
        }.onFailure {
            runCatching { startService(Intent(this, LeauOverlayService::class.java)) }
        }
    }

    private fun sendOverlayCommand(action: String) {
        runCatching {
            startService(Intent(this, LeauOverlayService::class.java).setAction(action))
        }
    }
}
