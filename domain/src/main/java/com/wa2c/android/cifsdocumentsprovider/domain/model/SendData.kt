package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.net.Uri

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
    /** Source file URI (File URI) */
    val sourceFileUri: Uri,
    /** Target file URI (File URI) */
    val targetFileUri: Uri,
    /** Send start time. */
    val startTime: Long = 0,
    /** Send progress size. */
    val progressSize: Long = 0,
    /** True if success */
    val state: SendDataState = SendDataState.READY,
) {
    /** Progress percentage */
    val progress: Int
        get() = if (progressSize > 0) (progressSize * 100 / size).toInt() else 0

    /** Elapsed Time */
    val elapsedTime: Long
        get() = (System.currentTimeMillis() - startTime)

    /** Speed (bps) */
    val bps: Long
        get() = (elapsedTime / 1000).let { if (it > 0) { progressSize / it } else { 0 } }

}

/**
 * Get current ready data
 */
fun List<SendData>.getCurrentReady(): SendData? {
    return firstOrNull { it.state.isReady }
}
