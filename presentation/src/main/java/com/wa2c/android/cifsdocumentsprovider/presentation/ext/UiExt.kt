package com.wa2c.android.cifsdocumentsprovider.presentation.ext

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendDataState
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Main Coroutine Scope
 */
class MainCoroutineScope: CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}


/**
 * Collect flow
 */
fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    observer: (T) -> Unit = { },
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            this@collectIn.collect { observer(it) }
        }
    }
}

/**
 * StateFlow map
 */
fun <T, K> StateFlow<T>.mapState(
    viewModelScope: CoroutineScope,
    transform: (data: T) -> K
): StateFlow<K> {
    return map {
        transform(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, transform(this.value))
}

/**
 * Summary Text
 * ex. 10% [10MB/100MB] 1MB/s (1:00)
 */
fun SendData.getSummaryText(context: Context): String {
    return when (state) {
        SendDataState.PROGRESS -> {
            val sendSize = " (${Formatter.formatShortFileSize(context, progressSize)}/${Formatter.formatShortFileSize(context, size)})"
            val sendSpeed = "${Formatter.formatShortFileSize(context, bps)}/s (${DateUtils.formatElapsedTime(elapsedTime / 1000)})"
            "$progress% $sendSize $sendSpeed"
        }
        else -> {
            context.getString(state.labelRes)
        }
    }
}

fun Context.createAuthenticatePendingIntent(id: String): PendingIntent {
    return PendingIntent.getActivity(
        this,
        100,
        Intent(this, MainActivity::class.java).also {
             it.putExtra("STORAGE_ID", id)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

fun Intent.getStorageId(): String? {
    return getStringExtra("STORAGE_ID")
}
