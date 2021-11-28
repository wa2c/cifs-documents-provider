package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Send data state.
 */
enum class SendDataState {
    /** In ready */
    READY,
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

    val isCompleted: Boolean
        get() = this == SUCCESS || this == FAILURE || this == CANCEL

}