package com.wa2c.android.cifsdocumentsprovider.domain.model

/**
 * Send data state.
 */
enum class SendDataState {
    /** In ready */
    READY,
    /** In confirmation */
    CONFIRM,
    /** In overwriting */
    OVERWRITE,
    /** In progress */
    PROGRESS,
    /** Succeeded */
    SUCCESS,
    /** Failed */
    FAILURE,
    /** Canceled */
    CANCEL,
    ;

    val isReady: Boolean
        get() = this == READY

    val inProgress: Boolean
        get() = this == PROGRESS

    val isCancelable: Boolean
        get() = this == READY || this == PROGRESS

    val isRetryable: Boolean
        get() = this == OVERWRITE || this == FAILURE || this == CANCEL

    val isFinished: Boolean
        get() = this == SUCCESS || this == FAILURE || this == CANCEL

    val isIncomplete: Boolean
        get() = this == FAILURE || this == CANCEL

}
