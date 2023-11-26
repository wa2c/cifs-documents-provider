package com.wa2c.android.cifsdocumentsprovider.presentation.worker

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Worker lifecycle owner
 */
class WorkerLifecycleOwner : LifecycleOwner {
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
