package com.wa2c.android.cifsdocumentsprovider.common.values

import androidx.annotation.StringRes
import com.wa2c.android.cifsdocumentsprovider.R

/**
 * Send data state.
 */
enum class SendDataState(
    @StringRes val labelRes: Int
) {
    /** In ready */
    READY(R.string.send_state_ready),
    /** In overwriting */
    OVERWRITE(R.string.send_state_overwrite),
    /** In progress */
    PROGRESS(R.string.send_state_cancel),
    /** Succeeded */
    SUCCESS(R.string.send_state_success),
    /** Failed */
    FAILURE(R.string.send_state_failure),
    /** Canceled */
    CANCEL(R.string.send_state_cancel),
    ;

    val isReady: Boolean
        get() = this == READY

    val isOverwrite: Boolean
        get() = this == OVERWRITE

    val inProgress: Boolean
        get() = this == PROGRESS

    val isCompleted: Boolean
        get() = this == SUCCESS || this == FAILURE || this == CANCEL

    val isRetryable: Boolean
        get() = this == OVERWRITE || this == FAILURE || this == CANCEL

}