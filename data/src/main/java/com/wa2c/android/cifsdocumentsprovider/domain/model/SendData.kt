package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState

/**
 * Send Data
 */
data class SendData(
    /** Unique ID */
    val id: String,
    /** File name */
    val name: String,
    /** File size */
    val size: Long,
    /** File mime type */
    val mimeType: String,
    /** Source URI (File URI) */
    val sourceUri: Uri,
    /** Target URI (File or Directory URI) */
    val targetUri: Uri,
) {
    /** Send start time. */
    var startTime: Long = 0
    /** Send progress size. */
    var progressSize: Long = 0
    /** True if success */
    var state: SendDataState = SendDataState.READY

    /** Progress percentage */
    val progress: Int
        get() = if (progressSize > 0) (progressSize * 100 / size).toInt() else 0

    /** Elapsed Time */
    val elapsedTime: Long
        get() = (System.currentTimeMillis() - startTime)

    /** Speed (bps) */
    val bps: Long
        get() = (elapsedTime / 1000).let { if (it > 0) { progressSize / it } else { 0 } }

    /**
     * Cancel
     */
    fun cancel() {
        state = SendDataState.CANCEL
    }
}
