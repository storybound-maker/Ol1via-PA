package com.liv.ol1viapa

import android.app.Application
import android.content.Intent
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
