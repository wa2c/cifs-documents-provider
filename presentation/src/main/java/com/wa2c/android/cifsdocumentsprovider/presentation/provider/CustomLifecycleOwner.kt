package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Custom lifecycle owner
 */
class CustomLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val handler = Handler(Looper.getMainLooper())
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    fun start() {
        handler.postAtFrontOfQueue {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
    }

    fun stop() {
        handler.postAtFrontOfQueue {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }
}
